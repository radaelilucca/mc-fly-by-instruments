package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.PanelActionPayload;
import com.radaeli.aeronauticsflybyinstruments.network.PanelScreenPayload;
import com.radaeli.ruilib.ui.Alignment;
import com.radaeli.ruilib.ui.Button;
import com.radaeli.ruilib.ui.Column;
import com.radaeli.ruilib.ui.Label;
import com.radaeli.ruilib.ui.Modal;
import com.radaeli.ruilib.ui.Panel;
import com.radaeli.ruilib.ui.Row;
import com.radaeli.ruilib.ui.Slider;
import com.radaeli.ruilib.ui.ScrollView;
import com.radaeli.ruilib.ui.TextField;
import com.radaeli.ruilib.ui.Spacer;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiContext;
import com.radaeli.ruilib.ui.UiPaint;
import com.radaeli.ruilib.ui.UiScreen;
import com.radaeli.ruilib.ui.UiSize;
import com.radaeli.ruilib.ui.UiText;
import com.radaeli.ruilib.ui.UiTheme;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/** Compact R-UI for table alert status and module output configuration. */
public final class PanelScreen extends UiScreen {
    private static final int WIDTH = 220;
    private static final List<String> CHANNELS = List.of("gpws.any", "gpws.terrain", "gpws.pull_up",
            "gpws.sink_rate", "gpws.dont_sink", "gpws.too_low", "gpws.bank_angle", "gpws.sensor_fail");
    private final PanelScreenPayload payload;
    private final State state;
    private String source;
    private String channel;
    private int severity;
    private int volume;
    private String testChannel;
    private String testThreshold;
    private boolean saved;
    private boolean discarded;
    private final java.util.function.Consumer<PanelActionPayload> sender;
    private final Runnable closeAction;

    public PanelScreen(PanelScreenPayload payload) {
        this(payload, p -> PacketDistributor.sendToServer(p), null);
    }

    PanelScreen(PanelScreenPayload payload, java.util.function.Consumer<PanelActionPayload> sender, Runnable closeAction) {
        this(payload, new State(payload), sender, closeAction);
    }

    private PanelScreen(PanelScreenPayload payload, State state,
                        java.util.function.Consumer<PanelActionPayload> sender, Runnable closeAction) {
        super(Component.translatable("gui.aeronautics_fly_by_instruments.panel.title"), state.root, UiTheme.DARK);
        this.payload = payload;
        this.state = state;
        this.sender = sender;
        this.closeAction = closeAction;
        source = payload.source(); channel = payload.channel();
        severity = payload.severity(); volume = payload.volume();
        testChannel = payload.testChannel(); testThreshold = Double.toString(payload.testThreshold());
        state.screen = this;
        state.update(this);
    }

