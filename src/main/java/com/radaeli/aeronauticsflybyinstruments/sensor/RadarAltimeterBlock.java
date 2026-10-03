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
    // Matches the Blockbench cuboids. The antenna's lower face remains at (8, 0, 8),
    // exactly where RadarAltimeterScanner starts its local downward casts.
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 3, 1, 15, 14, 15),
            Block.box(1, 14, 1, 15, 14.5, 15),
            Block.box(0.5, 14.5, 0.5, 15.5, 15.5, 15.5),
            Block.box(4, 15.5, 5, 12, 15.75, 11),
            Block.box(1, 1, 1, 15, 3, 15),
            Block.box(3, 0, 3, 13, 1, 13),
            Block.box(2.5, 8.5, 0.5, 8.5, 13.5, 1),
            Block.box(3.5, 9, 0, 7.5, 13, 0.5),
            Block.box(10, 5, 0.5, 14, 12.5, 1),
            Block.box(10.75, 9.75, 0, 13.25, 12.25, 0.5),
            Block.box(10.75, 5.25, 0, 13.25, 7.75, 0.5),
            Block.box(0, 12.5, 3, 1, 14, 13),
            Block.box(15, 12.5, 3, 16, 14, 13)).optimize();

    public RadarAltimeterBlock(Properties properties) {
        super(properties);
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
        // NORTH uses the existing 4x4 data socket, not the decorative TX/RX pair.
        // The top lead lands on the lid, clear of the raised identity badge.
        return (switch (face) {
            case NORTH -> new Vec3(5.5, 11, 0);
            case SOUTH -> new Vec3(8, 8, 15);
            case WEST -> new Vec3(1, 8, 8);
            case EAST -> new Vec3(15, 8, 8);
            case DOWN -> new Vec3(8, 0, 8);
            case UP -> new Vec3(8, 15.5, 3);
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
