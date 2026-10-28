package com.radaeli.aeronauticsflybyinstruments.avionics;

import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

class AvionicsComputerStatusTest {
    private static Map<AlertChannel, AlertReading> alerts(Object... pairs) {
        EnumMap<AlertChannel, AlertReading> result = new EnumMap<>(AlertChannel.class);
        for (AlertChannel channel : AlertChannel.values()) {
            result.put(channel, new AlertReading(channel, AlertState.INACTIVE, AlertSeverity.ADVISORY, 0));
        }
        for (int index = 0; index < pairs.length; index += 2) {
            AlertChannel channel = (AlertChannel) pairs[index];
            result.put(channel, new AlertReading(channel, AlertState.ACTIVE, (AlertSeverity) pairs[index + 1], 0));
        }
        return result;
    }

    @Test
    void warningsWinOverCautionsAndFollowGpwsPriority() {
        var status = AvionicsComputerStatus.of(alerts(AlertChannel.TERRAIN, AlertSeverity.CAUTION,
                AlertChannel.SINK_RATE, AlertSeverity.WARNING, AlertChannel.PULL_UP, AlertSeverity.WARNING),
                NetworkStatus.CONNECTED, false);
        assertEquals(AvionicsComputerStatus.Level.WARNING, status.level());
        assertEquals(AlertChannel.PULL_UP, status.channel());

        var caution = AvionicsComputerStatus.of(alerts(AlertChannel.BANK_ANGLE, AlertSeverity.CAUTION,
                AlertChannel.TERRAIN, AlertSeverity.CAUTION), NetworkStatus.CONNECTED, false);
        assertEquals(AvionicsComputerStatus.Level.CAUTION, caution.level());
        assertEquals(AlertChannel.TERRAIN, caution.channel());
    }

    @Test
    void inopShowsOnlyWithoutRiskAlertsAndTestOverridesEverything() {
        assertEquals(AvionicsComputerStatus.Level.INOP, AvionicsComputerStatus.of(
                alerts(AlertChannel.SENSOR_FAIL, AlertSeverity.CAUTION), NetworkStatus.CONNECTED, false).level());
        assertEquals(AvionicsComputerStatus.Level.INOP,
                AvionicsComputerStatus.of(alerts(), NetworkStatus.DISCONNECTED, false).level());
        assertEquals(AvionicsComputerStatus.Level.NORMAL,
                AvionicsComputerStatus.of(alerts(), NetworkStatus.CONNECTED, false).level());
        assertEquals("warning", AvionicsComputerStatus.of(alerts(), NetworkStatus.CONNECTED, true).level().screen());
    }

    @Test
    void theNoseComesFromTheSideThePlayerGaveTheScanner() {
        assertEquals(Optional.of(Direction.NORTH), ScannerSide.FRONT.nose(Direction.NORTH));
        assertEquals(Optional.of(Direction.NORTH), ScannerSide.REAR.nose(Direction.SOUTH));
        // Looking west at the aircraft's left side puts the nose to the north.
        assertEquals(Optional.of(Direction.NORTH), ScannerSide.LEFT.nose(Direction.WEST));
        assertEquals(Optional.of(Direction.NORTH), ScannerSide.RIGHT.nose(Direction.EAST));
        assertTrue(ScannerSide.UNSET.nose(Direction.NORTH).isEmpty(), "Nothing is inferred from the facing");
    }

    @Test
    void scannerSidesParseTheirIds() {
        for (ScannerSide side : ScannerSide.values()) assertEquals(Optional.of(side), ScannerSide.parse(side.id()));
        assertTrue(ScannerSide.parse("up").isEmpty());
        assertTrue(ScannerSide.parse("").isEmpty());
        assertFalse(ScannerSide.UNSET.set());
    }

    @Test
    void sensorListChangesIgnoreReadingsButNotHealth() {
        var ok = new AvionicsSensor("radar_altimeter", "primary", 0, -1, 0, AvionicsSensor.OK,
                OptionalDouble.of(10), "blocks", AvionicsSensor.ISSUE_NONE);
        var lower = new AvionicsSensor("radar_altimeter", "primary", 0, -1, 0, AvionicsSensor.OK,
                OptionalDouble.of(4), "blocks", AvionicsSensor.ISSUE_NONE);
        var stale = new AvionicsSensor("radar_altimeter", "primary", 0, -1, 0, AvionicsSensor.STALE,
                OptionalDouble.empty(), "blocks", AvionicsSensor.ISSUE_NONE);
        assertFalse(AvionicsComputerBlockEntity.sensorsChanged(List.of(ok), List.of(lower)));
        assertTrue(AvionicsComputerBlockEntity.sensorsChanged(List.of(ok), List.of(stale)));
        assertTrue(AvionicsComputerBlockEntity.sensorsChanged(List.of(ok), List.of()));
    }
}
