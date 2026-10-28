package com.radaeli.aeronauticsflybyinstruments.panel;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class PanelCoordinates {
    private PanelCoordinates() {}

    /** Table thickness: modules rest on it. 4 px, so a wire connector's body fits the table edge. */
    public static final double SURFACE = 4.0 / 16;
    /** Top of the module selection volume: table surface plus the tallest module (4 px). */
    public static final double MODULE_TOP = SURFACE + 4.0 / 16;

    /** Translation for a raw model centered at X/Z=8 pixels, with its mounting base at Y=0. */
    public static Vec3 modelOffset(PanelGrid.Module module) {
        return new Vec3((module.column() + module.width() / 2.0) / PanelGrid.SIZE - 0.5,
                SURFACE, (module.row() + module.height() / 2.0) / PanelGrid.SIZE - 0.5);
    }

    /** Convert block-local coordinates into a panel whose exposed face points up. */
    public static Vec3 toPanel(Direction facing, Vec3 point) {
        return switch (facing) {
            case UP -> point;
            case DOWN -> new Vec3(point.x, 1 - point.y, 1 - point.z);
            case NORTH -> new Vec3(point.x, 1 - point.z, point.y);
            case SOUTH -> new Vec3(1 - point.x, point.z, point.y);
            case WEST -> new Vec3(1 - point.z, 1 - point.x, point.y);
            case EAST -> new Vec3(point.z, point.x, point.y);
        };
    }

    public static Vec3 fromPanel(Direction facing, Vec3 point) {
        return switch (facing) {
            case UP -> point;
            case DOWN -> new Vec3(point.x, 1 - point.y, 1 - point.z);
            case NORTH -> new Vec3(point.x, point.z, 1 - point.y);
            case SOUTH -> new Vec3(1 - point.x, point.z, point.y);
            case WEST -> new Vec3(1 - point.y, point.z, 1 - point.x);
            case EAST -> new Vec3(point.y, point.z, point.x);
        };
    }

    public static int cell(double coordinate) {
        if (!Double.isFinite(coordinate) || coordinate < -0.000001 || coordinate > 1.000001) return -1;
        return Math.max(0, Math.min(PanelGrid.SIZE - 1, (int) Math.floor(coordinate * PanelGrid.SIZE)));
    }
}
