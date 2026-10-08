package com.radaeli.aeronauticsflybyinstruments.block;

import com.mojang.serialization.MapCodec;
import com.radaeli.aeronauticsflybyinstruments.avionics.GpwsComputerBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.network.GpwsNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/** Physical computer that evaluates the avionics network's GPWS channels. */
public final class GpwsComputerBlock extends BaseEntityBlock implements AvionicsWrenchable {
    public static final MapCodec<GpwsComputerBlock> CODEC = simpleCodec(GpwsComputerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    // Housing recesses remain real; only front, top and bottom sockets reach the cell edge.
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(1, 1, 1, 15, 15, 15), box(2, .25, 2, 14, 1.25, 14),
            box(.85, 2, 2, 1, 14, 14), box(15, 2, 2, 15.15, 14, 14),
            box(2, 15, 2, 14, 15.3, 14), box(2, 2, 15, 14, 14, 15.15),
            box(2, 2, .75, 14, 14, 1),
            box(3, 11.5, .35, 4.5, 13.5, .75), box(11.5, 11.5, .35, 13, 13.5, .75),
            box(3.5, 12, 0, 4.5, 13, .35), box(11.5, 12, 0, 12.5, 13, .35),
            box(4.5, 12, 0, 11.5, 13.25, .35),
            box(3, 5, .15, 5, 7, .75), box(10.5, 4.5, .15, 12.5, 6.5, .75),
            box(11, 7.5, .35, 12.5, 11.2, .75),
            box(6, 6, 0, 10, 10, .75),
            box(6, 15.15, 6, 10, 16, 10), box(6, 0, 6, 10, 1, 10)).optimize();
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, NORTH_SHAPE, Direction.EAST, rotateShape(Direction.EAST),
            Direction.SOUTH, rotateShape(Direction.SOUTH), Direction.WEST, rotateShape(Direction.WEST));

    private static VoxelShape rotateShape(Direction facing) {
        return NORTH_SHAPE.toAabbs().stream().map(b -> switch (facing) {
            case EAST -> Shapes.box(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX);
            case SOUTH -> Shapes.box(1 - b.maxX, b.minY, 1 - b.maxZ, 1 - b.minX, b.maxY, 1 - b.minZ);
            case WEST -> Shapes.box(b.minZ, b.minY, 1 - b.maxX, b.maxZ, b.maxY, 1 - b.minX);
            default -> throw new IllegalArgumentException("Horizontal orientation required");
        }).reduce(Shapes.empty(), Shapes::or).optimize();
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    public GpwsComputerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,
            BlockState> builder) {
        builder.add(FACING);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GpwsComputerBlockEntity(pos, state);
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.GPWS_COMPUTER_ENTITY.get(),
                GpwsComputerBlockEntity::serverTick);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                         BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof GpwsComputerBlockEntity computer) {
            GpwsNetwork.openConfiguration(serverPlayer, pos, computer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
