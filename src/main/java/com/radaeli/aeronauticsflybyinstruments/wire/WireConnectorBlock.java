package com.radaeli.aeronauticsflybyinstruments.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/** Brass data connector: joins the block it is mounted on to every wire that ends here. */
public final class WireConnectorBlock extends WireNodeBlock {
    /** Flange, body and gland, as in the Blockbench model (see {@link WireNodeBlock#box}). */
    public static final int[][] PARTS = {{5, 11, 0, 1}, {6, 10, 1, 3}, {7, 9, 3, WireAttachPoints.CONNECTOR_MOUTH}};
    /** Stem into the host block; drawn but left out of the shape. Closes the gap to inset devices. */
    public static final int[] STEM = {6, 10, -2, 1};
    /** On a modular table edge: no flange, the 4 px body fits the table's thickness. */
    public static final BooleanProperty SLIM = BooleanProperty.create("slim");
    private static final int[][] SLIM_PARTS = {{6, 10, 0, 3}, PARTS[2]};
    private final Map<Direction, VoxelShape> slimShapes = shapes(SLIM_PARTS);

    public WireConnectorBlock(BlockBehaviour.Properties properties) {
        super(properties, PARTS);
        registerDefaultState(defaultBlockState().setValue(SLIM, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SLIM);
    }

    @Override
    protected VoxelShape baseShape(BlockState state) {
        return state.getValue(SLIM) ? slimShapes.get(state.getValue(FACING)) : super.baseShape(state);
    }

    @Override
    protected BlockState withHost(BlockState state, BlockGetter level, BlockPos pos) {
        BlockState host = level.getBlockState(host(state, pos));
        return state.setValue(SLIM, tableEdgeSocket(host, state.getValue(FACING)) != null);
    }
}
