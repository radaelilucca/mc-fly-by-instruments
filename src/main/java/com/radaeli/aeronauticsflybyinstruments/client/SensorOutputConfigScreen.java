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
import com.radaeli.ruilib.ui.Tabs;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiScreen;
import com.radaeli.ruilib.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/** Staged range/mode/frequency editor shared by the three raycast/range sensors. */
public final class SensorOutputConfigScreen extends UiScreen {
    private static final int INVENTORY_WIDTH = 9 * 16 + 8;
    private static final int PANEL_WIDTH = INVENTORY_WIDTH + 16;
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

    SensorOutputConfigScreen(BlockPos position, String kind, int angle, SensorOutputConfig config,
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
        Column content = new Column(3, Alignment.START, Alignment.STRETCH);
        content.add(new Label(Component.translatable(titleKey(kind))));

        if (kind.equals("advanced")) {
            Row angleRow = new Row(4, Alignment.START, Alignment.CENTER);
            Label angleLabel = new Label(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.terrain_scanner.opening_angle"));
            angleLabel.tooltip(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.sensor_output.angle_tooltip"));
            angleRow.add(angleLabel);
            Slider angle = new Slider(30, 180, draft.angle, 1,
                    value -> draft.updateAngle(value, maxRange, kind));
            angle.setPreferredSize(INVENTORY_WIDTH - 60, 22);
            angle.tooltip(Component.translatable(
                    "gui.aeronautics_fly_by_instruments.sensor_output.angle_tooltip"));
            angleRow.add(angle);
            content.add(angleRow);
            Tabs sectors = new Tabs();
            for (var channel : new SensorOutputConfig.Channel[]{SensorOutputConfig.Channel.FRONT,
                    SensorOutputConfig.Channel.LEFT, SensorOutputConfig.Channel.RIGHT}) {
                sectors.addTab(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.sector."
                                + channel.name().toLowerCase(java.util.Locale.ROOT)),
                        channelControls(channel, kind, maxRange, draft));
            }
            sectors.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.sectors_tooltip"));
            content.add(sectors);
        } else {
            content.add(Spacer.fixed(1, 5));
            content.add(channelControls(kind.equals("altimeter") ? SensorOutputConfig.Channel.ALTITUDE
                    : SensorOutputConfig.Channel.FRONT, kind, maxRange, draft));
        }

        Stack centeredInventory = new Stack(Alignment.CENTER, Alignment.CENTER);
        centeredInventory.add(InventoryGrid.playerInventory().setSlotSize(16).setGap(1));
        content.add(centeredInventory);
        if (!kind.equals("advanced")) content.add(Spacer.fixed(1, 5));

        Row actions = new Row(8, Alignment.CENTER, Alignment.CENTER);
        Button save = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.apply"),
                () -> {
                    if (draft.submit(position, kind, maxRange)) {
                        draft.closeAlreadyApplied = true;
                        Minecraft.getInstance().setScreen(null);
                    }
                });
        save.setPreferredSize((INVENTORY_WIDTH - 8) / 2, UiTheme.DARK.tokens().buttonMinimumHeight());
        draft.applyButton = save;
        draft.updateApplyEnabled(maxRange, kind);
        Button cancel = new Button(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.cancel"), () -> {
            draft.discardOnClose = true;
            Minecraft.getInstance().setScreen(null);
        });
        cancel.setPreferredSize((INVENTORY_WIDTH - 8) / 2, UiTheme.DARK.tokens().buttonMinimumHeight());
        actions.add(save);
        actions.add(cancel);
        content.add(actions);

