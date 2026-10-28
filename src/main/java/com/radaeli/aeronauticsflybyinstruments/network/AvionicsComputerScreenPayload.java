package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Live snapshot for the Avionics Computer screen: overall state, sensors with roles and the GPWS
 * functions. The open screen asks for a fresh one every half second.
 */
public record AvionicsComputerScreenPayload(BlockPos position, String state, String alert, String sensitivity,
                                            String phase, String network, List<SensorLine> sensors,
                                            List<FunctionLine> functions) implements CustomPacketPayload {
    public static final Type<AvionicsComputerScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "avionics_computer_screen"));
    public static final StreamCodec<ByteBuf, AvionicsComputerScreenPayload> STREAM_CODEC = StreamCodec.of(
            AvionicsComputerScreenPayload::encode, AvionicsComputerScreenPayload::decode);
    private static final int MAX_LINES = 64;

    /** {@code value} is NaN when the sensor has no usable reading. */
    public record SensorLine(String type, String role, String status, double value, String issue) {}

    /** {@code state}: inactive, active, unavailable or inhibited; {@code reason} explains unavailable. */
    public record FunctionLine(String id, String state, String severity, String reason) {}

    public AvionicsComputerScreenPayload {
        position = position.immutable();
        sensors = List.copyOf(sensors);
        functions = List.copyOf(functions);
    }

    private static void encode(ByteBuf buf, AvionicsComputerScreenPayload p) {
        BlockPos.STREAM_CODEC.encode(buf, p.position);
        for (String value : List.of(p.state, p.alert, p.sensitivity, p.phase, p.network)) {
            ByteBufCodecs.STRING_UTF8.encode(buf, value);
        }
        ByteBufCodecs.VAR_INT.encode(buf, Math.min(MAX_LINES, p.sensors.size()));
        for (SensorLine line : p.sensors.subList(0, Math.min(MAX_LINES, p.sensors.size()))) {
            ByteBufCodecs.STRING_UTF8.encode(buf, line.type());
            ByteBufCodecs.STRING_UTF8.encode(buf, line.role());
            ByteBufCodecs.STRING_UTF8.encode(buf, line.status());
            buf.writeDouble(line.value());
            ByteBufCodecs.STRING_UTF8.encode(buf, line.issue());
        }
        ByteBufCodecs.VAR_INT.encode(buf, Math.min(MAX_LINES, p.functions.size()));
        for (FunctionLine line : p.functions.subList(0, Math.min(MAX_LINES, p.functions.size()))) {
            ByteBufCodecs.STRING_UTF8.encode(buf, line.id());
            ByteBufCodecs.STRING_UTF8.encode(buf, line.state());
            ByteBufCodecs.STRING_UTF8.encode(buf, line.severity());
            ByteBufCodecs.STRING_UTF8.encode(buf, line.reason());
        }
    }

    private static AvionicsComputerScreenPayload decode(ByteBuf buf) {
        BlockPos position = BlockPos.STREAM_CODEC.decode(buf);
        String state = ByteBufCodecs.STRING_UTF8.decode(buf);
        String alert = ByteBufCodecs.STRING_UTF8.decode(buf);
        String sensitivity = ByteBufCodecs.STRING_UTF8.decode(buf);
        String phase = ByteBufCodecs.STRING_UTF8.decode(buf);
        String network = ByteBufCodecs.STRING_UTF8.decode(buf);
        int sensorCount = Math.min(MAX_LINES, ByteBufCodecs.VAR_INT.decode(buf));
        List<SensorLine> sensors = new ArrayList<>();
        for (int index = 0; index < sensorCount; index++) {
            sensors.add(new SensorLine(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf), buf.readDouble(), ByteBufCodecs.STRING_UTF8.decode(buf)));
        }
        int functionCount = Math.min(MAX_LINES, ByteBufCodecs.VAR_INT.decode(buf));
        List<FunctionLine> functions = new ArrayList<>();
        for (int index = 0; index < functionCount; index++) {
            functions.add(new FunctionLine(ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf)));
        }
        return new AvionicsComputerScreenPayload(position, state, alert, sensitivity, phase, network, sensors,
                functions);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
