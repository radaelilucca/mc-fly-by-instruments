package com.radaeli.aeronauticsflybyinstruments.avionics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Persistable player settings. Runtime arming state intentionally lives elsewhere. */
public final class GpwsConfiguration {
    private GpwsProfile profile = GpwsProfile.BASIC;
    private GpwsSensitivity sensitivity = GpwsSensitivity.NORMAL;
    private FlightPhase phaseOverride;
    private final EnumMap<AlertChannel, EnumMap<FlightPhase, GpwsRule>> overrides =
            new EnumMap<>(AlertChannel.class);

    public GpwsProfile profile() { return profile; }
    public GpwsSensitivity sensitivity() { return sensitivity; }
    public Optional<FlightPhase> phaseOverride() { return Optional.ofNullable(phaseOverride); }

    public boolean setProfile(GpwsProfile updated) {
        if (updated == null || profile == updated) return false;
        profile = updated;
        if (updated == GpwsProfile.BASIC) {
            phaseOverride = null;
            overrides.clear();
        }
        return true;
    }

    public boolean setSensitivity(GpwsSensitivity updated) {
        if (updated == null || sensitivity == updated) return false;
        sensitivity = updated;
        return true;
    }

    public boolean setPhaseOverride(FlightPhase updated) {
        if (phaseOverride == updated) return false;
        phaseOverride = updated;
        return true;
    }

    public FlightPhase effectivePhase(FlightPhase automatic) {
        return phaseOverride == null ? automatic : phaseOverride;
    }

    public GpwsRule rule(AlertChannel channel, FlightPhase phase) {
        if (profile == GpwsProfile.BASIC) return defaultRule(channel, phase);
        EnumMap<FlightPhase, GpwsRule> phaseRules = overrides.get(channel);
        return phaseRules == null ? defaultRule(channel, phase) : phaseRules.getOrDefault(phase,
                defaultRule(channel, phase));
    }

    public boolean setRule(AlertChannel channel, FlightPhase phase, GpwsRule rule) {
        if (profile != GpwsProfile.ADVANCED || channel == null || phase == null || rule == null
                || rule.equals(rule(channel, phase))) return false;
        overrides.computeIfAbsent(channel, ignored -> new EnumMap<>(FlightPhase.class)).put(phase, rule);
        return true;
    }

    public Map<AlertChannel, Map<FlightPhase, GpwsRule>> overrides() {
        EnumMap<AlertChannel, Map<FlightPhase, GpwsRule>> copy = new EnumMap<>(AlertChannel.class);
        overrides.forEach((channel, rules) -> copy.put(channel, Map.copyOf(rules)));
        return Map.copyOf(copy);
    }

    public void save(CompoundTag tag) {
        tag.putString("Profile", profile.name());
        tag.putString("Sensitivity", sensitivity.name());
        tag.putString("PhaseOverride", phaseOverride == null ? "AUTO" : phaseOverride.name());
        ListTag rules = new ListTag();
        overrides.forEach((channel, phaseRules) -> phaseRules.forEach((phase, rule) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString("Channel", channel.name());
            entry.putString("Phase", phase.name());
            entry.putDouble("Caution", rule.cautionThreshold());
            entry.putDouble("Warning", rule.warningThreshold());
            entry.putDouble("SecondaryWarning", rule.secondaryWarningThreshold());
            entry.putDouble("HysteresisPercent", rule.hysteresisPercent());
            entry.putInt("ConfirmationSamples", rule.confirmationSamples());
            entry.putBoolean("Inhibited", rule.inhibited());
            rules.add(entry);
        }));
        tag.put("RuleOverrides", rules);
    }

    public static GpwsConfiguration load(CompoundTag tag) {
        GpwsConfiguration configuration = new GpwsConfiguration();
        parse(GpwsProfile.class, tag.getString("Profile")).ifPresent(configuration::setProfile);
        parse(GpwsSensitivity.class, tag.getString("Sensitivity")).ifPresent(configuration::setSensitivity);
        String phase = tag.getString("PhaseOverride");
        if (configuration.profile == GpwsProfile.ADVANCED && !phase.equals("AUTO")) {
            parse(FlightPhase.class, phase).ifPresent(configuration::setPhaseOverride);
        }
        if (configuration.profile != GpwsProfile.ADVANCED) return configuration;
        ListTag rules = tag.getList("RuleOverrides", Tag.TAG_COMPOUND);
        for (int index = 0; index < rules.size(); index++) {
            CompoundTag entry = rules.getCompound(index);
            var channel = parse(AlertChannel.class, entry.getString("Channel"));
            var rulePhase = parse(FlightPhase.class, entry.getString("Phase"));
            if (channel.isEmpty() || rulePhase.isEmpty()) continue;
            try {
                GpwsRule rule = new GpwsRule(entry.getDouble("Caution"), entry.getDouble("Warning"),
                        entry.getDouble("SecondaryWarning"), entry.getDouble("HysteresisPercent"),
                        entry.getInt("ConfirmationSamples"), entry.getBoolean("Inhibited"));
                configuration.setRule(channel.get(), rulePhase.get(), rule);
            } catch (IllegalArgumentException ignored) {
                // A malformed or out-of-range saved override is discarded; safe defaults remain active.
            }
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

    public static GpwsRule defaultRule(AlertChannel channel, FlightPhase phase) {
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
        return new GpwsRule(caution, warning, secondary, 10, 2, false);
    }
}
