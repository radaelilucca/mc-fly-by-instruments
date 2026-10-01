package com.radaeli.flightsafetyinstruments.avionics;

import java.util.Objects;

/** State of one alert channel; activation rules are supplied by the future GPWS engine. */
public record AlertReading(AlertChannel channel, AlertState state, AlertSeverity severity, long gameTime) {
    public AlertReading {
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(severity, "severity");
    }
}
