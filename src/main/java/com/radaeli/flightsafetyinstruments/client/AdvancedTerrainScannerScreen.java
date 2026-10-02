package com.radaeli.flightsafetyinstruments.client;

import com.radaeli.flightsafetyinstruments.network.TerrainScannerSetAnglePayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AdvancedTerrainScannerScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 112;
    private final BlockPos position;
    private int openingAngle;

    public AdvancedTerrainScannerScreen(BlockPos position, int openingAngle) {
        super(Component.translatable("gui.aeronautics_fly_by_instruments.terrain_scanner.title"));
        this.position = position.immutable();
        this.openingAngle = openingAngle;
    }

    public BlockPos position() { return position; }

    public void acceptServerAngle(int angle) {
        openingAngle = angle;
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        addRenderableWidget(Button.builder(Component.literal("−5°"), button -> adjust(-5))
                .bounds(left + 24, top + 48, 54, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+5°"), button -> adjust(5))
                .bounds(left + PANEL_WIDTH - 78, top + 48, 54, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.terrain_scanner.reset"),
                        button -> setAngle(90))
                .bounds(left + 24, top + 78, 78, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.aeronautics_fly_by_instruments.terrain_scanner.close"),
                        button -> onClose())
                .bounds(left + PANEL_WIDTH - 102, top + 78, 78, 20).build());
    }

    private void adjust(int delta) {
        setAngle(Math.clamp(openingAngle + delta, 30, 180));
    }

    private void setAngle(int angle) {
        if (angle == openingAngle) return;
        openingAngle = angle;
        PacketDistributor.sendToServer(new TerrainScannerSetAnglePayload(position, angle));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101418);
        graphics.renderOutline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFF6F8793);
        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFFFFFFFF);
        graphics.drawCenteredString(font,
                Component.translatable("gui.aeronautics_fly_by_instruments.terrain_scanner.opening_angle", openingAngle),
                width / 2, top + 34, 0xFFE5F1F5);
        graphics.drawCenteredString(font,
                Component.translatable("gui.aeronautics_fly_by_instruments.terrain_scanner.range"),
                width / 2, top + PANEL_HEIGHT - 11, 0xFF9EABB0);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
