package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.panel.PanelGrid;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;

import java.util.List;

public final class PanelNetwork {
    private static final String VERTICAL_KEY = "AeronauticsFlyByInstrumentsLamp2x1Vertical";
    private static boolean clientVertical;
    private static final List<String> CHANNELS = List.of("gpws.any", "gpws.terrain", "gpws.pull_up",
            "gpws.sink_rate", "gpws.dont_sink", "gpws.too_low", "gpws.bank_angle", "gpws.sensor_fail");
    private PanelNetwork() { }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(SetLampOrientationPayload.TYPE, SetLampOrientationPayload.STREAM_CODEC,
                (payload, context) -> {
                    var player = context.player();
                    var held = player.getMainHandItem();
                    if (!payload.offhand() && held.is(ModBlocks.LAMP_MODULE_2X1.get()))
                        player.getPersistentData().putBoolean(VERTICAL_KEY, payload.vertical());
                });
        registrar.playToClient(SyncLampOrientationPayload.TYPE, SyncLampOrientationPayload.STREAM_CODEC,
                (payload, context) -> clientVertical = payload.vertical());
        registrar.playToServer(PanelActionPayload.TYPE, PanelActionPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) handle(payload, player);
                }));
        registrar.playToClient(PanelScreenPayload.TYPE, PanelScreenPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (FMLEnvironment.dist == Dist.CLIENT)
                        com.radaeli.aeronauticsflybyinstruments.client.PanelClient.open(payload);
                }));
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

    public static void open(ServerPlayer player, ModularTableBlockEntity table, PanelGrid.Module module) {
        if (!canReach(player, table)) return;
        boolean general = module == null;
        String channel = general ? "gpws.any" : module.outputSource().equals("test")
                ? module.channel() : module.alertChannel();
        String source = general ? "gpws" : module.outputSource();
        int severity = general ? 2 : module.minimumSeverity();
        int volume = general ? 100 : module.buzzerVolume();
        int active = (int) table.grid().modules().stream().filter(m -> ModularTableBlockEntity.isLampModule(m)
                || m.type().equals("buzzer")).filter(table::isModuleActive).count();
        int total = (int) table.grid().modules().stream().filter(m -> ModularTableBlockEntity.isLampModule(m)
                || m.type().equals("buzzer") || m.type().equals("fms")).count();
        var status = general ? table.alertStatusId() : "";
        String summary = general ? table.grid().modules().stream().filter(m -> ModularTableBlockEntity.isLampModule(m)
                || m.type().equals("buzzer") || m.type().equals("fms")).map(m -> m.type() + "," + m.column() + "," + m.row() + ","
                + table.isModuleActive(m)).collect(java.util.stream.Collectors.joining(";")) : "";
        PacketDistributor.sendToPlayer(player, new PanelScreenPayload(table.getBlockPos(),
                general ? -1 : module.column(), general ? -1 : module.row(),
                general ? "" : module.type(), source, channel, severity, volume,
                general ? "test" : module.channel(), general ? 0.5 : module.threshold(), status,
                table.muted(), general ? total : 1,
                general ? active : table.isModuleActive(module) ? 1 : 0, summary,
                general ? table.interactionToken() : table.moduleToken(module.column(), module.row())));
    }

    private static boolean canReach(ServerPlayer player, ModularTableBlockEntity table) {
        return com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction.canReach(player, table);
    }

    private static void handle(PanelActionPayload p, ServerPlayer player) {
        if (p.column() < -1 || p.column() >= PanelGrid.SIZE || p.row() < -1 || p.row() >= PanelGrid.SIZE
                || (p.column() == -1) != (p.row() == -1)
                || !player.level().hasChunkAt(p.position())
                || !(player.level().getBlockEntity(p.position()) instanceof ModularTableBlockEntity table)
                || !canReach(player, table)) return;
        boolean generalPanel = p.column() == -1;
        boolean tableAction = generalPanel || p.action().equals("open_module");
        var expectedToken = tableAction ? table.interactionToken() : table.moduleToken(p.column(), p.row());
        if (expectedToken == null || !expectedToken.equals(p.interactionToken())) return;
        switch (p.action()) {
            case "save" -> {
                if (generalPanel) return;
                boolean validChannel = p.source().equals("gpws") ? CHANNELS.contains(p.channel())
                        : p.testChannel() != null && !p.testChannel().isBlank() && p.testChannel().length() <= 64;
                if (!validChannel || !(p.source().equals("gpws") || p.source().equals("test"))
                        || p.severity() < 1 || p.severity() > 3 || p.volume() < 0 || p.volume() > 100
                        || p.source().equals("test") && !Double.isFinite(p.testThreshold())) return;
                table.saveAlertDraft(p.column(), p.row(), p.source(), p.channel(), p.severity(),
                        p.testChannel(), p.testThreshold(), p.volume());
            }
            case "basic" -> {
                if (!generalPanel) return;
                table.applyBasicSetup();
            }
            case "test" -> { if (generalPanel) table.testPanel(); }
            case "test_buzzer" -> {
                if (generalPanel || p.volume() < 0 || p.volume() > 100
                        || table.grid().at(p.column(), p.row()).filter(m -> m.type().equals("buzzer")).isEmpty()) return;
                table.testBuzzer(p.column(), p.row(), p.volume());
            }
            case "mute" -> { if (generalPanel) table.setMuted(true); }
            case "unmute" -> { if (generalPanel) table.setMuted(false); }
            case "refresh" -> { if (generalPanel) open(player, table, null); }
            case "open_module" -> {
                if (generalPanel) return;
                table.grid().at(p.column(), p.row()).ifPresent(module -> {
                    if (ModularTableBlockEntity.isLampModule(module) || module.type().equals("buzzer"))
                        open(player, table, module);
                    else if (module.type().equals("fms"))
                        FmsNetwork.open(player, table.getBlockPos(), module.column(), module.row(), table);
                });
            }
            default -> { }
        }
        if (generalPanel && List.of("basic", "test", "mute", "unmute").contains(p.action()))
            open(player, table, null);
    }
}
