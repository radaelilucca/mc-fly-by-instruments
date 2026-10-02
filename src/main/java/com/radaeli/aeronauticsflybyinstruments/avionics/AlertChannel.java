package com.radaeli.aeronauticsflybyinstruments.avionics;

public enum AlertChannel {
    TERRAIN("terrain"),
    PULL_UP("pull_up"),
    SINK_RATE("sink_rate"),
    DONT_SINK("dont_sink"),
    TOO_LOW("too_low"),
    BANK_ANGLE("bank_angle"),
    SENSOR_FAIL("sensor_fail");

    private final String id;

    AlertChannel(String id) { this.id = id; }

    public String id() { return id; }
}
