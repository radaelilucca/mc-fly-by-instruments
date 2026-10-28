package com.radaeli.aeronauticsflybyinstruments.avionics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The only player settings of the GPWS function: one global sensitivity and per-function
 * inhibits. Envelopes are fixed, as on a certified GPWS (docs/avionics-computer-plan.md).
 */
public final class GpwsConfiguration {
    private GpwsSensitivity sensitivity = GpwsSensitivity.NORMAL;
    private final EnumSet<AlertChannel> inhibited = EnumSet.noneOf(AlertChannel.class);

    public GpwsSensitivity sensitivity() { return sensitivity; }
    public boolean inhibited(AlertChannel channel) { return inhibited.contains(channel); }
    public Set<AlertChannel> inhibitedChannels() { return Set.copyOf(inhibited); }

    public boolean setSensitivity(GpwsSensitivity updated) {
        if (updated == null || sensitivity == updated) return false;
        sensitivity = updated;
        return true;
    }

    /** GPWS INOP reports the system's own health and can never be inhibited. */
    public static boolean inhibitable(AlertChannel channel) {
        return channel != null && channel != AlertChannel.SENSOR_FAIL;
    }

    public boolean setInhibited(AlertChannel channel, boolean value) {
        if (!inhibitable(channel)) return false;
        return value ? inhibited.add(channel) : inhibited.remove(channel);
    }

    public GpwsRule rule(AlertChannel channel, FlightPhase phase) {
        return envelope(channel, phase);
    }

    public void save(CompoundTag tag) {
        tag.putString("Sensitivity", sensitivity.name());
        ListTag list = new ListTag();
        inhibited.forEach(channel -> list.add(StringTag.valueOf(channel.name())));
        tag.put("Inhibited", list);
    }

    /** Settings of the old GPWS Computer (profiles, modes, rule overrides) are dropped on purpose. */
    public static GpwsConfiguration load(CompoundTag tag) {
        GpwsConfiguration configuration = new GpwsConfiguration();
        parse(GpwsSensitivity.class, tag.getString("Sensitivity")).ifPresent(configuration::setSensitivity);
        ListTag list = tag.getList("Inhibited", Tag.TAG_STRING);
        for (int index = 0; index < list.size(); index++) {
            parse(AlertChannel.class, list.getString(index)).ifPresent(channel -> configuration.setInhibited(channel, true));
        }
        return configuration;
    }

    private static <E extends Enum<E>> Optional<E> parse(Class<E> type, String value) {
        try {
            return Optional.of(Enum.valueOf(type, value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    /** Fixed envelope per function and internal flight phase, in blocks, blocks/s, seconds and degrees. */
    public static GpwsRule envelope(AlertChannel channel, FlightPhase phase) {
        double caution = 0;
        double warning = 0;
        double secondary = 0;
        switch (channel) {
            case TERRAIN -> caution = phase == FlightPhase.APPROACH ? 8 : 32;
            case PULL_UP -> {
                warning = 8;      // Blocks to a known contact in the central forward cone.
                secondary = 2;    // Seconds to ground during descent.
            }
            case SINK_RATE -> { caution = 6; warning = 12; }
            case DONT_SINK -> warning = 4; // Blocks lost from a recent takeoff/climb peak.
            case TOO_LOW -> {
                caution = phase == FlightPhase.APPROACH ? 3 : 8;
                warning = phase == FlightPhase.APPROACH ? 1 : 3;
            }
            case BANK_ANGLE -> { caution = 30; warning = 45; }
            case SENSOR_FAIL -> { }
        }
        return new GpwsRule(caution, warning, secondary, 10, 2);
    }
}
