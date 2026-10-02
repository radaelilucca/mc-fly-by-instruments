package com.radaeli.flightsafetyinstruments.network;

import com.radaeli.flightsafetyinstruments.AeronauticsFlyByInstruments;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TerrainScannerSetAnglePayload(BlockPos position, int openingAngle) implements CustomPacketPayload {
    public static final Type<TerrainScannerSetAnglePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(AeronauticsFlyByInstruments.MOD_ID, "terrain_scanner_set_angle"));
    public static final StreamCodec<ByteBuf, TerrainScannerSetAnglePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TerrainScannerSetAnglePayload::position,
            ByteBufCodecs.VAR_INT, TerrainScannerSetAnglePayload::openingAngle,
            TerrainScannerSetAnglePayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
