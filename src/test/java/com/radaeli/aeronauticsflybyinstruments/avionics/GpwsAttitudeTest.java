package com.radaeli.aeronauticsflybyinstruments.avionics;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GpwsAttitudeTest {
    @Test
    void levelAndBankedForwardAxesProduceSignedBankAngles() {
        Vec3 north = new Vec3(0, 0, -1);
        assertEquals(0, GpwsAttitude.bankDegrees(north, new Vec3(0, 1, 0)).orElseThrow(), 1.0E-9);

        double angle = Math.toRadians(30);
        Vec3 bankedUp = new Vec3(Math.sin(angle), Math.cos(angle), 0);
        assertEquals(30, GpwsAttitude.bankDegrees(north, bankedUp).orElseThrow(), 1.0E-9);
    }

    @Test
    void bankIsUnavailableWhenForwardAxisIsVertical() {
        assertTrue(GpwsAttitude.bankDegrees(new Vec3(0, 1, 0), new Vec3(0, 0, 1)).isEmpty());
    }

    @Test
    void pitchWithoutRollDoesNotCreateFalseBank() {
        double pitch = Math.toRadians(25);
        Vec3 pitchedForward = new Vec3(0, Math.sin(pitch), -Math.cos(pitch));
        Vec3 pitchedUp = new Vec3(0, Math.cos(pitch), Math.sin(pitch));
        assertEquals(0, GpwsAttitude.bankDegrees(pitchedForward, pitchedUp).orElseThrow(), 1.0E-9);
    }
}
