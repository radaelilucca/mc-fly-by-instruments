package com.radaeli.aeronauticsflybyinstruments.avionics;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaTable;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** CC:Tweaked interface for reading GPWS state and changing server-authoritative settings. */
public final class GpwsComputerPeripheral implements IPeripheral {
    public static final String ALERT_EVENT = "gpws_alert_changed";
    public static final String PHASE_EVENT = "gpws_flight_phase_changed";
    public static final String MODE_EVENT = "gpws_mode_changed";
    public static final String CONFIGURATION_EVENT = "gpws_configuration_changed";

    private final GpwsComputerBlockEntity computer;
    private final Set<IComputerAccess> computers = ConcurrentHashMap.newKeySet();

    public GpwsComputerPeripheral(GpwsComputerBlockEntity computer) { this.computer = computer; }

    @Override public String getType() { return "gpws"; }
    @Override public Object getTarget() { return computer; }
    @Override public boolean equals(IPeripheral other) {
        return other instanceof GpwsComputerPeripheral peripheral && peripheral.computer == computer;
    }
    @Override public void attach(IComputerAccess attached) { computers.add(attached); }
    @Override public void detach(IComputerAccess detached) { computers.remove(detached); }

    @LuaFunction(mainThread = true)
    public Map<String, Object> getStatus() { return computer.statusSnapshot(); }

    @LuaFunction(mainThread = true)
    public Map<String, Object> getAlerts() {
        Map<String, Object> result = new HashMap<>();
        computer.evaluation().alerts().forEach((channel, reading) -> result.put(channel.id(), describe(reading)));
        return Map.copyOf(result);
    }

    @LuaFunction(mainThread = true)
    public Map<String, Object> getConfiguration() { return configurationSnapshot(); }

    @LuaFunction(mainThread = true)
    public boolean setMode(String value) {
        return parse(GpwsMode.class, value).map(computer::setMode).orElse(false);
    }

    @LuaFunction(mainThread = true)
    public boolean setProfile(String value) {
        return parse(GpwsProfile.class, value).map(computer::setProfile).orElse(false);
    }

    @LuaFunction(mainThread = true)
    public boolean setSensitivity(String value) {
        return parse(GpwsSensitivity.class, value).map(computer::setSensitivity).orElse(false);
    }

    @LuaFunction(mainThread = true)
    public boolean setAlertConfiguration(String channelName, String phaseName, LuaTable<?, ?> values) {
        var channel = parse(AlertChannel.class, channelName);
        var phase = parse(FlightPhase.class, phaseName);
        if (channel.isEmpty() || phase.isEmpty() || values == null || values.isEmpty()) return false;
        GpwsRule current = computer.configuration().rule(channel.get(), phase.get());
        double caution = current.cautionThreshold();
        double warning = current.warningThreshold();
        double secondary = current.secondaryWarningThreshold();
        double hysteresis = current.hysteresisPercent();
        int confirmations = current.confirmationSamples();
        boolean inhibited = current.inhibited();
        try {
            for (Map.Entry<?, ?> entry : values.entrySet()) {
                if (!(entry.getKey() instanceof String key)) return false;
                switch (key) {
                    case "cautionThreshold" -> caution = finiteNumber(entry.getValue());
                    case "warningThreshold" -> warning = finiteNumber(entry.getValue());
                    case "secondaryWarningThreshold" -> secondary = finiteNumber(entry.getValue());
                    case "hysteresisPercent" -> hysteresis = finiteNumber(entry.getValue());
                    case "confirmationSamples" -> {
                        double number = finiteNumber(entry.getValue());
                        if (number != Math.rint(number)) return false;
                        confirmations = (int) number;
                    }
                    case "inhibited" -> {
                        if (!(entry.getValue() instanceof Boolean value)) return false;
                        inhibited = value;
                    }
                    default -> { return false; }
                }
            }
            return computer.setAlertRule(channel.get(), phase.get(),
                    new GpwsRule(caution, warning, secondary, hysteresis, confirmations, inhibited));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @LuaFunction(mainThread = true)
    public boolean setPhaseOverride(String value) {
        if (value == null) return false;
        if (value.equalsIgnoreCase("auto")) return computer.setPhaseOverride(null);
        return parse(FlightPhase.class, value).map(computer::setPhaseOverride).orElse(false);
    }

    public void publishAlertsChanged(Map<AlertChannel, AlertReading> changed) {
        Map<String, Object> snapshot = new HashMap<>();
        changed.forEach((channel, reading) -> snapshot.put(channel.id(), describe(reading)));
        publish(ALERT_EVENT, Map.copyOf(snapshot));
    }

    public void publishPhaseChanged(FlightPhase phase, long gameTime) {
        publish(PHASE_EVENT, Map.of("phase", lower(phase), "gameTime", gameTime));
    }

    public void publishModeChanged(GpwsMode mode, GpwsProfile profile) {
        publish(MODE_EVENT, Map.of("mode", lower(mode), "profile", lower(profile)));
    }

    public void publishConfigurationChanged() {
        publish(CONFIGURATION_EVENT, configurationSnapshot());
    }

    public Map<String, Object> configurationSnapshot() {
        Map<String, Object> result = new HashMap<>();
        result.put("profile", lower(computer.configuration().profile()));
        result.put("sensitivity", lower(computer.configuration().sensitivity()));
        result.put("phaseOverride", computer.configuration().phaseOverride()
                .map(GpwsComputerPeripheral::lower).orElse("auto"));
        Map<String, Object> rules = new HashMap<>();
        for (AlertChannel channel : AlertChannel.values()) {
            Map<String, Object> phases = new HashMap<>();
            for (FlightPhase phase : FlightPhase.values()) {
                phases.put(lower(phase), describeRule(computer.configuration().rule(channel, phase)));
            }
            rules.put(channel.id(), Map.copyOf(phases));
        }
        result.put("rules", Map.copyOf(rules));
        return Map.copyOf(result);
    }

    private void publish(String event, Map<String, Object> snapshot) {
        for (IComputerAccess attached : computers) {
            attached.queueEvent(event, attached.getAttachmentName(), snapshot);
        }
    }

    public static Map<String, Object> describe(AlertReading reading) {
        return Map.of("state", lower(reading.state()), "severity", lower(reading.severity()),
                "gameTime", reading.gameTime());
    }

    public static boolean alertStateChanged(AlertReading previous, AlertReading updated) {
        return previous == null || previous.state() != updated.state() || previous.severity() != updated.severity();
    }

    private static Map<String, Object> describeRule(GpwsRule rule) {
        return Map.of("cautionThreshold", rule.cautionThreshold(), "warningThreshold", rule.warningThreshold(),
                "secondaryWarningThreshold", rule.secondaryWarningThreshold(),
                "hysteresisPercent", rule.hysteresisPercent(), "confirmationSamples", rule.confirmationSamples(),
                "inhibited", rule.inhibited());
    }

    private static double finiteNumber(Object value) {
        if (!(value instanceof Number number) || !Double.isFinite(number.doubleValue())) {
            throw new IllegalArgumentException("Expected a finite number");
        }
        return number.doubleValue();
    }

    private static <E extends Enum<E>> java.util.Optional<E> parse(Class<E> type, String value) {
        if (value == null) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(Enum.valueOf(type, value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }

    private static String lower(Enum<?> value) { return value.name().toLowerCase(Locale.ROOT); }
}
