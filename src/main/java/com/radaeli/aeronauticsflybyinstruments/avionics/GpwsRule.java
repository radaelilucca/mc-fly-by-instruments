package com.radaeli.aeronauticsflybyinstruments.avionics;

/**
 * Fixed envelope of one GPWS function in one internal flight phase. Threshold meanings are
 * channel-specific (see {@link GpwsConfiguration#envelope}); players cannot edit them.
 */
public record GpwsRule(double cautionThreshold, double warningThreshold, double secondaryWarningThreshold,
                       double hysteresisPercent, int confirmationSamples) {
    public GpwsRule {
        if (!validThreshold(cautionThreshold) || !validThreshold(warningThreshold)
                || !validThreshold(secondaryWarningThreshold)
                || !Double.isFinite(hysteresisPercent) || hysteresisPercent < 0 || hysteresisPercent > 90
                || confirmationSamples < 1 || confirmationSamples > 20) {
            throw new IllegalArgumentException("Invalid GPWS rule");
        }
    }

    private static boolean validThreshold(double value) {
        return Double.isFinite(value) && value >= 0 && value <= 4096;
    }
}
