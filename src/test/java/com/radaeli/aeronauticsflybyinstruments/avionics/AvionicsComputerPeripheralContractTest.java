package com.radaeli.aeronauticsflybyinstruments.avionics;

import dan200.computercraft.api.lua.LuaFunction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AvionicsComputerPeripheralContractTest {
    @Test
    void peripheralExposesTheGatewayMethodsAndEvents() {
        Set<String> methods = Arrays.stream(AvionicsComputerPeripheral.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(LuaFunction.class))
                .map(java.lang.reflect.Method::getName).collect(Collectors.toSet());
        assertEquals(Set.of("getStatus", "getSensors", "getAlerts", "getConfiguration", "test",
                "setSensitivity", "setInhibited"), methods);
        assertEquals("avionics_computer", AvionicsComputerPeripheral.TYPE);
        assertEquals("avionics_alert_changed", AvionicsComputerPeripheral.ALERT_EVENT);
        assertEquals("avionics_sensors_changed", AvionicsComputerPeripheral.SENSORS_EVENT);
        assertEquals("avionics_status_changed", AvionicsComputerPeripheral.STATUS_EVENT);
        assertEquals("avionics_configuration_changed", AvionicsComputerPeripheral.CONFIGURATION_EVENT);
    }

    @Test
    void envelopesAreFixedArcadeThresholds() {
        GpwsConfiguration configuration = new GpwsConfiguration();
        assertEquals(32, configuration.rule(AlertChannel.TERRAIN, FlightPhase.CRUISE).cautionThreshold(), 0.0);
        assertEquals(8, configuration.rule(AlertChannel.TERRAIN, FlightPhase.APPROACH).cautionThreshold(), 0.0);
        assertEquals(2, configuration.rule(AlertChannel.PULL_UP, FlightPhase.DESCENT)
                .secondaryWarningThreshold(), 0.0);
        assertEquals(6, configuration.rule(AlertChannel.SINK_RATE, FlightPhase.DESCENT).cautionThreshold(), 0.0);
    }

    @Test
    void sensorsAndAlertsAreDescribedWithStableFields() {
        var sensors = AvionicsComputerPeripheral.describeSensors(List.of(
                new AvionicsSensor("radar_altimeter", "primary", 0, -1, 0, AvionicsSensor.OK,
                        OptionalDouble.of(12.5), "blocks", AvionicsSensor.ISSUE_NONE),
                new AvionicsSensor("terrain_scanner", "front", 0, 0, -2, AvionicsSensor.OK,
                        OptionalDouble.empty(), "blocks", AvionicsSensor.ISSUE_NARROW_APERTURE)));
        assertEquals("primary", sensors.get(0).get("role"));
        assertEquals(12.5, sensors.get(0).get("value"));
        assertFalse(sensors.get(0).containsKey("issue"));
        assertFalse(sensors.get(1).containsKey("value"));
        assertEquals("narrow_aperture", sensors.get(1).get("issue"));

        var alerts = AvionicsComputerPeripheral.describeAlerts(Map.of(AlertChannel.TERRAIN,
                new AlertReading(AlertChannel.TERRAIN, AlertState.UNAVAILABLE, AlertSeverity.ADVISORY, 4)),
                Map.of(AlertChannel.TERRAIN, GpwsEngine.REASON_NO_SCANNER));
        @SuppressWarnings("unchecked")
        var terrain = (Map<String, Object>) alerts.get("terrain");
        assertEquals("unavailable", terrain.get("state"));
        assertEquals("no_scanner", terrain.get("reason"));
    }
}
