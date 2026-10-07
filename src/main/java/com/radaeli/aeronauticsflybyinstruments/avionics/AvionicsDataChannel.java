package com.radaeli.aeronauticsflybyinstruments.avionics;

import java.util.Map;
import java.util.Objects;

/** Typed value published by a connected avionics endpoint over the physical data network. */
public record AvionicsDataChannel<T>(String name, SensorReading<T> reading, Map<String, Object> metadata) {
    public AvionicsDataChannel(String name, SensorReading<T> reading) {
        this(name, reading, Map.of());
    }

    public AvionicsDataChannel {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Channel name is required");
        Objects.requireNonNull(reading, "reading");
        metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata"));
    }
}
