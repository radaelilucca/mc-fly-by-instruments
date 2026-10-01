package com.radaeli.flightsafetyinstruments.network;

import com.radaeli.flightsafetyinstruments.block.ModBlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class PanelNetwork {
    private static final String VERTICAL_KEY = "AeronauticsFlyByInstrumentsLamp2x1Vertical";
    private static boolean clientVertical;

    private PanelNetwork() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(SetLampOrientationPayload.TYPE,
                SetLampOrientationPayload.STREAM_CODEC, (payload, context) -> {
                    var player = context.player();
                    var held = payload.offhand() ? player.getOffhandItem() : player.getMainHandItem();
                    if (held.is(ModBlocks.LAMP_MODULE_2X1.get())) {
                        player.getPersistentData().putBoolean(VERTICAL_KEY, payload.vertical());
                    }
                });
        registrar.playToClient(SyncLampOrientationPayload.TYPE, SyncLampOrientationPayload.STREAM_CODEC,
                (payload, context) -> clientVertical = payload.vertical());
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player,
                    new SyncLampOrientationPayload(player.getPersistentData().getBoolean(VERTICAL_KEY)));
        }
    }

    public static boolean isVertical(Player player) {
        if (player == null) return false;
        return player.level().isClientSide ? clientVertical : player.getPersistentData().getBoolean(VERTICAL_KEY);
    }

    public static boolean rotateClient(boolean offhand) {
        clientVertical = !clientVertical;
        PacketDistributor.sendToServer(new SetLampOrientationPayload(offhand, clientVertical));
        return clientVertical;
    }
}
