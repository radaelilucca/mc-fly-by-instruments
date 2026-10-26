package com.radaeli.aeronauticsflybyinstruments.panel;

import com.radaeli.aeronauticsflybyinstruments.avionics.AlertChannel;
import com.radaeli.aeronauticsflybyinstruments.avionics.AlertReading;
import com.radaeli.aeronauticsflybyinstruments.avionics.AlertSeverity;
import com.radaeli.aeronauticsflybyinstruments.avionics.AlertState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PanelAlertLogicTest {
    @Test
    void severityRoutingIgnoresInactiveAndUnavailableReadings() {
        var advisory = new AlertReading(AlertChannel.TERRAIN, AlertState.ACTIVE, AlertSeverity.ADVISORY, 1);
        var warning = new AlertReading(AlertChannel.PULL_UP, AlertState.ACTIVE, AlertSeverity.WARNING, 1);
        var unavailable = new AlertReading(AlertChannel.SENSOR_FAIL, AlertState.UNAVAILABLE, AlertSeverity.WARNING, 1);
        var inactive = new AlertReading(AlertChannel.BANK_ANGLE, AlertState.INACTIVE, AlertSeverity.CAUTION, 1);
        assertEquals(1, PanelAlertLogic.severity(advisory));
        assertEquals(3, PanelAlertLogic.maximum(List.of(advisory, warning, unavailable, inactive)));
        assertEquals(0, PanelAlertLogic.severity(unavailable));
        assertTrue(PanelAlertLogic.active(3, 2));
        assertFalse(PanelAlertLogic.active(1, 2));
        assertFalse(PanelAlertLogic.active(0, 1));
    }

    @Test
    void buzzerCadenceAndVolumeDefineTheExpectedRadiusAndFalloff() {
        assertEquals(40, PanelAlertLogic.buzzerInterval(1));
        assertEquals(20, PanelAlertLogic.buzzerInterval(2));
        assertEquals(10, PanelAlertLogic.buzzerInterval(3));
        assertEquals(0, PanelAlertLogic.buzzerInterval(0));
        assertEquals(16, PanelAlertLogic.audibleRadius(100), 0.0001);
        assertEquals(8, PanelAlertLogic.audibleRadius(50), 0.0001);
        assertEquals(0.25f, PanelAlertLogic.audibleGain(50, 4), 0.0001f);
        assertEquals(0, PanelAlertLogic.audibleGain(50, 8));
        assertEquals(0, PanelAlertLogic.audibleGain(0, 0));
        assertThrows(IllegalArgumentException.class, () -> PanelAlertLogic.audibleRadius(101));
    }

    @Test
    void muteCanDetectBothNewChannelsAndEscalationsWithoutTreatingUnchangedAlertsAsNew() {
        var previous = java.util.Map.of("0,0|gpws.terrain", 2);
        assertFalse(PanelAlertLogic.hasNewOrEscalated(
                java.util.Map.of("0,0|gpws.terrain", 2), previous));
        assertTrue(PanelAlertLogic.hasNewOrEscalated(java.util.Map.of(
                "0,0|gpws.terrain", 2, "0,0|gpws.pull_up", 2), previous));
        assertTrue(PanelAlertLogic.hasNewOrEscalated(
                java.util.Map.of("0,0|gpws.terrain", 3), previous));
        assertFalse(PanelAlertLogic.hasNewOrEscalated(java.util.Map.of(), previous));
    }
}
