package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsModuleData;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FmsScreenPayload(BlockPos position, int column, int row, FmsModuleData data,
                               String dimension, double currentX, double currentY, double currentZ,
                               int navigatorDisplaySlot, String navigatorBoundId) implements CustomPacketPayload {
    public static final Type<FmsScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "fms_screen"));
    public static final StreamCodec<ByteBuf, FmsScreenPayload> STREAM_CODEC = StreamCodec.of(
            FmsScreenPayload::encode, FmsScreenPayload::decode);

    public FmsScreenPayload {
        position = position.immutable();
        data = data.copy();
    }

    private static void encode(ByteBuf buf, FmsScreenPayload payload) {
        FmsNetworkWire.writePosition(buf, payload.position);
        ByteBufCodecs.VAR_INT.encode(buf, payload.column);
        ByteBufCodecs.VAR_INT.encode(buf, payload.row);
        FmsNetworkWire.writeData(buf, payload.data);
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.dimension);
        buf.writeDouble(payload.currentX);
        buf.writeDouble(payload.currentY);
        buf.writeDouble(payload.currentZ);
        ByteBufCodecs.VAR_INT.encode(buf, payload.navigatorDisplaySlot);
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.navigatorBoundId);
    }

    private static FmsScreenPayload decode(ByteBuf buf) {
        return new FmsScreenPayload(FmsNetworkWire.readPosition(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), FmsNetworkWire.readData(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
