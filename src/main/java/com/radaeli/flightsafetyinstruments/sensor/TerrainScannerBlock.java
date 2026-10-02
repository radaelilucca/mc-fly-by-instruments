package com.radaeli.flightsafetyinstruments.sensor;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** Fixed 9-ray Terrain Scanner block. */
public final class TerrainScannerBlock extends AbstractTerrainScannerBlock {
    public static final MapCodec<TerrainScannerBlock> CODEC = simpleCodec(TerrainScannerBlock::new);

    public TerrainScannerBlock(Properties properties) { super(properties); }

    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
}
