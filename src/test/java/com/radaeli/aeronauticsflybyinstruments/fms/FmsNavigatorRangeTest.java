package com.radaeli.aeronauticsflybyinstruments.fms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FmsNavigatorRangeTest {
    @Test
    void sameContraptionHasNoLimit() {
        assertTrue(FmsNavigatorRange.reachable(true, 100_000.0 * 100_000.0));
    }

    @Test
    void otherwiseTheLinkReaches256Blocks() {
        assertTrue(FmsNavigatorRange.reachable(false, 0));
        assertTrue(FmsNavigatorRange.reachable(false, 256.0 * 256.0));
        assertFalse(FmsNavigatorRange.reachable(false, 256.01 * 256.01));
    }
}
