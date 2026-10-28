package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock;
import com.radaeli.aeronauticsflybyinstruments.network.PanelNetwork;
import com.radaeli.aeronauticsflybyinstruments.network.PanelScrollPayload;
import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction;
import com.radaeli.aeronauticsflybyinstruments.panel.TablePanel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PanelInputHandler {
    private PanelInputHandler() {}

    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (event.getScrollDeltaY() == 0) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !(player.level().getBlockEntity(hit.getBlockPos()) instanceof ModularTableBlockEntity table)) return;
        var target = TableInteraction.target(table, hit.getDirection(), hit.getLocation());

        // Wrench over a lamp or buzzer: configure it in the world (plan §5.4).
        if (ModularTableBaseBlock.isCreateWrench(player.getMainHandItem()) && configurable(target.module())) {
            PacketDistributor.sendToServer(new PanelScrollPayload(hit.getBlockPos(), hit.getDirection(),
                    hit.getLocation(), event.getScrollDeltaY() > 0 ? 1 : -1));
            event.setCanceled(true);
            return;
        }

        if (!target.exposed()) return;
        var held = TableInteraction.heldModule(player).orElse(null);
        if (held == null || !held.type().equals("lamp_2x1")) return;

        PanelNetwork.rotateClient(false);
        event.setCanceled(true);
    }

    static boolean configurable(TablePanel.Placed placed) {
        return placed != null && (ModularTableBlockEntity.isLampModule(placed.module())
                || placed.module().type().equals("buzzer"));
    }
}
