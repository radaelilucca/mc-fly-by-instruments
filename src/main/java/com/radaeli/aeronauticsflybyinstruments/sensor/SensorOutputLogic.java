package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.radaeli.aeronauticsflybyinstruments.avionics.ReadingStatus;
import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.Map;

/** Pure mapping from cached sensor measurements to the integer redstone channels. */
public final class SensorOutputLogic {
    /** Left/right sectors only produce output when the opening angle is above this value, in degrees. */
    public static final int SIDE_SECTORS_MIN_ANGLE = 50;

    private SensorOutputLogic() {}

    public static Map<SensorOutputConfig.Channel, Integer> altitude(SensorReadingView reading,
                                                                    SensorOutputConfig config) {
        EnumMap<SensorOutputConfig.Channel, Integer> outputs = zero(config);
        if (reading.status() == ReadingStatus.VALID && reading.value() != null) {
            outputs.put(SensorOutputConfig.Channel.ALTITUDE,
                    SensorOutputConfig.altitudePower(reading.value(), config.range(SensorOutputConfig.Channel.ALTITUDE)));
        }
        return Map.copyOf(outputs);
    }

    public static Map<SensorOutputConfig.Channel, Integer> terrain(TerrainScanSnapshot reading,
            SensorOutputConfig config, boolean advanced, int openingAngle) {
        EnumMap<SensorOutputConfig.Channel, Integer> outputs = zero(config);
        if (reading.status() != ReadingStatus.VALID) return Map.copyOf(outputs);
        double front = Double.POSITIVE_INFINITY;
        double left = Double.POSITIVE_INFINITY;
        double right = Double.POSITIVE_INFINITY;
        for (TerrainScanContact contact : reading.contacts()) {
            if (!advanced) {
                front = Math.min(front, contact.distance());
                continue;
            }
            double azimuth = Math.toDegrees(Math.atan2(contact.directionX(), contact.directionZ()));
            if (Math.abs(azimuth) <= 25.0) front = Math.min(front, contact.distance());
            else if (azimuth < -25.0) left = Math.min(left, contact.distance());
            else right = Math.min(right, contact.distance());
        }
        outputs.put(SensorOutputConfig.Channel.FRONT, power(front, config.range(SensorOutputConfig.Channel.FRONT)));
        if (advanced && openingAngle > SIDE_SECTORS_MIN_ANGLE) {
            outputs.put(SensorOutputConfig.Channel.LEFT, power(left, config.range(SensorOutputConfig.Channel.LEFT)));
            outputs.put(SensorOutputConfig.Channel.RIGHT, power(right, config.range(SensorOutputConfig.Channel.RIGHT)));
        }
        return Map.copyOf(outputs);
    }

    private static int power(double distance, SensorOutputConfig.Range range) {
        return Double.isFinite(distance) ? SensorOutputConfig.proximityPower(distance, range) : 0;
    }

    public static int weakPower(Map<SensorOutputConfig.Channel, Integer> outputs,
                                SensorOutputConfig config, boolean altimeter, boolean advanced,
                                Direction facing, Direction face) {
        if (config.mode() == SensorOutputConfig.Mode.LINK) return 0;
        if (altimeter || !advanced) {
            return outputs.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        }
        SensorOutputConfig.Channel channel = SensorOutputConfig.channelForFace(facing, face);
        return channel == null ? 0 : outputs.getOrDefault(channel, 0);
    }

    private static EnumMap<SensorOutputConfig.Channel, Integer> zero(SensorOutputConfig config) {
        EnumMap<SensorOutputConfig.Channel, Integer> result = new EnumMap<>(SensorOutputConfig.Channel.class);
        config.ranges().keySet().forEach(channel -> result.put(channel, 0));
        return result;
    }

    public record SensorReadingView(ReadingStatus status, Double value) {}
}
