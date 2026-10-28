package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Screen request: {@code refresh}, {@code test}, {@code sensitivity} (cycle) or {@code inhibit} (toggle a function). */
public record AvionicsComputerControlPayload(BlockPos position, String action, String value)
        implements CustomPacketPayload {
    public static final Type<AvionicsComputerControlPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "avionics_computer_control"));
    public static final StreamCodec<ByteBuf, AvionicsComputerControlPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AvionicsComputerControlPayload::position,
            ByteBufCodecs.STRING_UTF8, AvionicsComputerControlPayload::action,
            ByteBufCodecs.STRING_UTF8, AvionicsComputerControlPayload::value,
            AvionicsComputerControlPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
