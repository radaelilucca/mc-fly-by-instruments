package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.radaeli.aeronauticsflybyinstruments.block.CableSocketProvider;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static com.radaeli.aeronauticsflybyinstruments.sensor.SensorModelChecks.*;
import static org.junit.jupiter.api.Assertions.*;

/** Advanced Terrain Scanner v2: brass over industrial iron, three-sector lens bar. */
class AdvancedTerrainScannerAssetsTest {
    private static final String NAME = "terrain_scanner_advanced";

    @Test
    void followsTheGuideRulesAndOnlySwapsTheAtlasWhenPowered() throws Exception {
        assertGuideRules(NAME);
        assertGuideRules(NAME + "_powered");
        assertPoweredOnlySwapsAtlas(NAME);
    }

    @Test
    void outlineAndCollisionFollowTheModelInEveryFacingOnFloorAndCeiling() throws Exception {
        assertOutlineFollowsModel(ModBlocks.TERRAIN_SCANNER_ADVANCED.get().defaultBlockState(), NAME);
    }

    @Test
    void frontLensFacesTheCastAndBlockstatesRotateWithTheBlock() throws Exception {
        assertFrontLens(NAME);
        assertBlockstateAndItem(NAME);
    }

    @Test
    void threeLensesMatchTheLeftFrontAndRightOutputChannels() throws Exception {
        var model = model(NAME);
        var left = vector(element(model, "lens_left").getAsJsonArray("from"));
        var leftTo = vector(element(model, "lens_left").getAsJsonArray("to"));
        var right = vector(element(model, "lens_right").getAsJsonArray("from"));
        var rightTo = vector(element(model, "lens_right").getAsJsonArray("to"));
        var front = vector(element(model, "forward_antenna").getAsJsonArray("from"));
        // Facing NORTH, the scanner's own left (counter-clockwise) is WEST: low x.
        assertEquals(Direction.WEST, Direction.NORTH.getCounterClockWise());
        assertTrue((left[0] + leftTo[0]) / 2 < 8, "LEFT lens on the west side");
        assertTrue((right[0] + rightTo[0]) / 2 > 8, "RIGHT lens on the east side");
        assertTrue(front[2] < left[2] && front[2] < right[2], "FRONT lens protrudes past the side lenses");
        assertEquals(left[2], right[2]);
        assertEquals(leftTo[0] - left[0], rightTo[0] - right[0], "Side lenses are symmetrical");
    }

    @Test
    void isDistinctFromTheBasicScannerAndTheAltimeterButSharesTheBodyAndSockets() throws Exception {
        var advanced = textureIds(model(NAME));
        var basic = textureIds(model("terrain_scanner"));
        var altimeter = textureIds(model("radar_altimeter"));
        var shared = new HashSet<>(advanced);
        shared.retainAll(basic);
        assertTrue(shared.isEmpty(), "No shared textures with the Basic: " + shared);
        shared = new HashSet<>(advanced);
        shared.retainAll(altimeter);
        assertTrue(shared.isEmpty(), "No shared textures with the Altimeter: " + shared);
        assertEquals(element(model(NAME), "body").get("from"), element(model("terrain_scanner"), "body").get("from"));
        assertEquals(element(model(NAME), "body").get("to"), element(model("terrain_scanner"), "body").get("to"));

        for (var facing : Direction.Plane.HORIZONTAL) for (boolean upsideDown : new boolean[]{false, true}) {
            var a = ModBlocks.TERRAIN_SCANNER_ADVANCED.get().defaultBlockState()
                    .setValue(AbstractTerrainScannerBlock.FACING, facing).setValue(AbstractTerrainScannerBlock.UPSIDE_DOWN, upsideDown);
            var b = ModBlocks.TERRAIN_SCANNER.get().defaultBlockState()
                    .setValue(AbstractTerrainScannerBlock.FACING, facing).setValue(AbstractTerrainScannerBlock.UPSIDE_DOWN, upsideDown);
            assertTrue(Shapes.joinIsNotEmpty(a.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                    b.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), BooleanOp.NOT_SAME),
                    "Silhouettes must differ: " + facing);
            for (var face : Direction.values()) {
                assertEquals(((CableSocketProvider) b.getBlock()).cableSocket(b, face),
                        ((CableSocketProvider) a.getBlock()).cableSocket(a, face), facing + " " + face);
            }
        }
    }
}
