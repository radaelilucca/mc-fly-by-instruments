package com.radaeli.aeronauticsflybyinstruments.avionics;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;

/** Pure, tick-driven GPWS rules. It consumes sensor snapshots and never performs world queries. */
public final class GpwsEngine {
    private static final long TAKEOFF_PEAK_WINDOW_TICKS = 20L * 5L;
    private static final double GROUND_CLEARANCE = 1.5;
    private static final double GROUND_VERTICAL_SPEED = 0.5;
    private static final double TAKEOFF_VERTICAL_SPEED = 1.0;
    private static final double LEVEL_VERTICAL_SPEED = 0.5;
    private static final double PULL_UP_CONE_HALF_ANGLE = 22.5;

    private final EnumMap<AlertChannel, AlertLatch> latches = new EnumMap<>(AlertChannel.class);
    private FlightPhase automaticPhase = FlightPhase.UNKNOWN;
    private FlightPhase candidatePhase;
    private int candidateSamples;
    private long groundCandidateSince = -1;
    private boolean groundQualificationActive;
    private Double previousClearance;
    private long previousClearanceTime = -1;
    private Double verticalSpeed;
    private Double takeoffPeak;
    private long takeoffPeakTime = -1;
    private GpwsSensitivity sensitivityForCurrentEvaluation = GpwsSensitivity.NORMAL;

    public GpwsEngine() {
        for (AlertChannel channel : AlertChannel.values()) latches.put(channel, new AlertLatch());
    }

    public static boolean shouldAutoArm(GpwsProfile profile, GpwsMode mode, GpwsSample sample) {
        return profile == GpwsProfile.BASIC && mode == GpwsMode.STANDBY
                && sample.networkStatus() == NetworkStatus.CONNECTED && sample.altimeterCount() > 0
                && !sample.altimeterFault() && sample.clearance().isPresent();
    }

    public Evaluation evaluate(GpwsSample sample, GpwsConfiguration configuration, GpwsMode mode,
                               int sampleIntervalTicks) {
        if (sampleIntervalTicks < 1) throw new IllegalArgumentException("sampleIntervalTicks must be positive");
        sensitivityForCurrentEvaluation = configuration.sensitivity();
        updateVerticalSpeed(sample, sampleIntervalTicks);
        updateAutomaticPhase(sample);
        FlightPhase phase = configuration.effectivePhase(automaticPhase);
        updateTakeoffPeak(sample, phase);

        EnumMap<AlertChannel, AlertReading> alerts = new EnumMap<>(AlertChannel.class);
        if (mode == GpwsMode.TEST) {
            for (AlertChannel channel : AlertChannel.values()) {
                alerts.put(channel, new AlertReading(channel, AlertState.ACTIVE, AlertSeverity.WARNING,
                        sample.gameTime()));
            }
        } else if (mode == GpwsMode.OFF || mode == GpwsMode.STANDBY) {
            for (AlertChannel channel : AlertChannel.values()) {
                if (mode == GpwsMode.STANDBY && channel == AlertChannel.SENSOR_FAIL) {
                    GpwsRule diagnosticRule = configuration.rule(channel, phase);
                    if (diagnosticRule.inhibited()) {
                        latches.get(channel).forceInactive();
                        alerts.put(channel, inactive(channel, sample.gameTime()));
                    } else {
                        alerts.put(channel, updateLatch(channel, sensorFail(sample), diagnosticRule,
                                sample.gameTime()));
                    }
                } else {
                    latches.get(channel).forceInactive();
                    alerts.put(channel, inactive(channel, sample.gameTime()));
                }
            }
        } else {
            EnumMap<AlertChannel, Candidate> candidates = evaluateCandidates(sample, configuration, phase);
            for (AlertChannel channel : AlertChannel.values()) {
                GpwsRule rule = configuration.rule(channel, phase);
                if (rule.inhibited()) {
                    latches.get(channel).forceInactive();
                    alerts.put(channel, inactive(channel, sample.gameTime()));
                } else {
                    alerts.put(channel, updateLatch(channel, candidates.get(channel), rule, sample.gameTime()));
                }
            }
        }
        return new Evaluation(phase, automaticPhase, optional(verticalSpeed), Map.copyOf(alerts));
    }

    private void updateVerticalSpeed(GpwsSample sample, int sampleIntervalTicks) {
        if (sample.clearance().isEmpty()) {
            previousClearance = null;
            previousClearanceTime = -1;
            verticalSpeed = null;
            return;
        }

        double current = sample.clearance().getAsDouble();
        long measurementTime = sample.clearanceGameTime();
        if (previousClearance != null && measurementTime > previousClearanceTime) {
            long elapsed = measurementTime - previousClearanceTime;
            if (elapsed <= sampleIntervalTicks * 2L) {
                verticalSpeed = (current - previousClearance) * 20.0 / elapsed;
            } else {
                verticalSpeed = null;
            }
            previousClearance = current;
            previousClearanceTime = measurementTime;
        } else if (previousClearance == null || measurementTime < previousClearanceTime) {
            previousClearance = current;
            previousClearanceTime = measurementTime;
            verticalSpeed = null;
        }
    }

