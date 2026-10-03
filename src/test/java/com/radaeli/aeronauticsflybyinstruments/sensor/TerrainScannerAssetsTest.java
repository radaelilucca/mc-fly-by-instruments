package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class TerrainScannerAssetsTest {
    private static final String ROOT = "/assets/aeronautics_fly_by_instruments/";

    private JsonObject json(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(ROOT + path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private double[] rotate(double x, double y, double z, Direction facing) {
        return switch (facing) {
            case NORTH -> new double[]{x, y, z};
            case EAST -> new double[]{1 - z, y, x};
            case SOUTH -> new double[]{1 - x, y, 1 - z};
            case WEST -> new double[]{z, y, 1 - x};
            default -> throw new IllegalArgumentException();
        };
    }

    @Test
    void outlineAndCollisionFollowTheExportedModelInAllHorizontalFacings() throws Exception {
        var model = json("models/block/terrain_scanner.json");
        for (var facing : Direction.Plane.HORIZONTAL) {
            var volume = Shapes.empty();
            for (var entry : model.getAsJsonArray("elements")) {
                var cube = entry.getAsJsonObject();
                assertFalse(cube.has("rotation"));
                var from = cube.getAsJsonArray("from");
                var to = cube.getAsJsonArray("to");
                for (int axis = 0; axis < 3; axis++) {
                    assertTrue(from.get(axis).getAsDouble() >= 0);
                    assertTrue(to.get(axis).getAsDouble() <= 16);
                    assertTrue(from.get(axis).getAsDouble() < to.get(axis).getAsDouble());
                }
                var a = rotate(from.get(0).getAsDouble() / 16, from.get(1).getAsDouble() / 16,
                        from.get(2).getAsDouble() / 16, facing);
                var b = rotate(to.get(0).getAsDouble() / 16, to.get(1).getAsDouble() / 16,
                        to.get(2).getAsDouble() / 16, facing);
                volume = Shapes.or(volume, Shapes.box(Math.min(a[0], b[0]), Math.min(a[1], b[1]),
                        Math.min(a[2], b[2]), Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])));
            }
            var state = ModBlocks.TERRAIN_SCANNER.get().defaultBlockState()
                    .setValue(AbstractTerrainScannerBlock.FACING, facing);
            assertFalse(state.canOcclude());
            assertFalse(Shapes.joinIsNotEmpty(volume, state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                    BooleanOp.NOT_SAME), facing.toString());
            assertFalse(Shapes.joinIsNotEmpty(volume, state.getCollisionShape(EmptyBlockGetter.INSTANCE,
                    BlockPos.ZERO, CollisionContext.empty()), BooleanOp.NOT_SAME), facing.toString());
        }
    }

    @Test
    void frontAntennaAndBlockstateRotationsAgreeWithCastDirection() throws Exception {
        var model = json("models/block/terrain_scanner.json");
        JsonObject antenna = null;
        for (var entry : model.getAsJsonArray("elements")) {
            var cube = entry.getAsJsonObject();
            if (cube.get("name").getAsString().equals("forward_antenna")) antenna = cube;
        }
        assertNotNull(antenna);
        var from = antenna.getAsJsonArray("from");
        var to = antenna.getAsJsonArray("to");
        assertEquals(8, (from.get(0).getAsDouble() + to.get(0).getAsDouble()) / 2);
        assertEquals(8, (from.get(1).getAsDouble() + to.get(1).getAsDouble()) / 2);
        assertEquals(0.25, from.get(2).getAsDouble());
        var frontTexture = antenna.getAsJsonObject("faces").getAsJsonObject("north").get("texture").getAsString();
        assertEquals("aeronautics_fly_by_instruments:block/terrain_scanner_antenna",
                model.getAsJsonObject("textures").get(frontTexture.substring(1)).getAsString());
        var variants = json("blockstates/terrain_scanner.json").getAsJsonObject("variants");
        for (var facing : Direction.Plane.HORIZONTAL) {
            var variant = variants.getAsJsonObject("facing=" + facing.getSerializedName());
            assertEquals("aeronautics_fly_by_instruments:block/terrain_scanner", variant.get("model").getAsString());
            int expectedRotation = switch (facing) {
                case NORTH -> 0; case EAST -> 90; case SOUTH -> 180; case WEST -> 270;
                default -> throw new IllegalArgumentException();
            };
            assertEquals(expectedRotation, variant.has("y") ? variant.get("y").getAsInt() : 0);
            var frontCentre = rotate(0.5, 0.5, 0.25 / 16, facing);
            assertEquals(facing.getStepX(), Math.signum(frontCentre[0] - 0.5));
            assertEquals(facing.getStepZ(), Math.signum(frontCentre[2] - 0.5));
        }
    }

    @Test
    void texturesAreOpaqueAndUvReferencesAreValid() throws Exception {
        var model = json("models/block/terrain_scanner.json");
        assertFalse(model.has("render_type"), "Scanner is opaque, not a luminous glass panel");
        for (var texture : model.getAsJsonObject("textures").entrySet()) {
            String path = texture.getValue().getAsString().split(":", 2)[1];
            try (var input = getClass().getResourceAsStream(ROOT + "textures/" + path + ".png")) {
                assertNotNull(input, path);
                var image = ImageIO.read(input);
                assertNotNull(image);
                assertEquals(32, image.getWidth());
                assertEquals(32, image.getHeight());
                for (int x = 0; x < 32; x++) for (int y = 0; y < 32; y++) {
                    assertEquals(255, image.getRGB(x, y) >>> 24, path);
                }
            }
        }
        for (var entry : model.getAsJsonArray("elements")) {
            for (var face : entry.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                var value = face.getValue().getAsJsonObject();
                assertFalse(value.has("cullface"));
                assertTrue(model.getAsJsonObject("textures").has(value.get("texture").getAsString().substring(1)));
                for (var uv : value.getAsJsonArray("uv")) assertTrue(uv.getAsDouble() >= 0 && uv.getAsDouble() <= 16);
            }
        }
        assertEquals("aeronautics_fly_by_instruments:block/terrain_scanner",
                json("models/item/terrain_scanner.json").get("parent").getAsString());
    }
}
