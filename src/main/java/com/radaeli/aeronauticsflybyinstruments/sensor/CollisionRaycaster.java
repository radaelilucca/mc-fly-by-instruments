package com.radaeli.aeronauticsflybyinstruments.sensor;

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

/** Bounded block-collider raycast that stops safely at unloaded chunk boundaries. */
public final class CollisionRaycaster {
    private static final double CHUNK_EDGE_EPSILON = 1.0E-5;

    private CollisionRaycaster() {}

    public static Result cast(Level level, Vec3 origin, Vec3 direction, double maxRange, SubLevel ignoredSubLevel) {
        if (!(maxRange > 0.0) || !Double.isFinite(maxRange)) {
            throw new IllegalArgumentException("maxRange must be finite and positive");
        }
        Vec3 unitDirection = direction.normalize();
        if (unitDirection.lengthSqr() < 0.99) throw new IllegalArgumentException("direction must be non-zero");

        double traveled = 0.0;
        while (traveled < maxRange) {
            double probeDistance = Math.min(maxRange, traveled + CHUNK_EDGE_EPSILON * 2.0);
            BlockPos currentBlock = BlockPos.containing(origin.add(unitDirection.scale(probeDistance)));
            if (!level.hasChunkAt(currentBlock)) return Result.incomplete(probeDistance);

            double boundaryDistance = nextHorizontalChunkBoundary(origin, unitDirection, currentBlock, traveled);
            boolean reachesRangeLimit = !Double.isFinite(boundaryDistance)
                    || boundaryDistance > maxRange + CHUNK_EDGE_EPSILON;
            double endDistance;
            boolean nextChunkLoaded = true;
            if (reachesRangeLimit) {
                endDistance = maxRange;
            } else {
                Vec3 nextChunkProbe = origin.add(unitDirection.scale(boundaryDistance + CHUNK_EDGE_EPSILON * 2.0));
                nextChunkLoaded = level.hasChunkAt(BlockPos.containing(nextChunkProbe));
                endDistance = nextChunkLoaded
                        ? Math.min(maxRange, boundaryDistance + CHUNK_EDGE_EPSILON * 2.0)
                        : boundaryDistance - CHUNK_EDGE_EPSILON;
            }

            if (endDistance <= traveled) return Result.incomplete(traveled);
            Vec3 from = origin.add(unitDirection.scale(traveled));
            Vec3 to = origin.add(unitDirection.scale(endDistance));
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
                    return Result.hit(distance);
                }
            }

            if (!nextChunkLoaded) return Result.incomplete(endDistance);
            if (reachesRangeLimit) return Result.clear();
            traveled = endDistance;
        }
        return Result.clear();
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

    public record Result(Kind kind, double distance) {
        public static Result hit(double distance) { return new Result(Kind.HIT, distance); }
        public static Result clear() { return new Result(Kind.CLEAR, Double.NaN); }
        /** For INCOMPLETE, distance is the safely sampled distance before the unloaded boundary. */
        public static Result incomplete(double distance) { return new Result(Kind.INCOMPLETE, distance); }
        public boolean hit() { return kind == Kind.HIT; }
        public boolean completed() { return kind != Kind.INCOMPLETE; }
    }

    public enum Kind { HIT, CLEAR, INCOMPLETE }
}
