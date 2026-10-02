package com.radaeli.aeronauticsflybyinstruments.avionics;

import net.minecraft.core.BlockPos;

/** Minimal world adapter so topology traversal can be tested without a running game. */
public interface NetworkAccess {
    boolean isLoaded(BlockPos pos);
    boolean isCable(BlockPos pos);
    String endpointType(BlockPos pos);
}
