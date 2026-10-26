package com.radaeli.aeronauticsflybyinstruments.panel;

import com.radaeli.aeronauticsflybyinstruments.avionics.AlertReading;
import com.radaeli.aeronauticsflybyinstruments.avionics.AlertSeverity;
import com.radaeli.aeronauticsflybyinstruments.avionics.AlertState;

import java.util.Collection;
import java.util.Map;

/** Pure alert routing rules shared by the table tick and unit tests. */
public final class PanelAlertLogic {
    private PanelAlertLogic() { }

    public static int severity(AlertReading reading) {
        if (reading == null || reading.state() != AlertState.ACTIVE) return 0;
        return switch (reading.severity()) {
            case ADVISORY -> 1;
            case CAUTION -> 2;
            case WARNING -> 3;
        };
    }

    public static int maximum(Collection<AlertReading> readings) {
        return readings.stream().mapToInt(PanelAlertLogic::severity).max().orElse(0);
    }

    public static boolean active(int actual, int minimum) {
        return minimum >= 1 && minimum <= 3 && actual >= minimum;
    }

    public static boolean hasNewOrEscalated(Map<String, Integer> current, Map<String, Integer> previous) {
        return current.entrySet().stream().anyMatch(entry ->
                entry.getValue() > previous.getOrDefault(entry.getKey(), 0));
    }

    public static int buzzerInterval(int severity) {
        return switch (severity) {
            case 1 -> 40;
            case 2 -> 20;
            case 3 -> 10;
            default -> 0;
        };
    }

    public static double audibleRadius(int volumePercent) {
        if (volumePercent < 0 || volumePercent > 100) throw new IllegalArgumentException("Volume must be 0..100");
        return 16.0 * volumePercent / 100.0;
    }

    public static float audibleGain(int volumePercent, double distance) {
        double radius = audibleRadius(volumePercent);
        if (radius == 0 || !Double.isFinite(distance) || distance < 0 || distance >= radius) return 0;
        return (float) (volumePercent / 100.0 * (1.0 - distance / radius));
    }

    public static AlertSeverity fromRank(int rank) {
        return switch (rank) {
            case 1 -> AlertSeverity.ADVISORY;
            case 2 -> AlertSeverity.CAUTION;
            case 3 -> AlertSeverity.WARNING;
            default -> null;
        };
    }
}
