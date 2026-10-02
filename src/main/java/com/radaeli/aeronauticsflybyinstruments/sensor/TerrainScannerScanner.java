package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.radaeli.aeronauticsflybyinstruments.avionics.ReadingStatus;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Server-side terrain sweep. Contacts are block-collider hits; entities are intentionally excluded. */
public final class TerrainScannerScanner {
    private TerrainScannerScanner() {}

    public static TerrainScanSnapshot scan(Level level, TerrainScannerBlockEntity scanner,
                                           int maxRange, boolean advanced, int apertureDegrees) {
        return scanInternal(level, scanner, maxRange, advanced, apertureDegrees, false).snapshot();
    }

    public static DebugScan scanDetailed(Level level, TerrainScannerBlockEntity scanner,
                                         int maxRange, boolean advanced, int apertureDegrees) {
        return scanInternal(level, scanner, maxRange, advanced, apertureDegrees, true);
    }

    private static DebugScan scanInternal(Level level, TerrainScannerBlockEntity scanner,
                                          int maxRange, boolean advanced, int apertureDegrees,
                                          boolean includeDebugRays) {
        List<TerrainScanGeometry.DirectionVector> directions = advanced
                ? TerrainScanGeometry.advancedDirections(apertureDegrees)
                : TerrainScanGeometry.simpleDirections();
        int totalRays = directions.size();

        var containing = Sable.HELPER.getContaining(scanner);
        SubLevel host = containing instanceof SubLevel subLevel ? subLevel : null;
        Direction facing = scanner.getBlockState().getValue(AbstractTerrainScannerBlock.FACING);
        Vec3 forward = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vec3 localOrigin = Vec3.atCenterOf(scanner.getBlockPos()).add(forward.scale(0.501));
        Vec3 worldOrigin = host == null ? localOrigin : host.logicalPose().transformPosition(localOrigin);

        List<TerrainScanContact> contacts = new ArrayList<>();
        List<TerrainScanDebugRay> debugRays = includeDebugRays ? new ArrayList<>(totalRays) : null;
        int sampledRays = 0;
        for (int index = 0; index < totalRays; index++) {
            var localDirection = directions.get(index);
            Vec3 levelDirection = TerrainScanGeometry.toLevelLocal(facing, localDirection);
            Vec3 worldDirection = host == null ? levelDirection : host.logicalPose().transformNormal(levelDirection);
            CollisionRaycaster.Result result = CollisionRaycaster.cast(level, worldOrigin, worldDirection,
                    maxRange, host);
            if (includeDebugRays) {
                double displayLength = result.kind() == CollisionRaycaster.Kind.CLEAR ? maxRange : result.distance();
                debugRays.add(new TerrainScanDebugRay(index + 1, worldOrigin,
                        worldOrigin.add(worldDirection.scale(displayLength)), result.kind()));
            }
            if (result.completed()) sampledRays++;
            if (result.hit()) {
                contacts.add(new TerrainScanContact(index + 1, result.distance(),
                        localDirection.x(), localDirection.y(), localDirection.z()));
            }
        }

        long gameTime = level.getGameTime();
        TerrainScanSnapshot snapshot = sampledRays == 0
                ? TerrainScanSnapshot.unavailable(totalRays, gameTime)
                : new TerrainScanSnapshot(ReadingStatus.VALID, contacts, sampledRays, totalRays, gameTime);
        return new DebugScan(snapshot, includeDebugRays ? List.copyOf(debugRays) : List.of());
    }

    public record DebugScan(TerrainScanSnapshot snapshot, List<TerrainScanDebugRay> rays) {}
}
