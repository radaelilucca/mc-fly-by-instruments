package com.radaeli.aeronauticsflybyinstruments.avionics;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.*;

class GpwsEngineTest {
    private static final int INTERVAL = 5;

    @Test
    void estimatesVerticalSpeedAndStabilizesAutomaticFlightPhase() {
        GpwsEngine engine = new GpwsEngine();
        GpwsConfiguration configuration = new GpwsConfiguration();

        var first = engine.evaluate(sample(0, 40, 0), configuration, GpwsMode.TEST, INTERVAL);
        assertTrue(first.verticalSpeed().isEmpty());
        assertEquals(FlightPhase.UNKNOWN, first.phase());

        var second = engine.evaluate(sample(5, 40, 5), configuration, GpwsMode.TEST, INTERVAL);
        assertEquals(0, second.verticalSpeed().orElseThrow(), 1.0E-9);
        assertEquals(FlightPhase.UNKNOWN, second.phase());

        var third = engine.evaluate(sample(10, 40, 10), configuration, GpwsMode.TEST, INTERVAL);
        assertEquals(FlightPhase.CRUISE, third.phase());
    }

    @Test
    void groundPhaseRequiresOneContinuousSecond() {
        GpwsEngine engine = new GpwsEngine();
        GpwsConfiguration configuration = new GpwsConfiguration();
        engine.evaluate(sample(0, 1, 0), configuration, GpwsMode.TEST, INTERVAL);
        assertEquals(FlightPhase.UNKNOWN,
                engine.evaluate(sample(5, 1, 5), configuration, GpwsMode.TEST, INTERVAL).phase());
        engine.evaluate(sample(10, 1, 10), configuration, GpwsMode.TEST, INTERVAL);
        engine.evaluate(sample(15, 1, 15), configuration, GpwsMode.TEST, INTERVAL);
        assertEquals(FlightPhase.UNKNOWN,
                engine.evaluate(sample(20, 1, 20), configuration, GpwsMode.TEST, INTERVAL).phase());
        assertEquals(FlightPhase.GROUND,
                engine.evaluate(sample(25, 1, 25), configuration, GpwsMode.TEST, INTERVAL).phase());
    }

    @Test
    void groundQualificationDoesNotEmitTransientLowClearanceWarning() {
        GpwsEngine engine = new GpwsEngine();
        GpwsConfiguration configuration = new GpwsConfiguration();
        engine.evaluate(sample(0, 1, 0), configuration, GpwsMode.ARMED, INTERVAL);
        var settling = engine.evaluate(sample(5, 1, 5), configuration, GpwsMode.ARMED, INTERVAL);
        assertNotEquals(AlertState.ACTIVE, settling.alerts().get(AlertChannel.TOO_LOW).state());
        assertNotEquals(AlertState.ACTIVE, settling.alerts().get(AlertChannel.PULL_UP).state());
    }

    @Test
    void unknownAltitudeImmediatelyMakesAltitudeDependentChannelsUnavailable() {
        GpwsEngine engine = new GpwsEngine();
        var input = sample(10, null, 10);
        var result = engine.evaluate(input, new GpwsConfiguration(), GpwsMode.ARMED, INTERVAL);
        assertEquals(FlightPhase.UNKNOWN, result.phase());
        assertEquals(AlertState.UNAVAILABLE, result.alerts().get(AlertChannel.TOO_LOW).state());
        assertEquals(AlertState.UNAVAILABLE, result.alerts().get(AlertChannel.SINK_RATE).state());
    }

    @Test
    void partialTerrainCoverageCannotClaimClearButKnownContactCanAlert() {
        GpwsEngine emptyPartial = new GpwsEngine();
        var partial = sample(0, 40, 0, 1, false, false, List.of());
        var partialResult = emptyPartial.evaluate(partial, new GpwsConfiguration(), GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.UNAVAILABLE, partialResult.alerts().get(AlertChannel.TERRAIN).state());

        GpwsEngine hitPartial = new GpwsEngine();
        var hit = sample(0, 40, 0, 1, false, false, List.of(new GpwsSample.TerrainContact(20, 10)));
        hitPartial.evaluate(hit, new GpwsConfiguration(), GpwsMode.ARMED, INTERVAL);
        var alerted = hitPartial.evaluate(sample(5, 40, 5, 1, false, false,
                List.of(new GpwsSample.TerrainContact(20, 10))), new GpwsConfiguration(), GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.ACTIVE, alerted.alerts().get(AlertChannel.TERRAIN).state());
        assertEquals(AlertSeverity.CAUTION, alerted.alerts().get(AlertChannel.TERRAIN).severity());
    }

