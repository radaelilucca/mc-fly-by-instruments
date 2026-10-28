package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.panel.PanelCoordinates;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FmsCompassTest {
    private static final double EPSILON = 1e-9;

    @Test
    void cardHasTenDegreeTicksAndTheEightPointsLongest() {
        int points = 0, degrees = 0;
        for (int i = 0; i < FmsCompass.TICKS; i++) {
            float length = 1 - FmsCompass.TICK_INNER[i];
            if (Math.abs(length - FmsCompass.POINT_LENGTH) < 1e-6) points++;
            else if (Math.abs(length - FmsCompass.DEGREE_LENGTH) < 1e-6) degrees++;
        }
        assertEquals(40, FmsCompass.TICKS);
        assertEquals(8, points);
        // 36 ten-degree ticks, of which N, E, S and W are points; plus the four intercardinals.
        assertEquals(32, degrees);
        assertTrue(FmsCompass.POINT_LENGTH > FmsCompass.DEGREE_LENGTH);
        // Ticks run clockwise from north: 0, 10, 20, 30, 40, 45, 50, ...
        assertEquals(0, FmsCompass.TICK_SIN[0], 1e-6);
        assertEquals(Math.sin(Math.toRadians(45)), FmsCompass.TICK_SIN[5], 1e-6);
    }

    @Test
    void onlyTheFourCardinalsHaveLetters() {
        assertEquals("NESW", FmsCompass.LETTERS);
        assertEquals(4, FmsCompass.LETTER_RECTS.length);
        for (float[] rects : FmsCompass.LETTER_RECTS) {
            assertTrue(rects.length > 0 && rects.length % 4 == 0);
            for (int i = 0; i < rects.length; i += 4) {
                assertTrue(rects[i] >= 0 && rects[i + 2] <= FmsCompass.GLYPH_WIDTH && rects[i] < rects[i + 2]);
                assertTrue(rects[i + 1] >= 0 && rects[i + 3] <= FmsCompass.GLYPH_HEIGHT);
            }
        }
    }

    @Test
    void bearingIsClockwiseFromNorthInMinecraftAxes() {
        assertEquals(0, FmsCompass.bearing(0, 0, 0, -10), EPSILON);              // north: -Z
        assertEquals(Math.PI / 2, FmsCompass.bearing(0, 0, 10, 0), EPSILON);     // east: +X
        assertEquals(Math.PI, Math.abs(FmsCompass.bearing(0, 0, 0, 10)), EPSILON); // south
        assertEquals(-Math.PI / 2, FmsCompass.bearing(0, 0, -10, 0), EPSILON);   // west
    }

    @Test
    void floorPanelHeadsWhereTheTopOfTheScreenPoints() {
        // Floor table (facing up): the top of the screen is panel -Z, which is north.
        assertEquals(0, heading(Direction.UP), EPSILON);
        // Ceiling table: the panel is flipped, the top of the screen points south.
        assertEquals(Math.PI, Math.abs(heading(Direction.DOWN)), EPSILON);
    }

    @Test
    void wallPanelHeadsIntoThePanel() {
        // A wall panel faces the pilot, who looks into it: facing north means heading south.
        assertEquals(Math.PI, Math.abs(heading(Direction.NORTH)), EPSILON);
        assertEquals(0, heading(Direction.SOUTH), EPSILON);
        assertEquals(Math.PI / 2, heading(Direction.WEST), EPSILON);
        assertEquals(-Math.PI / 2, heading(Direction.EAST), EPSILON);
    }

    @Test
    void contraptionYawTurnsTheHeading() {
        // A floor panel on a contraption yawed 90 degrees clockwise (north turns to east).
        Vec3 up = new Vec3(1, 0, 0), normal = new Vec3(0, 1, 0);
        assertEquals(Math.PI / 2, FmsCompass.heading(up, normal), EPSILON);
        // Pitched nose-up 60 degrees: the top of the screen still leans north enough to use.
        Vec3 pitched = new Vec3(0, Math.sin(Math.toRadians(60)), -Math.cos(Math.toRadians(60)));
        assertEquals(0, FmsCompass.heading(pitched, new Vec3(0, 0.5, 0.87)), 1e-6);
    }

    @Test
    void introEasesIn() {
        assertEquals(0, FmsCompass.intro(0));
        assertEquals(1, FmsCompass.intro(FmsCompass.INTRO_SECONDS));
        assertTrue(FmsCompass.intro(FmsCompass.INTRO_SECONDS / 2) > 0.5f);
    }

    /** Same derivation as ModularTableRenderer, without a contraption. */
    private static double heading(Direction facing) {
        Vec3 center = new Vec3(0.5, 3.0 / 16, 0.5);
        Vec3 local = PanelCoordinates.fromPanel(facing, center);
        Vec3 up = PanelCoordinates.fromPanel(facing, center.add(0, 0, -1)).subtract(local);
        Vec3 normal = PanelCoordinates.fromPanel(facing, center.add(0, 1, 0)).subtract(local);
        return FmsCompass.heading(up, normal);
    }
}
