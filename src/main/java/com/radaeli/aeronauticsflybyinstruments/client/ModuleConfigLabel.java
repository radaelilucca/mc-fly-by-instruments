package com.radaeli.aeronauticsflybyinstruments.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock;
import com.radaeli.aeronauticsflybyinstruments.panel.LampIndication;
import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.panel.TableInteraction;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * Floating label beside the crosshair while a Create wrench points at a lamp or buzzer (plan
 * §5.4): the current indication and the next one the scroll would pick, or the buzzer's volume.
 * Same panel language as the screens, smaller (visual guide §12).
 */
public final class ModuleConfigLabel {
    private static final float SCALE = 0.85f;
    private static final int WIDTH = 150;
    private static final String KEY = "gui.aeronautics_fly_by_instruments.panel.";

    private ModuleConfigLabel() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(AeronauticsFlyByInstruments.MOD_ID,
                "module_config_label"), ModuleConfigLabel::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null || minecraft.screen != null || minecraft.options.hideGui
                || !ModularTableBaseBlock.isCreateWrench(player.getMainHandItem())
                || !(minecraft.hitResult instanceof BlockHitResult hit)
                || !(player.level().getBlockEntity(hit.getBlockPos()) instanceof ModularTableBlockEntity table)) return;
        var placed = TableInteraction.target(table, hit.getDirection(), hit.getLocation()).module();
        if (!PanelInputHandler.configurable(placed)) return;
        var module = placed.module();
        var owner = placed.owner();
        boolean buzzer = module.type().equals("buzzer");
        String current = owner.indication(module);

        HudPanel panel = new HudPanel(WIDTH);
        String name = Component.translatable(KEY + "module." + module.type()).getString();
        panel.accent(LampIndication.color(current, 0) == LampIndication.Color.RED
                ? HudPanel.palette().error() : HudPanel.palette().warning());
        panel.header(name, Component.translatable(KEY + "scroll_hint").getString());
        panel.choice(PanelScreen.indicationLabel(current), "", true, true, null);
        String next = LampIndication.next(buzzer ? "buzzer" : "lamp", current, 1);
        if (!next.equals(current)) panel.choice(PanelScreen.indicationLabel(next), "", false, true, null);
        if (buzzer) panel.row(Component.translatable(KEY + "volume").getString(), module.buzzerVolume() + "%");

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(SCALE, SCALE, 1);
        int x = Math.round((graphics.guiWidth() / 2f + 12) / SCALE);
        int y = Math.round((graphics.guiHeight() / 2f - panel.height() * SCALE / 2) / SCALE);
        panel.draw(graphics, minecraft.font, x, y, -1, -1, null);
        pose.popPose();
    }
}