    private void updateAutomaticPhase(GpwsSample sample) {
        groundQualificationActive = false;
        if (sample.clearance().isEmpty()) {
            automaticPhase = FlightPhase.UNKNOWN;
            candidatePhase = null;
            candidateSamples = 0;
            groundCandidateSince = -1;
            return;
        }

        double clearance = sample.clearance().getAsDouble();
        if (verticalSpeed == null) {
            groundCandidateSince = -1;
            return;
        }
        FlightPhase next = inferPhase(clearance, verticalSpeed);
        if (next == FlightPhase.GROUND) {
            groundQualificationActive = true;
            candidatePhase = null;
            candidateSamples = 0;
            if (automaticPhase == FlightPhase.GROUND) return;
            if (groundCandidateSince < 0) groundCandidateSince = sample.gameTime();
            if (sample.gameTime() - groundCandidateSince >= 20) {
                automaticPhase = FlightPhase.GROUND;
                groundCandidateSince = -1;
            }
            return;
        }

        groundCandidateSince = -1;
        if (next == automaticPhase) {
            candidatePhase = null;
            candidateSamples = 0;
            return;
        }
        if (candidatePhase == next) candidateSamples++;
        else {
            candidatePhase = next;
            candidateSamples = 1;
        }
        if (candidateSamples >= 2) {
            automaticPhase = next;
            candidatePhase = null;
            candidateSamples = 0;
        }
    }

    private static FlightPhase inferPhase(double clearance, double verticalSpeed) {
        if (clearance <= GROUND_CLEARANCE && Math.abs(verticalSpeed) <= GROUND_VERTICAL_SPEED) {
            return FlightPhase.GROUND;
        }
        if (verticalSpeed > TAKEOFF_VERTICAL_SPEED && clearance < 8) return FlightPhase.TAKEOFF;
        if (verticalSpeed > LEVEL_VERTICAL_SPEED) return FlightPhase.CLIMB;
        if (verticalSpeed < -LEVEL_VERTICAL_SPEED) {
            return clearance <= 32 ? FlightPhase.APPROACH : FlightPhase.DESCENT;
        }
        if (clearance > 8) return FlightPhase.CRUISE;
        return FlightPhase.UNKNOWN;
    }

    private void updateTakeoffPeak(GpwsSample sample, FlightPhase phase) {
        if (phase == FlightPhase.GROUND) {
            takeoffPeak = null;
            takeoffPeakTime = -1;
            return;
        }
        if (sample.clearance().isPresent() && (phase == FlightPhase.TAKEOFF || phase == FlightPhase.CLIMB)) {
            double clearance = sample.clearance().getAsDouble();
            if (takeoffPeak == null || clearance > takeoffPeak) {
                takeoffPeak = clearance;
                takeoffPeakTime = sample.gameTime();
            }
        }
        if (takeoffPeakTime >= 0 && sample.gameTime() - takeoffPeakTime > TAKEOFF_PEAK_WINDOW_TICKS) {
            takeoffPeak = null;
            takeoffPeakTime = -1;
        }
    }

    private EnumMap<AlertChannel, Candidate> evaluateCandidates(GpwsSample sample,
                                                                 GpwsConfiguration configuration,
                                                                 FlightPhase phase) {
        EnumMap<AlertChannel, Candidate> result = new EnumMap<>(AlertChannel.class);
        result.put(AlertChannel.SENSOR_FAIL, sensorFail(sample));

        boolean ground = phase == FlightPhase.GROUND
                || (configuration.phaseOverride().isEmpty() && groundQualificationActive);
        result.put(AlertChannel.TERRAIN, ground ? Candidate.inactive()
                : terrain(sample, configuration.rule(AlertChannel.TERRAIN, phase)));
        result.put(AlertChannel.PULL_UP, ground ? Candidate.inactive()
                : pullUp(sample, configuration.rule(AlertChannel.PULL_UP, phase)));
        result.put(AlertChannel.SINK_RATE, ground ? Candidate.inactive()
                : sinkRate(configuration.rule(AlertChannel.SINK_RATE, phase)));
        result.put(AlertChannel.DONT_SINK, ground ? Candidate.inactive()
                : dontSink(sample, phase, configuration.rule(AlertChannel.DONT_SINK, phase)));
        result.put(AlertChannel.TOO_LOW, ground ? Candidate.inactive()
                : tooLow(sample, configuration.rule(AlertChannel.TOO_LOW, phase)));
        result.put(AlertChannel.BANK_ANGLE, ground ? Candidate.inactive()
                : bankAngle(sample, configuration.rule(AlertChannel.BANK_ANGLE, phase)));
        return result;
    }

