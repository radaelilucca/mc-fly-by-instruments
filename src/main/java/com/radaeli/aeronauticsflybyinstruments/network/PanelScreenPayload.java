package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Table screen state. General panel: {@code column == -1}, {@code litSummary} lists the lit lamps as
 * {@code type,column,row,indication,color} joined by {@code ;}. Module screen: its type, indication and volume.
 */
public record PanelScreenPayload(BlockPos position, int column, int row, String moduleType, String indication,
                                 int volume, String status, boolean muted, int moduleCount, int activeCount,
                                 String litSummary, java.util.UUID interactionToken) implements CustomPacketPayload {
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
        ByteBufCodecs.STRING_UTF8.encode(buf, p.moduleType); ByteBufCodecs.STRING_UTF8.encode(buf, p.indication);
        ByteBufCodecs.VAR_INT.encode(buf, p.volume); ByteBufCodecs.STRING_UTF8.encode(buf, p.status);
        ByteBufCodecs.BOOL.encode(buf, p.muted); ByteBufCodecs.VAR_INT.encode(buf, p.moduleCount);
        ByteBufCodecs.VAR_INT.encode(buf, p.activeCount); ByteBufCodecs.STRING_UTF8.encode(buf, p.litSummary);
    }
    private static PanelScreenPayload decode(ByteBuf buf) {
        var position = FmsNetworkWire.readPosition(buf);
        var token = new java.util.UUID(buf.readLong(), buf.readLong());
        return new PanelScreenPayload(position, ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.STRING_UTF8.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), ByteBufCodecs.BOOL.decode(buf),
                ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                ByteBufCodecs.STRING_UTF8.decode(buf), token);
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
