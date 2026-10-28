package com.radaeli.aeronauticsflybyinstruments.wire;

import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WireRouteTest {
    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final List<BlockPos> PATH = List.of(new BlockPos(10, 64, 10), new BlockPos(14, 65, 10),
            new BlockPos(14, 65, 18), new BlockPos(20, 64, 18));

    private static WireRoute route() {
        return WireRoute.of(ID, PATH, List.of(0, 1, 2, 0));
    }

    @Test
    void isStoredRelativeToTheFirstConnector() {
        var route = route();
        assertTrue(route.ownedByHolder());
        assertEquals(new BlockPos(10, 0, 8), route.farEnd());
        assertEquals(new BlockPos(4, 1, 0), route.anchors().get(1).offset());
    }

    @Test
    void rebasingKeepsEveryAbsolutePosition() {
        var route = route();
        for (var anchor : route.anchors()) {
            var seen = route.rebasedTo(anchor.offset());
            BlockPos holder = PATH.getFirst().offset(anchor.offset());
            for (int i = 0; i < PATH.size(); i++) {
                assertEquals(PATH.get(i), holder.offset(seen.anchors().get(i).offset()));
                assertEquals(route.anchors().get(i).lane(), seen.anchors().get(i).lane());
            }
            assertNotNull(seen.self());
        }
    }

    @Test
    void onlyTheEndsHaveAFarEnd() {
        var route = route();
        assertEquals(new BlockPos(-10, 0, -8), route.rebasedTo(new BlockPos(10, 0, 8)).farEnd());
        assertNull(route.rebasedTo(route.anchors().get(1).offset()).farEnd());
    }

    @Test
    void survivesACodecRoundTrip() {
        var route = route();
        var json = WireRoute.CODEC.encodeStart(JsonOps.INSTANCE, route).getOrThrow();
        assertEquals(route, WireRoute.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void rotatesWithAContraption() {
        // 90° clockwise around Y: (x, z) -> (-z, x).
        var rotated = route().transformed(pos -> new BlockPos(-pos.getZ(), pos.getY(), pos.getX()));
        assertEquals(new BlockPos(-8, 0, 10), rotated.farEnd());
        assertEquals(route().length(), rotated.length(), 1e-9);
        assertTrue(rotated.ownedByHolder());
    }

    @Test
    void rejectsRepeatedAnchorsAndSinglePoints() {
        assertThrows(IllegalArgumentException.class, () -> WireRoute.of(ID, List.of(BlockPos.ZERO), List.of(0)));
        assertThrows(IllegalArgumentException.class,
                () -> WireRoute.of(ID, List.of(BlockPos.ZERO, BlockPos.ZERO), List.of(0, 0)));
    }

    @Test
    void rulesCheckSpanPointsAndRepeats() {
        var path = List.of(BlockPos.ZERO, new BlockPos(5, 0, 0));
        assertEquals(WireRules.Check.OK, WireRules.canAppend(path, new BlockPos(5, 0, 24), 24, 32));
        assertEquals(WireRules.Check.TOO_LONG, WireRules.canAppend(path, new BlockPos(5, 0, 25), 24, 32));
        assertEquals(WireRules.Check.ALREADY_IN_ROUTE, WireRules.canAppend(path, BlockPos.ZERO, 24, 32));
        assertEquals(WireRules.Check.TOO_MANY_POINTS, WireRules.canAppend(path, new BlockPos(1, 0, 0), 24, 2));
    }

    @Test
    void costRoundsUpPerStartedItem() {
        assertEquals(1, WireRules.cost(0.5, 8));
        assertEquals(1, WireRules.cost(8, 8));
        assertEquals(2, WireRules.cost(8.01, 8));
        assertEquals(3, WireRules.cost(17, 8));
    }

    @Test
    void lanesFillCentreFirst() {
        assertEquals(0, WireRules.freeLane(List.of()));
        assertEquals(1, WireRules.freeLane(List.of(0)));
        assertEquals(0, WireRules.freeLane(List.of(1, 2)));
        assertEquals(-1, WireRules.freeLane(List.of(0, 1, 2)));
    }
}
