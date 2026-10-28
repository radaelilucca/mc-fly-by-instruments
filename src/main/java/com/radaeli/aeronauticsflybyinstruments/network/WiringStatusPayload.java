package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** Network state per connector: {@code status} is a {@code NetworkStatus} ordinal, endpoints a total count. */
public record WiringStatusPayload(List<Entry> entries) implements CustomPacketPayload {
    public record Entry(BlockPos connector, int status, int endpoints) {
        static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Entry::connector, ByteBufCodecs.VAR_INT, Entry::status,
                ByteBufCodecs.VAR_INT, Entry::endpoints, Entry::new);
    }

    public static final Type<WiringStatusPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "wiring_status"));
    public static final StreamCodec<ByteBuf, WiringStatusPayload> STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list(WiringStatusRequestPayload.MAX_CONNECTORS)),
            WiringStatusPayload::entries, WiringStatusPayload::new);

    public WiringStatusPayload {
        entries = List.copyOf(entries);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
