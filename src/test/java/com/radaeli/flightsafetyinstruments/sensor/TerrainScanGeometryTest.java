package com.radaeli.flightsafetyinstruments.sensor;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TerrainScanGeometryTest {
    @Test
    void simpleScannerUsesNineUnitRaysInsideFixedNinetyDegreeCone() {
        var directions = TerrainScanGeometry.simpleDirections();
        assertEquals(9, directions.size());
        assertEquals(new TerrainScanGeometry.DirectionVector(0.0, 0.0, 1.0), directions.getFirst());
        for (var direction : directions) {
            double length = Math.sqrt(direction.x() * direction.x() + direction.y() * direction.y()
                    + direction.z() * direction.z());
            assertEquals(1.0, length, 1.0E-12);
            assertTrue(direction.z() >= Math.cos(Math.toRadians(45.0)) - 1.0E-12);
        }
    }

    @Test
    void advancedRayBudgetScalesWithApertureAreaAndStaysBounded() {
        int narrow = TerrainScanGeometry.advancedRayCount(30);
        int defaultAngle = TerrainScanGeometry.advancedRayCount(90);
        int widest = TerrainScanGeometry.advancedRayCount(180);

        assertTrue(narrow >= 9 && narrow <= 81);
        assertTrue(defaultAngle > narrow);
        assertEquals(81, widest);
        assertEquals(widest, TerrainScanGeometry.advancedDirections(180).size());
        assertTrue(TerrainScanGeometry.advancedDirections(90).stream().allMatch(direction -> {
            double length = Math.sqrt(direction.x() * direction.x() + direction.y() * direction.y()
                    + direction.z() * direction.z());
            return Math.abs(length - 1.0) < 1.0E-12
                    && direction.z() >= Math.cos(Math.toRadians(45.0)) - 1.0E-12;
        }));
        assertThrows(IllegalArgumentException.class, () -> TerrainScanGeometry.advancedRayCount(29));
        assertThrows(IllegalArgumentException.class, () -> TerrainScanGeometry.advancedRayCount(181));
    }

    @Test
    void centerRayFollowsHorizontalBlockFacing() {
        var center = new TerrainScanGeometry.DirectionVector(0.0, 0.0, 1.0);
        assertEquals(new Vec3(0, 0, -1), TerrainScanGeometry.toLevelLocal(Direction.NORTH, center));
        assertEquals(new Vec3(1, 0, 0), TerrainScanGeometry.toLevelLocal(Direction.EAST, center));
        assertEquals(new Vec3(0, 0, 1), TerrainScanGeometry.toLevelLocal(Direction.SOUTH, center));
        assertEquals(new Vec3(-1, 0, 0), TerrainScanGeometry.toLevelLocal(Direction.WEST, center));
    }
}