    private void cycleSource() {
        source = source.equals("gpws") ? "test" : "gpws";
        state.update(this);
    }
    private void cycleChannel() {
        int index = CHANNELS.indexOf(channel);
        channel = CHANNELS.get((index + 1 + CHANNELS.size()) % CHANNELS.size());
        state.update(this);
    }
    private void cycleSeverity() {
        severity = severity % 3 + 1;
        state.update(this);
    }
    private void setVolume(double value) {
        volume = (int) Math.round(value);
        state.updateVolume(volume);
    }
    private Component sourceLabel() {
        return Component.translatable("gui.aeronautics_fly_by_instruments.panel.source",
                Component.translatable("gui.aeronautics_fly_by_instruments.panel.source." + source));
    }
    private Component channelLabel() {
        return Component.translatable("gui.aeronautics_fly_by_instruments.panel.channel", Component.literal(channel));
    }
    private Component severityLabel() {
        String value = switch (severity) { case 1 -> "advisory"; case 2 -> "caution"; default -> "warning"; };
        return Component.translatable("gui.aeronautics_fly_by_instruments.panel.severity",
                Component.translatable("gui.aeronautics_fly_by_instruments.panel.severity." + value));
    }
    private void save() {
        captureDraft();
        if (source.equals("test")) {
            if (!validDraft()) return;
        }
        if (payload.column() >= 0) send("save");
        saved = true;
        closeScreen();
    }
    private void send(String action) {
        send(action, payload.column(), payload.row());
    }
    private void send(String action, int column, int row) {
        sender.accept(new PanelActionPayload(payload.position(), column, row,
                action, source, channel, severity, volume, testChannel, parseThreshold(), payload.interactionToken()));
    }
    private void captureDraft() {
        if (state.testChannelField != null) testChannel = state.testChannelField.value().trim();
        if (state.testThresholdField != null) testThreshold = state.testThresholdField.value().trim();
    }
    private boolean validDraft() {
        if (!testChannel.isBlank() && Double.isFinite(parseThreshold())) return true;
        if (net.minecraft.client.Minecraft.getInstance().player != null)
            net.minecraft.client.Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("gui.aeronautics_fly_by_instruments.panel.invalid_test"), true);
        return false;
    }
    private double parseThreshold() {
        try { double value = Double.parseDouble(testThreshold); return Double.isFinite(value) ? value : Double.NaN; }
        catch (NumberFormatException invalid) { return Double.NaN; }
    }
    private void closeWithoutSaving() {
        discarded = true;
        closeScreen();
    }
    private void closeScreen() { if (closeAction == null) onClose(); else closeAction.run(); }
    @Override public void removed() {
        if (!saved && !discarded && payload.column() >= 0) {
            captureDraft();
            if (!source.equals("test") || validDraft()) {
                send("save");
                saved = true;
            }
        }
        super.removed();
    }
    @Override public boolean isPauseScreen() { return false; }

    private static final class State {
        final Column content = new Column(5, Alignment.START, Alignment.STRETCH);
        final Panel root;
        PanelScreen screen;
        Button sourceButton, channelButton, severityButton;
        Text volumeText;
        Slider volumeSlider;
        TextField testChannelField, testThresholdField;
        State(PanelScreenPayload payload) {
            content.add(new Label(Component.translatable("gui.aeronautics_fly_by_instruments.panel.title")));
            if (payload.column() < 0) {
                content.add(new Label(Component.translatable("gui.aeronautics_fly_by_instruments.panel.connected",
                        Component.translatable("gui.aeronautics_fly_by_instruments.panel.network." + payload.networkStatus()))));
                content.add(new Label(Component.translatable("gui.aeronautics_fly_by_instruments.panel.module_count",
                        payload.activeCount(), payload.moduleCount())));
                if (!payload.moduleSummary().isBlank()) {
                    ScrollView moduleList = new ScrollView(2);
                    moduleList.setPreferredSize(null, 84);
                    for (String entry : payload.moduleSummary().split(";")) {
                        String[] data = entry.split(",", 4);
                        if (data.length != 4) continue;
                        int column = Integer.parseInt(data[1]);
                        int row = Integer.parseInt(data[2]);
                        Component state = Component.translatable(Boolean.parseBoolean(data[3])
                                ? "gui.aeronautics_fly_by_instruments.panel.module_on"
                                : "gui.aeronautics_fly_by_instruments.panel.module_off");
                        String moduleNameKey = switch (data[0]) {
                            case "fms" -> "item.aeronautics_fly_by_instruments.fms_module";
                            case "lamp" -> "gui.aeronautics_fly_by_instruments.panel.module.lamp";
                            case "lamp_2x1" -> "gui.aeronautics_fly_by_instruments.panel.module.lamp_2x1";
                            default -> "gui.aeronautics_fly_by_instruments.panel.module.buzzer";
                        };
                        Button module = new Button(Component.translatable(
                                "gui.aeronautics_fly_by_instruments.panel.module_entry",
                                Component.translatable(moduleNameKey), column + 1, row + 1, state), () -> {
                            if (screen != null) screen.send("open_module", column, row);
                        });
                        module.setPreferredSize(WIDTH - 20, 18);
                        moduleList.add(module);
                    }
                    content.add(moduleList);
                }
                Button setup = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.panel.basic_setup"), this::confirmSetup);
                setup.setPreferredSize(WIDTH - 16, null); content.add(setup);
                Row actions = new Row(5, Alignment.CENTER, Alignment.CENTER);
                Button test = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.panel.test"),
                        () -> send("test"));
                Button mute = new Button(Component.translatable(payload.muted()
                        ? "gui.aeronautics_fly_by_instruments.panel.unmute"
                        : "gui.aeronautics_fly_by_instruments.panel.mute"), () -> send(payload.muted() ? "unmute" : "mute"));
                test.setPreferredSize(94, null); mute.setPreferredSize(94, null);
                actions.add(test); actions.add(mute); content.add(actions);
            } else {
                content.add(new Label(Component.translatable(payload.activeCount() > 0
                        ? "gui.aeronautics_fly_by_instruments.panel.module_active"
                        : "gui.aeronautics_fly_by_instruments.panel.module_inactive")));
                sourceButton = new Button(Component.empty(), () -> screen.cycleSource());
                sourceButton.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.panel.source_tooltip"));
                channelButton = new Button(Component.empty(), () -> screen.cycleChannel());
                channelButton.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.panel.channel_tooltip"));
                severityButton = new Button(Component.empty(), () -> screen.cycleSeverity());
                severityButton.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.panel.severity_tooltip"));
                content.add(sourceButton); content.add(channelButton); content.add(severityButton);
                testChannelField = new TextField(Component.translatable("gui.aeronautics_fly_by_instruments.panel.test_channel"));
                testChannelField.setMaxLength(64); testChannelField.setPreferredSize(WIDTH - 16, 22);
                testChannelField.setValue(payload.testChannel());
                testThresholdField = new TextField(Component.translatable("gui.aeronautics_fly_by_instruments.panel.test_threshold"));
                testThresholdField.setMaxLength(24); testThresholdField.setPreferredSize(WIDTH - 16, 22);
                testThresholdField.setValue(Double.toString(payload.testThreshold()));
                content.add(testChannelField); content.add(testThresholdField);
                if (payload.moduleType().equals("buzzer")) {
                    volumeText = new Text();
                    volumeText.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.panel.volume_tooltip"));
                    volumeSlider = new Slider(0, 100, payload.volume(), 1, this::sliderChanged);
                    volumeSlider.setPreferredSize(WIDTH - 20, 20);
                    volumeSlider.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.panel.volume_tooltip"));
                    content.add(volumeText); content.add(volumeSlider);
                    Button testSound = new Button(
                            Component.translatable("gui.aeronautics_fly_by_instruments.panel.test_sound"),
                            this::testSound);
                    testSound.setPreferredSize(WIDTH - 16, null);
                    content.add(testSound);
                }
                content.add(Spacer.fixed(1, 2));
                Row buttons = new Row(5, Alignment.CENTER, Alignment.CENTER);
                Button save = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.panel.save"), () -> screen.save());
                Button cancel = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.panel.cancel"), () -> screen.closeWithoutSaving());
                save.setPreferredSize(94, null); cancel.setPreferredSize(94, null);
                buttons.add(save); buttons.add(cancel); content.add(buttons);
            }
            root = new Panel(content, com.radaeli.ruilib.ui.Insets.all(8));
            root.setPreferredSize(WIDTH, null);
        }
        private void sliderChanged(double value) { if (screen != null) screen.setVolume(value); }
        private void testSound() {
            if (screen != null) {
                screen.captureDraft();
                screen.send("test_buzzer");
            }
        }
        void update(PanelScreen target) {
            if (sourceButton == null) return;
            if (testChannelField != null) {
                target.testChannel = testChannelField.value();
                target.testThreshold = testThresholdField.value();
            }
            sourceButton.setLabel(target.sourceLabel());
            channelButton.setLabel(target.channelLabel());
            channelButton.setEnabled(target.source.equals("gpws"));
            testChannelField.setEnabled(target.source.equals("test"));
            testThresholdField.setEnabled(target.source.equals("test"));
            severityButton.setLabel(target.severityLabel());
            updateVolume(target.volume);
        }
        void updateVolume(int value) {
            if (volumeText != null) volumeText.setText(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.panel.volume", value));
        }
        private void send(String action) {
            if (screen != null) {
                if (testChannelField != null) {
                    screen.testChannel = testChannelField.value();
                    screen.testThreshold = testThresholdField.value();
                }
                screen.send(action);
            }
        }
        private void confirmSetup() {
            Column modalContent = new Column(6, Alignment.CENTER, Alignment.STRETCH);
            modalContent.add(new Label(Component.translatable("gui.aeronautics_fly_by_instruments.panel.confirm_setup")));
            modalContent.add(new Button(Component.translatable("gui.aeronautics_fly_by_instruments.panel.confirm"), () -> {
                send("basic"); if (screen != null) screen.closeModal();
            }));
            modalContent.add(new Button(Component.translatable("gui.aeronautics_fly_by_instruments.panel.cancel"),
                    () -> { if (screen != null) screen.closeModal(); }));
            if (screen != null) screen.showModal(new Modal(
                    Component.translatable("gui.aeronautics_fly_by_instruments.panel.basic_setup"), modalContent, 200, 92));
        }
    }

    private static final class Text extends UiComponent {
        private Component text = Component.empty();
        void setText(Component value) { text = value; }
        @Override protected UiSize measureIntrinsic(net.minecraft.client.gui.Font font, UiTheme theme, int maxWidth, int maxHeight) {
            return new UiSize(UiText.width(font, text, theme), UiText.height(font, theme)).constrain(maxWidth, maxHeight);
        }
        @Override public void render(UiContext context) {
            UiPaint.text(context, text, bounds().x(), bounds().y(), context.theme().palette().text());
        }
    }
}
