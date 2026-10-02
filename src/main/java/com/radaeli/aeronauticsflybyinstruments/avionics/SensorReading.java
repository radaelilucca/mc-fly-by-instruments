package com.radaeli.aeronauticsflybyinstruments.avionics;

import java.util.Objects;
import java.util.Optional;

/** Typed sensor value contract shared by future acquisition instruments. */
public record SensorReading<T>(ReadingStatus status, Optional<T> value, String unit, long gameTime) {
    public SensorReading {
        Objects.requireNonNull(status, "status");
        value = Objects.requireNonNull(value, "value");
        unit = Objects.requireNonNull(unit, "unit");
        if ((status == ReadingStatus.VALID) != value.isPresent()) {
            throw new IllegalArgumentException("Only VALID readings contain a value");
        }
    }

    public static <T> SensorReading<T> valid(T value, String unit, long gameTime) {
        return new SensorReading<>(ReadingStatus.VALID, Optional.of(Objects.requireNonNull(value)), unit, gameTime);
    }

    public static <T> SensorReading<T> unavailable(String unit, long gameTime) {
        return new SensorReading<>(ReadingStatus.UNAVAILABLE, Optional.empty(), unit, gameTime);
    }

    public static <T> SensorReading<T> failed(String unit, long gameTime) {
        return new SensorReading<>(ReadingStatus.FAILED, Optional.empty(), unit, gameTime);
    }
}
