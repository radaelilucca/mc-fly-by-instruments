package com.radaeli.flightsafetyinstruments.avionics;

import com.radaeli.flightsafetyinstruments.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Network topology endpoint and holder of the bridge's independent CC:Tweaked peripheral. */
public final class NetworkBridgeBlockEntity extends BlockEntity implements AvionicsEndpoint {
    private final NetworkBridgePeripheral peripheral = new NetworkBridgePeripheral(this);

    public NetworkBridgeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.NETWORK_BRIDGE_ENTITY.get(), pos, state);
    }

    public NetworkBridgePeripheral peripheral() {
        return peripheral;
    }

    @Override
    public String avionicsType() {
        return "network_bridge";
    }
}
