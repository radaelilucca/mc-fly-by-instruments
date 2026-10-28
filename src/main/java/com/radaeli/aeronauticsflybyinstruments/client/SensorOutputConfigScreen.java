package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.avionics.ScannerSide;
import com.radaeli.aeronauticsflybyinstruments.config.ModConfig;
import com.radaeli.aeronauticsflybyinstruments.network.ApplySensorOutputConfigPayload;
import com.radaeli.aeronauticsflybyinstruments.network.SensorOutputScreenPayload;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig.Channel;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputLogic;
import com.radaeli.aeronauticsflybyinstruments.sensor.TerrainScanGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Redstone output of the Radar Altimeter and both Terrain Scanners, in the mod's screen language
 * (visual guide §12): steppers for the side of the aircraft a scanner looks at, the aperture and
 * each channel's range, and two Link slots per channel that open {@link FrequencyPickerScreen}.
 * Every change applies immediately.
 */
public final class SensorOutputConfigScreen extends HudScreen {
    static final String GUI = "gui.aeronautics_fly_by_instruments.sensor_output.";
    private static final String BLOCK = "block.aeronautics_fly_by_instruments.";
    private static final int WIDTH = 220;
    static final int FIRST_FREQUENCY_COLOR = 0xFF9E3434;
    static final int SECOND_FREQUENCY_COLOR = 0xFF344D9E;
    private static final int APERTURE_STEP = 5;

    private final BlockPos position;
    private final String kind;
    private final int maxRange;
    private final SensorOutputConfig config;
    private final Consumer<ApplySensorOutputConfigPayload> sender;
    private final Supplier<List<ItemStack>> inventory;
    private int angle;
    /** Empty for the altimeter, which has no side. */
    private String side;
    private Channel selectedChannel;
    private boolean selectedFirst;

    public SensorOutputConfigScreen(SensorOutputScreenPayload payload) {
        this(payload.position(), payload.kind(), payload.openingAngle(), payload.side(), payload.config(),
                payload.kind().equals("altimeter") ? ModConfig.RADAR_ALTIMETER_MAX_RANGE.get()
                        : ModConfig.TERRAIN_SCANNER_MAX_RANGE.get(),
                PacketDistributor::sendToServer, SensorOutputConfigScreen::playerItems);
    }

    SensorOutputConfigScreen(BlockPos position, String kind, int angle, String side, SensorOutputConfig config,
                             int maxRange, Consumer<ApplySensorOutputConfigPayload> sender,
                             Supplier<List<ItemStack>> inventory) {
        super(Component.translatable(GUI + "title"));
        this.position = position.immutable();
        this.kind = kind;
        this.angle = angle;
        this.side = kind.equals("altimeter") ? "" : ScannerSide.parse(side).orElse(ScannerSide.UNSET).id();
        this.config = config.copy();
        this.maxRange = maxRange;
        this.sender = sender;
        this.inventory = inventory;
    }

    static String blockKey(String kind) {
        return BLOCK + (kind.equals("altimeter") ? "radar_altimeter"
                : kind.equals("advanced") ? "terrain_scanner_advanced" : "terrain_scanner");
    }

    boolean advanced() { return kind.equals("advanced"); }
    int angle() { return angle; }
    String side() { return side; }

    /** Tells the Avionics Computer which side of the aircraft this scanner looks at. */
    void chooseSide(ScannerSide chosen) {
        if (side.isEmpty() || chosen == ScannerSide.UNSET || chosen.id().equals(side)) return;
        side = chosen.id();
        apply();
    }
    SensorOutputConfig config() { return config.copy(); }

    /** Channels in screen order: the altimeter's altitude, or the scanner's front then sides. */
    List<Channel> channels() {
        List<Channel> result = new ArrayList<>();
        for (Channel channel : Channel.values()) {
            if (config.range(channel) != null) result.add(channel);
        }
        return result;
    }

