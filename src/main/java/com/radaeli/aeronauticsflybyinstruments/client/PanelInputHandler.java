package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.network.PanelNetwork;
import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.InputEvent;

public final class PanelInputHandler {
    private PanelInputHandler() {}

    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (event.getScrollDeltaY() == 0) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !(player.level().getBlockEntity(hit.getBlockPos()) instanceof ModularTableBlockEntity table)
                || hit.getDirection() != table.getBlockState().getValue(ModularTableBaseBlock.FACING)) return;

        boolean mainHand = player.getMainHandItem().is(ModBlocks.LAMP_MODULE_2X1.get());
        boolean offhand = player.getOffhandItem().is(ModBlocks.LAMP_MODULE_2X1.get());
        if (!mainHand && !offhand) return;

        PanelNetwork.rotateClient(!mainHand && offhand);
        event.setCanceled(true);
    }
}