    @Test
    void pullUpUsesKnownContactImmediatelyAndRequiresCoverageToReportClear() {
        GpwsEngine detected = new GpwsEngine();
        var knownHit = sample(0, null, 0, 1, false, false,
                List.of(new GpwsSample.TerrainContact(7, 12)));
        var detectedResult = detected.evaluate(knownHit, new GpwsConfiguration(), GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.ACTIVE, detectedResult.alerts().get(AlertChannel.PULL_UP).state());
        assertEquals(AlertSeverity.WARNING, detectedResult.alerts().get(AlertChannel.PULL_UP).severity());

        GpwsEngine partial = new GpwsEngine();
        var first = partial.evaluate(sample(0, 40, 0, 1, false, false, List.of()),
                new GpwsConfiguration(), GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.UNAVAILABLE, first.alerts().get(AlertChannel.PULL_UP).state());
    }

    @Test
    void pullUpUsesEstimatedTimeToGroundWhenNoTerrainScannerIsInstalled() {
        GpwsEngine engine = new GpwsEngine();
        GpwsConfiguration configuration = new GpwsConfiguration();
        engine.evaluate(sample(0, 20, 0), configuration, GpwsMode.ARMED, INTERVAL);
        engine.evaluate(sample(5, 18, 5), configuration, GpwsMode.ARMED, INTERVAL);
        var warning = engine.evaluate(sample(10, 16, 10), configuration, GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.ACTIVE, warning.alerts().get(AlertChannel.PULL_UP).state());
        assertEquals(AlertSeverity.WARNING, warning.alerts().get(AlertChannel.PULL_UP).severity());
    }

    @Test
    void dontSinkWarnsAfterFourBlocksOfRecentTakeoffOrClimbLoss() {
        GpwsEngine engine = new GpwsEngine();
        GpwsConfiguration configuration = new GpwsConfiguration();
        configuration.setProfile(GpwsProfile.ADVANCED);
        configuration.setPhaseOverride(FlightPhase.TAKEOFF);
        engine.evaluate(sample(0, 3, 0), configuration, GpwsMode.ARMED, INTERVAL);
        engine.evaluate(sample(5, 3.5, 5), configuration, GpwsMode.ARMED, INTERVAL);
        engine.evaluate(sample(10, 4.5, 10), configuration, GpwsMode.ARMED, INTERVAL);
        engine.evaluate(sample(15, 3.5, 15), configuration, GpwsMode.ARMED, INTERVAL);
        configuration.setPhaseOverride(FlightPhase.APPROACH);
        var first = engine.evaluate(sample(20, 0.5, 20), configuration, GpwsMode.ARMED, INTERVAL);
        assertNotEquals(AlertState.ACTIVE, first.alerts().get(AlertChannel.DONT_SINK).state());
        var warning = engine.evaluate(sample(25, 0.4, 25), configuration, GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.ACTIVE, warning.alerts().get(AlertChannel.DONT_SINK).state());
        assertEquals(AlertSeverity.WARNING, warning.alerts().get(AlertChannel.DONT_SINK).severity());
    }

    @Test
    void basicProfileCanArmWithRadarOnlyWithoutTreatingOptionalScannerAsFailure() {
        GpwsEngine engine = new GpwsEngine();
        var first = engine.evaluate(sample(0, 40, 0), new GpwsConfiguration(), GpwsMode.ARMED, INTERVAL);
        var second = engine.evaluate(sample(5, 40, 5), new GpwsConfiguration(), GpwsMode.ARMED, INTERVAL);
        var third = engine.evaluate(sample(10, 40, 10), new GpwsConfiguration(), GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.INACTIVE, second.alerts().get(AlertChannel.SENSOR_FAIL).state());
        assertEquals(AlertState.UNAVAILABLE, first.alerts().get(AlertChannel.TERRAIN).state());
        assertEquals(AlertState.INACTIVE, third.alerts().get(AlertChannel.PULL_UP).state());
    }

    @Test
    void basicAutoArmingRequiresCompleteNetworkAndValidRadarClearanceOnly() {
        GpwsSample radarOnly = sample(0, 40, 0);
        assertTrue(GpwsEngine.shouldAutoArm(GpwsProfile.BASIC, GpwsMode.STANDBY, radarOnly));
        assertFalse(GpwsEngine.shouldAutoArm(GpwsProfile.ADVANCED, GpwsMode.STANDBY, radarOnly));
        assertFalse(GpwsEngine.shouldAutoArm(GpwsProfile.BASIC, GpwsMode.OFF, radarOnly));
        assertFalse(GpwsEngine.shouldAutoArm(GpwsProfile.BASIC, GpwsMode.STANDBY,
                new GpwsSample(0, NetworkStatus.INCOMPLETE, 1, 0, 0, false, OptionalDouble.empty(), 0,
                        0, false, false, false, List.of(), OptionalDouble.of(0))));
    }

