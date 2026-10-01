package com.radaeli.flightsafetyinstruments.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.radaeli.flightsafetyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.flightsafetyinstruments.panel.PanelGrid;
import com.radaeli.flightsafetyinstruments.panel.PanelCoordinates;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** Thin mounting surface with a persistent virtual grid. */
public final class ModularTableBaseBlock extends Block implements EntityBlock {
    public static final MapCodec<ModularTableBaseBlock> CODEC = simpleCodec(ModularTableBaseBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final ResourceLocation CREATE_WRENCH_ID =
            ResourceLocation.fromNamespaceAndPath("create", "wrench");

    // Match the Create Copycat Panel's 3-pixel thickness, without material copying.
    private static final VoxelShape UP = box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape DOWN = box(0, 13, 0, 16, 16, 16);
    private static final VoxelShape NORTH = box(0, 0, 13, 16, 16, 16);
    private static final VoxelShape SOUTH = box(0, 0, 0, 16, 16, 3);
    private static final VoxelShape WEST = box(13, 0, 0, 16, 16, 16);
    private static final VoxelShape EAST = box(0, 0, 0, 3, 16, 16);

    public ModularTableBaseBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos supportPos = pos.relative(facing.getOpposite());
        return level.getBlockState(supportPos).isFaceSturdy(level, supportPos, facing);
    }

    public static boolean isCreateWrench(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(CREATE_WRENCH_ID);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ModularTableBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!isCreateWrench(stack) || hit.getDirection() != state.getValue(FACING)
                || !(level.getBlockEntity(pos) instanceof ModularTableBlockEntity table)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        var point = PanelCoordinates.toPanel(state.getValue(FACING), hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)));
        int column = PanelCoordinates.cell(point.x);
        int row = PanelCoordinates.cell(point.z);
        var module = table.grid().at(column, row);
        if (module.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && table.removeModule(column, row)) {
            var moduleStack = itemStack(module.get());
            if (!player.addItem(moduleStack)) popResource(level, pos, moduleStack);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static ItemStack itemStack(PanelGrid.Module module) {
        return new ItemStack(module.type().equals("lamp_2x1")
                ? ModBlocks.LAMP_MODULE_2X1.get() : ModBlocks.LAMP_MODULE.get());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof ModularTableBlockEntity table) {
            for (var module : table.grid().modules()) {
                popResource(level, pos, itemStack(module));
            }
        }
        super.onRemove(state, level, pos, newState, moving);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        var facing = state.getValue(FACING);
        VoxelShape shape = switch (facing) {
            case UP -> UP;
            case DOWN -> DOWN;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
        if (level.getBlockEntity(pos) instanceof ModularTableBlockEntity table) {
            for (var module : table.grid().modules()) {
                double x = module.column() / 3.0 + 0.06;
                double z = module.row() / 3.0 + 0.06;
                var start = PanelCoordinates.fromPanel(facing, new Vec3(x, 3.0 / 16, z));
                var end = PanelCoordinates.fromPanel(facing, new Vec3(
                        (module.column() + module.width()) / 3.0 - 0.06, 5.0 / 16,
                        (module.row() + module.height()) / 3.0 - 0.06));
                shape = Shapes.or(shape, Shapes.box(Math.min(start.x, end.x), Math.min(start.y, end.y), Math.min(start.z, end.z),
                        Math.max(start.x, end.x), Math.max(start.y, end.y), Math.max(start.z, end.z)));
            }
        }
        return shape;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
