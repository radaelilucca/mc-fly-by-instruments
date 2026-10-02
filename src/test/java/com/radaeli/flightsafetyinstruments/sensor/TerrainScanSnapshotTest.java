package com.radaeli.flightsafetyinstruments.sensor;

import com.radaeli.flightsafetyinstruments.avionics.ReadingStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TerrainScanSnapshotTest {
    private static final TerrainScanContact CONTACT = new TerrainScanContact(1, 12.5, 0.0, 0.0, 1.0);

    @Test
    void emptyCompleteSweepIsValidAndReportsFullCoverage() {
        var scan = new TerrainScanSnapshot(ReadingStatus.VALID, List.of(), 9, 9, 100);
        assertTrue(scan.contacts().isEmpty());
        assertEquals(1.0, scan.coverage());
    }

    @Test
    void partialSweepKeepsKnownContactsAndReportsCompletedRayRatio() {
        var scan = new TerrainScanSnapshot(ReadingStatus.VALID, List.of(CONTACT), 5, 9, 105);
        assertEquals(List.of(CONTACT), scan.contacts());
        assertEquals(5.0 / 9.0, scan.coverage(), 1.0E-12);
    }

    @Test
    void zeroCoverageIsUnavailableAndRuntimeFailureIsFailed() {
        assertEquals(ReadingStatus.UNAVAILABLE, TerrainScanSnapshot.unavailable(81, 110).status());
        assertEquals(0.0, TerrainScanSnapshot.unavailable(81, 110).coverage());
        assertEquals(ReadingStatus.FAILED, TerrainScanSnapshot.failed(81, 115).status());
    }

    @Test
    void rejectsValidScanWithoutAnyCompletedRay() {
        assertThrows(IllegalArgumentException.class,
                () -> new TerrainScanSnapshot(ReadingStatus.VALID, List.of(), 0, 9, 120));
    }
}
