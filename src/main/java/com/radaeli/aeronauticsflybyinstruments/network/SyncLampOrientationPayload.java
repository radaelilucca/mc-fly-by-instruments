package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncLampOrientationPayload(boolean vertical) implements CustomPacketPayload {
    public static final Type<SyncLampOrientationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeronauticsFlyByInstruments.MOD_ID, "sync_lamp_orientation"));
    public static final StreamCodec<ByteBuf, SyncLampOrientationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SyncLampOrientationPayload::vertical,
            SyncLampOrientationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
