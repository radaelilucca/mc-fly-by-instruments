package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.config.ModConfig;
import com.radaeli.aeronauticsflybyinstruments.network.ApplySensorOutputConfigPayload;
import com.radaeli.aeronauticsflybyinstruments.network.SensorOutputScreenPayload;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig;
import com.radaeli.ruilib.ui.Alignment;
import com.radaeli.ruilib.ui.Bounds;
import com.radaeli.ruilib.ui.Button;
import com.radaeli.ruilib.ui.Column;
import com.radaeli.ruilib.ui.InventoryGrid;
import com.radaeli.ruilib.ui.Insets;
import com.radaeli.ruilib.ui.ItemSlot;
import com.radaeli.ruilib.ui.Label;
import com.radaeli.ruilib.ui.Panel;
import com.radaeli.ruilib.ui.Row;
import com.radaeli.ruilib.ui.Slider;
import com.radaeli.ruilib.ui.Spacer;
import com.radaeli.ruilib.ui.Stack;
import com.radaeli.ruilib.ui.TextField;
import com.radaeli.ruilib.ui.Toggle;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiScreen;
import com.radaeli.ruilib.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumMap;

/** Staged range/mode/frequency editor shared by the three raycast/range sensors. */
public final class SensorOutputConfigScreen extends UiScreen {
    private static final int WIDTH = 280;
    private static final int INVENTORY_WIDTH = 9 * 16 + 8;
    private static final int ALTIMETER_WIDTH = INVENTORY_WIDTH + 16;
    private static final Component TITLE = Component.translatable(
            "gui.aeronautics_fly_by_instruments.sensor_output.title");
    private final Draft draft;
    private final BlockPos position;
    private final String kind;
    private final int maxRange;
    private boolean closeSubmitted;

    public SensorOutputConfigScreen(SensorOutputScreenPayload payload) {
        this(payload.position(), payload.kind(), payload.openingAngle(), payload.config(),
                payload.kind().equals("altimeter") ? ModConfig.RADAR_ALTIMETER_MAX_RANGE.get()
                        : ModConfig.TERRAIN_SCANNER_MAX_RANGE.get());
    }

    private SensorOutputConfigScreen(BlockPos position, String kind, int angle, SensorOutputConfig config,
                                     int maxRange) {
        this(position.immutable(), kind, maxRange, new Draft(angle, config));
    }

    private SensorOutputConfigScreen(BlockPos position, String kind, int maxRange, Draft draft) {
        super(TITLE, createRoot(position, kind, maxRange, draft), UiTheme.DARK);
        this.draft = draft;
        this.position = position;
        this.kind = kind;
        this.maxRange = maxRange;
    }

