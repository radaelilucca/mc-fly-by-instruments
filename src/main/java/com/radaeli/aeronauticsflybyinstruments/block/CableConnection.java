package com.radaeli.aeronauticsflybyinstruments.block;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Maps the SOUTH-facing Blockbench lead onto one device attachment, without overshoot. */
public record CableConnection(Direction direction, Vec3 socket) {
    private static final Vec3 CENTER = new Vec3(.5, .5, .5);
    public static final double FLEX_START = 8.0 / 16;
    public static final double FLEX_END = 13.0 / 16;

    public CableConnection {
        if (direction == null || socket == null
                || !Double.isFinite(socket.x) || !Double.isFinite(socket.y) || !Double.isFinite(socket.z)) {
            throw new IllegalArgumentException("Cable connection requires a finite socket");
        }
    }

    /** Socket in cable-local coordinates, including the adjacent device's one-cell offset. */
    public Vec3 target() {
        return socket.add(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    public Vec3 transform(Vec3 point, boolean terminal) {
        Vec3 delta = inverseRotate(target().subtract(CENTER));
        // Canonical end is z=1. The actual end is the surface, with zero penetration.
        double advance = Math.max(0, delta.z - .5);
        double t = terminal ? 1 : (point.z - FLEX_START) / (FLEX_END - FLEX_START);
        Vec3 aligned = new Vec3(point.x - .5 + delta.x * t,
                point.y - .5 + delta.y * t, point.z - .5 + advance * t);
        return rotate(aligned).add(CENTER);
    }

    /** Conservative short boxes follow the slanted jacket for selection/collision. */
    public VoxelShape outline() {
        VoxelShape result = Shapes.empty();
        for (int i = 0; i < 5; i++) {
            result = Shapes.or(result, bounds(6, 6, 8 + i, 10, 10, 9 + i, false));
        }
        result = Shapes.or(result, bounds(5.875, 5.875, 13, 10.125, 10.125, 14.5, true));
        result = Shapes.or(result, bounds(5.75, 5.75, 14.5, 10.25, 10.25, 15.25, true));
        return Shapes.or(result, bounds(6.25, 6.25, 15.25, 9.75, 9.75, 16, true));
    }

    private VoxelShape bounds(double x0, double y0, double z0, double x1, double y1, double z1, boolean terminal) {
        double minX = Double.POSITIVE_INFINITY, minY = minX, minZ = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX, maxZ = maxX;
        for (double x : new double[]{x0, x1}) for (double y : new double[]{y0, y1}) for (double z : new double[]{z0, z1}) {
            Vec3 p = transform(new Vec3(x / 16, y / 16, z / 16), terminal);
            minX = Math.min(minX, p.x); minY = Math.min(minY, p.y); minZ = Math.min(minZ, p.z);
            maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y); maxZ = Math.max(maxZ, p.z);
        }
        return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private Vec3 rotate(Vec3 p) {
        return switch (direction) {
            case SOUTH -> p;
            case NORTH -> new Vec3(-p.x, p.y, -p.z);
            case EAST -> new Vec3(p.z, p.y, -p.x);
            case WEST -> new Vec3(-p.z, p.y, p.x);
            case UP -> new Vec3(p.x, p.z, -p.y);
            case DOWN -> new Vec3(p.x, -p.z, p.y);
        };
    }

    private Vec3 inverseRotate(Vec3 p) {
        return switch (direction) {
            case SOUTH -> p;
            case NORTH -> new Vec3(-p.x, p.y, -p.z);
            case EAST -> new Vec3(-p.z, p.y, p.x);
            case WEST -> new Vec3(p.z, p.y, -p.x);
            case UP -> new Vec3(p.x, -p.z, p.y);
            case DOWN -> new Vec3(p.x, p.z, -p.y);
        };
    }
}
