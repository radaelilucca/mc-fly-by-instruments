package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.GpwsControlPayload;
import com.radaeli.aeronauticsflybyinstruments.network.GpwsScreenPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Small game-facing setup panel; detailed channel tuning remains optional via CC:Tweaked. */
public final class GpwsComputerScreen extends Screen {
    private static final int PANEL_WIDTH = 280;
    private static final int PANEL_HEIGHT = 206;
    private BlockPos position;
    private String profile;
    private String sensitivity;
    private String mode;
    private String phase;
    private String networkStatus;

    public GpwsComputerScreen(GpwsScreenPayload payload) {
        super(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.title"));
        accept(payload);
    }

    public BlockPos position() { return position; }

    public void accept(GpwsScreenPayload payload) {
        position = payload.position().immutable();
        profile = payload.profile();
        sensitivity = payload.sensitivity();
        mode = payload.mode();
        phase = payload.phase();
        networkStatus = payload.networkStatus();
        if (minecraft != null) init();
    }

    @Override protected void init() {
        clearWidgets();
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int pairWidth = 124;
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.profile_basic"),
                        button -> send("profile", "basic"))
                .bounds(left + 12, top + 34, pairWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.profile_advanced"),
                        button -> send("profile", "advanced"))
                .bounds(left + PANEL_WIDTH - 12 - pairWidth, top + 34, pairWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.enable"),
                        button -> send("enable", ""))
                .bounds(left + 12, top + 60, 124, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.disable"),
                        button -> send("disable", ""))
                .bounds(left + PANEL_WIDTH - 136, top + 60, 124, 20).build());
        addRenderableWidget(Button.builder(Component.literal("−"), button -> adjustSensitivity(-1))
                .bounds(left + 32, top + 86, 28, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> adjustSensitivity(1))
                .bounds(left + PANEL_WIDTH - 60, top + 86, 28, 20).build());
        if (profile.equals("advanced")) {
            addRenderableWidget(modeButton("arm", "armed", left + 13, top + 112, 78));
            addRenderableWidget(modeButton("standby", "standby", left + 101, top + 112, 78));
            addRenderableWidget(modeButton("test", "test", left + 189, top + 112, 78));
        } else {
            addRenderableWidget(modeButton("test", "test", left + (PANEL_WIDTH - 78) / 2, top + 112, 78));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.close"),
                        button -> onClose())
                .bounds(left + (PANEL_WIDTH - 88) / 2, top + 178, 88, 20).build());
    }

    private Button modeButton(String label, String value, int x, int y, int buttonWidth) {
        return Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws." + label),
                button -> send("mode", value)).bounds(x, y, buttonWidth, 20).build();
    }

    private void adjustSensitivity(int delta) {
        List<String> values = List.of("low", "normal", "high");
        int index = values.indexOf(sensitivity);
        send("sensitivity", values.get(Math.floorMod(index + delta, values.size())));
    }

    private void send(String action, String value) {
        PacketDistributor.sendToServer(new GpwsControlPayload(position, action, value));
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101418);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF6F8793);
        graphics.drawCenteredString(font, title, width / 2, top + 10, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("gui.aeronautics_fly_by_instruments.gpws.profile_status",
                        Component.translatable("gui.aeronautics_fly_by_instruments.gpws.profile_" + profile)),
                width / 2, top + 23, 0xFF9EABB0);
        graphics.drawCenteredString(font,
                Component.translatable("gui.aeronautics_fly_by_instruments.gpws.sensitivity_" + sensitivity),
                width / 2, top + 91, 0xFFE5F1F5);
        graphics.drawCenteredString(font,
                Component.translatable("gui.aeronautics_fly_by_instruments.gpws.status",
                        Component.translatable("gui.aeronautics_fly_by_instruments.gpws.mode." + mode),
                        Component.translatable("gui.aeronautics_fly_by_instruments.gpws.phase." + phase)),
                width / 2, top + 143, 0xFFE5F1F5);
        graphics.drawCenteredString(font,
                Component.translatable("gui.aeronautics_fly_by_instruments.gpws.network",
                        Component.translatable("gui.aeronautics_fly_by_instruments.gpws.network." + networkStatus)),
                width / 2, top + 158, 0xFF9EABB0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