    @Override
    protected HudPanel build() {
        var palette = HudPanel.palette();
        config.selectAutomaticMode();
        boolean link = config.mode() == SensorOutputConfig.Mode.LINK;
        HudPanel panel = new HudPanel(WIDTH)
                .header(tr(blockKey(kind)), tr(GUI + (link ? "status.link" : "status.faces")));
        if (!side.isEmpty()) {
            if (side.equals(ScannerSide.UNSET.id())) {
                panel.accent(palette.warning()).highlight(tr(GUI + "side_unset"), palette.warning());
            }
            panel.section(tr(GUI + "scan"));
            panel.stepper(tr(GUI + "side"), tr(GUI + "side." + side), () -> stepSide(-1), () -> stepSide(1));
            panel.tooltip(tr(GUI + "side_tooltip"));
            if (advanced()) {
                panel.stepper(tr(GUI + "aperture"), angle + "°", () -> stepAngle(-APERTURE_STEP),
                        () -> stepAngle(APERTURE_STEP));
                panel.tooltip(tr(GUI + "angle_tooltip"));
            }
        }
        for (Channel channel : channels()) {
            panel.section(channelLabel(channel));
            if (!channelActive(channel)) {
                panel.row(tr(GUI + "output"), tr(GUI + "side_off", SensorOutputLogic.SIDE_SECTORS_MIN_ANGLE),
                        palette.warning(), null);
                continue;
            }
            boolean altitude = channel == Channel.ALTITUDE;
            String rangeTooltip = tr(GUI + (altitude ? "range_tooltip" : "terrain_range_tooltip")) + "\n"
                    + tr(GUI + (altitude ? "range_output_tooltip" : "terrain_output_tooltip")) + "\n"
                    + tr(GUI + "step_tooltip");
            var range = config.range(channel);
            panel.stepper(tr(GUI + (altitude ? "low" : "near")), range.min() + " m",
                    () -> stepRange(channel, false, -step()), () -> stepRange(channel, false, step()));
            panel.tooltip(rangeTooltip);
            panel.stepper(tr(GUI + (altitude ? "high" : "far")), range.max() + " m",
                    () -> stepRange(channel, true, -step()), () -> stepRange(channel, true, step()));
            panel.tooltip(rangeTooltip);
            var pair = config.frequencies(channel);
            String slotTooltip = tr(GUI + "slot_tooltip");
            panel.items(tr(GUI + "link"), List.of(
                    new HudPanel.Slot(pair.first(), FIRST_FREQUENCY_COLOR, false,
                            () -> openPicker(channel, true), () -> clearSlot(channel, true), slotTooltip),
                    new HudPanel.Slot(pair.second(), SECOND_FREQUENCY_COLOR, false,
                            () -> openPicker(channel, false), () -> clearSlot(channel, false), slotTooltip)),
                    tr(linkStatusKey(channel)), linkStatusColor(channel));
            panel.tooltip(tr(GUI + (advanced() ? "advanced_frequency_tooltip" : "frequency_tooltip")));
        }
        return panel;
    }

    /** Opens the frequency picker for one channel with the clicked slot armed. */
    void openPicker(Channel channel, boolean first) {
        selectedChannel = channel;
        selectedFirst = first;
        if (minecraft != null) minecraft.setScreen(new FrequencyPickerScreen(this, channel));
    }

    /** Cycles the side the scanner looks at; an unset scanner starts at the front (or the right, backwards). */
    void stepSide(int delta) {
        List<ScannerSide> sides = List.of(ScannerSide.FRONT, ScannerSide.REAR, ScannerSide.LEFT, ScannerSide.RIGHT);
        int current = sides.indexOf(ScannerSide.parse(side).orElse(ScannerSide.UNSET));
        int next = current < 0 ? (delta > 0 ? 0 : sides.size() - 1) : Math.floorMod(current + delta, sides.size());
        chooseSide(sides.get(next));
    }

    String channelLabel(Channel channel) {
        return tr(GUI + "channel." + channel.name().toLowerCase(Locale.ROOT) + (advanced() ? "" : "_only"));
    }

    private static int step() {
        return hasShiftDown() ? 10 : 1;
    }

    boolean channelActive(Channel channel) {
        boolean side = channel == Channel.LEFT || channel == Channel.RIGHT;
        return !side || angle > SensorOutputLogic.SIDE_SECTORS_MIN_ANGLE;
    }

    void stepAngle(int delta) {
        int updated = Math.clamp(angle + delta, TerrainScanGeometry.MIN_APERTURE_DEGREES,
                TerrainScanGeometry.MAX_APERTURE_DEGREES);
        if (!advanced() || updated == angle) return;
        angle = updated;
        if (selectedChannel != null && !channelActive(selectedChannel)) selectedChannel = null;
        apply();
    }

