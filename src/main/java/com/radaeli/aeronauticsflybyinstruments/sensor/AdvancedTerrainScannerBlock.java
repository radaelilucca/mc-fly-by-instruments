package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.mojang.serialization.MapCodec;
import com.radaeli.aeronauticsflybyinstruments.block.CableSocketProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Configurable Advanced Terrain Scanner block. */
public final class AdvancedTerrainScannerBlock extends AbstractTerrainScannerBlock implements CableSocketProvider {
    public static final MapCodec<AdvancedTerrainScannerBlock> CODEC = simpleCodec(AdvancedTerrainScannerBlock::new);

    public AdvancedTerrainScannerBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return TerrainScannerBlock.housingShape(state);
    }

    @Override
    public Vec3 cableSocket(BlockState state, Direction face) {
        return TerrainScannerBlock.housingSocket(state, face);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TerrainScannerBlockEntity scanner) {
            com.radaeli.aeronauticsflybyinstruments.network.SensorOutputNetwork.open(serverPlayer, pos, "advanced",
                    scanner.openingAngle(), scanner.outputConfig());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
