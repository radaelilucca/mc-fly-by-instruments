package com.radaeli.aeronauticsflybyinstruments.panel;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PanelGridTest {
    @Test
    void nineLampsFitAndAnOccupiedCellCannotBeReused() {
        var grid = new PanelGrid();
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                assertTrue(grid.place(new PanelGrid.Module("lamp", column, row, 1, 1)));
                assertFalse(grid.place(new PanelGrid.Module("lamp", column, row, 1, 1)));
            }
        }
        assertEquals(9, grid.modules().size());
        assertTrue(grid.removeAt(1, 1).isPresent());
        assertTrue(grid.canFit(1, 1, 1, 1));
        assertFalse(grid.canFit(0, 0, 2, 2));
    }

    @Test
    void largerFootprintsOwnEveryCellAndRemoveFromAnyCoveredCell() {
        var grid = new PanelGrid();
        var module = new PanelGrid.Module("future_2x2", 0, 0, 2, 2);
        assertTrue(grid.place(module));
        assertEquals(module, grid.at(1, 1).orElseThrow());
        assertFalse(grid.canFit(1, 0, 1, 2));
        assertTrue(grid.canFit(2, 0, 1, 2));
        assertTrue(grid.place(new PanelGrid.Module("future_1x2", 2, 0, 1, 2)));
        assertEquals(module, grid.removeAt(1, 1).orElseThrow());
        assertTrue(grid.canFit(0, 0, 2, 2));
        assertTrue(grid.at(2, 1).isPresent());
    }

    @Test
    void rejectsInvalidFootprintsAndBoundsWithoutChangingTheGrid() {
        var grid = new PanelGrid();
        assertFalse(grid.canFit(-1, 0, 1, 1));
        assertFalse(grid.canFit(0, -1, 1, 1));
        assertFalse(grid.canFit(3, 0, 1, 1));
        assertFalse(grid.canFit(0, 3, 1, 1));
        assertFalse(grid.canFit(2, 2, 2, 2));
        assertFalse(grid.canFit(0, 0, 0, 1));
        assertFalse(grid.canFit(0, 0, Integer.MAX_VALUE, 1));
        assertTrue(grid.modules().isEmpty());
    }

    @Test
    void configuresModuleWithoutChangingItsFootprint() {
        var grid = new PanelGrid();
        assertTrue(grid.place(new PanelGrid.Module("lamp_2x1", 1, 0, 1, 2)));
        assertTrue(grid.configureAt(1, 1, "terrain", 12.5));
        var configured = grid.at(1, 1).orElseThrow();
        assertEquals("terrain", configured.channel());
        assertEquals(12.5, configured.threshold());
        assertEquals(1, configured.width());
        assertEquals(2, configured.height());
    }
}
