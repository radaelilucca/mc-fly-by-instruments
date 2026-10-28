package com.radaeli.aeronauticsflybyinstruments.avionics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvionicsContractsTest {
    @Test
    void readingsRequireValuesOnlyWhenValid() {
        assertEquals(12.5, SensorReading.valid(12.5, "blocks", 40).value().orElseThrow());
        assertTrue(SensorReading.<Double>unavailable("blocks", 41).value().isEmpty());
        assertTrue(SensorReading.<Double>failed("blocks", 42).value().isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> new SensorReading<>(ReadingStatus.VALID, java.util.Optional.empty(), "blocks", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new SensorReading<>(ReadingStatus.FAILED, java.util.Optional.of(1.0), "blocks", 0));
    }

    @Test
    void stableAlertChannelIdsAndInternalPhasesArePresent() {
        assertEquals("dont_sink", AlertChannel.DONT_SINK.id());
        assertEquals(7, AlertChannel.values().length);
        assertEquals(AlertState.INHIBITED, AlertState.valueOf("INHIBITED"));
        assertEquals(7, FlightPhase.values().length);
        assertEquals(FlightPhase.GROUND, FlightPhase.valueOf("GROUND"));
        assertEquals(FlightPhase.UNKNOWN, FlightPhase.valueOf("UNKNOWN"));
    }
}
