package com.radaeli.aeronauticsflybyinstruments.avionics;

import java.util.List;

/** Implemented by endpoints that publish typed values to consumers on a connected cable network. */
public interface AvionicsSignalProvider {
    List<AvionicsDataChannel<?>> avionicsSignals();
}
