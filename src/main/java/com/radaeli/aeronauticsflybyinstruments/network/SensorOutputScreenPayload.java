package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SensorOutputScreenPayload(BlockPos position, String kind, int openingAngle, String side,
                                        SensorOutputConfig config) implements CustomPacketPayload {
    public static final Type<SensorOutputScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "sensor_output_screen"));
    public static final StreamCodec<ByteBuf, SensorOutputScreenPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> SensorOutputConfigWire.encodePayload(buf, payload.position, payload.kind,
                    payload.openingAngle, payload.side, payload.config),
            buf -> {
                var fields = SensorOutputConfigWire.decodePayload(buf);
                return new SensorOutputScreenPayload(fields.position(), fields.kind(), fields.angle(), fields.side(),
                        fields.config());
            });
    public SensorOutputScreenPayload {
        position = position.immutable();
        config = config.copy();
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
