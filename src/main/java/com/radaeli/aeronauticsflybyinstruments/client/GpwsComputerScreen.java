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
    private static final int PANEL_WIDTH = 264;
    private static final int PANEL_HEIGHT = 184;
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
        int pairWidth = 116;
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.profile_basic"),
                        button -> send("profile", "basic"))
                .bounds(left + 10, top + 30, pairWidth, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.profile_advanced"),
                        button -> send("profile", "advanced"))
                .bounds(left + PANEL_WIDTH - 10 - pairWidth, top + 30, pairWidth, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.enable"),
                        button -> send("enable", ""))
                .bounds(left + 10, top + 53, 116, 18).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.disable"),
                        button -> send("disable", ""))
                .bounds(left + PANEL_WIDTH - 126, top + 53, 116, 18).build());
        addRenderableWidget(Button.builder(Component.literal("−"), button -> adjustSensitivity(-1))
                .bounds(left + 30, top + 76, 28, 18).build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> adjustSensitivity(1))
                .bounds(left + PANEL_WIDTH - 58, top + 76, 28, 18).build());
        if (profile.equals("advanced")) {
            addRenderableWidget(modeButton("arm", "armed", left + 10, top + 99, 74));
            addRenderableWidget(modeButton("standby", "standby", left + 95, top + 99, 74));
            addRenderableWidget(modeButton("test", "test", left + 180, top + 99, 74));
        } else {
            addRenderableWidget(modeButton("test", "test", left + (PANEL_WIDTH - 74) / 2, top + 99, 74));
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.close"),
                        button -> onClose())
                .bounds(left + (PANEL_WIDTH - 80) / 2, top + 158, 80, 18).build());
    }

    private Button modeButton(String label, String value, int x, int y, int buttonWidth) {
        return Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.gpws." + label),
                button -> send("mode", value)).bounds(x, y, buttonWidth, 18).build();
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
        graphics.drawCenteredString(font, title, width / 2, top + 7, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("gui.aeronautics_fly_by_instruments.gpws.profile_status",
                        Component.translatable("gui.aeronautics_fly_by_instruments.gpws.profile_" + profile)),
                width / 2, top + 19, 0xFF9EABB0);
        graphics.drawCenteredString(font,
                Component.translatable("gui.aeronautics_fly_by_instruments.gpws.sensitivity_" + sensitivity),
                width / 2, top + 80, 0xFFE5F1F5);
        graphics.drawCenteredString(font,
                Component.translatable("gui.aeronautics_fly_by_instruments.gpws.status",
                        Component.translatable("gui.aeronautics_fly_by_instruments.gpws.mode." + mode),
                        Component.translatable("gui.aeronautics_fly_by_instruments.gpws.phase." + phase)),
                width / 2, top + 123, 0xFFE5F1F5);
        graphics.drawCenteredString(font,
                Component.translatable("gui.aeronautics_fly_by_instruments.gpws.network",
                        Component.translatable("gui.aeronautics_fly_by_instruments.gpws.network." + networkStatus)),
                width / 2, top + 140, 0xFF9EABB0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override public boolean isPauseScreen() { return false; }
}
