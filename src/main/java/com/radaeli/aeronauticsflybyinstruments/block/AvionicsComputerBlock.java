package com.radaeli.aeronauticsflybyinstruments.block;

import com.mojang.serialization.MapCodec;
import com.radaeli.aeronauticsflybyinstruments.avionics.AvionicsComputerBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.avionics.AvionicsComputerStatus;
import com.radaeli.aeronauticsflybyinstruments.network.AvionicsComputerNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;

/** Physical Avionics Computer: sensor hub, GPWS function and CC:Tweaked gateway of one aircraft. */
public final class AvionicsComputerBlock extends BaseEntityBlock implements AvionicsWrenchable {
    public static final MapCodec<AvionicsComputerBlock> CODEC = simpleCodec(AvionicsComputerBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Screen texture only (same geometry): follows {@link AvionicsComputerStatus}. */
    public static final EnumProperty<Screen> SCREEN = EnumProperty.create("screen", Screen.class);
    // Follows models/block/avionics_computer.json: iron base, inset body, brass lid, screen, test button.
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            box(0, 0, 0, 16, 2, 16), box(1, 2, 1, 15, 14, 15), box(0, 14, 0, 16, 16, 16),
            box(2, 5, 0, 14, 13, 1), box(7, 2, 0, 9, 4, 1)).optimize();
    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
            Direction.NORTH, NORTH_SHAPE, Direction.EAST, rotateShape(Direction.EAST),
            Direction.SOUTH, rotateShape(Direction.SOUTH), Direction.WEST, rotateShape(Direction.WEST));

    public enum Screen implements StringRepresentable {
        NORMAL, CAUTION, WARNING, INOP;

        public static Screen of(AvionicsComputerStatus.Level level) {
            return valueOf(level.screen().toUpperCase(Locale.ROOT));
        }

        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    private static VoxelShape rotateShape(Direction facing) {
        return NORTH_SHAPE.toAabbs().stream().map(b -> switch (facing) {
            case EAST -> Shapes.box(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX);
            case SOUTH -> Shapes.box(1 - b.maxX, b.minY, 1 - b.maxZ, 1 - b.minX, b.maxY, 1 - b.minZ);
            case WEST -> Shapes.box(b.minZ, b.minY, 1 - b.maxX, b.maxZ, b.maxY, 1 - b.minX);
            default -> throw new IllegalArgumentException("Horizontal orientation required");
        }).reduce(Shapes.empty(), Shapes::or).optimize();
    }

    /**
     * Whether a hit lands on the test button ([7,2]-[9,4] on the front face, model units), after
     * turning the hit into the model's NORTH frame. Half a unit of slack keeps it easy to press.
     */
    public static boolean hitsTestButton(Direction facing, Direction face, Vec3 local) {
        if (face != facing) return false;
        double across = switch (facing) {
            case NORTH -> 1 - local.x;
            case SOUTH -> local.x;
            case EAST -> 1 - local.z;
            case WEST -> local.z;
            default -> -1;
        };
        double u = 16 - across * 16, v = local.y * 16;
        return u >= 6.5 && u <= 9.5 && v >= 1.5 && v <= 4.5;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                            CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    public AvionicsComputerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SCREEN, Screen.INOP));
    }

    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,
            BlockState> builder) {
        builder.add(FACING, SCREEN);
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
        return new AvionicsComputerBlockEntity(pos, state);
    }

    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                            BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.AVIONICS_COMPUTER_ENTITY.get(),
                AvionicsComputerBlockEntity::serverTick);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                         BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof AvionicsComputerBlockEntity computer) {
            Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
            if (hitsTestButton(state.getValue(FACING), hit.getDirection(), local)) {
                if (computer.startTest()) {
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.aeronautics_fly_by_instruments.avionics_computer.testing"), true);
                }
            } else {
                AvionicsComputerNetwork.open(serverPlayer, pos, computer);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
