package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ApplySensorOutputConfigPayload(BlockPos position, String kind, int openingAngle,
                                             SensorOutputConfig config) implements CustomPacketPayload {
    public static final Type<ApplySensorOutputConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "apply_sensor_output_config"));
    public static final StreamCodec<ByteBuf, ApplySensorOutputConfigPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> SensorOutputConfigWire.encodePayload(buf, payload.position, payload.kind,
                    payload.openingAngle, payload.config),
            buf -> {
                var fields = SensorOutputConfigWire.decodePayload(buf);
                return new ApplySensorOutputConfigPayload(fields.position(), fields.kind(), fields.angle(), fields.config());
            });
    public ApplySensorOutputConfigPayload {
        position = position.immutable();
        config = config.copy();
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
