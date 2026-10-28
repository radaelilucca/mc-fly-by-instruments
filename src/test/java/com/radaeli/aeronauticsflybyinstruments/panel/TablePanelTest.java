package com.radaeli.aeronauticsflybyinstruments.panel;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsModuleData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/** Joined tables act as one panel: shared grid, spanning footprints and loss of support. */
class TablePanelTest {
    private static final HolderLookup.Provider REGISTRIES = HolderLookup.Provider.create(Stream.empty());
    private final Map<BlockPos, ModularTableBlockEntity> world = new HashMap<>();
    private TablePanel.Lookup original;

    @BeforeEach
    void useFakeWorld() {
        original = TablePanel.lookup;
        TablePanel.lookup = (from, pos) -> world.get(pos);
    }

    @AfterEach
    void restoreLookup() {
        TablePanel.lookup = original;
    }

    private ModularTableBlockEntity table(int x, int y, int z, Direction facing) {
        var pos = new BlockPos(x, y, z);
        var table = new ModularTableBlockEntity(pos, ModBlocks.MODULAR_TABLE_BASE.get().defaultBlockState()
                .setValue(ModularTableBaseBlock.FACING, facing));
        world.put(pos, table);
        return table;
    }

    @Test
    void panelAxesFollowThePanelCoordinatesOfEveryFace() {
        for (Direction facing : Direction.values()) {
            var origin = PanelCoordinates.fromPanel(facing, new Vec3(0.25, 0.1, 0.25));
            var column = PanelCoordinates.fromPanel(facing, new Vec3(0.75, 0.1, 0.25)).subtract(origin).scale(2);
            var row = PanelCoordinates.fromPanel(facing, new Vec3(0.25, 0.1, 0.75)).subtract(origin).scale(2);
            assertEquals(Vec3.atLowerCornerOf(TablePanel.columnAxis(facing)), column, facing + " columns");
            assertEquals(Vec3.atLowerCornerOf(TablePanel.rowAxis(facing)), row, facing + " rows");
        }
    }

    @Test
    void aTwoCellModuleCrossesIntoTheJoinedTableAndBlocksItsCells() {
        var left = table(0, 0, 0, Direction.UP);
        var right = table(1, 0, 0, Direction.UP);
        assertTrue(TablePanel.canFit(left, 2, 0, 2, 1));
        assertTrue(left.placeSpanning("lamp_2x1", 2, 0, 2, 1, null));

        var crossing = TablePanel.localAt(right, 0, 0).orElseThrow();
        assertSame(left, crossing.owner());
        assertEquals(-1, crossing.column(), "Anchored one cell before the right table");
        assertFalse(TablePanel.canFit(right, 0, 0, 1, 1));
        assertTrue(TablePanel.canFit(right, 1, 0, 1, 1));
        assertEquals(1, TablePanel.covering(right).size());
        assertFalse(TablePanel.canFit(left, 1, 0, 2, 1), "Overlaps the crossing lamp");

        var panel = TablePanel.of(right);
        assertEquals(2, panel.members().size());
        assertSame(left, panel.leader());
        var shared = panel.at(3, 0).orElseThrow();
        assertEquals(2, shared.column());
        assertEquals(left, shared.owner());
        assertTrue(TablePanel.supported(left, crossing.module()));
    }

    @Test
    void footprintsNeedATableUnderEveryCell() {
        var lone = table(0, 0, 0, Direction.UP);
        assertFalse(TablePanel.canFit(lone, 2, 0, 2, 1));
        assertTrue(TablePanel.canFit(lone, 1, 0, 2, 1));
        table(1, 0, 0, Direction.NORTH); // a wall table beside it is not joined
        assertFalse(TablePanel.canFit(lone, 2, 0, 2, 1));
        assertFalse(TablePanel.canFit(lone, 3, 0, 1, 1), "The anchor stays in the clicked table");
    }

    @Test
    void anFmsCanSitOverFourTablesAndLosesSupportWithAnyOfThem() {
        var a = table(0, 0, 0, Direction.UP);
        table(1, 0, 0, Direction.UP);
        table(0, 0, 1, Direction.UP);
        var diagonal = table(1, 0, 1, Direction.UP);
        assertTrue(TablePanel.canFit(a, 2, 1, 2, 3));
        assertTrue(a.placeSpanning("fms", 2, 1, 2, 3, new FmsModuleData()));
        var fms = a.grid().at(2, 1).orElseThrow();
        assertTrue(TablePanel.localAt(diagonal, 0, 0).isPresent());
        assertTrue(TablePanel.supported(a, fms));
        world.remove(diagonal.getBlockPos());
        assertFalse(TablePanel.supported(a, fms));
    }

    @Test
    void panelFrameStartsAtTheTopLeftTableWhateverTheFace() {
        // On a south wall, columns grow towards -X.
        var first = table(0, 5, 0, Direction.SOUTH);
        var second = table(-1, 5, 0, Direction.SOUTH);
        var below = table(0, 4, 0, Direction.SOUTH);
        var panel = TablePanel.of(second);
        assertEquals(3, panel.members().size());
        assertEquals(0, panel.originColumn(first));
        assertEquals(3, panel.originColumn(second));
        assertEquals(3, panel.originRow(first), "Rows grow upwards on walls");
        assertEquals(0, panel.originRow(below));
    }

    @Test
    void panelWideControlsReachEveryJoinedTable() {
        var a = table(0, 0, 0, Direction.UP);
        var b = table(0, 0, 1, Direction.UP);
        var apart = table(5, 0, 0, Direction.UP);
        TablePanel.of(a).setMuted(true);
        assertTrue(a.muted());
        assertTrue(b.muted());
        assertFalse(apart.muted());
        assertTrue(TablePanel.of(b).setTestSignal("terrain", 0.8));
        assertEquals(0.8, a.testSignal("terrain"));
    }

    @Test
    void spanningModulesPersistAndRemovingThemKeepsOtherLampOverrides() {
        var table = table(0, 0, 0, Direction.UP);
        table(1, 0, 0, Direction.UP);
        assertTrue(table.placeSpanning("lamp_2x1", 2, 0, 2, 1, null));
        assertTrue(table.placeLamp(0, 1));
        assertTrue(table.setLampTestIntensity(0, 1, 9));

        var restored = new ModularTableBlockEntity(table.getBlockPos(), table.getBlockState());
        restored.loadCustomOnly(table.saveWithoutMetadata(REGISTRIES), REGISTRIES);
        assertEquals(table.grid().modules(), restored.grid().modules());

        // Cell (3, 0) of the crossing lamp would alias the key of (0, 1).
        assertTrue(table.removeModule(2, 0));
        assertEquals(9, table.lampRenderIntensity(table.grid().at(0, 1).orElseThrow()));
    }

    @Test
    void singleTableRulesStayStrict() {
        var table = table(0, 0, 0, Direction.UP);
        assertFalse(table.placeLamp2x1(2, 0, false), "Without a panel check the 3 x 3 limit holds");
        assertFalse(table.grid().placeAnchored(new PanelGrid.Module("lamp", 3, 0, 1, 1)));
        assertTrue(PanelGrid.spans(new PanelGrid.Module("lamp_2x1", 2, 0, 2, 1)));
        assertFalse(PanelGrid.spans(new PanelGrid.Module("lamp_2x1", 1, 0, 2, 1)));
    }
}