        Panel panel = new Panel(content, Insets.all(8));
        panel.setPreferredSize(PANEL_WIDTH, null);
        draft.panel = panel;
        return panel;
    }

    private static String titleKey(String kind) {
        return kind.equals("altimeter") ? "gui.aeronautics_fly_by_instruments.sensor_output.altimeter"
                : kind.equals("advanced") ? "gui.aeronautics_fly_by_instruments.sensor_output.advanced"
                : "gui.aeronautics_fly_by_instruments.sensor_output.terrain";
    }
    private static Column channelControls(SensorOutputConfig.Channel channel, String kind, int maxRange, Draft draft) {
        Column controls = new Column(3, Alignment.START, Alignment.STRETCH);
        if (kind.equals("advanced")) controls.add(Spacer.fixed(1, 4));
        boolean altitude = kind.equals("altimeter");
        Component measurement = Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output."
                + (altitude ? "range_tooltip" : "terrain_range_tooltip"));
        Component mapping = Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output."
                + (altitude ? "range_output_tooltip" : "terrain_output_tooltip"));
        String rangeLabelKey = altitude ? "measure_range" : "terrain_measure_range";
        Label rangeLabel = new Label(Component.translatable(
                "gui.aeronautics_fly_by_instruments.sensor_output." + rangeLabelKey));
        rangeLabel.tooltip(measurement, mapping);
        controls.add(rangeLabel);
        var range = draft.config.range(channel);
        Slider slider = new Slider(0, maxRange, range.min(), 1, ignored -> { });
        slider.setValues(range.min(), range.max());
        slider.setOnRangeChange((lower, upper) -> draft.updateRange(channel, lower, upper, maxRange, kind));
        slider.setPreferredSize(INVENTORY_WIDTH, 22);
        slider.tooltip(measurement, mapping);
        controls.add(slider);

        Row frequencies = new Row(4, Alignment.START, Alignment.CENTER);
        Label frequencyLabel = new Label(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.output_frequency"));
        frequencyLabel.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output."
                        + (kind.equals("advanced") ? "advanced_frequency_tooltip" : "frequency_tooltip")),
                Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.frequency_picker_tooltip"));
        frequencies.add(frequencyLabel);
        var pair = draft.config.frequencies(channel);
        Row slots = new Row(0, Alignment.START, Alignment.CENTER);
        for (boolean first : new boolean[]{true, false}) {
            ItemSlot slot = new ItemSlot(first ? pair.first() : pair.second(),
                    stack -> draft.updateFrequency(channel, first, stack));
            slot.setPreferredSize(19, 19);
            slot.setBackgroundColor(first ? 0xFF9E3434 : 0xFF344D9E);
            slot.tooltip(Component.translatable("gui.aeronautics_fly_by_instruments.sensor_output.frequency_picker_tooltip"));
            slots.add(slot);
        }
        frequencies.add(slots);
        controls.add(frequencies);
        return controls;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public Bounds jeiGuiBounds() { return draft.panel.bounds(); }

    @Override
    protected void layoutRoot(UiTheme theme) {
        int panelWidth = Math.min(PANEL_WIDTH, Math.max(1, width - 24));
        draft.panel.setPreferredSize(panelWidth, null);
        var size = draft.panel.measure(font, theme, panelWidth, Math.max(1, height - 12));
        draft.panel.layout(font, theme, new Bounds((width - size.width()) / 2,
                (height - size.height()) / 2, size.width(), size.height()));
    }

    @Override
    public void onClose() {
        if (!draft.discardOnClose && !draft.closeAlreadyApplied) {
            draft.submit(position, kind, maxRange);
            closeSubmitted = true;
        }
        super.onClose();
    }

    @Override
    public void removed() {
        if (!draft.discardOnClose
                && !draft.closeAlreadyApplied && !closeSubmitted) {
            draft.submit(position, kind, maxRange);
        }
        super.removed();
    }

    private static final class Draft {
        private int angle;
        private final SensorOutputConfig config;
        private Panel panel;
        private Button applyButton;
        private boolean discardOnClose;
        private boolean closeAlreadyApplied;
        private Draft(int angle, SensorOutputConfig config) {
            this.angle = angle; this.config = config.copy();
        }
        private boolean isValid(int maxRange, String kind) {
            return config.ranges().values().stream().allMatch(range -> range.valid(maxRange))
                    && (!kind.equals("advanced") || angle >= 30 && angle <= 180);
        }
        private void updateApplyEnabled(int maxRange, String kind) {
            if (applyButton != null) applyButton.setEnabled(isValid(maxRange, kind));
        }
        private void updateFrequency(SensorOutputConfig.Channel channel, boolean first, ItemStack stack) {
            var old = config.frequencies(channel);
            config.setFrequencies(channel, new SensorOutputConfig.FrequencyPair(
                    first ? stack : old.first(), first ? old.second() : stack));
        }
        private void updateRange(SensorOutputConfig.Channel channel, double lower, Double upper, int maximumRange, String kind) {
            if (upper == null) return;
            var range = new SensorOutputConfig.Range((int) Math.round(lower), (int) Math.round(upper));
            if (range.valid(maximumRange)) config.setRange(channel, range);
            updateApplyEnabled(maximumRange, kind);
        }
        private void updateAngle(double value, int maximumRange, String kind) {
            angle = (int) Math.round(value);
            updateApplyEnabled(maximumRange, kind);
        }
        private boolean submit(BlockPos position, String kind, int maxRange) {
            config.selectAutomaticMode();
            if (isValid(maxRange, kind)) {
                PacketDistributor.sendToServer(new ApplySensorOutputConfigPayload(position, kind, angle, config));
                return true;
            }
            return false;
        }
    }
}
