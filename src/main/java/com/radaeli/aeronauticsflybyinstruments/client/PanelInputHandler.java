package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.PanelNetwork;
import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent;

public final class PanelInputHandler {
    private PanelInputHandler() {}

    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (event.getScrollDeltaY() == 0) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (minecraft.screen != null || player == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !(player.level().getBlockEntity(hit.getBlockPos()) instanceof ModularTableBlockEntity table)
                || !com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction
                .target(table, hit.getDirection(), hit.getLocation()).exposed()) return;

        var held = com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction.heldModule(player).orElse(null);
        if (held == null || !held.type().equals("lamp_2x1")) return;

        PanelNetwork.rotateClient(false);
        event.setCanceled(true);
    }
}
