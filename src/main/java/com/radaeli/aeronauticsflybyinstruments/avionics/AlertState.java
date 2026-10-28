package com.radaeli.aeronauticsflybyinstruments.avionics;

public enum AlertState {
    INACTIVE,
    ACTIVE,
    /** INOP: the function lacks a usable input; the engine reports why. */
    UNAVAILABLE,
    /** The player switched the function off on the Avionics Computer. */
    INHIBITED
}
