package com.radaeli.aeronauticsflybyinstruments.avionics;

import dan200.computercraft.api.lua.LuaFunction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GpwsPeripheralContractTest {
    @Test
    void peripheralExposesTheApprovedMethodsAndEvents() {
        Set<String> methods = Arrays.stream(GpwsComputerPeripheral.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(LuaFunction.class))
                .map(java.lang.reflect.Method::getName).collect(Collectors.toSet());
        assertEquals(Set.of("getStatus", "getAlerts", "getConfiguration", "setMode", "setProfile",
                "setSensitivity", "setAlertConfiguration", "setPhaseOverride"), methods);
        assertEquals("gpws_alert_changed", GpwsComputerPeripheral.ALERT_EVENT);
        assertEquals("gpws_flight_phase_changed", GpwsComputerPeripheral.PHASE_EVENT);
        assertEquals("gpws_mode_changed", GpwsComputerPeripheral.MODE_EVENT);
        assertEquals("gpws_configuration_changed", GpwsComputerPeripheral.CONFIGURATION_EVENT);
    }

    @Test
    void basicConfigurationHasArcadeThresholdsWithoutTerrainHardware() {
        GpwsConfiguration configuration = new GpwsConfiguration();
        assertEquals(GpwsProfile.BASIC, configuration.profile());
        assertEquals(32, configuration.rule(AlertChannel.TERRAIN, FlightPhase.CRUISE).cautionThreshold(), 0.0);
        assertEquals(8, configuration.rule(AlertChannel.TERRAIN, FlightPhase.APPROACH).cautionThreshold(), 0.0);
        assertEquals(2, configuration.rule(AlertChannel.PULL_UP, FlightPhase.DESCENT)
                .secondaryWarningThreshold(), 0.0);
        assertEquals(6, configuration.rule(AlertChannel.SINK_RATE, FlightPhase.DESCENT).cautionThreshold(), 0.0);
    }
}
