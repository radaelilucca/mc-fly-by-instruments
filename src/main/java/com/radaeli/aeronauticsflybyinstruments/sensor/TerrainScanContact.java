package com.radaeli.aeronauticsflybyinstruments.sensor;

/** One block-collision return, expressed in the scanner's right/up/forward frame. */
public record TerrainScanContact(int rayIndex, double distance, double directionX, double directionY,
                                 double directionZ) {}
