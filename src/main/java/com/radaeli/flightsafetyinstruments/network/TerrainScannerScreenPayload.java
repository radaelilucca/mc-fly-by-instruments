package com.radaeli.flightsafetyinstruments.network;

import com.radaeli.flightsafetyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Opens or updates the Advanced Terrain Scanner config screen on the owning client. */
public record TerrainScannerScreenPayload(BlockPos position, int openingAngle) implements CustomPacketPayload {
    public static final Type<TerrainScannerScreenPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeronauticsFlyByInstruments.MOD_ID, "terrain_scanner_screen"));
    public static final StreamCodec<ByteBuf, TerrainScannerScreenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerrainScannerScreenPayload::position,
            ByteBufCodecs.VAR_INT, TerrainScannerScreenPayload::openingAngle,
            TerrainScannerScreenPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
