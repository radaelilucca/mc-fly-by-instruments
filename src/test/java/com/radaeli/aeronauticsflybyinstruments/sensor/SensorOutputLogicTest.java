package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.radaeli.aeronauticsflybyinstruments.avionics.ReadingStatus;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SensorOutputLogicTest {
    @Test void altitudeOutputClampsAndRoundsLinearRamp() {
        var range = new SensorOutputConfig.Range(0, 32);
        assertEquals(0, SensorOutputConfig.altitudePower(-1, range));
        assertEquals(0, SensorOutputConfig.altitudePower(0, range));
        assertEquals(8, SensorOutputConfig.altitudePower(16, range));
        assertEquals(15, SensorOutputConfig.altitudePower(32, range));
        assertEquals(15, SensorOutputConfig.altitudePower(100, range));
        assertEquals(0, SensorOutputConfig.altitudePower(Double.NaN, range));
    }

    @Test void terrainDistanceMapsNearToFifteenAndFarToZero() {
        var range = new SensorOutputConfig.Range(4, 32);
        assertEquals(15, SensorOutputConfig.proximityPower(0, range));
        assertEquals(15, SensorOutputConfig.proximityPower(4, range));
        assertEquals(8, SensorOutputConfig.proximityPower(18, range));
        assertEquals(0, SensorOutputConfig.proximityPower(32, range));
        assertEquals(0, SensorOutputConfig.proximityPower(Double.POSITIVE_INFINITY, range));
    }

    @Test void invalidOrUnavailableTerrainHasNoOutput() {
        var config = new SensorOutputConfig(false, false);
        assertFalse(new SensorOutputConfig.Range(4, 32).valid(31));
        assertFalse(new SensorOutputConfig.Range(32, 32).valid(128));
        assertEquals(0, SensorOutputLogic.terrain(TerrainScanSnapshot.unavailable(9, 0), config,
                false, 90).get(SensorOutputConfig.Channel.FRONT));
        assertEquals(0, SensorOutputLogic.terrain(TerrainScanSnapshot.failed(9, 0), config,
                false, 90).get(SensorOutputConfig.Channel.FRONT));
        var clear = new TerrainScanSnapshot(ReadingStatus.VALID, List.of(), 9, 9, 1);
        assertEquals(0, SensorOutputLogic.terrain(clear, config, false, 90).get(SensorOutputConfig.Channel.FRONT));
    }

    @Test void basicScannerUsesNearestHitFromTheEntireFixedCone() {
        var config = new SensorOutputConfig(false, false);
        var scan = new TerrainScanSnapshot(ReadingStatus.VALID,
                List.of(new TerrainScanContact(0, 20, 0, 0, 1),
                        new TerrainScanContact(1, 5, 0.7, 0, 0.7)), 9, 9, 1);
        assertEquals(14, SensorOutputLogic.terrain(scan, config, false, 90)
                .get(SensorOutputConfig.Channel.FRONT));
    }

    @Test void advancedTerrainSplitsByLocalAzimuthAndDisablesSidesAtFiftyDegrees() {
        var config = new SensorOutputConfig(true, false);
        var contacts = List.of(
                new TerrainScanContact(0, 18, 0, 0, 1),
                new TerrainScanContact(1, 4, -1, 0, 1),
                new TerrainScanContact(2, 10, 1, 0, 1));
        var scan = new TerrainScanSnapshot(ReadingStatus.VALID, contacts, 9, 9, 1);
        var narrow = SensorOutputLogic.terrain(scan, config, true, 50);
        assertEquals(8, narrow.get(SensorOutputConfig.Channel.FRONT));
        assertEquals(0, narrow.get(SensorOutputConfig.Channel.LEFT));
        assertEquals(0, narrow.get(SensorOutputConfig.Channel.RIGHT));
        var wide = SensorOutputLogic.terrain(scan, config, true, 51);
        assertEquals(15, wide.get(SensorOutputConfig.Channel.LEFT));
        assertEquals(12, wide.get(SensorOutputConfig.Channel.RIGHT));
    }

    @Test void advancedOutputsFollowFacingAndBasicRepeatsOnEveryFace() {
        var config = new SensorOutputConfig(true, false);
        config.mode(SensorOutputConfig.Mode.FACES);
        Map<SensorOutputConfig.Channel, Integer> levels = Map.of(
                SensorOutputConfig.Channel.FRONT, 3, SensorOutputConfig.Channel.LEFT, 7,
                SensorOutputConfig.Channel.RIGHT, 11);
        assertEquals(3, SensorOutputLogic.weakPower(levels, config, false, true, Direction.NORTH, Direction.NORTH));
        assertEquals(7, SensorOutputLogic.weakPower(levels, config, false, true, Direction.NORTH, Direction.WEST));
        assertEquals(11, SensorOutputLogic.weakPower(levels, config, false, true, Direction.NORTH, Direction.EAST));
        assertEquals(0, SensorOutputLogic.weakPower(levels, config, false, true, Direction.NORTH, Direction.UP));
        config.mode(SensorOutputConfig.Mode.LINK);
        assertEquals(0, SensorOutputLogic.weakPower(levels, config, false, true, Direction.NORTH, Direction.NORTH));
    }
}
