package com.radaeli.aeronauticsflybyinstruments.wire;

import net.minecraft.world.level.block.state.BlockBehaviour;

/** Industrial iron cleat: wires pass through it, up to three side by side. Carries no data itself. */
public final class WireSupportBlock extends WireNodeBlock {
    /** Hull of the base, corner posts and cap in the Blockbench model (see {@link WireNodeBlock#box}). */
    public static final int[][] PARTS = {{4, 12, 0, 4}};

    public WireSupportBlock(BlockBehaviour.Properties properties) {
        super(properties, PARTS);
    }
}
