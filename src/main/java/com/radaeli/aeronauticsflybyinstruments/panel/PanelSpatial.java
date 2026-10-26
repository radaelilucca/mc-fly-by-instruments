package com.radaeli.aeronauticsflybyinstruments.panel;

import com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Converts a mounted module's local panel position into the shared world coordinate frame. */
public final class PanelSpatial {
    private PanelSpatial() { }

    public static Vec3 moduleWorldPosition(ModularTableBlockEntity table, PanelGrid.Module module) {
        double emitterHeight = module.type().equals("buzzer") ? 1.9 / 16.0 : 0;
        Vec3 panel = PanelCoordinates.fromPanel(table.getBlockState().getValue(ModularTableBaseBlock.FACING),
                new Vec3((module.column() + module.width() / 2.0) / PanelGrid.SIZE,
                        3.0 / 16.0 + emitterHeight, (module.row() + module.height() / 2.0) / PanelGrid.SIZE));
        return toWorld(table, Vec3.atLowerCornerOf(table.getBlockPos()).add(panel.x, panel.y, panel.z));
    }

    public static Vec3 tableWorldPosition(ModularTableBlockEntity table) {
        return toWorld(table, Vec3.atCenterOf(table.getBlockPos()));
    }

    public static Vec3 entityWorldPosition(Entity entity) {
        return FmsLocation.forEntity(entity);
    }

    private static Vec3 toWorld(ModularTableBlockEntity table, Vec3 local) {
        SubLevel subLevel = Sable.HELPER.getContaining(table);
        return subLevel == null ? local : subLevel.logicalPose().transformPosition(local);
    }
}
