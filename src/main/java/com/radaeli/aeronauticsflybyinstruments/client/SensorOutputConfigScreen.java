package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.client.DynamicText.Line;
import com.radaeli.aeronauticsflybyinstruments.client.DynamicText.Tone;
import com.radaeli.aeronauticsflybyinstruments.config.ModConfig;
import com.radaeli.aeronauticsflybyinstruments.network.ApplySensorOutputConfigPayload;
import com.radaeli.aeronauticsflybyinstruments.network.SensorOutputScreenPayload;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig.Channel;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputLogic;
import com.radaeli.ruilib.ui.Alignment;
import com.radaeli.ruilib.ui.Bounds;
import com.radaeli.ruilib.ui.Button;
import com.radaeli.ruilib.ui.Column;
import com.radaeli.ruilib.ui.InventoryGrid;
import com.radaeli.ruilib.ui.Insets;
import com.radaeli.ruilib.ui.ItemSlot;
import com.radaeli.ruilib.ui.Panel;
import com.radaeli.ruilib.ui.Row;
import com.radaeli.ruilib.ui.Slider;
import com.radaeli.ruilib.ui.Spacer;
import com.radaeli.ruilib.ui.Tabs;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiScreen;
import com.radaeli.ruilib.ui.UiTheme;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Staged range/frequency editor shared by the three raycast/range sensors.
 *
 * <p>Visual hierarchy: the block name is the only heading; control labels use the primary text
 * color; captions and statuses use muted or semantic colors and update live with the draft.
 * See {@link SensorScreenLayout} for the responsive arrangement.</p>
 */
public final class SensorOutputConfigScreen extends UiScreen {
    private static final String GUI = "gui.aeronautics_fly_by_instruments.sensor_output.";
    private static final String BLOCK = "block.aeronautics_fly_by_instruments.";
    private static final Component TITLE = Component.translatable(GUI + "title");
    private static final int SCREEN_MARGIN = 12;
    private static final int FREQUENCY_SLOT_SIZE = 18;
    private static final int FIRST_FREQUENCY_COLOR = 0xFF9E3434;
    private static final int SECOND_FREQUENCY_COLOR = 0xFF344D9E;
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
        Column settings = new Column(5, Alignment.START, Alignment.STRETCH);
        if (kind.equals("advanced")) {
            settings.add(apertureRow(kind, maxRange, draft));
            settings.add(sectorTabs(kind, maxRange, draft));
        } else {
            Channel channel = kind.equals("altimeter") ? Channel.ALTITUDE : Channel.FRONT;
            settings.add(channelControls(channel, kind, maxRange, draft, true));
        }
        draft.refreshSideSectors();

        // Bold keeps the heading crisp in the pixel font; scaling it would blur the glyphs.
        DynamicText header = DynamicText.of(Component.translatable(blockKey(kind)).withStyle(ChatFormatting.BOLD),
                Tone.TEXT);
        DynamicText pickerHint = DynamicText.of(Component.translatable(GUI + "picker_hint"), Tone.MUTED);
        pickerHint.tooltip(Component.translatable(GUI + "frequency_picker_tooltip"));
        InventoryGrid picker = InventoryGrid.playerInventory().setSlotSize(16).setGap(1);

