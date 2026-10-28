package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Wiring View asks for the network state behind the connectors it is drawing. */
public record WiringStatusRequestPayload(List<BlockPos> connectors) implements CustomPacketPayload {
    public static final int MAX_CONNECTORS = 128;
    public static final Type<WiringStatusRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "wiring_status_request"));
    public static final StreamCodec<ByteBuf, WiringStatusRequestPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_CONNECTORS)), WiringStatusRequestPayload::connectors,
            WiringStatusRequestPayload::new);

    public WiringStatusRequestPayload {
        connectors = List.copyOf(connectors.subList(0, Math.min(MAX_CONNECTORS, connectors.size())));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
