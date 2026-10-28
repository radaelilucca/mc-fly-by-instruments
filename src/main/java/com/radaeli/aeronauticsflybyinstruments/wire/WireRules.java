package com.radaeli.aeronauticsflybyinstruments.wire;

import net.minecraft.core.BlockPos;

import java.util.List;

/** Placement rules shared by the server (authority) and the client preview. */
public final class WireRules {
    public static final int CONNECTOR_CAPACITY = 4;
    public static final int SUPPORT_LANES = 3;

    private WireRules() {}

    public enum Check {
        OK(null),
        TOO_LONG("too_long"),
        TOO_MANY_POINTS("too_many_points"),
        ALREADY_IN_ROUTE("already_in_route"),
        FULL("full"),
        OTHER_STRUCTURE("other_structure");

        private final String key;

        Check(String key) { this.key = key; }

        public String translationKey() {
            return key == null ? null : "message.aeronautics_fly_by_instruments.wire." + key;
        }
    }

    /** Whether {@code next} can follow the route built so far (capacity and context are checked by the caller). */
    public static Check canAppend(List<BlockPos> route, BlockPos next, int maxSpan, int maxAnchors) {
        if (route.contains(next)) return Check.ALREADY_IN_ROUTE;
        if (route.size() + 1 > maxAnchors) return Check.TOO_MANY_POINTS;
        if (!route.isEmpty() && route.getLast().distSqr(next) > (double) maxSpan * maxSpan) return Check.TOO_LONG;
        return Check.OK;
    }

    /** Wire items needed for a route, rounding up; at least one. */
    public static int cost(double length, int blocksPerItem) {
        return Math.max(1, (int) Math.ceil(length / Math.max(1, blocksPerItem) - 1e-9));
    }

    /** First lane not in use, or -1 when the support is full. Centre first, then the sides. */
    public static int freeLane(List<Integer> usedLanes) {
        for (int lane = 0; lane < SUPPORT_LANES; lane++) if (!usedLanes.contains(lane)) return lane;
        return -1;
    }
}
