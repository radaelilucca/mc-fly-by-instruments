package com.radaeli.flightsafetyinstruments.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModularTableBaseBlockTest {
    private static ModularTableBaseBlock block;

    @BeforeAll
    static void initializeMinecraft() {
        block = ModBlocks.MODULAR_TABLE_BASE.get();
    }

    @Test
    void everyMountingFaceHasThinCollisionAgainstItsSupport() {
        for (Direction facing : Direction.values()) {
            var state = block.defaultBlockState().setValue(ModularTableBaseBlock.FACING, facing);
            var outline = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            var collision = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                    CollisionContext.empty());
            var axis = facing.getAxis();
            double expectedMin = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0 : 13.0 / 16;
            assertEquals(expectedMin, outline.min(axis), 0.000001, facing.toString());
            assertEquals(expectedMin + 3.0 / 16, outline.max(axis), 0.000001, facing.toString());
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
    void structureRotationPreservesMountingDirection() {
        for (Direction facing : Direction.values()) {
            var state = block.defaultBlockState().setValue(ModularTableBaseBlock.FACING, facing);
            assertEquals(Rotation.CLOCKWISE_90.rotate(facing),
                    state.rotate(Rotation.CLOCKWISE_90).getValue(ModularTableBaseBlock.FACING));
        }
    }
}
