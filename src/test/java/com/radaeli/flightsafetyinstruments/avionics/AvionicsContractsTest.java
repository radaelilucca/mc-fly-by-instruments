package com.radaeli.flightsafetyinstruments.avionics;

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
    void stableAlertChannelIdsAndGpwsModesArePresent() {
        assertEquals("dont_sink", AlertChannel.DONT_SINK.id());
        assertEquals(7, AlertChannel.values().length);
        assertEquals(GpwsMode.ARMED, GpwsMode.valueOf("ARMED"));
        assertEquals(5, FlightPhase.values().length);
    }
}