    @Test
    void sinkRateUsesBlocksPerSecondAndConfirmationSamples() {
        GpwsEngine engine = new GpwsEngine();
        GpwsConfiguration configuration = new GpwsConfiguration();
        engine.evaluate(sample(0, 40, 0), configuration, GpwsMode.ARMED, INTERVAL);
        var second = engine.evaluate(sample(5, 37, 5), configuration, GpwsMode.ARMED, INTERVAL);
        assertEquals(-12, second.verticalSpeed().orElseThrow(), 1.0E-9);
        assertNotEquals(AlertState.ACTIVE, second.alerts().get(AlertChannel.SINK_RATE).state());
        var third = engine.evaluate(sample(10, 34, 10), configuration, GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.ACTIVE, third.alerts().get(AlertChannel.SINK_RATE).state());
        assertEquals(AlertSeverity.WARNING, third.alerts().get(AlertChannel.SINK_RATE).severity());
    }

    @Test
    void approachClearanceUsesSensitivityAndHysteresisBeforeClearing() {
        GpwsConfiguration configuration = new GpwsConfiguration();
        configuration.setPhaseOverride(FlightPhase.APPROACH);
        GpwsEngine engine = new GpwsEngine();
        engine.evaluate(sample(0, 2, 0), configuration, GpwsMode.ARMED, INTERVAL);
        var active = engine.evaluate(sample(5, 2, 5), configuration, GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertSeverity.CAUTION, active.alerts().get(AlertChannel.TOO_LOW).severity());
        engine.evaluate(sample(10, 3.2, 10), configuration, GpwsMode.ARMED, INTERVAL);
        var withinHysteresis = engine.evaluate(sample(15, 3.2, 15), configuration, GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.ACTIVE, withinHysteresis.alerts().get(AlertChannel.TOO_LOW).state());
        engine.evaluate(sample(20, 3.4, 20), configuration, GpwsMode.ARMED, INTERVAL);
        var clearing = engine.evaluate(sample(25, 3.4, 25), configuration, GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.INACTIVE, clearing.alerts().get(AlertChannel.TOO_LOW).state());

        GpwsConfiguration high = new GpwsConfiguration();
        high.setPhaseOverride(FlightPhase.APPROACH);
        high.setSensitivity(GpwsSensitivity.HIGH);
        GpwsEngine highEngine = new GpwsEngine();
        highEngine.evaluate(sample(0, 3.5, 0), high, GpwsMode.ARMED, INTERVAL);
        var sensitive = highEngine.evaluate(sample(5, 3.5, 5), high, GpwsMode.ARMED, INTERVAL);
        assertEquals(AlertState.ACTIVE, sensitive.alerts().get(AlertChannel.TOO_LOW).state());
    }

    @Test
    void standbyKeepsSensorFailureButSuppressesRiskAndTestActivatesEveryChannel() {
        GpwsEngine engine = new GpwsEngine();
        GpwsConfiguration configuration = new GpwsConfiguration();
        var missingSensors = sample(0, null, 0, 0, false, false, List.of());
        engine.evaluate(missingSensors, configuration, GpwsMode.STANDBY, INTERVAL);
        var standby = engine.evaluate(sample(5, null, 5, 0, false, false, List.of()),
                configuration, GpwsMode.STANDBY, INTERVAL);
        assertEquals(AlertState.ACTIVE, standby.alerts().get(AlertChannel.SENSOR_FAIL).state());
        assertEquals(AlertSeverity.CAUTION, standby.alerts().get(AlertChannel.SENSOR_FAIL).severity());
        assertEquals(AlertState.INACTIVE, standby.alerts().get(AlertChannel.TERRAIN).state());

        var test = engine.evaluate(missingSensors, configuration, GpwsMode.TEST, INTERVAL);
        assertTrue(test.alerts().values().stream().allMatch(alert -> alert.state() == AlertState.ACTIVE
                && alert.severity() == AlertSeverity.WARNING));
    }

    private static GpwsSample sample(long now, Number clearance, long measurementTime) {
        return sample(now, clearance, measurementTime, 0, false, false, List.of());
    }

    private static GpwsSample sample(long now, Number clearance, long measurementTime, int scannerCount,
                                     boolean complete, boolean pullUpComplete,
                                     List<GpwsSample.TerrainContact> contacts) {
        return new GpwsSample(now, NetworkStatus.CONNECTED, 12, 2, clearance == null ? 0 : 1,
                false, clearance == null ? OptionalDouble.empty() : OptionalDouble.of(clearance.doubleValue()), measurementTime,
                scannerCount, false, complete, pullUpComplete, contacts, OptionalDouble.of(0));
    }
}
