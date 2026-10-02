package com.radaeli.flightsafetyinstruments.sensor;

import com.radaeli.flightsafetyinstruments.avionics.SensorReading;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Server-side multi-cast sampler that follows the local lower face through Sable poses. */
public final class RadarAltimeterScanner {
    private RadarAltimeterScanner() {}

    public static SensorReading<Double> scan(Level level, RadarAltimeterBlockEntity sensor, int maxRange) {
        return scanInternal(level, sensor, maxRange, false).reading();
    }

    public static DebugScan scanDetailed(Level level, RadarAltimeterBlockEntity sensor, int maxRange) {
        return scanInternal(level, sensor, maxRange, true);
    }

    private static DebugScan scanInternal(Level level, RadarAltimeterBlockEntity sensor, int maxRange,
                                          boolean includeDebugRays) {
        if (maxRange < 1) throw new IllegalArgumentException("maxRange must be positive");
        BlockPos localPos = sensor.getBlockPos();
        Vec3 localOrigin = new Vec3(localPos.getX() + 0.5, localPos.getY(), localPos.getZ() + 0.5);
        var containing = Sable.HELPER.getContaining(sensor);
        SubLevel host = containing instanceof SubLevel subLevel ? subLevel : null;
        Vec3 worldOrigin = host == null ? localOrigin : host.logicalPose().transformPosition(localOrigin);

        List<Double> distances = new ArrayList<>(RadarAltimeterScan.CAST_COUNT);
        List<TerrainScanDebugRay> debugRays = includeDebugRays
                ? new ArrayList<>(RadarAltimeterScan.CAST_COUNT) : null;
        int rayIndex = 0;
        for (RadarAltimeterScan.Direction direction : RadarAltimeterScan.directions()) {
            rayIndex++;
            Vec3 localDirection = new Vec3(direction.x(), direction.y(), direction.z());
            Vec3 worldDirection = host == null ? localDirection : host.logicalPose().transformNormal(localDirection);
            worldDirection = worldDirection.normalize();
            CollisionRaycaster.Result result = CollisionRaycaster.cast(level, worldOrigin, worldDirection, maxRange, host);
            if (result.hit()) distances.add(result.distance());
            if (includeDebugRays) {
                double displayLength = result.kind() == CollisionRaycaster.Kind.CLEAR ? maxRange : result.distance();
                debugRays.add(new TerrainScanDebugRay(rayIndex, worldOrigin,
                        worldOrigin.add(worldDirection.scale(displayLength)), result.kind()));
            }
        }
        SensorReading<Double> reading = RadarAltimeterScan.aggregate(distances, level.getGameTime());
        return new DebugScan(reading, includeDebugRays ? List.copyOf(debugRays) : List.of());
    }

    public record DebugScan(SensorReading<Double> reading, List<TerrainScanDebugRay> rays) {}
}