        SensorScreenLayout layout = new SensorScreenLayout(header, settings, pickerHint, picker,
                actions(position, kind, maxRange, draft));
        Panel panel = new Panel(layout, Insets.all(8));
        draft.panel = panel;
        draft.layout = layout;
        return panel;
    }

    static String blockKey(String kind) {
        return BLOCK + (kind.equals("altimeter") ? "radar_altimeter"
                : kind.equals("advanced") ? "terrain_scanner_advanced" : "terrain_scanner");
    }

    private static Row apertureRow(String kind, int maxRange, Draft draft) {
        Component tooltip = Component.translatable(GUI + "angle_tooltip");
        Row row = new Row(6, Alignment.START, Alignment.CENTER);
        DynamicText label = DynamicText.of(Component.translatable(GUI + "aperture"), Tone.TEXT);
        label.tooltip(tooltip);
        row.add(label);
        Slider angle = new Slider(30, 180, draft.angle, 1, value -> draft.updateAngle(value, maxRange, kind));
        angle.setValueFormatter(value -> Math.round(value) + "°");
        angle.tooltip(tooltip);
        row.add(angle);
        return row;
    }

    private static Tabs sectorTabs(String kind, int maxRange, Draft draft) {
        Tabs sectors = new Tabs();
        for (Channel channel : new Channel[]{Channel.FRONT, Channel.LEFT, Channel.RIGHT}) {
            // The tab strip has no bottom margin of its own; the page adds it so the first control
            // does not touch the selected tab.
            Column page = new Column(0, Alignment.START, Alignment.STRETCH);
            page.add(Spacer.fixed(1, 5));
            page.add(channelControls(channel, kind, maxRange, draft, false));
            sectors.addTab(Component.translatable(GUI + "sector." + channel.name().toLowerCase(Locale.ROOT)), page);
        }
        sectors.tooltip(Component.translatable(GUI + "sectors_tooltip"));
        return sectors;
    }

    private static Column channelControls(Channel channel, String kind, int maxRange, Draft draft,
                                          boolean labelled) {
        boolean altitude = channel == Channel.ALTITUDE;
        Component measurement = Component.translatable(GUI + (altitude ? "range_tooltip" : "terrain_range_tooltip"));
        Component mapping = Component.translatable(GUI + (altitude ? "range_output_tooltip" : "terrain_output_tooltip"));
        Column controls = new Column(0, Alignment.START, Alignment.STRETCH);
        if (labelled) {
            DynamicText label = DynamicText.of(Component.translatable(GUI
                    + (altitude ? "altitude_range" : "detection_range")), Tone.TEXT);
            label.tooltip(measurement, mapping);
            controls.add(label);
            controls.add(Spacer.fixed(1, 2));
        }

        var range = draft.config.range(channel);
        Slider slider = new Slider(0, maxRange, range.min(), 1, ignored -> { });
        slider.setValues(range.min(), range.max());
        slider.setOnRangeChange((lower, upper) -> draft.updateRange(channel, lower, upper, maxRange, kind));
        slider.tooltip(measurement, mapping);
        controls.add(slider);
        controls.add(Spacer.fixed(1, 2));
        DynamicText caption = new DynamicText(() -> draft.caption(channel));
        caption.tooltip(mapping);
        controls.add(caption);
        controls.add(Spacer.fixed(1, 4));

        Component linkTooltip = Component.translatable(GUI
                + (kind.equals("advanced") ? "advanced_frequency_tooltip" : "frequency_tooltip"));
        Component pickerTooltip = Component.translatable(GUI + "frequency_picker_tooltip");
        Row link = new Row(4, Alignment.START, Alignment.CENTER);
        DynamicText linkLabel = DynamicText.of(Component.translatable(GUI + "link"), Tone.TEXT);
        linkLabel.tooltip(linkTooltip, pickerTooltip);
        link.add(linkLabel);
        var pair = draft.config.frequencies(channel);
        Row slots = new Row(2, Alignment.START, Alignment.CENTER);
        List<UiComponent> sideControls = new ArrayList<>(List.of(slider));
        for (boolean first : new boolean[]{true, false}) {
            ItemSlot slot = new ItemSlot(first ? pair.first() : pair.second(),
                    stack -> draft.updateFrequency(channel, first, stack));
            slot.setPreferredSize(FREQUENCY_SLOT_SIZE, FREQUENCY_SLOT_SIZE);
            slot.setBackgroundColor(first ? FIRST_FREQUENCY_COLOR : SECOND_FREQUENCY_COLOR);
            slot.tooltip(pickerTooltip);
            slots.add(slot);
            sideControls.add(slot);
        }
        link.add(slots);
        DynamicText status = new DynamicText(() -> draft.linkStatus(channel)).fillWidth();
        status.tooltip(linkTooltip);
        link.add(status);
        controls.add(link);
        draft.registerSideControls(channel, sideControls);
        return controls;
    }

    private static Row actions(BlockPos position, String kind, int maxRange, Draft draft) {
        int buttonWidth = (SensorScreenLayout.COLUMN_WIDTH - 8) / 2;
        Row actions = new Row(8, Alignment.CENTER, Alignment.CENTER);
        Button save = new Button(Component.translatable(GUI + "apply"), () -> {
            if (draft.submit(position, kind, maxRange)) {
                draft.closeAlreadyApplied = true;
                Minecraft.getInstance().setScreen(null);
            }
        });
        save.setPreferredSize(buttonWidth, UiTheme.DARK.tokens().buttonMinimumHeight());
        draft.applyButton = save;
        draft.updateApplyEnabled(maxRange, kind);
        Button cancel = new Button(Component.translatable(GUI + "cancel"), () -> {
            draft.discardOnClose = true;
            Minecraft.getInstance().setScreen(null);
        });
        cancel.setPreferredSize(buttonWidth, UiTheme.DARK.tokens().buttonMinimumHeight());
        actions.add(save);
        actions.add(cancel);
        return actions;
    }

    @Override public boolean isPauseScreen() { return false; }
    @Override public Bounds jeiGuiBounds() { return draft.panel.bounds(); }

    SensorScreenLayout.Mode layoutMode() {
        return draft.layout.mode();
    }

    @Override
    protected void layoutRoot(UiTheme theme) {
        var size = draft.panel.measure(font, theme, Math.max(1, width - 2 * SCREEN_MARGIN),
                Math.max(1, height - SCREEN_MARGIN));
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

    static final class Draft {
        private int angle;
        private final SensorOutputConfig config;
        private final Map<Channel, List<UiComponent>> sideControls = new EnumMap<>(Channel.class);
        private Panel panel;
        private SensorScreenLayout layout;
        private Button applyButton;
        private boolean discardOnClose;
        private boolean closeAlreadyApplied;

        private Draft(int angle, SensorOutputConfig config) {
            this.angle = angle;
            this.config = config.copy();
        }

        private boolean isValid(int maxRange, String kind) {
            return config.ranges().values().stream().allMatch(range -> range.valid(maxRange))
                    && (!kind.equals("advanced") || angle >= 30 && angle <= 180);
        }

        private void updateApplyEnabled(int maxRange, String kind) {
            if (applyButton != null) applyButton.setEnabled(isValid(maxRange, kind));
        }

        private void updateFrequency(Channel channel, boolean first, ItemStack stack) {
            var old = config.frequencies(channel);
            config.setFrequencies(channel, new SensorOutputConfig.FrequencyPair(
                    first ? stack : old.first(), first ? old.second() : stack));
        }

        private void updateRange(Channel channel, double lower, Double upper, int maximumRange, String kind) {
            if (upper == null) return;
            var range = new SensorOutputConfig.Range((int) Math.round(lower), (int) Math.round(upper));
            if (range.valid(maximumRange)) config.setRange(channel, range);
            updateApplyEnabled(maximumRange, kind);
        }

        private void updateAngle(double value, int maximumRange, String kind) {
            angle = (int) Math.round(value);
            refreshSideSectors();
            updateApplyEnabled(maximumRange, kind);
        }

        private void registerSideControls(Channel channel, List<UiComponent> controls) {
            if (isSide(channel)) sideControls.put(channel, List.copyOf(controls));
        }

        /** Side sectors stay visible but inert while the scan is too narrow to feed them. */
        private void refreshSideSectors() {
            boolean active = sideSectorsActive();
            sideControls.values().forEach(controls -> controls.forEach(control -> control.setEnabled(active)));
        }

        private boolean sideSectorsActive() {
            return angle > SensorOutputLogic.SIDE_SECTORS_MIN_ANGLE;
        }

        private static boolean isSide(Channel channel) {
            return channel == Channel.LEFT || channel == Channel.RIGHT;
        }

        Line caption(Channel channel) {
            if (isSide(channel) && !sideSectorsActive()) {
                return new Line(Component.translatable(GUI + "side_off",
                        SensorOutputLogic.SIDE_SECTORS_MIN_ANGLE), Tone.WARNING);
            }
            var range = config.range(channel);
            String key = channel == Channel.ALTITUDE ? "caption.altitude" : "caption.distance";
            return new Line(Component.translatable(GUI + key, range.min(), range.max()), Tone.MUTED);
        }

        /** Mirrors {@link SensorOutputConfig#selectAutomaticMode()} so the player sees the routing before saving. */
        Line linkStatus(Channel channel) {
            var pair = config.frequencies(channel);
            if (pair.enabled()) return new Line(Component.translatable(GUI + "status.link"), Tone.SUCCESS);
            if (!pair.first().isEmpty() || !pair.second().isEmpty()) {
                return new Line(Component.translatable(GUI + "status.incomplete"), Tone.WARNING);
            }
            boolean linkMode = config.ranges().keySet().stream()
                    .anyMatch(other -> other != channel && config.frequencies(other).enabled());
            return linkMode ? new Line(Component.translatable(GUI + "status.off"), Tone.WARNING)
                    : new Line(Component.translatable(GUI + "status.faces"), Tone.MUTED);
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

    Draft draft() {
        return draft;
    }
}
