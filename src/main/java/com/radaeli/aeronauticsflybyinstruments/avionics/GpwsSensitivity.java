package com.radaeli.aeronauticsflybyinstruments.avionics;

public enum GpwsSensitivity {
    LOW,
    NORMAL,
    HIGH;

    /** Scale for limits where a smaller measured value means greater danger. */
    public double lowValueThresholdScale() {
        return switch (this) {
            case LOW -> 0.8;
            case NORMAL -> 1.0;
            case HIGH -> 1.2;
        };
    }

    /** Scale for limits where a larger measured value means greater danger. */
    public double highValueThresholdScale() {
        return switch (this) {
            case LOW -> 1.2;
            case NORMAL -> 1.0;
            case HIGH -> 0.8;
        };
    }
}
