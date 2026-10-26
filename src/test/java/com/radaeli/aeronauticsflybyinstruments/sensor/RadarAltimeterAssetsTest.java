package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Radar Altimeter v2 (Create brass family): geometry, cast origin and docs/visual-design-guide.md rules. */
class RadarAltimeterAssetsTest {
    private static final String NS = "aeronautics_fly_by_instruments";
    private static final Map<String, int[]> FACE_AXES = Map.of(
            "north", new int[]{0, 1}, "south", new int[]{0, 1}, "east", new int[]{2, 1},
            "west", new int[]{2, 1}, "up", new int[]{0, 2}, "down", new int[]{0, 2});

    private JsonObject json(String namespace, String relativePath) throws Exception {
        try (var input = getClass().getResourceAsStream("/assets/" + namespace + "/" + relativePath)) {
            assertNotNull(input, namespace + ":" + relativePath);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private JsonObject model(String name) throws Exception {
        return json(NS, "models/block/" + name + ".json");
    }

    /** Resolves "namespace:block/name" against its own namespace (create:... loads from Create's jar). */
    private BufferedImage texture(String id) throws Exception {
        String[] parts = id.split(":", 2);
        // Class#getResourceAsStream only searches this test's own module under FML; the class loader
        // also sees the other mods' jars, which is where create:... textures live.
        try (var input = getClass().getClassLoader()
                .getResourceAsStream("assets/" + parts[0] + "/textures/" + parts[1] + ".png")) {
            assertNotNull(input, id);
            var image = ImageIO.read(input);
            assertNotNull(image, id);
            return image;
        }
    }

    private static double[] vector(JsonArray array) {
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    @Test
    void exportedCubesMatchOutlineAndCollisionWithoutMovingToAnotherBlock() throws Exception {
        var model = model("radar_altimeter");
        var volume = Shapes.empty();
        for (var entry : model.getAsJsonArray("elements")) {
            var cube = entry.getAsJsonObject();
            assertFalse(cube.has("rotation"));
            var from = vector(cube.getAsJsonArray("from"));
            var to = vector(cube.getAsJsonArray("to"));
            for (int axis = 0; axis < 3; axis++) {
                assertTrue(from[axis] >= 0);
                assertTrue(to[axis] <= 16);
                assertTrue(from[axis] < to[axis]);
                assertEquals(Math.rint(from[axis]), from[axis], "Guide v2: integer coordinates");
                assertEquals(Math.rint(to[axis]), to[axis], "Guide v2: integer coordinates");
            }
            volume = Shapes.or(volume, Block.box(from[0], from[1], from[2], to[0], to[1], to[2]));
        }
        var state = ModBlocks.RADAR_ALTIMETER.get().defaultBlockState();
        var outline = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        var collision = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty());
        assertFalse(Shapes.joinIsNotEmpty(volume, outline, BooleanOp.NOT_SAME));
        assertFalse(Shapes.joinIsNotEmpty(volume, collision, BooleanOp.NOT_SAME));
        assertFalse(Shapes.block().equals(outline));
        assertFalse(state.canOcclude());
        assertTrue(state.getProperties().isEmpty(), "Visual must not introduce a configurable cast direction");
    }

    @Test
    void downwardLensIsCentredOnTheCastOriginAndShowsTheGlassOnlyBelow() throws Exception {
        var model = model("radar_altimeter");
        JsonObject lens = null;
        for (var entry : model.getAsJsonArray("elements")) {
            var cube = entry.getAsJsonObject();
            if (cube.get("name").getAsString().equals("downward_antenna")) lens = cube;
        }
        assertNotNull(lens);
        var from = vector(lens.getAsJsonArray("from"));
        var to = vector(lens.getAsJsonArray("to"));
        assertEquals(0, from[1], "The lens is the lowest part of the block");
        assertEquals(8, (from[0] + to[0]) / 2);
        assertEquals(8, (from[2] + to[2]) / 2);
        var faces = lens.getAsJsonObject("faces");
        var down = faces.getAsJsonObject("down");
        assertEquals(NS + ":block/radar_altimeter_lens",
                model.getAsJsonObject("textures").get(down.get("texture").getAsString().substring(1)).getAsString());
        for (String side : new String[]{"north", "south", "east", "west"}) {
            assertNotEquals(down.get("uv"), faces.getAsJsonObject(side).get("uv"),
                    "The glass belongs to the sensing face, the rim to the sides");
        }
        for (var entry : model.getAsJsonArray("elements")) {
            var cube = entry.getAsJsonObject();
            if (cube == lens) continue;
            for (var face : cube.getAsJsonObject("faces").entrySet()) {
                assertNotEquals(down.get("texture"), face.getValue().getAsJsonObject().get("texture"),
                        cube.get("name").getAsString() + " must not reuse the lens glass");
            }
        }
    }

    @Test
    void texturesResolveInTheirOwnNamespaceAreOpaqueAndOneTexelPerUnit() throws Exception {
        for (String name : new String[]{"radar_altimeter", "radar_altimeter_powered"}) {
            var model = model(name);
            var textures = model.getAsJsonObject("textures");
            for (var entry : textures.entrySet()) {
                var id = entry.getValue().getAsString();
                var image = texture(id);
                if (id.startsWith(NS + ":")) {
                    assertEquals(image.getWidth(), image.getHeight(), id);
                    assertTrue(image.getWidth() == 16 || image.getWidth() == 32, id + ": 16 or 32 px");
                    for (int x = 0; x < image.getWidth(); x++) for (int y = 0; y < image.getHeight(); y++) {
                        assertEquals(255, image.getRGB(x, y) >>> 24, id);
                    }
                }
            }
            for (var entry : model.getAsJsonArray("elements")) {
                var cube = entry.getAsJsonObject();
                var from = vector(cube.getAsJsonArray("from"));
                var to = vector(cube.getAsJsonArray("to"));
                for (var face : cube.getAsJsonObject("faces").entrySet()) {
                    var json = face.getValue().getAsJsonObject();
                    assertFalse(json.has("cullface"), "Inset faces must not disappear against neighbouring blocks");
                    var key = json.get("texture").getAsString().substring(1);
                    assertTrue(textures.has(key));
                    var image = texture(textures.get(key).getAsString());
                    var uv = json.getAsJsonArray("uv");
                    for (var value : uv) assertTrue(value.getAsDouble() >= 0 && value.getAsDouble() <= 16);
                    int[] axes = FACE_AXES.get(face.getKey());
                    double geometryU = to[axes[0]] - from[axes[0]];
                    double geometryV = to[axes[1]] - from[axes[1]];
                    if (json.has("rotation") && json.get("rotation").getAsInt() % 180 != 0) {
                        double swap = geometryU; geometryU = geometryV; geometryV = swap;
                    }
                    double texelsU = Math.abs(uv.get(2).getAsDouble() - uv.get(0).getAsDouble()) * image.getWidth() / 16;
                    double texelsV = Math.abs(uv.get(3).getAsDouble() - uv.get(1).getAsDouble()) * image.getHeight() / 16;
                    String where = name + " " + cube.get("name").getAsString() + "." + face.getKey();
                    assertEquals(geometryU, texelsU, 1e-9, where + ": guide v2 requires 1 texel per unit (u)");
                    assertEquals(geometryV, texelsV, 1e-9, where + ": guide v2 requires 1 texel per unit (v)");
                }
            }
        }
        var item = json(NS, "models/item/radar_altimeter.json");
        assertEquals(NS + ":block/radar_altimeter", item.get("parent").getAsString());
        assertEquals(NS + ":block/radar_altimeter",
                json(NS, "blockstates/radar_altimeter.json").getAsJsonObject("variants").getAsJsonObject("")
                        .get("model").getAsString());
    }

    @Test
    void poweredVariantOnlySwapsTheAtlas() throws Exception {
        var off = model("radar_altimeter");
        var on = model("radar_altimeter_powered");
        assertEquals(off.get("elements"), on.get("elements"), "Powered state must not change the geometry");
        var offTextures = off.getAsJsonObject("textures");
        var onTextures = on.getAsJsonObject("textures");
        assertEquals(offTextures.keySet(), onTextures.keySet());
        int changed = 0;
        for (var key : offTextures.keySet()) {
            if (key.equals("particle")) continue;
            var a = offTextures.get(key).getAsString();
            var b = onTextures.get(key).getAsString();
            if (!a.equals(b)) {
                changed++;
                assertEquals(NS + ":block/radar_altimeter", a);
                assertEquals(NS + ":block/radar_altimeter_powered", b);
            }
        }
        assertEquals(1, changed, "Only the atlas with the redstone slot changes");
    }
}
