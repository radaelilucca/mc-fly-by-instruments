package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PanelScreenPayload(BlockPos position, int column, int row, String moduleType,
                                 String source, String channel, int severity, int volume,
                                 String testChannel, double testThreshold,
                                 String networkStatus, boolean muted, int moduleCount,
                                 int activeCount, String moduleSummary, java.util.UUID interactionToken) implements CustomPacketPayload {
    public static final Type<PanelScreenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "panel_screen"));
    public static final StreamCodec<ByteBuf, PanelScreenPayload> STREAM_CODEC = StreamCodec.of(
            PanelScreenPayload::encode, PanelScreenPayload::decode);

    public PanelScreenPayload { position = position.immutable(); }
    private static void encode(ByteBuf buf, PanelScreenPayload p) {
        FmsNetworkWire.writePosition(buf, p.position);
        buf.writeLong(p.interactionToken.getMostSignificantBits());
        buf.writeLong(p.interactionToken.getLeastSignificantBits());
        ByteBufCodecs.VAR_INT.encode(buf, p.column); ByteBufCodecs.VAR_INT.encode(buf, p.row);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.moduleType); ByteBufCodecs.STRING_UTF8.encode(buf, p.source);
        ByteBufCodecs.STRING_UTF8.encode(buf, p.channel); ByteBufCodecs.VAR_INT.encode(buf, p.severity);
        ByteBufCodecs.VAR_INT.encode(buf, p.volume); ByteBufCodecs.STRING_UTF8.encode(buf, p.testChannel);
        buf.writeDouble(p.testThreshold); ByteBufCodecs.STRING_UTF8.encode(buf, p.networkStatus);
        ByteBufCodecs.BOOL.encode(buf, p.muted); ByteBufCodecs.VAR_INT.encode(buf, p.moduleCount);
        ByteBufCodecs.VAR_INT.encode(buf, p.activeCount); ByteBufCodecs.STRING_UTF8.encode(buf, p.moduleSummary);
    }
    private static PanelScreenPayload decode(ByteBuf buf) {
        var position = FmsNetworkWire.readPosition(buf);
        var token = new java.util.UUID(buf.readLong(), buf.readLong());
        return new PanelScreenPayload(position, ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), buf.readDouble(),
                ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), token);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
