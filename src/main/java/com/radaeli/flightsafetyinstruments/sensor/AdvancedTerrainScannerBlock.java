package com.radaeli.flightsafetyinstruments.sensor;

import com.mojang.serialization.MapCodec;
import com.radaeli.flightsafetyinstruments.network.TerrainScannerNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;

/** Configurable Advanced Terrain Scanner block. */
public final class AdvancedTerrainScannerBlock extends AbstractTerrainScannerBlock {
    public static final MapCodec<AdvancedTerrainScannerBlock> CODEC = simpleCodec(AdvancedTerrainScannerBlock::new);

    public AdvancedTerrainScannerBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TerrainScannerBlockEntity scanner) {
            TerrainScannerNetwork.openConfiguration(serverPlayer, pos, scanner.openingAngle());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