    private static UiComponent createRoot(BlockPos position, String kind, int maxRange, Draft draft) {
        if (kind.equals("altimeter")) return createAltimeterRoot(position, maxRange, draft);

        Column content = new Column(3, Alignment.START, Alignment.STRETCH);
        content.add(new Label(Component.translatable(titleKey(kind))));

        if (kind.equals("advanced")) {
            Row angleRow = new Row(5, Alignment.CENTER, Alignment.CENTER);
            angleRow.add(new Label(Component.translatable("gui.aeronautics_fly_by_instruments.terrain_scanner.opening_angle")));
            TextField angle = numberField(Integer.toString(draft.angle), 3, value -> {
                draft.angleText = value;
                draft.parseAngle();
                draft.updateApplyEnabled(maxRange, kind);
            });
            angle.setPreferredSize(42, 18);
            angleRow.add(angle);
            content.add(angleRow);
        }

        Toggle linkMode = new Toggle(Component.translatable(
                "gui.aeronautics_fly_by_instruments.sensor_output.mode_link"),
                draft.config.mode() == SensorOutputConfig.Mode.LINK,
                enabled -> draft.config.mode(enabled ? SensorOutputConfig.Mode.LINK : SensorOutputConfig.Mode.FACES));
        content.add(linkMode);

        for (SensorOutputConfig.Channel channel : draft.config.ranges().keySet()) {
            var range = draft.config.range(channel);
            Row outputRow = new Row(4, Alignment.CENTER, Alignment.CENTER);
            outputRow.add(new Label(Component.translatable(channelKey(channel))));
            boolean altitudeOutput = kind.equals("altimeter");
            outputRow.add(new Label(Component.translatable(altitudeOutput
                    ? "gui.aeronautics_fly_by_instruments.sensor_output.zero_at"
                    : "gui.aeronautics_fly_by_instruments.sensor_output.fifteen_below")));
            TextField min = numberField(Integer.toString(range.min()), 4, value -> {
                draft.updateRangeText(channel, true, value, maxRange);
                draft.updateApplyEnabled(maxRange, kind);
            });
            min.setPreferredSize(40, 18); outputRow.add(min);
            outputRow.add(new Label(Component.translatable(altitudeOutput
                    ? "gui.aeronautics_fly_by_instruments.sensor_output.fifteen_at"
                    : "gui.aeronautics_fly_by_instruments.sensor_output.zero_above")));
            TextField max = numberField(Integer.toString(range.max()), 4, value -> {
                draft.updateRangeText(channel, false, value, maxRange);
                draft.updateApplyEnabled(maxRange, kind);
            });
            max.setPreferredSize(40, 18); outputRow.add(max);
            var pair = draft.config.frequencies(channel);
            ItemSlot slotA = new ItemSlot(pair.first(), stack -> draft.updateFrequency(channel, true, stack));
            slotA.setPreferredSize(16, 16);
            slotA.setBackgroundColor(0xFF9E3434);
            ItemSlot slotB = new ItemSlot(pair.second(), stack -> draft.updateFrequency(channel, false, stack));
            slotB.setPreferredSize(16, 16);
            slotB.setBackgroundColor(0xFF344D9E);
            outputRow.add(new Label(Component.literal("A"))); outputRow.add(slotA);
            outputRow.add(new Label(Component.literal("B"))); outputRow.add(slotB);
            draft.rangeFields.put(channel, new TextField[]{min, max});
            content.add(outputRow);
        }

        InventoryGrid inventory = InventoryGrid.playerInventory().setSlotSize(16).setGap(1);
        content.add(inventory);

        Row actions = new Row(8, Alignment.CENTER, Alignment.CENTER);
        Button apply = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.apply"), () -> {
            if (!draft.isValid(maxRange, kind)) return;
            PacketDistributor.sendToServer(new ApplySensorOutputConfigPayload(position, kind, draft.angle, draft.config));
        });
        apply.setPreferredSize(86, UiTheme.DARK.tokens().buttonMinimumHeight());
        draft.applyButton = apply;
        Button cancel = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.cancel"),
                () -> Minecraft.getInstance().setScreen(null));
        cancel.setPreferredSize(86, UiTheme.DARK.tokens().buttonMinimumHeight());
        actions.add(apply); actions.add(cancel); content.add(actions);

        Panel panel = new Panel(content);
        panel.setPreferredSize(WIDTH, null);
        draft.panel = panel;
        Stack root = new Stack(Alignment.CENTER, Alignment.CENTER); root.add(panel);
        return root;
    }

    private static UiComponent createAltimeterRoot(BlockPos position, int maxRange, Draft draft) {
        Column content = new Column(3, Alignment.START, Alignment.STRETCH);
        content.add(new Label(Component.translatable(titleKey("altimeter"))));
        content.add(Spacer.fixed(1, 5));
        Label rangeLabel = new Label(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.measure_range"));
        rangeLabel.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.range_tooltip"),
                Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.range_output_tooltip"));
        content.add(rangeLabel);

        var range = draft.config.range(SensorOutputConfig.Channel.ALTITUDE);
        Slider measuredRange = new Slider(0, maxRange, range.min(), 1, value -> { });
        measuredRange.setValues(range.min(), range.max());
        measuredRange.setOnRangeChange((lower, upper) -> draft.updateAltitudeRange(lower, upper, maxRange));
        measuredRange.setPreferredSize(INVENTORY_WIDTH, 22);
        measuredRange.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.range_tooltip"),
                Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.range_output_tooltip"));
        content.add(measuredRange);

        var pair = draft.config.frequencies(SensorOutputConfig.Channel.ALTITUDE);
        Row frequencies = new Row(4, Alignment.START, Alignment.CENTER);
        Label frequencyLabel = new Label(Component.translatable(
                "gui.aeronautics_fly_by_instruments.sensor_output.output_frequency"));
        frequencyLabel.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.frequency_tooltip"),
                Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.frequency_picker_tooltip"));
        frequencies.add(frequencyLabel);
        Row frequencySlots = new Row(0, Alignment.CENTER, Alignment.CENTER);
        ItemSlot slotA = new ItemSlot(pair.first(), stack -> draft.updateFrequency(
                SensorOutputConfig.Channel.ALTITUDE, true, stack));
        slotA.setPreferredSize(19, 19);
        slotA.setBackgroundColor(0xFF9E3434);
        slotA.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.frequency_picker_tooltip"));
        ItemSlot slotB = new ItemSlot(pair.second(), stack -> draft.updateFrequency(
                SensorOutputConfig.Channel.ALTITUDE, false, stack));
        slotB.setPreferredSize(19, 19);
        slotB.setBackgroundColor(0xFF344D9E);
        slotB.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.frequency_picker_tooltip"));
        frequencySlots.add(slotA);
        frequencySlots.add(slotB);
        frequencies.add(frequencySlots);
        content.add(frequencies);

        Stack centeredInventory = new Stack(Alignment.CENTER, Alignment.CENTER);
        centeredInventory.add(InventoryGrid.playerInventory().setSlotSize(16).setGap(1));
        content.add(centeredInventory);
        content.add(Spacer.fixed(1, 5));

        Row actions = new Row(8, Alignment.CENTER, Alignment.CENTER);
        Button apply = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.apply"),
                () -> {
                    if (draft.submit(position, "altimeter", maxRange)) {
                        draft.closeAlreadyApplied = true;
                        Minecraft.getInstance().setScreen(null);
                    }
                });
        apply.setPreferredSize((INVENTORY_WIDTH - 8) / 2, UiTheme.DARK.tokens().buttonMinimumHeight());
        draft.applyButton = apply;
        Button cancel = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.cancel"), () -> {
            draft.discardOnClose = true;
            Minecraft.getInstance().setScreen(null);
        });
        cancel.setPreferredSize((INVENTORY_WIDTH - 8) / 2, UiTheme.DARK.tokens().buttonMinimumHeight());
        actions.add(apply);
        actions.add(cancel);
        content.add(actions);

        Panel panel = new Panel(content, new Insets(2, 8, 8, 8));
        panel.setPreferredSize(ALTIMETER_WIDTH, null);
        draft.panel = panel;
        return panel;
    }

    private static String titleKey(String kind) {
        return kind.equals("altimeter") ? "gui.aeronautics_fly_by_instruments.sensor_output.altimeter"
                : kind.equals("advanced") ? "gui.aeronautics_fly_by_instruments.sensor_output.advanced"
                : "gui.aeronautics_fly_by_instruments.sensor_output.terrain";
    }
    private static String channelKey(SensorOutputConfig.Channel channel) {
        return "gui.aeronautics_fly_by_instruments.sensor_output.channel." + channel.name().toLowerCase(java.util.Locale.ROOT);
    }

    private static TextField numberField(String initialValue, int maxLength, java.util.function.Consumer<String> responder) {
        TextField field = new TextField();
        field.setMaxLength(maxLength);
        field.setValue(initialValue);
        field.setResponder(responder);
        return field;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public Bounds jeiGuiBounds() { return draft.panel.bounds(); }

    @Override
    protected void layoutRoot(UiTheme theme) {
        if (kind.equals("altimeter")) {
            int panelWidth = Math.min(ALTIMETER_WIDTH, Math.max(1, width - 24));
            draft.panel.setPreferredSize(panelWidth, null);
            var size = draft.panel.measure(font, theme, panelWidth, Math.max(1, height - 12));
            draft.panel.layout(font, theme, new Bounds((width - size.width()) / 2,
                    (height - size.height()) / 2, size.width(), size.height()));
            return;
        }
        super.layoutRoot(theme);
    }

    @Override
    public void onClose() {
        if (kind.equals("altimeter") && !draft.discardOnClose && !draft.closeAlreadyApplied) {
            draft.submit(position, kind, maxRange);
            closeSubmitted = true;
        }
        super.onClose();
    }

    @Override
    public void removed() {
        if (kind.equals("altimeter") && !draft.discardOnClose
                && !draft.closeAlreadyApplied && !closeSubmitted) {
            draft.submit(position, kind, maxRange);
        }
        super.removed();
    }

    private static final class Draft {
        private int angle;
        private String angleText;
        private final SensorOutputConfig config;
        private final EnumMap<SensorOutputConfig.Channel, TextField[]> rangeFields = new EnumMap<>(SensorOutputConfig.Channel.class);
        private Panel panel;
        private Button applyButton;
        private boolean discardOnClose;
        private boolean closeAlreadyApplied;
        private Draft(int angle, SensorOutputConfig config) {
            this.angle = angle; this.angleText = Integer.toString(angle); this.config = config.copy();
        }
        private void updateRangeText(SensorOutputConfig.Channel channel, boolean minimum, String value, int maxRange) {
            TextField[] fields = rangeFields.get(channel);
            String minText = minimum ? value : fields == null ? Integer.toString(config.range(channel).min()) : fields[0].value();
            String maxText = minimum ? fields == null ? Integer.toString(config.range(channel).max()) : fields[1].value() : value;
            try {
                int min = Integer.parseInt(minText), max = Integer.parseInt(maxText);
                if (new SensorOutputConfig.Range(min, max).valid(maxRange)) {
                    config.setRange(channel, new SensorOutputConfig.Range(min, max));
                }
            } catch (NumberFormatException ignored) { }
        }
        private void parseAngle() {
            try { int value = Integer.parseInt(angleText); if (value >= 30 && value <= 180) angle = value; }
            catch (NumberFormatException ignored) { }
        }
        private boolean isValid(int maxRange, String kind) {
            if (config.ranges().values().stream().anyMatch(range -> !range.valid(maxRange))) return false;
            if (kind.equals("advanced")) {
                try { int value = Integer.parseInt(angleText); if (value < 30 || value > 180) return false; }
                catch (NumberFormatException exception) { return false; }
            }
            for (var entry : rangeFields.entrySet()) {
                try {
                    int min = Integer.parseInt(entry.getValue()[0].value());
                    int max = Integer.parseInt(entry.getValue()[1].value());
                    if (!new SensorOutputConfig.Range(min, max).valid(maxRange)) return false;
                } catch (NumberFormatException exception) { return false; }
            }
            return true;
        }
        private void updateApplyEnabled(int maxRange, String kind) {
            if (applyButton != null) applyButton.setEnabled(isValid(maxRange, kind));
        }
        private void updateFrequency(SensorOutputConfig.Channel channel, boolean first, ItemStack stack) {
            var old = config.frequencies(channel);
            config.setFrequencies(channel, new SensorOutputConfig.FrequencyPair(
                    first ? stack : old.first(), first ? old.second() : stack));
        }

        private void updateAltitudeRange(double lower, Double upper, int maximumRange) {
            if (upper == null) return;
            var range = new SensorOutputConfig.Range((int) Math.round(lower), (int) Math.round(upper));
            if (range.valid(maximumRange)) {
                config.setRange(SensorOutputConfig.Channel.ALTITUDE, range);
            }
            updateApplyEnabled(maximumRange, "altimeter");
        }

        private boolean submit(BlockPos position, String kind, int maxRange) {
            if (kind.equals("altimeter")) {
                var pair = config.frequencies(SensorOutputConfig.Channel.ALTITUDE);
                config.mode(pair.enabled() ? SensorOutputConfig.Mode.LINK : SensorOutputConfig.Mode.FACES);
            }
            if (isValid(maxRange, kind)) {
                PacketDistributor.sendToServer(new ApplySensorOutputConfigPayload(position, kind, angle, config));
                return true;
            }
            return false;
        }
    }
}
