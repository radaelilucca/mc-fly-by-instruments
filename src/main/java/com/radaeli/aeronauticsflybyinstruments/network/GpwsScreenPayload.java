package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GpwsScreenPayload(BlockPos position, String profile, String sensitivity, String mode,
                                String phase, String networkStatus) implements CustomPacketPayload {
    public static final Type<GpwsScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "gpws_screen"));
    public static final StreamCodec<ByteBuf, GpwsScreenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GpwsScreenPayload::position,
            ByteBufCodecs.STRING_UTF8, GpwsScreenPayload::profile,
            ByteBufCodecs.STRING_UTF8, GpwsScreenPayload::sensitivity,
            ByteBufCodecs.STRING_UTF8, GpwsScreenPayload::mode,
            ByteBufCodecs.STRING_UTF8, GpwsScreenPayload::phase,
            ByteBufCodecs.STRING_UTF8, GpwsScreenPayload::networkStatus,
            GpwsScreenPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
