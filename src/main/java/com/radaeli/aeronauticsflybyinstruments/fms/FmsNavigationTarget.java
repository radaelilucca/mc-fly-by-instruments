package com.radaeli.aeronauticsflybyinstruments.fms;

import com.radaeli.aeronauticsflybyinstruments.item.FmsNavigatorItem;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Aeronautics Navigation Table target: the active waypoint of the bound FMS module. The table
 * re-resolves it every server tick, so a waypoint change reaches the table on the next tick.
 * Range rule: {@link FmsNavigatorRange}.
 */
public final class FmsNavigationTarget implements NavigationTarget {
    @Override
    public boolean equals(Object other) {
        return other instanceof FmsNavigationTarget;
    }

    @Override
    public int hashCode() {
        return FmsNavigationTarget.class.hashCode();
    }

    @Override
    public Vec3 getTarget(NavTableBlockEntity navigationTable, ItemStack navigator) {
        // NavTableBlockEntity implements Ponder's VirtualBlockEntity API; resolving the host
        // level reflectively avoids making that optional compile-time API a hard dependency.
        final net.minecraft.world.level.Level level;
        try {
            Object value = ((Object) navigationTable).getClass().getMethod("getLevel").invoke(navigationTable);
            level = value instanceof net.minecraft.world.level.Level found ? found : null;
        } catch (ReflectiveOperationException failure) {
            return null;
        }
        if (level == null || level.isClientSide) return null;
        var moduleId = FmsNavigatorItem.boundFmsId(navigator);
        if (moduleId == null) return null;
        var table = FmsModuleIndex.find(level, moduleId);
        if (table == null) return null;
        var moduleSubLevel = FmsLocation.subLevel(table);
        boolean sameContraption = moduleSubLevel != null && moduleSubLevel == navigationTable.subLevel;
        double distanceSquared = FmsLocation.forBlockEntity(table).distanceToSqr(navigationTable.getProjectedSelfPos());
        if (!FmsNavigatorRange.reachable(sameContraption, distanceSquared)) return null;
        var data = table.findFmsById(moduleId).map(found -> found.data()).orElse(null);
        if (data == null) return null;
        var waypoint = data.selectedWaypoint().orElse(null);
        if (waypoint == null || !waypoint.dimension().equals(level.dimension().location().toString())) return null;
        return new Vec3(waypoint.x(), waypoint.y(), waypoint.z());
    }
}
