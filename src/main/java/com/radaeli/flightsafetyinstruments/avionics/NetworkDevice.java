package com.radaeli.flightsafetyinstruments.avionics;

/** Network endpoint described relative to the querying endpoint, without an internal graph ID. */
public record NetworkDevice(String type, int x, int y, int z) {}