    private Candidate terrain(GpwsSample sample, GpwsRule rule) {
        if (sample.terrainScannerCount() == 0) return Candidate.unavailable();
        if (sample.terrainContacts().isEmpty()) {
            return sample.terrainCoverageComplete() ? Candidate.inactive() : Candidate.unavailable();
        }
        double nearest = sample.terrainContacts().stream().mapToDouble(GpwsSample.TerrainContact::distance)
                .min().orElse(Double.POSITIVE_INFINITY);
        AlertSeverity severity = lowerThresholdSeverity(AlertChannel.TERRAIN, nearest, rule);
        if (severity != null) return Candidate.active(severity);
        return sample.terrainCoverageComplete() ? Candidate.inactive() : Candidate.unavailable();
    }

    private Candidate pullUp(GpwsSample sample, GpwsRule rule) {
        boolean altitudeTrendAvailable = sample.clearance().isPresent() && verticalSpeed != null;
        boolean terrainCoverageAvailable = sample.terrainScannerCount() > 0
                && sample.pullUpTerrainCoverageComplete();
        boolean terrainOptional = sample.terrainScannerCount() == 0;
        boolean knownTerrainDanger = sample.terrainContacts().stream()
                .anyMatch(contact -> contact.angleFromForwardDegrees() <= PULL_UP_CONE_HALF_ANGLE
                        && lowerThresholdSeverity(AlertChannel.PULL_UP, contact.distance(), rule) != null);
        double timeThreshold = scaledLowThreshold(rule.secondaryWarningThreshold());
        if (currentSeverity(AlertChannel.PULL_UP) == AlertSeverity.WARNING) {
            timeThreshold *= 1 + rule.hysteresisPercent() / 100.0;
        }
        boolean timeToGroundDanger = altitudeTrendAvailable && verticalSpeed < 0
                && lowerValueAtRisk(sample.clearance().getAsDouble() / -verticalSpeed, timeThreshold);
        if (knownTerrainDanger || timeToGroundDanger) return Candidate.active(AlertSeverity.WARNING);
        if (altitudeTrendAvailable && (terrainOptional || terrainCoverageAvailable)) return Candidate.inactive();
        if (terrainCoverageAvailable) return Candidate.inactive();
        return Candidate.unavailable();
    }

    private Candidate sinkRate(GpwsRule rule) {
        if (verticalSpeed == null) return Candidate.unavailable();
        return Candidate.fromSeverity(highThresholdSeverity(AlertChannel.SINK_RATE, Math.max(0, -verticalSpeed), rule));
    }

    private Candidate dontSink(GpwsSample sample, FlightPhase phase, GpwsRule rule) {
        if (sample.clearance().isEmpty()) return Candidate.unavailable();
        boolean takeoffOrClimb = phase == FlightPhase.TAKEOFF || phase == FlightPhase.CLIMB;
        if (!takeoffOrClimb && (takeoffPeak == null || takeoffPeakTime < 0
                || sample.gameTime() - takeoffPeakTime > TAKEOFF_PEAK_WINDOW_TICKS)) return Candidate.inactive();
        if (takeoffPeak == null) return Candidate.unavailable();
        double lostClearance = Math.max(0, takeoffPeak - sample.clearance().getAsDouble());
        return Candidate.fromSeverity(highThresholdSeverity(AlertChannel.DONT_SINK, lostClearance, rule));
    }

    private Candidate tooLow(GpwsSample sample, GpwsRule rule) {
        if (sample.clearance().isEmpty()) return Candidate.unavailable();
        return Candidate.fromSeverity(lowerThresholdSeverity(AlertChannel.TOO_LOW,
                sample.clearance().getAsDouble(), rule));
    }

    private Candidate bankAngle(GpwsSample sample, GpwsRule rule) {
        if (sample.bankAngleDegrees().isEmpty()) return Candidate.unavailable();
        return Candidate.fromSeverity(highThresholdSeverity(AlertChannel.BANK_ANGLE,
                Math.abs(sample.bankAngleDegrees().getAsDouble()), rule));
    }

    private static Candidate sensorFail(GpwsSample sample) {
        boolean failed = sample.networkStatus() != NetworkStatus.CONNECTED || sample.altimeterCount() == 0
                || sample.altimeterFault() || sample.terrainScannerFault();
        return failed ? Candidate.active(AlertSeverity.CAUTION) : Candidate.inactive();
    }

