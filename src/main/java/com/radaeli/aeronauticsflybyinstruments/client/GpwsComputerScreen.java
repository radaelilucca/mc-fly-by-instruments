package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.GpwsControlPayload;
import com.radaeli.aeronauticsflybyinstruments.network.GpwsScreenPayload;
import com.radaeli.ruilib.ui.Alignment;
import com.radaeli.ruilib.ui.Bounds;
import com.radaeli.ruilib.ui.Button;
import com.radaeli.ruilib.ui.Column;
import com.radaeli.ruilib.ui.Insets;
import com.radaeli.ruilib.ui.Panel;
import com.radaeli.ruilib.ui.Row;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiContext;
import com.radaeli.ruilib.ui.UiPaint;
import com.radaeli.ruilib.ui.UiScreen;
import com.radaeli.ruilib.ui.UiSize;
import com.radaeli.ruilib.ui.UiText;
import com.radaeli.ruilib.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Small game-facing setup panel; detailed channel tuning remains optional via CC:Tweaked. */
public final class GpwsComputerScreen extends UiScreen {
    private static final int PANEL_WIDTH = 264;
    private static final int CONTENT_WIDTH = PANEL_WIDTH - 16;
    private static final int BUTTON_WIDTH = 116;
    private static final int BUTTON_HEIGHT = 18;
    private static final int MODE_BUTTON_WIDTH = 74;
    private static final Component TITLE =
            Component.translatable("gui.aeronautics_fly_by_instruments.gpws.title");

    private final State state;

    public GpwsComputerScreen(GpwsScreenPayload payload) {
        this(payload, new State(payload));
    }

    private GpwsComputerScreen(GpwsScreenPayload payload, State state) {
        super(TITLE, createRoot(state), UiTheme.DARK);
        this.state = state;
        accept(payload);
    }

    private static UiComponent createRoot(State state) {
        Column content = new Column(UiTheme.DARK.tokens().smallGap(), Alignment.CENTER, Alignment.CENTER);
        content.add(new TextLine(TITLE, CONTENT_WIDTH, false));
        state.profileLine = new TextLine(Component.empty(), CONTENT_WIDTH, true);
        content.add(state.profileLine);

        Row profiles = centeredRow();
        profiles.add(button("profile_basic", () -> state.send("profile", "basic"), BUTTON_WIDTH));
        profiles.add(button("profile_advanced", () -> state.send("profile", "advanced"), BUTTON_WIDTH));
        content.add(profiles);

        Row enableControls = centeredRow();
        enableControls.add(button("enable", () -> state.send("enable", ""), BUTTON_WIDTH));
        enableControls.add(button("disable", () -> state.send("disable", ""), BUTTON_WIDTH));
        content.add(enableControls);

        Row sensitivityControls = centeredRow();
        sensitivityControls.add(buttonText("−", () -> state.adjustSensitivity(-1), 28));
        state.sensitivityLine = new TextLine(Component.empty(), 132, false);
        sensitivityControls.add(state.sensitivityLine);
        sensitivityControls.add(buttonText("+", () -> state.adjustSensitivity(1), 28));
        content.add(sensitivityControls);

        state.modeControls = centeredRow();
        content.add(state.modeControls);

        state.statusLine = new TextLine(Component.empty(), CONTENT_WIDTH, false);
        content.add(state.statusLine);
        state.networkLine = new TextLine(Component.empty(), CONTENT_WIDTH, true);
        content.add(state.networkLine);

        content.add(button("close", () -> Minecraft.getInstance().setScreen(null), 80));

        Panel panel = new Panel(content, Insets.all(8));
        panel.setPreferredSize(PANEL_WIDTH, null);
        state.panel = panel;
        state.updateModeControls();
        state.updateText();
        return panel;
    }

    private static Row centeredRow() {
        return new Row(UiTheme.DARK.tokens().smallGap(), Alignment.CENTER, Alignment.CENTER);
    }

    private static Button button(String translationKey, Runnable action, int width) {
        Button button = new Button(Component.translatable(
                "gui.aeronautics_fly_by_instruments.gpws." + translationKey), action);
        button.setPreferredSize(width, BUTTON_HEIGHT);
        return button;
    }

    private static Button buttonText(String text, Runnable action, int width) {
        Button button = new Button(Component.literal(text), action);
        button.setPreferredSize(width, BUTTON_HEIGHT);
        return button;
    }

