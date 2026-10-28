package com.radaeli.aeronauticsflybyinstruments.panel;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class TableInteractionTest {
    private ModularTableBlockEntity table(Direction facing) {
        return new ModularTableBlockEntity(new BlockPos(12,20,-7), ModBlocks.MODULAR_TABLE_BASE.get()
                .defaultBlockState().setValue(ModularTableBaseBlock.FACING, facing));
    }

    @Test void allFootprintsCoveredCellsResolveTheAnchorOnEveryOrientation() {
        for (Direction facing : Direction.values()) for (int kind=0;kind<5;kind++) {
            var table=table(facing);
            switch(kind) {
                case 0 -> table.placeLamp(0,0);
                case 1 -> table.placeBuzzer(0,0);
                case 2 -> table.placeLamp2x1(0,0,false);
                case 3 -> table.placeLamp2x1(0,0,true);
                case 4 -> table.placeFmsModule(0,0,new com.radaeli.aeronauticsflybyinstruments.fms.FmsModuleData());
            }
            var module=table.grid().modules().getFirst();
            for(int row=0;row<module.height();row++) for(int col=0;col<module.width();col++) {
                var hit=Vec3.atLowerCornerOf(table.getBlockPos()).add(PanelCoordinates.fromPanel(facing,
                        new Vec3((col+.5)/3,5.0/16,(row+.5)/3)));
                var target=TableInteraction.target(table,facing,hit);
                assertEquals(module,target.module().module(),facing+" "+kind);
                assertEquals(0,target.column()); assertEquals(0,target.row());
            }
        }
    }

    @Test void installationTokenSurvivesConfigurationButNotRemovalOrReload() {
        var table=table(Direction.UP);
        assertTrue(table.placeBuzzer(0,0));
        var token=table.moduleToken(0,0);
        assertTrue(table.saveAlertDraft(0,0,"gpws","gpws.any",3,"test",.5,42));
        assertEquals(token,table.moduleToken(0,0));
        var registries=HolderLookup.Provider.create(Stream.empty());
        var saved=table.saveWithoutMetadata(registries);
        assertFalse(saved.contains("interactionToken"));
        assertTrue(table.removeModule(0,0));
        assertNull(table.moduleToken(0,0));
        assertTrue(table.placeBuzzer(0,0));
        assertNotEquals(token,table.moduleToken(0,0));
        var beforeReload=table.moduleToken(0,0);
        table.loadCustomOnly(saved,registries);
        assertNotEquals(beforeReload,table.moduleToken(0,0));
    }

    @Test void invalidDraftAndPreviewNeverChangeSavedVolume() {
        var table=table(Direction.UP);
        table.placeBuzzer(0,0);
        var before=table.grid().modules();
        assertFalse(table.saveAlertDraft(0,0,"gpws","gpws.any",2,"test",.5,101));
        assertFalse(table.saveAlertDraft(0,0,"bad","gpws.any",2,"test",.5,20));
        assertFalse(table.testBuzzer(0,0,20)); // no server; no persistence side effects
        assertEquals(before,table.grid().modules());
    }

    @Test void teardownReturnsContentsOnlyOnce() {
        var table=table(Direction.UP);
        table.placeBuzzer(0,0); table.placeLamp2x1(1,0,false);
        assertEquals(2,table.drainModuleStacks().size());
        assertTrue(table.drainModuleStacks().isEmpty());
        assertTrue(table.grid().modules().isEmpty());
        assertNull(table.moduleToken(0,0));
    }
}
