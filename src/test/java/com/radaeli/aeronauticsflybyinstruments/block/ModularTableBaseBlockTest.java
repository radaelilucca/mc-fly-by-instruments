package com.radaeli.aeronauticsflybyinstruments.block;

import com.radaeli.aeronauticsflybyinstruments.panel.PanelCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import com.radaeli.aeronauticsflybyinstruments.panel.PanelGrid;
import net.minecraft.world.item.ItemStack;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModularTableBaseBlockTest {
    private static ModularTableBaseBlock block;

    @BeforeAll
    static void initializeMinecraft() {
        block = ModBlocks.MODULAR_TABLE_BASE.get();
    }

    @Test
    void removingEachModuleReturnsItsOwnItem() {
        assertEquals(ModBlocks.BUZZER_MODULE.get(),
                ModularTableBaseBlock.itemStack(new PanelGrid.Module("buzzer", 0, 0, 1, 1)).getItem());
        assertEquals(ModBlocks.LAMP_MODULE.get(),
                ModularTableBaseBlock.itemStack(new PanelGrid.Module("lamp", 0, 0, 1, 1)).getItem());
        assertEquals(ModBlocks.LAMP_MODULE_2X1.get(),
                ModularTableBaseBlock.itemStack(new PanelGrid.Module("lamp_2x1", 0, 0, 2, 1)).getItem());
        assertEquals(ItemStack.EMPTY,
                ModularTableBaseBlock.itemStack(new PanelGrid.Module("unknown", 0, 0, 1, 1)));
    }

    @Test
    void moduleStacksAreRecognizedForSuppressingTheGuiFallback() {
        assertTrue(ModularTableBaseBlock.isPanelModule(new ItemStack(ModBlocks.LAMP_MODULE.get())));
        assertTrue(ModularTableBaseBlock.isPanelModule(new ItemStack(ModBlocks.LAMP_MODULE_2X1.get())));
        assertTrue(ModularTableBaseBlock.isPanelModule(new ItemStack(ModBlocks.BUZZER_MODULE.get())));
        assertTrue(ModularTableBaseBlock.isPanelModule(new ItemStack(ModBlocks.FMS_MODULE.get())));
        assertFalse(ModularTableBaseBlock.isPanelModule(ItemStack.EMPTY));
    }

    @Test
    void everyMountingFaceHasThinCollisionAgainstItsSupport() {
        for (Direction facing : Direction.values()) {
            var state = block.defaultBlockState().setValue(ModularTableBaseBlock.FACING, facing);
            var outline = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            var collision = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                    CollisionContext.empty());
            var axis = facing.getAxis();
            double expectedMin = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : 1 - PanelCoordinates.SURFACE;
            assertEquals(expectedMin, outline.min(axis), 0.000001, facing.toString());
            assertEquals(expectedMin + PanelCoordinates.SURFACE, outline.max(axis), 0.000001, facing.toString());
            assertEquals(outline.bounds(), collision.bounds(), facing.toString());
            for (Direction.Axis otherAxis : Direction.Axis.values()) {
                if (otherAxis != axis) {
                    assertEquals(0, outline.min(otherAxis), 0.000001);
                    assertEquals(1, outline.max(otherAxis), 0.000001);
                }
            }
        }
    }

    @Test
    void defaultMountingIsOnTopOfTheSupportingBlock() {
        assertEquals(Direction.UP, block.defaultBlockState().getValue(ModularTableBaseBlock.FACING));
    }

    @Test
    void blockLightComesFromTheLightProperty() {
        assertEquals(0, block.defaultBlockState().getLightEmission());
        for (int light = 0; light <= 15; light++) {
            assertEquals(light, block.defaultBlockState().setValue(ModularTableBaseBlock.LIGHT, light).getLightEmission());
        }
    }

    @Test
    void structureRotationPreservesMountingDirection() {
        for (Direction facing : Direction.values()) {
            var state = block.defaultBlockState().setValue(ModularTableBaseBlock.FACING, facing);
            assertEquals(Rotation.CLOCKWISE_90.rotate(facing),
                    state.rotate(Rotation.CLOCKWISE_90).getValue(ModularTableBaseBlock.FACING));
        }
    }
}
