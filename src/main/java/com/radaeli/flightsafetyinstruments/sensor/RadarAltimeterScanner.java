package com.radaeli.flightsafetyinstruments.sensor;

import com.radaeli.flightsafetyinstruments.avionics.SensorReading;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Server-side multi-cast sampler that follows the local lower face through Sable poses. */
public final class RadarAltimeterScanner {
    private static final double CHUNK_EDGE_EPSILON = 1.0E-5;

    private RadarAltimeterScanner() {}

    public static SensorReading<Double> scan(Level level, RadarAltimeterBlockEntity sensor, int maxRange) {
        if (maxRange < 1) throw new IllegalArgumentException("maxRange must be positive");
        BlockPos localPos = sensor.getBlockPos();
        Vec3 localOrigin = new Vec3(localPos.getX() + 0.5, localPos.getY(), localPos.getZ() + 0.5);
        SubLevelAccess containing = Sable.HELPER.getContaining(sensor);
        SubLevel host = containing instanceof SubLevel subLevel ? subLevel : null;
        Vec3 worldOrigin = host == null ? localOrigin : host.logicalPose().transformPosition(localOrigin);

        List<Double> distances = new ArrayList<>(RadarAltimeterScan.CAST_COUNT);
        for (RadarAltimeterScan.Direction direction : RadarAltimeterScan.directions()) {
            Vec3 localDirection = new Vec3(direction.x(), direction.y(), direction.z());
            Vec3 worldDirection = host == null ? localDirection : host.logicalPose().transformNormal(localDirection);
            worldDirection = worldDirection.normalize();
            CastResult result = castLoadedChunksOnly(level, worldOrigin, worldDirection, maxRange, host);
            if (result.hit()) distances.add(result.distance());
        }
        return RadarAltimeterScan.aggregate(distances, level.getGameTime());
    }

    private static CastResult castLoadedChunksOnly(Level level, Vec3 origin, Vec3 direction,
                                                    double maxRange, SubLevel ignoredSubLevel) {
        double traveled = 0.0;
        while (traveled < maxRange) {
            double probeDistance = Math.min(maxRange, traveled + CHUNK_EDGE_EPSILON * 2.0);
            BlockPos currentBlock = BlockPos.containing(origin.add(direction.scale(probeDistance)));
            if (!level.hasChunkAt(currentBlock)) return CastResult.unavailable();

            double boundaryDistance = nextHorizontalChunkBoundary(origin, direction, currentBlock, traveled);
            boolean reachesRangeLimit = !Double.isFinite(boundaryDistance)
                    || boundaryDistance > maxRange + CHUNK_EDGE_EPSILON;
            double endDistance;
            boolean nextChunkLoaded = true;
            if (reachesRangeLimit) {
                endDistance = maxRange;
            } else {
                Vec3 nextChunkProbe = origin.add(direction.scale(boundaryDistance + CHUNK_EDGE_EPSILON * 2.0));
                nextChunkLoaded = level.hasChunkAt(BlockPos.containing(nextChunkProbe));
                endDistance = nextChunkLoaded
                        ? Math.min(maxRange, boundaryDistance + CHUNK_EDGE_EPSILON * 2.0)
                        : boundaryDistance - CHUNK_EDGE_EPSILON;
            }

            if (endDistance <= traveled) return CastResult.unavailable();
            Vec3 from = origin.add(direction.scale(traveled));
            Vec3 to = origin.add(direction.scale(endDistance));
            ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    net.minecraft.world.phys.shapes.CollisionContext.empty());
            if (context instanceof ClipContextExtension extension) {
                extension.sable$setIgnoredSubLevel(ignoredSubLevel);
            }
            BlockHitResult hit = level.clip(context);
            if (hit.getType() == HitResult.Type.BLOCK) {
                Vec3 hitPosition = globalHitPosition(level, hit);
                double distance = origin.distanceTo(hitPosition);
                if (Double.isFinite(distance) && distance >= 0.0 && distance <= maxRange + 1.0E-3) {
                    return CastResult.hit(distance);
                }
            }

            if (!nextChunkLoaded) return CastResult.unavailable();
            if (reachesRangeLimit) return CastResult.noHit();
            traveled = endDistance;
        }
        return CastResult.noHit();
    }

    private static Vec3 globalHitPosition(Level level, BlockHitResult hit) {
        SubLevelAccess hitSubLevel = Sable.HELPER.getContaining(level, hit.getBlockPos());
        return hitSubLevel == null ? hit.getLocation() : hitSubLevel.logicalPose().transformPosition(hit.getLocation());
    }

    private static double nextHorizontalChunkBoundary(Vec3 origin, Vec3 direction, BlockPos currentBlock,
                                                       double afterDistance) {
        double nextX = Double.POSITIVE_INFINITY;
        double nextZ = Double.POSITIVE_INFINITY;
        if (direction.x > 1.0E-10) {
            nextX = (((currentBlock.getX() >> 4) + 1) * 16.0 - origin.x) / direction.x;
        } else if (direction.x < -1.0E-10) {
            nextX = ((currentBlock.getX() >> 4) * 16.0 - origin.x) / direction.x;
        }
        if (direction.z > 1.0E-10) {
            nextZ = (((currentBlock.getZ() >> 4) + 1) * 16.0 - origin.z) / direction.z;
        } else if (direction.z < -1.0E-10) {
            nextZ = ((currentBlock.getZ() >> 4) * 16.0 - origin.z) / direction.z;
        }
        double next = Math.min(nextX, nextZ);
        while (next <= afterDistance + CHUNK_EDGE_EPSILON) {
            if (nextX <= nextZ && Double.isFinite(nextX)) {
                nextX += 16.0 / Math.abs(direction.x);
            } else if (Double.isFinite(nextZ)) {
                nextZ += 16.0 / Math.abs(direction.z);
            } else {
                return Double.POSITIVE_INFINITY;
            }
            next = Math.min(nextX, nextZ);
        }
        return next;
    }

    private record CastResult(boolean hit, double distance) {
        static CastResult hit(double distance) { return new CastResult(true, distance); }
        static CastResult noHit() { return new CastResult(false, Double.NaN); }
        static CastResult unavailable() { return new CastResult(false, Double.NaN); }
    }
}
