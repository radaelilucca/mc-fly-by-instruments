package com.radaeli.flightsafetyinstruments.network;

import com.radaeli.flightsafetyinstruments.sensor.TerrainScannerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class TerrainScannerNetwork {
    private TerrainScannerNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(TerrainScannerSetAnglePayload.TYPE, TerrainScannerSetAnglePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (!(context.player() instanceof ServerPlayer player)) return;
                    BlockPos pos = payload.position();
                    if (!player.level().hasChunkAt(pos)
                            || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0) return;
                    if (!(player.level().getBlockEntity(pos) instanceof TerrainScannerBlockEntity scanner)
                            || !scanner.advanced()
                            || payload.openingAngle() < 30 || payload.openingAngle() > 180) return;
                    scanner.setOpeningAngle(payload.openingAngle());
                    PacketDistributor.sendToPlayer(player,
                            new TerrainScannerScreenPayload(pos, scanner.openingAngle()));
                }));
        registrar.playToClient(TerrainScannerScreenPayload.TYPE, TerrainScannerScreenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        com.radaeli.flightsafetyinstruments.client.TerrainScannerClient
                                .openOrUpdate(payload.position(), payload.openingAngle());
                    }
                }));
    }

    public static void openConfiguration(ServerPlayer player, BlockPos position, int openingAngle) {
        PacketDistributor.sendToPlayer(player, new TerrainScannerScreenPayload(position, openingAngle));
    }
}
