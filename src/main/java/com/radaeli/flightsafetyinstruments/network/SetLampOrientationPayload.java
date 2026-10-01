package com.radaeli.flightsafetyinstruments.network;

import com.radaeli.flightsafetyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SetLampOrientationPayload(boolean offhand, boolean vertical) implements CustomPacketPayload {
    public static final Type<SetLampOrientationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeronauticsFlyByInstruments.MOD_ID, "set_lamp_orientation"));
    public static final StreamCodec<ByteBuf, SetLampOrientationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SetLampOrientationPayload::offhand,
            ByteBufCodecs.BOOL, SetLampOrientationPayload::vertical,
            SetLampOrientationPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
