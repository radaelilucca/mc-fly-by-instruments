package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.mojang.serialization.MapCodec;
import com.radaeli.aeronauticsflybyinstruments.block.AvionicsWrenchable;
import com.radaeli.aeronauticsflybyinstruments.block.CableSocketProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import com.radaeli.aeronauticsflybyinstruments.network.SensorOutputNetwork;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Fixed-orientation range sensor whose marked lower face emits the measurement beam. */
public final class RadarAltimeterBlock extends BaseEntityBlock implements AvionicsWrenchable, CableSocketProvider {
    public static final MapCodec<RadarAltimeterBlock> CODEC = simpleCodec(RadarAltimeterBlock::new);
    // Matches the Blockbench cuboids of the v2 model (docs/visual-design-guide.md): brass body,
    // lens bezel and the downward lens, whose lower face stays centred at (8, 0, 8), exactly where
    // RadarAltimeterScanner starts its local downward casts.
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 2, 0, 16, 16, 16),
            Block.box(2, 1, 2, 14, 2, 14),
            Block.box(4, 0, 4, 12, 1, 12)).optimize();

    public RadarAltimeterBlock(Properties properties) {
        super(properties);
    }

    @Override public boolean isSignalSource(BlockState state) { return true; }
    @Override public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof RadarAltimeterBlockEntity sensor ? sensor.outputPower(direction) : 0;
    }
    @Override public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) { return 0; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof RadarAltimeterBlockEntity sensor) {
            SensorOutputNetwork.open(serverPlayer, pos, "altimeter", 90, sensor.outputConfig());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RadarAltimeterBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public Vec3 cableSocket(BlockState state, Direction face) {
        // Centred on every face of the v2 body; the lower lead lands on the lens centre.
        return (switch (face) {
            case NORTH -> new Vec3(8, 9, 0);
            case SOUTH -> new Vec3(8, 9, 16);
            case WEST -> new Vec3(0, 9, 8);
            case EAST -> new Vec3(16, 9, 8);
            case DOWN -> new Vec3(8, 0, 8);
            case UP -> new Vec3(8, 16, 8);
        }).scale(1.0 / 16);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, ModBlocks.RADAR_ALTIMETER_ENTITY.get(),
                RadarAltimeterBlockEntity::serverTick);
    }
}
