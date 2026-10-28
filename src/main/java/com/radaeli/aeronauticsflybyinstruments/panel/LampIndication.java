package com.radaeli.aeronauticsflybyinstruments.panel;

import com.radaeli.aeronauticsflybyinstruments.avionics.AlertChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What a lamp or buzzer indicates, in the player's words (plan §5.3), mapped onto the stored
 * module fields (source, alert channel, minimum severity) so saves and the Lua API keep working.
 *
 * <ul>
 *   <li>{@code warning}: Alerta GPWS, red: any function at WARNING.</li>
 *   <li>{@code caution}: Atenção GPWS, amber: any function at CAUTION or above.</li>
 *   <li>{@code inop}: GPWS INOP, amber: the computer reports INOP or cannot be reached.</li>
 *   <li>{@code function:<id>}: one GPWS function; lit red at WARNING, amber at CAUTION.</li>
 *   <li>{@code redstone_link}: reserved, see plan §7. Stored, never lit yet.</li>
 *   <li>{@code test}: local test signal driven from Lua ({@code setTestSignal}).</li>
 * </ul>
 * Buzzers use only {@code caution} ("todos os alertas") and {@code warning} ("só vermelhos").
 */
public final class LampIndication {
    public static final String WARNING = "warning";
    public static final String CAUTION = "caution";
    public static final String INOP = "inop";
    public static final String FUNCTION = "function:";
    public static final String REDSTONE_LINK = "redstone_link";
    public static final String TEST = "test";
    public static final String SOURCE_REDSTONE_LINK = "redstone_link";
    /** Flip when the Redstone Link mode ships: the scroll cycle and the screen start offering it. */
    public static final boolean REDSTONE_LINK_AVAILABLE = false;

    public enum Color { RED, AMBER }

    private LampIndication() {}

    public static String of(PanelGrid.Module module) {
        return switch (module.outputSource()) {
            case "test" -> TEST;
            case SOURCE_REDSTONE_LINK -> REDSTONE_LINK;
            default -> {
                String channel = module.alertChannel();
                if (channel.equals("gpws.any")) yield module.minimumSeverity() >= 3 ? WARNING : CAUTION;
                if (channel.equals("gpws." + AlertChannel.SENSOR_FAIL.id())) yield INOP;
                yield FUNCTION + channel.substring("gpws.".length());
            }
        };
    }

    /** Functions a lamp can follow one by one (GPWS INOP has its own indication). */
    public static List<String> functions() {
        List<String> result = new ArrayList<>();
        for (AlertChannel channel : AlertChannel.values()) {
            if (channel != AlertChannel.SENSOR_FAIL) result.add(FUNCTION + channel.id());
        }
        return List.copyOf(result);
    }

    /** Every indication a player may pick for this module type, in screen order. */
    public static List<String> choices(String moduleType) {
        if (moduleType.equals("buzzer")) return List.of(CAUTION, WARNING);
        List<String> result = new ArrayList<>(List.of(WARNING, CAUTION, INOP));
        result.addAll(functions());
        result.add(REDSTONE_LINK);
        return List.copyOf(result);
    }

    public static boolean available(String indication) {
        return !indication.equals(REDSTONE_LINK) || REDSTONE_LINK_AVAILABLE;
    }

    /** The wrench scroll cycle: the main indications only; anything else restarts at Alerta GPWS. */
    public static String next(String moduleType, String current, int direction) {
        List<String> cycle = new ArrayList<>(moduleType.equals("buzzer") ? List.of(CAUTION, WARNING)
                : List.of(WARNING, CAUTION));
        if (!moduleType.equals("buzzer") && REDSTONE_LINK_AVAILABLE) cycle.add(REDSTONE_LINK);
        int index = cycle.indexOf(current);
        if (index < 0) return cycle.getFirst();
        return cycle.get(Math.floorMod(index + (direction < 0 ? -1 : 1), cycle.size()));
    }

    /** Stored fields for an indication: {source, alert channel, minimum severity}. */
    public record Fields(String source, String alertChannel, int minimumSeverity) {}

    public static Optional<Fields> fields(String indication) {
        if (indication == null) return Optional.empty();
        if (indication.equals(WARNING)) return Optional.of(new Fields("gpws", "gpws.any", 3));
        if (indication.equals(CAUTION)) return Optional.of(new Fields("gpws", "gpws.any", 2));
        if (indication.equals(INOP)) return Optional.of(new Fields("gpws", "gpws.sensor_fail", 1));
        if (indication.equals(REDSTONE_LINK)) {
            return Optional.of(new Fields(SOURCE_REDSTONE_LINK, "gpws.any", 2));
        }
        if (indication.startsWith(FUNCTION) && functions().contains(indication)) {
            return Optional.of(new Fields("gpws", "gpws." + indication.substring(FUNCTION.length()), 2));
        }
        return Optional.empty();
    }

    /**
     * Lens colour. Fixed for the main indications; a single-function lamp is amber at rest and
     * while cautioning, red while warning. {@code severity} is the module's current severity (0-3).
     */
    public static Color color(String indication, int severity) {
        if (indication.equals(WARNING) || indication.equals(TEST)) return Color.RED;
        if (indication.startsWith(FUNCTION) && severity >= 3) return Color.RED;
        return Color.AMBER;
    }
}
