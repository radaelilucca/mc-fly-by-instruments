package com.radaeli.aeronauticsflybyinstruments.block;

import com.radaeli.aeronauticsflybyinstruments.avionics.AvionicsEndpoint;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.EnumMap;
import java.util.Map;

/** Physical data cable. Adjacent cable segments and avionics endpoints connect automatically. */
public final class AvionicsCableBlock extends Block implements AvionicsWrenchable {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Direction.class);
    private static final VoxelShape CORE = box(6, 6, 6, 10, 10, 10);
    private static final Map<Direction, VoxelShape> ARMS = new EnumMap<>(Direction.class);

    static {
        CONNECTIONS.put(Direction.DOWN, BooleanProperty.create("down"));
        CONNECTIONS.put(Direction.UP, BooleanProperty.create("up"));
        CONNECTIONS.put(Direction.NORTH, BooleanProperty.create("north"));
        CONNECTIONS.put(Direction.SOUTH, BooleanProperty.create("south"));
        CONNECTIONS.put(Direction.WEST, BooleanProperty.create("west"));
        CONNECTIONS.put(Direction.EAST, BooleanProperty.create("east"));
        ARMS.put(Direction.DOWN, box(6, 0, 6, 10, 8, 10));
        ARMS.put(Direction.UP, box(6, 8, 6, 10, 16, 10));
        ARMS.put(Direction.NORTH, box(6, 6, 0, 10, 10, 8));
        ARMS.put(Direction.SOUTH, box(6, 6, 8, 10, 10, 16));
        ARMS.put(Direction.WEST, box(0, 6, 6, 8, 10, 10));
        ARMS.put(Direction.EAST, box(8, 6, 6, 16, 10, 10));
    }

    public AvionicsCableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any()
                .setValue(CONNECTIONS.get(Direction.DOWN), false)
                .setValue(CONNECTIONS.get(Direction.UP), false)
                .setValue(CONNECTIONS.get(Direction.NORTH), false)
                .setValue(CONNECTIONS.get(Direction.SOUTH), false)
                .setValue(CONNECTIONS.get(Direction.WEST), false)
                .setValue(CONNECTIONS.get(Direction.EAST), false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        CONNECTIONS.values().forEach(builder::add);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(direction), connectsTo(level, pos.relative(direction)));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(CONNECTIONS.get(direction), connectsTo(level, neighborPos));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction direction : Direction.values()) {
            if (state.getValue(CONNECTIONS.get(direction))) shape = Shapes.or(shape, ARMS.get(direction));
        }
        return shape;
    }

    private static boolean connectsTo(BlockGetter level, BlockPos pos) {
        if (level.getBlockState(pos).is(ModBlocks.AVIONICS_CABLE.get())) return true;
        return level.getBlockEntity(pos) instanceof AvionicsEndpoint;
    }
}
