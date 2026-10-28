package com.radaeli.aeronauticsflybyinstruments.avionics;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The one state that matters now, as a real GPWS shows it: the highest-priority alert, else INOP,
 * else normal. Drives the block's screen, the header of the computer screen and the API.
 */
public record AvionicsComputerStatus(Level level, AlertChannel channel) {
    public enum Level {
        NORMAL, CAUTION, WARNING, INOP, TEST;

        public String id() { return name().toLowerCase(Locale.ROOT); }

        /** Model variant of the block's screen ({@code screen} blockstate property). */
        public String screen() { return this == TEST ? "warning" : id(); }
    }

    /** Alert priority of a real GPWS: one voice at a time, the most urgent first. */
    public static final List<AlertChannel> PRIORITY = List.of(AlertChannel.PULL_UP, AlertChannel.TERRAIN,
            AlertChannel.TOO_LOW, AlertChannel.SINK_RATE, AlertChannel.DONT_SINK, AlertChannel.BANK_ANGLE);

    public static AvionicsComputerStatus of(Map<AlertChannel, AlertReading> alerts, NetworkStatus network,
                                            boolean testing) {
        if (testing) return new AvionicsComputerStatus(Level.TEST, null);
        for (AlertSeverity severity : List.of(AlertSeverity.WARNING, AlertSeverity.CAUTION)) {
            for (AlertChannel channel : PRIORITY) {
                AlertReading reading = alerts.get(channel);
                if (reading != null && reading.state() == AlertState.ACTIVE && reading.severity() == severity) {
                    return new AvionicsComputerStatus(severity == AlertSeverity.WARNING ? Level.WARNING
                            : Level.CAUTION, channel);
                }
            }
        }
        AlertReading inop = alerts.get(AlertChannel.SENSOR_FAIL);
        if (network != NetworkStatus.CONNECTED || inop != null && inop.state() == AlertState.ACTIVE) {
            return new AvionicsComputerStatus(Level.INOP, AlertChannel.SENSOR_FAIL);
        }
        return new AvionicsComputerStatus(Level.NORMAL, null);
    }

    public String channelId() { return channel == null ? "" : channel.id(); }
}
