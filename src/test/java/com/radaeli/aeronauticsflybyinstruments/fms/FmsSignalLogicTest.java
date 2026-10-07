package com.radaeli.aeronauticsflybyinstruments.fms;

import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FmsSignalLogicTest {
    @Test
    void noSelectedWaypointHasValidFalseAndUnavailableDistance() {
        var snapshot = FmsSignalLogic.sample(new FmsModuleData(), "minecraft:overworld", Vec3.ZERO, 40);
        assertEquals("VALID", snapshot.waypointSelected().status().name());
        assertEquals(Boolean.FALSE, snapshot.waypointSelected().value().orElseThrow());
        assertEquals("UNAVAILABLE", snapshot.distanceToWaypoint().status().name());
    }

    @Test
    void horizontalDistanceIgnoresAltitudeAndUsesSelectedTarget() {
        var waypoint = waypoint("minecraft:overworld", 3, 300, 4);
        var data = new FmsModuleData(UUID.randomUUID(), List.of(waypoint), waypoint.id());

        var snapshot = FmsSignalLogic.sample(data, "minecraft:overworld", new Vec3(0, -20, 0), 50);

        assertEquals(Boolean.TRUE, snapshot.waypointSelected().value().orElseThrow());
        assertEquals(5.0, snapshot.distanceToWaypoint().value().orElseThrow(), 1e-9);
    }

    @Test
    void selectedTargetInAnotherDimensionIsNotAUsableDistance() {
        var waypoint = waypoint("minecraft:the_nether", 3, 64, 4);
        var data = new FmsModuleData(UUID.randomUUID(), List.of(waypoint), waypoint.id());

        var snapshot = FmsSignalLogic.sample(data, "minecraft:overworld", Vec3.ZERO, 60);

        assertEquals(Boolean.TRUE, snapshot.waypointSelected().value().orElseThrow());
        assertEquals("UNAVAILABLE", snapshot.distanceToWaypoint().status().name());
    }

    @Test
    void comparisonsIgnoreSamplingTimeButDetectSignalChanges() {
        var waypoint = waypoint("minecraft:overworld", 10, 64, 0);
        var data = new FmsModuleData(UUID.randomUUID(), List.of(waypoint), waypoint.id());
        var first = FmsSignalLogic.sample(data, "minecraft:overworld", Vec3.ZERO, 1);
        var same = FmsSignalLogic.sample(data, "minecraft:overworld", Vec3.ZERO, 2);
        var moved = FmsSignalLogic.sample(data, "minecraft:overworld", new Vec3(2, 0, 0), 3);
        assertTrue(FmsSignalLogic.sameValues(first, same));
        assertFalse(FmsSignalLogic.sameValues(first, moved));
    }

    private static FmsWaypoint waypoint(String dimension, double x, double y, double z) {
        return new FmsWaypoint(UUID.randomUUID(), "Destination", dimension, x, y, z);
    }
}
