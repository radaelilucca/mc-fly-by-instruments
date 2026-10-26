package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PanelActionPayload(BlockPos position, int column, int row, String action,
                                String source, String channel, int severity, int volume,
                                String testChannel, double testThreshold, java.util.UUID interactionToken)
        implements CustomPacketPayload {
    public static final Type<PanelActionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "panel_action"));
    public static final StreamCodec<ByteBuf, PanelActionPayload> STREAM_CODEC = StreamCodec.of(
            PanelActionPayload::encode, PanelActionPayload::decode);
    public PanelActionPayload { position = position.immutable(); }
    private static void encode(ByteBuf buf, PanelActionPayload p) {
        FmsNetworkWire.writePosition(buf, p.position);
        buf.writeLong(p.interactionToken.getMostSignificantBits());
        buf.writeLong(p.interactionToken.getLeastSignificantBits());
        ByteBufCodecs.VAR_INT.encode(buf, p.column); ByteBufCodecs.VAR_INT.encode(buf, p.row);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.action); ByteBufCodecs.STRING_UTF8.encode(buf, p.source);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.channel); ByteBufCodecs.VAR_INT.encode(buf, p.severity);
        ByteBufCodecs.VAR_INT.encode(buf, p.volume); ByteBufCodecs.STRING_UTF8.encode(buf, p.testChannel);
        buf.writeDouble(p.testThreshold);
    }
    private static PanelActionPayload decode(ByteBuf buf) {
        var position = FmsNetworkWire.readPosition(buf);
        var token = new java.util.UUID(buf.readLong(), buf.readLong());
        return new PanelActionPayload(position, ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), buf.readDouble(), token);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
