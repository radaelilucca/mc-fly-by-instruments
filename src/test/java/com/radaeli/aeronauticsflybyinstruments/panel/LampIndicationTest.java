package com.radaeli.aeronauticsflybyinstruments.panel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LampIndicationTest {
    private static PanelGrid.Module module(String type, String source, String channel, int severity) {
        return new PanelGrid.Module(type, 0, 0, 1, 1, "test", 0.5, source, channel, severity, 100);
    }

    @Test
    void storedFieldsReadAsIndications() {
        assertEquals("warning", LampIndication.of(module("lamp", "gpws", "gpws.any", 3)));
        assertEquals("caution", LampIndication.of(module("lamp", "gpws", "gpws.any", 2)));
        assertEquals("caution", LampIndication.of(module("lamp", "gpws", "gpws.any", 1)));
        assertEquals("inop", LampIndication.of(module("lamp", "gpws", "gpws.sensor_fail", 2)));
        assertEquals("function:terrain", LampIndication.of(module("lamp", "gpws", "gpws.terrain", 3)));
        assertEquals("test", LampIndication.of(module("lamp", "test", "gpws.any", 2)));
        assertEquals("redstone_link", LampIndication.of(module("lamp", "redstone_link", "gpws.any", 2)));
    }

    @Test
    void everyOfferedIndicationRoundTripsThroughTheStoredFields() {
        for (String type : new String[]{"lamp", "buzzer"}) {
            for (String indication : LampIndication.choices(type)) {
                var fields = LampIndication.fields(indication).orElseThrow();
                var stored = module(type, fields.source(), fields.alertChannel(), fields.minimumSeverity());
                assertEquals(indication, LampIndication.of(stored), indication);
            }
        }
        assertTrue(LampIndication.fields("function:sensor_fail").isEmpty(), "INOP has its own indication");
        assertTrue(LampIndication.fields("anything").isEmpty());
    }

    @Test
    void buzzersOnlyChooseBetweenAllAlertsAndRedOnly() {
        assertEquals(java.util.List.of("caution", "warning"), LampIndication.choices("buzzer"));
    }

    @Test
    void redstoneLinkIsReservedButNotOfferedYet() {
        assertTrue(LampIndication.choices("lamp").contains("redstone_link"));
        assertFalse(LampIndication.available("redstone_link"));
        assertEquals("caution", LampIndication.next("lamp", "warning", 1));
        assertEquals("warning", LampIndication.next("lamp", "caution", 1));
        assertEquals("warning", LampIndication.next("lamp", "caution", -1));
        assertEquals("warning", LampIndication.next("lamp", "function:terrain", 1), "Other indications restart the cycle");
    }

    @Test
    void colourFollowsTheIndicationAndSingleFunctionsFollowSeverity() {
        assertEquals(LampIndication.Color.RED, LampIndication.color("warning", 0));
        assertEquals(LampIndication.Color.AMBER, LampIndication.color("caution", 3));
        assertEquals(LampIndication.Color.AMBER, LampIndication.color("inop", 2));
        assertEquals(LampIndication.Color.AMBER, LampIndication.color("function:pull_up", 0));
        assertEquals(LampIndication.Color.AMBER, LampIndication.color("function:pull_up", 2));
        assertEquals(LampIndication.Color.RED, LampIndication.color("function:pull_up", 3));
    }
}
