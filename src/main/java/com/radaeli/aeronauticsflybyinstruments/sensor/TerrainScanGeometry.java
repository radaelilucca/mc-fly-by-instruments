package com.radaeli.aeronauticsflybyinstruments.sensor;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Deterministic ray distributions around the scanner's local forward (+Z) axis. */
public final class TerrainScanGeometry {
    public static final int SIMPLE_RAY_COUNT = 9;
    public static final int MIN_ADVANCED_RAYS = 9;
    public static final int MAX_ADVANCED_RAYS = 81;
    public static final int SIMPLE_APERTURE_DEGREES = 90;
    public static final int DEFAULT_ADVANCED_APERTURE_DEGREES = 90;
    public static final int MIN_APERTURE_DEGREES = 30;
    public static final int MAX_APERTURE_DEGREES = 180;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private TerrainScanGeometry() {}

    public static List<DirectionVector> simpleDirections() {
        double halfAngle = Math.toRadians(SIMPLE_APERTURE_DEGREES / 2.0);
        double radial = Math.sin(halfAngle);
        double forward = Math.cos(halfAngle);
        List<DirectionVector> result = new ArrayList<>(SIMPLE_RAY_COUNT);
        result.add(new DirectionVector(0.0, 0.0, 1.0));
        for (int index = 0; index < 8; index++) {
            double azimuth = index * Math.PI / 4.0;
            result.add(new DirectionVector(radial * Math.cos(azimuth), radial * Math.sin(azimuth), forward));
        }
        return List.copyOf(result);
    }

    public static int advancedRayCount(int apertureDegrees) {
        validateAperture(apertureDegrees);
        double halfAngle = Math.toRadians(apertureDegrees / 2.0);
        int rays = (int) Math.round(MIN_ADVANCED_RAYS
                + (MAX_ADVANCED_RAYS - MIN_ADVANCED_RAYS) * (1.0 - Math.cos(halfAngle)));
        return Math.clamp(rays, MIN_ADVANCED_RAYS, MAX_ADVANCED_RAYS);
    }

    public static List<DirectionVector> advancedDirections(int apertureDegrees) {
        int rayCount = advancedRayCount(apertureDegrees);
        double halfAngle = Math.toRadians(apertureDegrees / 2.0);
        double minCosine = Math.cos(halfAngle);
        List<DirectionVector> result = new ArrayList<>(rayCount);
        result.add(new DirectionVector(0.0, 0.0, 1.0));
        for (int index = 1; index < rayCount; index++) {
            double fraction = (index - 0.5) / (rayCount - 1.0);
            double cosine = 1.0 - fraction * (1.0 - minCosine);
            double radial = Math.sqrt(Math.max(0.0, 1.0 - cosine * cosine));
            double azimuth = (index - 1) * GOLDEN_ANGLE;
            result.add(new DirectionVector(radial * Math.cos(azimuth), radial * Math.sin(azimuth), cosine));
        }
        return List.copyOf(result);
    }

    public static void validateAperture(int apertureDegrees) {
        if (apertureDegrees < MIN_APERTURE_DEGREES || apertureDegrees > MAX_APERTURE_DEGREES) {
            throw new IllegalArgumentException("Aperture must be between 30 and 180 degrees");
        }
    }

    /** Maps scanner right/up/forward components into the block's level-local axes. */
    public static Vec3 toLevelLocal(Direction facing, DirectionVector direction) {
        Vec3 forward = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up).normalize();
        return right.scale(direction.x()).add(up.scale(direction.y())).add(forward.scale(direction.z())).normalize();
    }

    public record DirectionVector(double x, double y, double z) {}
}
