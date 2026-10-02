package com.radaeli.aeronauticsflybyinstruments.avionics;

/** Marker/contract implemented by block entities that can join an avionics cable network. */
public interface AvionicsEndpoint {
    /** Stable, player-facing endpoint type (for example {@code modular_table}). */
    String avionicsType();
}
