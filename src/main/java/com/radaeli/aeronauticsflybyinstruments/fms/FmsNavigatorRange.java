package com.radaeli.aeronauticsflybyinstruments.fms;

/** Link range between an FMS module and a Navigation Table holding its FMS Compass. */
public final class FmsNavigatorRange {
    /** Blocks; applies between different contraptions, or when either one is in the world. */
    public static final double LINK_RANGE = 256;

    private FmsNavigatorRange() {}

    /** Same contraption: no limit. Otherwise {@link #LINK_RANGE} blocks between the two. */
    public static boolean reachable(boolean sameContraption, double distanceSquared) {
        return sameContraption || distanceSquared <= LINK_RANGE * LINK_RANGE;
    }
}
