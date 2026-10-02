package com.radaeli.aeronauticsflybyinstruments.panel;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PanelCoordinatesTest {
    @Test
    void clicksOnEveryCellMapCorrectlyForEveryMountingDirection() {
        for (var facing : Direction.values()) {
            for (int row = 0; row < 3; row++) {
                for (int column = 0; column < 3; column++) {
                    var panelPoint = new Vec3((column + 0.5) / 3, 3.0 / 16, (row + 0.5) / 3);
                    var hit = PanelCoordinates.fromPanel(facing, panelPoint);
                    var transformed = PanelCoordinates.toPanel(facing, hit);
                    assertEquals(column, PanelCoordinates.cell(transformed.x));
                    assertEquals(row, PanelCoordinates.cell(transformed.z));
                    assertEquals(panelPoint.x, transformed.x, 0.000001);
                    assertEquals(panelPoint.y, transformed.y, 0.000001);
                    assertEquals(panelPoint.z, transformed.z, 0.000001);
                }
            }
        }
    }

    @Test
    void outerEdgesRemainInsideButInvalidCoordinatesAreRejected() {
        assertEquals(0, PanelCoordinates.cell(0));
        assertEquals(1, PanelCoordinates.cell(1.0 / 3));
        assertEquals(2, PanelCoordinates.cell(2.0 / 3));
        assertEquals(2, PanelCoordinates.cell(1));
        assertEquals(-1, PanelCoordinates.cell(-0.01));
        assertEquals(-1, PanelCoordinates.cell(1.01));
        assertEquals(-1, PanelCoordinates.cell(Double.NaN));
    }
}
