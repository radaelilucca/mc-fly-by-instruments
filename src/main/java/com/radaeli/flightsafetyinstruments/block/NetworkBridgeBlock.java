package com.radaeli.flightsafetyinstruments.block;

import com.mojang.serialization.MapCodec;
import com.radaeli.flightsafetyinstruments.avionics.NetworkBridgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Physical CC:Tweaked gateway for querying an Avionics Cable network. */
public final class NetworkBridgeBlock extends BaseEntityBlock {
    public static final MapCodec<NetworkBridgeBlock> CODEC = simpleCodec(NetworkBridgeBlock::new);

    public NetworkBridgeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NetworkBridgeBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
