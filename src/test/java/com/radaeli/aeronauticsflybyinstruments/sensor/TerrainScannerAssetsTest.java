package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import org.junit.jupiter.api.Test;

import static com.radaeli.aeronauticsflybyinstruments.sensor.SensorModelChecks.*;
import static org.junit.jupiter.api.Assertions.*;

/** Terrain Scanner (Basic) v2: Create andesite tier with a stepped front nozzle. */
class TerrainScannerAssetsTest {
    private static final String NAME = "terrain_scanner";

    @Test
    void followsTheGuideRulesAndOnlySwapsTheAtlasWhenPowered() throws Exception {
        assertGuideRules(NAME);
        assertGuideRules(NAME + "_powered");
        assertPoweredOnlySwapsAtlas(NAME);
    }

    @Test
    void outlineAndCollisionFollowTheModelInEveryFacingOnFloorAndCeiling() throws Exception {
        assertOutlineFollowsModel(ModBlocks.TERRAIN_SCANNER.get().defaultBlockState(), NAME);
    }

    @Test
    void frontLensFacesTheCastAndBlockstatesRotateWithTheBlock() throws Exception {
        assertFrontLens(NAME);
        assertBlockstateAndItem(NAME);
    }

    @Test
    void nozzleStepsDownTowardsTheLensLikeATelescope() throws Exception {
        var model = model(NAME);
        String[] steps = {"nozzle_ring", "nozzle_tip", "forward_antenna"};
        double previousWidth = Double.MAX_VALUE, previousFront = Double.MAX_VALUE;
        for (String step : steps) {
            var cube = element(model, step);
            var from = vector(cube.getAsJsonArray("from"));
            var to = vector(cube.getAsJsonArray("to"));
            double width = to[0] - from[0];
            assertEquals(width, to[1] - from[1], step + " is square");
            assertTrue(width < previousWidth, step + " narrows towards the front");
            assertTrue(from[2] < previousFront, step + " sits in front of the previous step");
            assertEquals(8, (from[0] + to[0]) / 2);
            assertEquals(8, (from[1] + to[1]) / 2);
            previousWidth = width;
            previousFront = from[2];
        }
    }
}
