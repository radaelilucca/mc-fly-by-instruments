package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Cut one wire with the wrench: the connector that draws it and the wire id. */
public record CutWirePayload(BlockPos holder, UUID route) implements CustomPacketPayload {
    public static final Type<CutWirePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "cut_wire"));
    public static final StreamCodec<ByteBuf, CutWirePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CutWirePayload::holder, UUIDUtil.STREAM_CODEC, CutWirePayload::route,
            CutWirePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
