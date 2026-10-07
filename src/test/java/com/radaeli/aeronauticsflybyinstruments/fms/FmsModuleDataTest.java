package com.radaeli.aeronauticsflybyinstruments.fms;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FmsModuleDataTest {
    @Test
    void waypointCrudAndSelectionStayConsistent() {
        var data = new FmsModuleData();
        var waypoint = waypoint("Home", 12, 64, -7);
        assertTrue(data.add(waypoint));
        assertFalse(data.add(waypoint));
        assertTrue(data.select(waypoint.id()));
        assertEquals(waypoint, data.selectedWaypoint().orElseThrow());

        var renamed = new FmsWaypoint(waypoint.id(), "Runway", waypoint.dimension(), 20, 72, 4);
        assertTrue(data.update(renamed));
        assertEquals("Runway", data.selectedWaypoint().orElseThrow().name());
        assertTrue(data.delete(waypoint.id()));
        assertTrue(data.selectedWaypoint().isEmpty());
        assertFalse(data.delete(waypoint.id()));
    }

    @Test
    void nbtRoundTripPreservesModuleIdentityWaypointsAndTarget() {
        UUID moduleId = UUID.randomUUID();
        var waypoint = waypoint("Mountain Base", 1.5, 88, -32.25);
        var original = new FmsModuleData(moduleId, List.of(waypoint), waypoint.id());

        var restored = FmsModuleData.load(original.save());

        assertEquals(moduleId, restored.id());
        assertEquals(List.of(waypoint), restored.waypoints());
        assertEquals(waypoint.id(), restored.selectedWaypointId().orElseThrow());
    }

    @Test
    void corruptOrMissingTargetDoesNotSurviveLoad() {
        var waypoint = waypoint("Valid", 0, 64, 0);
        var tag = new CompoundTag();
        tag.putUUID("Id", UUID.randomUUID());
        tag.putUUID("SelectedWaypoint", UUID.randomUUID());
        var list = new net.minecraft.nbt.ListTag();
        list.add(waypoint.save());
        tag.put("Waypoints", list);

        assertTrue(FmsModuleData.load(tag).selectedWaypoint().isEmpty());
        assertTrue(FmsModuleData.load(new CompoundTag()).waypoints().isEmpty());
    }

    @Test
    void validatesNamesCoordinatesAndDimension() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> new FmsWaypoint(id, "  ", "minecraft:overworld", 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new FmsWaypoint(id, "x".repeat(33), "minecraft:overworld", 0, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new FmsWaypoint(id, "Bad", "minecraft:overworld", Double.NaN, 0, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new FmsWaypoint(id, "Bad", "", 0, 0, 0));
    }

    @Test
    void waypointCoordinatesAreFlooredToContainingBlocksOnCreateAndLegacyLoad() {
        var created = waypoint("Landing", 3.25, -60.0, -10.671499778056285);
        assertEquals(3.0, created.x(), 0.0);
        assertEquals(-60.0, created.y(), 0.0);
        assertEquals(-11.0, created.z(), 0.0);
        assertEquals(-11.0, created.save().getDouble("Z"), 0.0);

        var legacyTag = created.save();
        legacyTag.putDouble("X", -1.3278908072976996);
        legacyTag.putDouble("Y", -60.4);
        legacyTag.putDouble("Z", -10.671499778056285);
        var moduleTag = new FmsModuleData(UUID.randomUUID(), List.of(created), null).save();
        moduleTag.getList("Waypoints", net.minecraft.nbt.Tag.TAG_COMPOUND).set(0, legacyTag);
        var restored = FmsModuleData.load(moduleTag).waypoints().getFirst();

        assertEquals(-2.0, restored.x(), 0.0);
        assertEquals(-61.0, restored.y(), 0.0);
        assertEquals(-11.0, restored.z(), 0.0);
    }

    private static FmsWaypoint waypoint(String name, double x, double y, double z) {
        return new FmsWaypoint(UUID.randomUUID(), name, "minecraft:overworld", x, y, z);
    }
}
