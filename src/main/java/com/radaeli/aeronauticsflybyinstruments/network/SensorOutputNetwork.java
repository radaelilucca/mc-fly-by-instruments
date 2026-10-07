package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.sensor.RadarAltimeterBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig;
import com.radaeli.aeronauticsflybyinstruments.sensor.TerrainScanGeometry;
import com.radaeli.aeronauticsflybyinstruments.sensor.TerrainScannerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SensorOutputNetwork {
    private SensorOutputNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(ApplySensorOutputConfigPayload.TYPE, ApplySensorOutputConfigPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> apply(payload, context.player())));
        registrar.playToClient(SensorOutputScreenPayload.TYPE, SensorOutputScreenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist == Dist.CLIENT) {
                        com.radaeli.aeronauticsflybyinstruments.client.SensorOutputClient.openOrUpdate(payload);
                    }
                }));
    }

    public static void open(ServerPlayer player, BlockPos pos, String kind, int angle,
                            SensorOutputConfig config) {
        PacketDistributor.sendToPlayer(player, new SensorOutputScreenPayload(pos, kind, angle, config));
    }

    private static void apply(ApplySensorOutputConfigPayload payload, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        BlockPos pos = payload.position();
        if (!serverPlayer.level().hasChunkAt(pos)
                || serverPlayer.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0) return;
        var entity = serverPlayer.level().getBlockEntity(pos);
        String kind = payload.kind();
        if (entity instanceof RadarAltimeterBlockEntity altimeter && kind.equals("altimeter")) {
            if (altimeter.validOutputConfig(payload.config())) altimeter.applyOutputConfig(payload.config());
            // The client owns the staged editor. Do not send another open-screen payload
            // after Apply/ESC: it can arrive after the screen has closed and trap the player.
        } else if (entity instanceof TerrainScannerBlockEntity scanner
                && kind.equals(scanner.advanced() ? "advanced" : "terrain")) {
            boolean validAngle = !scanner.advanced() || payload.openingAngle() >= TerrainScanGeometry.MIN_APERTURE_DEGREES
                    && payload.openingAngle() <= TerrainScanGeometry.MAX_APERTURE_DEGREES;
            if (validAngle && scanner.validOutputConfig(payload.config())) {
                if (scanner.advanced()) scanner.setOpeningAngle(payload.openingAngle());
                scanner.applyOutputConfig(payload.config());
            }
            open(serverPlayer, pos, kind, scanner.openingAngle(), scanner.outputConfig());
        }
    }
}
