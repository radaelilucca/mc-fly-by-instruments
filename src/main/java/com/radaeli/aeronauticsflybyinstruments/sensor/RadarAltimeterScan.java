package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.radaeli.aeronauticsflybyinstruments.avionics.SensorReading;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Deterministic beam geometry and return aggregation shared by the scanner and tests. */
public final class RadarAltimeterScan {
    public static final int CAST_COUNT = 9;
    public static final int MINIMUM_VALID_RETURNS = 3;
    public static final double HALF_ANGLE_DEGREES = 20.0;
    private static final List<Direction> DIRECTIONS = createDirections();

    private RadarAltimeterScan() {}

    /** One center cast and eight evenly spaced casts on a ring around local -Y. */
    public static List<Direction> directions() {
        return DIRECTIONS;
    }

    public static SensorReading<Double> aggregate(List<Double> distances, long gameTime) {
        List<Double> valid = new ArrayList<>();
        for (Double distance : distances) {
            if (distance != null && Double.isFinite(distance) && distance >= 0.0) valid.add(distance);
        }
        if (valid.size() < MINIMUM_VALID_RETURNS) return SensorReading.unavailable("blocks", gameTime);
        valid.sort(Comparator.naturalOrder());
        int middle = valid.size() / 2;
        double median = valid.size() % 2 == 1
                ? valid.get(middle)
                : (valid.get(middle - 1) + valid.get(middle)) / 2.0;
        return SensorReading.valid(median, "blocks", gameTime);
    }

    private static List<Direction> createDirections() {
        double angle = Math.toRadians(HALF_ANGLE_DEGREES);
        double horizontal = Math.sin(angle);
        double down = -Math.cos(angle);
        List<Direction> directions = new ArrayList<>(CAST_COUNT);
        directions.add(new Direction(0.0, -1.0, 0.0));
        for (int index = 0; index < 8; index++) {
            double azimuth = Math.toRadians(index * 45.0);
            directions.add(new Direction(horizontal * Math.cos(azimuth), down,
                    horizontal * Math.sin(azimuth)));
        }
        return List.copyOf(directions);
    }

    public record Direction(double x, double y, double z) {}
}
