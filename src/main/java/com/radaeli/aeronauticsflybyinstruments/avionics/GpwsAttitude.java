package com.radaeli.aeronauticsflybyinstruments.avionics;

import net.minecraft.world.phys.Vec3;

import java.util.OptionalDouble;

/** Small, testable attitude helper for extracting roll around a contraption's forward axis. */
public final class GpwsAttitude {
    private static final Vec3 WORLD_UP = new Vec3(0, 1, 0);

    private GpwsAttitude() {}

    public static OptionalDouble bankDegrees(Vec3 worldForward, Vec3 worldUp) {
        if (worldForward.lengthSqr() < 1.0E-8 || worldUp.lengthSqr() < 1.0E-8) return OptionalDouble.empty();
        Vec3 forward = worldForward.normalize();
        Vec3 referenceUp = WORLD_UP.subtract(forward.scale(WORLD_UP.dot(forward)));
        Vec3 actualUp = worldUp.subtract(forward.scale(worldUp.dot(forward)));
        if (referenceUp.lengthSqr() < 1.0E-8 || actualUp.lengthSqr() < 1.0E-8) return OptionalDouble.empty();
        referenceUp = referenceUp.normalize();
        actualUp = actualUp.normalize();
        double sine = forward.dot(referenceUp.cross(actualUp));
        double cosine = Math.clamp(referenceUp.dot(actualUp), -1.0, 1.0);
        return OptionalDouble.of(Math.toDegrees(Math.atan2(sine, cosine)));
    }
}
