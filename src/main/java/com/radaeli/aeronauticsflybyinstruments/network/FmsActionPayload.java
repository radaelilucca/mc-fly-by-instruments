package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record FmsActionPayload(BlockPos position, int column, int row, String action, String waypointId,
                               String name, double x, double y, double z, int navigatorDisplaySlot,
                               boolean confirmRebind) implements CustomPacketPayload {
    public static final Type<FmsActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "fms_action"));
    public static final StreamCodec<ByteBuf, FmsActionPayload> STREAM_CODEC = StreamCodec.of(
            FmsActionPayload::encode, FmsActionPayload::decode);

    public FmsActionPayload {
        position = position.immutable();
        action = action == null ? "" : action;
        waypointId = waypointId == null ? "" : waypointId;
        name = name == null ? "" : name;
    }

    private static void encode(ByteBuf buf, FmsActionPayload payload) {
        FmsNetworkWire.writePosition(buf, payload.position);
        ByteBufCodecs.VAR_INT.encode(buf, payload.column);
        ByteBufCodecs.VAR_INT.encode(buf, payload.row);
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.action);
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.waypointId);
        ByteBufCodecs.STRING_UTF8.encode(buf, payload.name);
        buf.writeDouble(payload.x);
        buf.writeDouble(payload.y);
        buf.writeDouble(payload.z);
        ByteBufCodecs.VAR_INT.encode(buf, payload.navigatorDisplaySlot);
        buf.writeBoolean(payload.confirmRebind);
    }

    private static FmsActionPayload decode(ByteBuf buf) {
        return new FmsActionPayload(FmsNetworkWire.readPosition(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                buf.readDouble(), buf.readDouble(), buf.readDouble(), ByteBufCodecs.VAR_INT.decode(buf),
                buf.readBoolean());
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
