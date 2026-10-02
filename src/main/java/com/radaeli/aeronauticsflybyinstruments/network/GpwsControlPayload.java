package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GpwsControlPayload(BlockPos position, String action, String value) implements CustomPacketPayload {
    public static final Type<GpwsControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "gpws_control"));
    public static final StreamCodec<ByteBuf, GpwsControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GpwsControlPayload::position,
            ByteBufCodecs.STRING_UTF8, GpwsControlPayload::action,
            ByteBufCodecs.STRING_UTF8, GpwsControlPayload::value,
            GpwsControlPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