    /** Moves one end of a channel's range; the ends never cross and stay within the sensor's reach. */
    void stepRange(Channel channel, boolean far, int delta) {
        var range = config.range(channel);
        if (range == null) return;
        var updated = far
                ? new SensorOutputConfig.Range(range.min(), Math.clamp(range.max() + delta, range.min() + 1, maxRange))
                : new SensorOutputConfig.Range(Math.clamp(range.min() + delta, 0, range.max() - 1), range.max());
        if (updated.equals(range) || !updated.valid(maxRange)) return;
        config.setRange(channel, updated);
        apply();
    }

    boolean selected(Channel channel, boolean first) {
        return channel == selectedChannel && first == selectedFirst;
    }

    /** Arms a Link slot for the next picked item; clicking the armed slot again disarms it. */
    void selectSlot(Channel channel, boolean first) {
        if (selected(channel, first)) {
            selectedChannel = null;
        } else {
            selectedChannel = channel;
            selectedFirst = first;
        }
    }

    void clearSlot(Channel channel, boolean first) {
        var pair = config.frequencies(channel);
        if ((first ? pair.first() : pair.second()).isEmpty()) return;
        config.setFrequencies(channel, new SensorOutputConfig.FrequencyPair(
                first ? ItemStack.EMPTY : pair.first(), first ? pair.second() : ItemStack.EMPTY));
        apply();
    }

    /**
     * Puts a copy of the item (never consumed) in the channel's armed slot, or in its first empty
     * slot. The pair's other slot is armed next while it is still empty.
     */
    void pick(Channel channel, ItemStack stack) {
        if (stack.isEmpty() || !channelActive(channel)) return;
        var pair = config.frequencies(channel);
        if (pair == null) return;
        boolean first;
        if (selectedChannel == channel) first = selectedFirst;
        else if (pair.first().isEmpty()) first = true;
        else if (pair.second().isEmpty()) first = false;
        else return;
        config.setFrequencies(channel, new SensorOutputConfig.FrequencyPair(first ? stack : pair.first(),
                first ? pair.second() : stack));
        var updated = config.frequencies(channel);
        if ((first ? updated.second() : updated.first()).isEmpty()) {
            selectedChannel = channel;
            selectedFirst = !first;
        } else {
            selectedChannel = null;
        }
        apply();
    }

    /** An item dragged onto one slot of the pair goes into that slot, whatever is armed. */
    void dropFrequency(Channel channel, boolean first, ItemStack stack) {
        if (!channelActive(channel) || config.frequencies(channel) == null) return;
        selectedChannel = channel;
        selectedFirst = first;
        pick(channel, stack);
    }

    /** Mirrors {@link SensorOutputConfig#selectAutomaticMode()} so the player sees the routing live. */
    String linkStatusKey(Channel channel) {
        var pair = config.frequencies(channel);
        if (pair.enabled()) return GUI + "status.link";
        if (!pair.first().isEmpty() || !pair.second().isEmpty()) return GUI + "status.incomplete";
        boolean linkMode = channels().stream()
                .anyMatch(other -> other != channel && config.frequencies(other).enabled());
        return GUI + (linkMode ? "status.off" : "status.faces");
    }

    private int linkStatusColor(Channel channel) {
        var palette = HudPanel.palette();
        String key = linkStatusKey(channel);
        if (key.endsWith("status.link")) return palette.success();
        return key.endsWith("status.faces") ? palette.mutedText() : palette.warning();
    }

    private void apply() {
        config.selectAutomaticMode();
        boolean validAngle = !advanced() || angle >= TerrainScanGeometry.MIN_APERTURE_DEGREES
                && angle <= TerrainScanGeometry.MAX_APERTURE_DEGREES;
        if (!validAngle || !config.ranges().values().stream().allMatch(range -> range.valid(maxRange))) return;
        sender.accept(new ApplySensorOutputConfigPayload(position, kind, angle, side, config));
    }

    /** The player's inventory slots in their usual order: hotbar 0–8, then the main rows. */
    List<ItemStack> inventory() {
        return inventory.get();
    }

    private static List<ItemStack> playerItems() {
        var player = Minecraft.getInstance().player;
        return player == null ? List.of() : List.copyOf(player.getInventory().items);
    }
}
