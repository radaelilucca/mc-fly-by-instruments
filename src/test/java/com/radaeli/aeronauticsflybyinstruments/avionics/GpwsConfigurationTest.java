package com.radaeli.aeronauticsflybyinstruments.avionics;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GpwsConfigurationTest {
    @Test
    void advancedSettingsAndRulesPersistButRuntimeModeDoesNotBelongToConfiguration() {
        GpwsConfiguration configuration = new GpwsConfiguration();
        configuration.setProfile(GpwsProfile.ADVANCED);
        configuration.setSensitivity(GpwsSensitivity.HIGH);
        configuration.setPhaseOverride(FlightPhase.APPROACH);
        GpwsRule rule = new GpwsRule(4, 1.5, 0, 12, 3, true);
        assertTrue(configuration.setRule(AlertChannel.TOO_LOW, FlightPhase.APPROACH, rule));

        CompoundTag saved = new CompoundTag();
        configuration.save(saved);
        GpwsConfiguration loaded = GpwsConfiguration.load(saved);

        assertEquals(GpwsProfile.ADVANCED, loaded.profile());
        assertEquals(GpwsSensitivity.HIGH, loaded.sensitivity());
        assertEquals(FlightPhase.APPROACH, loaded.phaseOverride().orElseThrow());
        assertEquals(rule, loaded.rule(AlertChannel.TOO_LOW, FlightPhase.APPROACH));
        assertFalse(saved.contains("Mode"));
    }

    @Test
    void basicProfileAlwaysUsesSimpleDefaultsAndAutomaticPhase() {
        GpwsConfiguration configuration = new GpwsConfiguration();
        configuration.setProfile(GpwsProfile.ADVANCED);
        configuration.setPhaseOverride(FlightPhase.CRUISE);
        configuration.setRule(AlertChannel.TOO_LOW, FlightPhase.APPROACH,
                new GpwsRule(20, 10, 0, 0, 1, true));
        configuration.setProfile(GpwsProfile.BASIC);

        assertTrue(configuration.phaseOverride().isEmpty());
        assertEquals(3, configuration.rule(AlertChannel.TOO_LOW, FlightPhase.APPROACH).cautionThreshold(), 0.0);
        assertFalse(configuration.setRule(AlertChannel.TOO_LOW, FlightPhase.APPROACH,
                new GpwsRule(20, 10, 0, 0, 1, true)));
    }

    @Test
    void invalidPersistedRulesAreDiscardedInFavorOfDefaults() {
        CompoundTag saved = new CompoundTag();
        saved.putString("Profile", "ADVANCED");
        saved.putString("Sensitivity", "NORMAL");
        CompoundTag invalid = new CompoundTag();
        invalid.putString("Channel", "TOO_LOW");
        invalid.putString("Phase", "APPROACH");
        invalid.putDouble("Caution", Double.NaN);
        invalid.putDouble("Warning", 1);
        invalid.putDouble("SecondaryWarning", 0);
        invalid.putDouble("HysteresisPercent", 10);
        invalid.putInt("ConfirmationSamples", 2);
        ListTag rules = new ListTag();
        rules.add(invalid);
        saved.put("RuleOverrides", rules);

        GpwsConfiguration loaded = GpwsConfiguration.load(saved);
        assertEquals(3, loaded.rule(AlertChannel.TOO_LOW, FlightPhase.APPROACH).cautionThreshold(), 0.0);
    }

    @Test
    void alertEventsIgnoreOnlyMeasurementTimestampChanges() {
        AlertReading first = new AlertReading(AlertChannel.TERRAIN, AlertState.ACTIVE, AlertSeverity.CAUTION, 5);
        AlertReading sameStateLater = new AlertReading(AlertChannel.TERRAIN, AlertState.ACTIVE,
                AlertSeverity.CAUTION, 10);
        AlertReading changedSeverity = new AlertReading(AlertChannel.TERRAIN, AlertState.ACTIVE,
                AlertSeverity.WARNING, 10);
        assertFalse(GpwsComputerPeripheral.alertStateChanged(first, sameStateLater));
        assertTrue(GpwsComputerPeripheral.alertStateChanged(first, changedSeverity));
    }
}