    public BlockPos position() {
        return state.position;
    }

    public void accept(GpwsScreenPayload payload) {
        String previousProfile = state.profile;
        state.position = payload.position().immutable();
        state.profile = payload.profile();
        state.sensitivity = payload.sensitivity();
        state.mode = payload.mode();
        state.phase = payload.phase();
        state.networkStatus = payload.networkStatus();
        state.updateText();
        if (!state.profile.equals(previousProfile)) {
            state.updateModeControls();
            if (minecraft != null) layoutRoot(UiTheme.DARK);
        }
    }

    @Override
    protected void layoutRoot(UiTheme theme) {
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(1, width - 24));
        state.panel.setPreferredSize(panelWidth, null);
        UiSize measured = state.panel.measure(font, theme, panelWidth, Math.max(1, height - 12));
        state.panel.layout(font, theme, new Bounds((width - measured.width()) / 2,
                (height - measured.height()) / 2, measured.width(), measured.height()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static final class State {
        private BlockPos position;
        private String profile;
        private String sensitivity;
        private String mode;
        private String phase;
        private String networkStatus;
        private TextLine profileLine;
        private TextLine sensitivityLine;
        private TextLine statusLine;
        private TextLine networkLine;
        private Row modeControls;
        private Panel panel;

        private State(GpwsScreenPayload payload) {
            acceptValues(payload);
        }

        private void acceptValues(GpwsScreenPayload payload) {
            position = payload.position().immutable();
            profile = payload.profile();
            sensitivity = payload.sensitivity();
            mode = payload.mode();
            phase = payload.phase();
            networkStatus = payload.networkStatus();
        }

        private void updateText() {
            if (profileLine == null) return;
            profileLine.setText(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.gpws.profile_status",
                    Component.translatable("gui.aeronautics_fly_by_instruments.gpws.profile_" + profile)));
            sensitivityLine.setText(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.gpws.sensitivity_" + sensitivity));
            statusLine.setText(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.status",
                    Component.translatable("gui.aeronautics_fly_by_instruments.gpws.mode." + mode),
                    Component.translatable("gui.aeronautics_fly_by_instruments.gpws.phase." + phase)));
            networkLine.setText(Component.translatable("gui.aeronautics_fly_by_instruments.gpws.network",
                    Component.translatable("gui.aeronautics_fly_by_instruments.gpws.network." + networkStatus)));
        }

        private void updateModeControls() {
            if (modeControls == null) return;
            List.copyOf(modeControls.children()).forEach(modeControls::remove);
            if (profile.equals("advanced")) {
                modeControls.add(button("arm", () -> send("mode", "armed"), MODE_BUTTON_WIDTH));
                modeControls.add(button("standby", () -> send("mode", "standby"), MODE_BUTTON_WIDTH));
                modeControls.add(button("test", () -> send("mode", "test"), MODE_BUTTON_WIDTH));
            } else {
                modeControls.add(button("test", () -> send("mode", "test"), MODE_BUTTON_WIDTH));
            }
        }

        private void adjustSensitivity(int delta) {
            List<String> values = List.of("low", "normal", "high");
            int index = values.indexOf(sensitivity);
            send("sensitivity", values.get(Math.floorMod(index + delta, values.size())));
        }

        private void send(String action, String value) {
            PacketDistributor.sendToServer(new GpwsControlPayload(position, action, value));
        }
    }

    /** Dynamic text leaf rendered and measured through R-UI's component/theme pipeline. */
    private static final class TextLine extends UiComponent {
        private Component text;
        private final int width;
        private final boolean muted;

        private TextLine(Component text, int width, boolean muted) {
            this.text = text;
            this.width = width;
            this.muted = muted;
            setPreferredSize(width, 10);
        }

        private void setText(Component text) {
            this.text = text;
        }

        @Override
        protected UiSize measureIntrinsic(net.minecraft.client.gui.Font font, UiTheme theme,
                                          int maxWidth, int maxHeight) {
            return new UiSize(width, 10).constrain(maxWidth, maxHeight);
        }

        @Override
        public void render(UiContext context) {
            int color = muted ? context.theme().palette().mutedText() : context.theme().palette().text();
            UiPaint.textCentered(context, text, bounds().centerX(),
                    bounds().centerY() - UiText.height(context) / 2, color);
        }
    }
}
