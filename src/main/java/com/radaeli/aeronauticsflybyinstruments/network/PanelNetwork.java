package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.panel.PanelGrid;
import com.radaeli.aeronauticsflybyinstruments.panel.TablePanel;
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
        registrar.playToServer(PanelScrollPayload.TYPE, PanelScrollPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) scroll(payload, player);
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

    /**
     * Opens the panel of every table joined to {@code table}. {@code placed} (panel frame) selects one
     * module; its column/row are what the screen sends back, resolved again by {@link #handle}.
     */
    public static void open(ServerPlayer player, ModularTableBlockEntity table, TablePanel.Placed placed) {
        if (!canReach(player, table)) return;
        boolean general = placed == null;
        var module = general ? null : placed.module();
        var listed = TablePanel.of(table).modules().stream().filter(m -> ModularTableBlockEntity.isLampModule(m.module())
                || m.module().type().equals("buzzer") || m.module().type().equals("fms")).toList();
        var lit = listed.stream().filter(m -> ModularTableBlockEntity.isLampModule(m.module())
                && m.owner().isModuleActive(m.module())).toList();
        int active = (int) listed.stream().filter(m -> m.owner().isModuleActive(m.module())).count();
        String summary = general ? lit.stream().map(m -> m.module().type() + "," + m.column() + "," + m.row() + ","
                + m.owner().indication(m.module()) + ","
                + m.owner().lampColor(m.module()).name().toLowerCase(java.util.Locale.ROOT)).collect(java.util.stream.Collectors.joining(";")) : "";
        PacketDistributor.sendToPlayer(player, new PanelScreenPayload(table.getBlockPos(),
                general ? -1 : placed.column(), general ? -1 : placed.row(),
                general ? "" : module.type(), general ? "" : placed.owner().indication(module),
                general ? 100 : module.buzzerVolume(), table.panelStatusId(), table.muted(),
                general ? listed.size() : 1,
                general ? active : placed.owner().isModuleActive(module) ? 1 : 0, summary,
                general ? table.interactionToken() : placed.owner().moduleToken(module.column(), module.row())));
    }

    /** Wrench scroll: lamps cycle the main indications, buzzers step the volume by 10%. */
    private static void scroll(PanelScrollPayload p, ServerPlayer player) {
        if (p.direction() == 0 || !player.level().hasChunkAt(p.position())
                || !com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock.isCreateWrench(
                        player.getMainHandItem())
                || !(player.level().getBlockEntity(p.position()) instanceof ModularTableBlockEntity table)
                || !canReach(player, table)) return;
        var placed = com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction
                .target(table, p.face(), p.hit()).module();
        if (placed == null) return;
        var module = placed.module();
        var owner = placed.owner();
        if (ModularTableBlockEntity.isLampModule(module)) {
            owner.setIndication(module.column(), module.row(), com.radaeli.aeronauticsflybyinstruments.panel
                    .LampIndication.next("lamp", owner.indication(module), p.direction()));
        } else if (module.type().equals("buzzer")) {
            owner.setBuzzerVolume(module.column(), module.row(),
                    Math.clamp(module.buzzerVolume() + (p.direction() > 0 ? 10 : -10), 0, 100));
        }
    }

    private static boolean canReach(ServerPlayer player, ModularTableBlockEntity table) {
        return com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction.canReach(player, table);
    }

    private static void handle(PanelActionPayload p, ServerPlayer player) {
        int limit = PanelGrid.SIZE * TablePanel.MAX_TABLES;
        if (p.column() < -1 || p.column() >= limit || p.row() < -1 || p.row() >= limit
                || (p.column() == -1) != (p.row() == -1)
                || !player.level().hasChunkAt(p.position())
                || !(player.level().getBlockEntity(p.position()) instanceof ModularTableBlockEntity clicked)
                || !canReach(player, clicked)) return;
        boolean generalPanel = p.column() == -1;
        boolean tableAction = generalPanel || p.action().equals("open_module");
        // Column/row are in the panel frame; resolve them to the owning table and its own cells.
        var panel = TablePanel.of(clicked);
        var placed = generalPanel ? null : panel.at(p.column(), p.row()).orElse(null);
        if (!generalPanel && placed == null) return;
        var table = generalPanel ? clicked : placed.owner();
        int column = generalPanel ? -1 : placed.module().column(), row = generalPanel ? -1 : placed.module().row();
        var expectedToken = tableAction ? clicked.interactionToken() : table.moduleToken(column, row);
        if (expectedToken == null || !expectedToken.equals(p.interactionToken())) return;
        switch (p.action()) {
            case "indication" -> {
                if (generalPanel) return;
                table.setIndication(column, row, p.channel());
                open(player, clicked, panel.at(p.column(), p.row()).orElse(null));
            }
            case "volume" -> {
                if (generalPanel || p.volume() < 0 || p.volume() > 100) return;
                table.setBuzzerVolume(column, row, p.volume());
            }
            case "test" -> { if (generalPanel) panel.testPanel(); }
            case "test_buzzer" -> {
                if (generalPanel || p.volume() < 0 || p.volume() > 100
                        || !placed.module().type().equals("buzzer")) return;
                table.testBuzzer(column, row, p.volume());
            }
            case "mute" -> { if (generalPanel) panel.setMuted(true); }
            case "unmute" -> { if (generalPanel) panel.setMuted(false); }
            case "refresh" -> { if (generalPanel) open(player, clicked, null); }
            case "open_module" -> {
                if (generalPanel) return;
                var module = placed.module();
                if (ModularTableBlockEntity.isLampModule(module) || module.type().equals("buzzer"))
                    open(player, clicked, placed);
                else if (module.type().equals("fms"))
                    FmsNetwork.open(player, table.getBlockPos(), column, row, table);
            }
            default -> { }
        }
        if (generalPanel && List.of("test", "mute", "unmute").contains(p.action()))
            open(player, clicked, null);
    }
}
