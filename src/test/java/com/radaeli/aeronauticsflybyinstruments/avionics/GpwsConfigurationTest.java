package com.radaeli.aeronauticsflybyinstruments.avionics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GpwsConfigurationTest {
    @Test
    void sensitivityAndInhibitsPersist() {
        GpwsConfiguration configuration = new GpwsConfiguration();
        assertTrue(configuration.setSensitivity(GpwsSensitivity.HIGH));
        assertTrue(configuration.setInhibited(AlertChannel.TOO_LOW, true));
        assertFalse(configuration.setInhibited(AlertChannel.TOO_LOW, true));

        CompoundTag saved = new CompoundTag();
        configuration.save(saved);
        GpwsConfiguration loaded = GpwsConfiguration.load(saved);

        assertEquals(GpwsSensitivity.HIGH, loaded.sensitivity());
        assertEquals(Set.of(AlertChannel.TOO_LOW), loaded.inhibitedChannels());
        assertFalse(saved.contains("Mode"));
    }

    @Test
    void gpwsInopCanNeverBeInhibited() {
        GpwsConfiguration configuration = new GpwsConfiguration();
        assertFalse(configuration.setInhibited(AlertChannel.SENSOR_FAIL, true));
        assertFalse(configuration.inhibited(AlertChannel.SENSOR_FAIL));
    }

    @Test
    void oldGpwsComputerSettingsAreDroppedExceptSensitivity() {
        CompoundTag saved = new CompoundTag();
        saved.putString("Profile", "ADVANCED");
        saved.putString("Sensitivity", "LOW");
        saved.putString("PhaseOverride", "APPROACH");
        CompoundTag override = new CompoundTag();
        override.putString("Channel", "TOO_LOW");
        override.putString("Phase", "CRUISE");
        override.putDouble("Caution", 20);
        ListTag rules = new ListTag();
        rules.add(override);
        saved.put("RuleOverrides", rules);

        GpwsConfiguration loaded = GpwsConfiguration.load(saved);
        assertEquals(GpwsSensitivity.LOW, loaded.sensitivity());
        assertTrue(loaded.inhibitedChannels().isEmpty());
        assertEquals(8, loaded.rule(AlertChannel.TOO_LOW, FlightPhase.CRUISE).cautionThreshold(), 0.0);
    }

    @Test
    void alertEventsIgnoreOnlyMeasurementTimestampChanges() {
        AlertReading first = new AlertReading(AlertChannel.TERRAIN, AlertState.ACTIVE, AlertSeverity.CAUTION, 5);
        AlertReading sameStateLater = new AlertReading(AlertChannel.TERRAIN, AlertState.ACTIVE,
                AlertSeverity.CAUTION, 10);
        AlertReading changedSeverity = new AlertReading(AlertChannel.TERRAIN, AlertState.ACTIVE,
                AlertSeverity.WARNING, 10);
        assertFalse(AvionicsComputerPeripheral.alertStateChanged(first, sameStateLater));
        assertTrue(AvionicsComputerPeripheral.alertStateChanged(first, changedSeverity));
    }
}