    private AlertSeverity lowerThresholdSeverity(AlertChannel channel, double value, GpwsRule rule) {
        AlertSeverity previous = currentSeverity(channel);
        double warning = scaledLowThreshold(rule.warningThreshold());
        double caution = scaledLowThreshold(rule.cautionThreshold());
        double hysteresis = rule.hysteresisPercent() / 100.0;
        if (warning > 0 && value <= warning * (previous == AlertSeverity.WARNING ? 1 + hysteresis : 1)) {
            return AlertSeverity.WARNING;
        }
        if (caution > 0 && value <= caution * (previous != null ? 1 + hysteresis : 1)) {
            return AlertSeverity.CAUTION;
        }
        return null;
    }

    private AlertSeverity highThresholdSeverity(AlertChannel channel, double value, GpwsRule rule) {
        AlertSeverity previous = currentSeverity(channel);
        double warning = scaledHighThreshold(rule.warningThreshold());
        double caution = scaledHighThreshold(rule.cautionThreshold());
        double hysteresis = rule.hysteresisPercent() / 100.0;
        if (warning > 0 && value >= warning * (previous == AlertSeverity.WARNING ? 1 - hysteresis : 1)) {
            return AlertSeverity.WARNING;
        }
        if (caution > 0 && value >= caution * (previous != null ? 1 - hysteresis : 1)) {
            return AlertSeverity.CAUTION;
        }
        return null;
    }

    private double scaledLowThreshold(double value) {
        return value * sensitivityForCurrentEvaluation.lowValueThresholdScale();
    }

    private double scaledHighThreshold(double value) {
        return value * sensitivityForCurrentEvaluation.highValueThresholdScale();
    }

    private static boolean lowerValueAtRisk(double value, double threshold) {
        return threshold > 0 && value <= threshold;
    }

    private AlertReading updateLatch(AlertChannel channel, Candidate candidate, GpwsRule rule, long gameTime) {
        AlertLatch latch = latches.get(channel);
        int required = channel == AlertChannel.PULL_UP && candidate.severity() != null
                && latch.state != AlertState.ACTIVE ? 1 : rule.confirmationSamples();
        return latch.update(channel, candidate, required, gameTime);
    }

    private AlertSeverity currentSeverity(AlertChannel channel) {
        AlertLatch latch = latches.get(channel);
        return latch.state == AlertState.ACTIVE ? latch.severity : null;
    }

    private static AlertReading inactive(AlertChannel channel, long gameTime) {
        return new AlertReading(channel, AlertState.INACTIVE, AlertSeverity.ADVISORY, gameTime);
    }

    private static OptionalDouble optional(Double value) {
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public record Evaluation(FlightPhase phase, FlightPhase automaticPhase, OptionalDouble verticalSpeed,
                             Map<AlertChannel, AlertReading> alerts) {}

    private record Candidate(boolean available, AlertSeverity severity) {
        static Candidate unavailable() { return new Candidate(false, null); }
        static Candidate inactive() { return new Candidate(true, null); }
        static Candidate active(AlertSeverity severity) { return new Candidate(true, severity); }
        static Candidate fromSeverity(AlertSeverity severity) {
            return severity == null ? inactive() : active(severity);
        }
    }

    private static final class AlertLatch {
        private AlertState state = AlertState.UNAVAILABLE;
        private AlertSeverity severity = AlertSeverity.ADVISORY;
        private AlertSeverity candidateSeverity;
        private int candidateCount;

        AlertReading update(AlertChannel channel, Candidate candidate, int confirmations, long gameTime) {
            if (!candidate.available()) {
                state = AlertState.UNAVAILABLE;
                severity = AlertSeverity.ADVISORY;
                candidateSeverity = null;
                candidateCount = 0;
                return new AlertReading(channel, state, severity, gameTime);
            }
            AlertState desiredState = candidate.severity() == null ? AlertState.INACTIVE : AlertState.ACTIVE;
            AlertSeverity desiredSeverity = candidate.severity() == null
                    ? AlertSeverity.ADVISORY : candidate.severity();
            if (state == desiredState && severity == desiredSeverity) {
                candidateSeverity = null;
                candidateCount = 0;
                return new AlertReading(channel, state, severity, gameTime);
            }
            if (candidateSeverity == desiredSeverity) candidateCount++;
            else {
                candidateSeverity = desiredSeverity;
                candidateCount = 1;
            }
            if (candidateCount >= confirmations) {
                state = desiredState;
                severity = desiredSeverity;
                candidateSeverity = null;
                candidateCount = 0;
            }
            return new AlertReading(channel, state, severity, gameTime);
        }

        void forceInactive() {
            state = AlertState.INACTIVE;
            severity = AlertSeverity.ADVISORY;
            candidateSeverity = null;
            candidateCount = 0;
        }
    }
}
