package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.TerrainScannerSetAnglePayload;
import com.radaeli.ruilib.ui.Alignment;
import com.radaeli.ruilib.ui.Button;
import com.radaeli.ruilib.ui.Column;
import com.radaeli.ruilib.ui.Label;
import com.radaeli.ruilib.ui.Panel;
import com.radaeli.ruilib.ui.Row;
import com.radaeli.ruilib.ui.Slider;
import com.radaeli.ruilib.ui.Stack;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiScreen;
import com.radaeli.ruilib.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

public final class AdvancedTerrainScannerScreen extends UiScreen {
    private static final int MIN_ANGLE = 30;
    private static final int MAX_ANGLE = 180;
    private static final int DEFAULT_ANGLE = 90;
    private static final int PANEL_WIDTH = 330;
    private static final int PANEL_HEIGHT = 138;
    private static final Component TITLE =
            Component.translatable("gui.aeronautics_fly_by_instruments.terrain_scanner.title");
    private final BlockPos position;
    private final ScreenState state;

    public AdvancedTerrainScannerScreen(BlockPos position, int openingAngle) {
        this(position.immutable(), new ScreenState(openingAngle));
    }

    private AdvancedTerrainScannerScreen(BlockPos position, ScreenState state) {
        super(TITLE, createRoot(position, state), UiTheme.DARK);
        this.position = position;
        this.state = state;
    }

    public BlockPos position() { return position; }

    public int openingAngle() { return state.openingAngle; }

    public void acceptServerAngle(int angle) {
        int acceptedAngle = normalizeAngle(angle);
        state.receivingServerUpdate = true;
        try {
            state.slider.setValue(acceptedAngle);
            state.openingAngle = acceptedAngle;
        } finally {
            state.receivingServerUpdate = false;
        }
    }

    private static UiComponent createRoot(BlockPos position, ScreenState state) {
        Column content = new Column(UiTheme.DARK.tokens().mediumGap(), Alignment.START, Alignment.STRETCH);
        content.add(new Label(TITLE));
        content.add(new Label(Component.translatable(
                "gui.aeronautics_fly_by_instruments.terrain_scanner.opening_angle")));

        Slider slider = new Slider(MIN_ANGLE, MAX_ANGLE, normalizeAngle(state.openingAngle), 5, value -> {
            int angle = (int) Math.round(value);
            state.openingAngle = angle;
            if (!state.receivingServerUpdate) {
                PacketDistributor.sendToServer(new TerrainScannerSetAnglePayload(position, angle));
            }
        });
        slider.setPreferredSize(PANEL_WIDTH - 32, 22);
        state.slider = slider;
        content.add(slider);
        content.add(new Label(Component.translatable(
                "gui.aeronautics_fly_by_instruments.terrain_scanner.range")));

        Row actions = new Row(UiTheme.DARK.tokens().smallGap(), Alignment.CENTER, Alignment.CENTER);
        Button reset = new Button(Component.translatable(
                "gui.aeronautics_fly_by_instruments.terrain_scanner.reset"),
                () -> state.slider.setValue(DEFAULT_ANGLE));
        reset.setPreferredSize(86, UiTheme.DARK.tokens().buttonMinimumHeight());
        actions.add(reset);
        Button close = new Button(Component.translatable(
                "gui.aeronautics_fly_by_instruments.terrain_scanner.close"),
                () -> Minecraft.getInstance().setScreen(null));
        close.setPreferredSize(86, UiTheme.DARK.tokens().buttonMinimumHeight());
        actions.add(close);
        content.add(actions);

        Panel panel = new Panel(content);
        panel.setPreferredSize(PANEL_WIDTH, PANEL_HEIGHT);
        Stack root = new Stack(Alignment.CENTER, Alignment.CENTER);
        root.add(panel);
        return root;
    }

    private static int normalizeAngle(int angle) {
        return Math.clamp(angle, MIN_ANGLE, MAX_ANGLE);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class ScreenState {
        private int openingAngle;
        private boolean receivingServerUpdate;
        private Slider slider;

        private ScreenState(int openingAngle) {
            this.openingAngle = normalizeAngle(openingAngle);
        }
    }
}
