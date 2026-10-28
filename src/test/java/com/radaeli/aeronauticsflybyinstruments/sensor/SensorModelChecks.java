package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Shared checks for the v2 sensor models (docs/visual-design-guide.md). */
public final class SensorModelChecks {
    static final String NS = "aeronautics_fly_by_instruments";
    private static final Map<String, int[]> FACE_AXES = Map.of(
            "north", new int[]{0, 1}, "south", new int[]{0, 1}, "east", new int[]{2, 1},
            "west", new int[]{2, 1}, "up", new int[]{0, 2}, "down", new int[]{0, 2});

    private SensorModelChecks() {}

    static JsonObject json(String namespace, String relativePath) throws Exception {
        try (var input = SensorModelChecks.class.getResourceAsStream("/assets/" + namespace + "/" + relativePath)) {
            assertNotNull(input, namespace + ":" + relativePath);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    public static JsonObject model(String name) throws Exception {
        return json(NS, "models/block/" + name + ".json");
    }

    /** Resolves "namespace:block/name" against its own namespace (create:... loads from Create's jar). */
    static BufferedImage texture(String id) throws Exception {
        String[] parts = id.split(":", 2);
        // Class#getResourceAsStream only searches this test's own module under FML; the class loader
        // also sees the other mods' jars, which is where create:... textures live.
        try (var input = SensorModelChecks.class.getClassLoader()
                .getResourceAsStream("assets/" + parts[0] + "/textures/" + parts[1] + ".png")) {
            assertNotNull(input, id);
            var image = ImageIO.read(input);
            assertNotNull(image, id);
            return image;
        }
    }

    static boolean hasAnimationMeta(String id) throws Exception {
        String[] parts = id.split(":", 2);
        try (var input = SensorModelChecks.class.getClassLoader()
                .getResourceAsStream("assets/" + parts[0] + "/textures/" + parts[1] + ".png.mcmeta")) {
            return input != null && JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8))
                    .getAsJsonObject().has("animation");
        }
    }

    public static double[] vector(JsonArray array) {
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    public static JsonObject element(JsonObject model, String name) {
        for (var entry : model.getAsJsonArray("elements")) {
            if (entry.getAsJsonObject().get("name").getAsString().equals(name)) return entry.getAsJsonObject();
        }
        throw new AssertionError("Missing element " + name);
    }

    public static Set<String> textureIds(JsonObject model) {
        var ids = new HashSet<String>();
        for (var entry : model.getAsJsonObject("textures").entrySet()) ids.add(entry.getValue().getAsString());
        return ids;
    }

    /** Guide v2: unrotated integer cuboids in 0..16, own textures opaque 16/32 px, 1 texel per unit. */
    public static void assertGuideRules(String name) throws Exception {
        assertGuideRules(name, 0);
    }

    /** As above, but cuboids may reach {@code intoHost} units below Y = 0, into the block a node is mounted on. */
    public static void assertGuideRules(String name, int intoHost) throws Exception {
        var model = model(name);
        var textures = model.getAsJsonObject("textures");
        for (var entry : textures.entrySet()) {
            var id = entry.getValue().getAsString();
            var image = texture(id);
            if (!id.startsWith(NS + ":")) continue;
            if (image.getHeight() > image.getWidth()) {
                // Animated strip of square frames: Minecraft only animates it with its .mcmeta.
                assertEquals(0, image.getHeight() % image.getWidth(), id + ": whole square frames");
                assertTrue(hasAnimationMeta(id), id + ": animated strip needs a .png.mcmeta");
            } else {
                assertEquals(image.getWidth(), image.getHeight(), id);
            }
            assertTrue(image.getWidth() == 16 || image.getWidth() == 32, id + ": 16 or 32 px");
            for (int x = 0; x < image.getWidth(); x++) for (int y = 0; y < image.getHeight(); y++) {
                assertEquals(255, image.getRGB(x, y) >>> 24, id);
            }
        }
        for (var entry : model.getAsJsonArray("elements")) {
            var cube = entry.getAsJsonObject();
            assertFalse(cube.has("rotation"), name + ": carcass cuboids stay unrotated");
            var from = vector(cube.getAsJsonArray("from"));
            var to = vector(cube.getAsJsonArray("to"));
            for (int axis = 0; axis < 3; axis++) {
                assertTrue(from[axis] >= (axis == 1 ? -intoHost : 0) && to[axis] <= 16 && from[axis] < to[axis],
                        name + ": inside the block");
                assertEquals(Math.rint(from[axis]), from[axis], name + ": integer coordinates");
                assertEquals(Math.rint(to[axis]), to[axis], name + ": integer coordinates");
            }
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
                String where = name + " " + cube.get("name").getAsString() + "." + face.getKey();
                assertEquals(geometryU, Math.abs(uv.get(2).getAsDouble() - uv.get(0).getAsDouble()) * image.getWidth() / 16,
                        1e-9, where + ": 1 texel per unit (u)");
                // UVs of an animated strip address one square frame.
                assertEquals(geometryV, Math.abs(uv.get(3).getAsDouble() - uv.get(1).getAsDouble()) * image.getWidth() / 16,
                        1e-9, where + ": 1 texel per unit (v)");
            }
        }
    }

    /** The _powered model keeps the geometry and only swaps the atlas carrying the redstone slots. */
    static void assertPoweredOnlySwapsAtlas(String name) throws Exception {
        var off = model(name);
        var on = model(name + "_powered");
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
                assertEquals(NS + ":block/" + name, a);
                assertEquals(NS + ":block/" + name + "_powered", b);
            }
        }
        assertEquals(1, changed, "Only the atlas with the redstone slots changes");
    }

    // --- Horizontal scanners (FACING + UPSIDE_DOWN) ------------------------------------------

    /** Point of the NORTH-facing source (0..1) as placed with the given facing / ceiling mount. */
    static double[] place(double x, double y, double z, Direction facing, boolean upsideDown) {
        double[] p = switch (facing) {
            case NORTH -> new double[]{x, y, z};
            case EAST -> new double[]{1 - z, y, x};
            case SOUTH -> new double[]{1 - x, y, 1 - z};
            case WEST -> new double[]{z, y, 1 - x};
            default -> throw new IllegalArgumentException();
        };
        if (!upsideDown) return p;
        return facing.getAxis() == Direction.Axis.Z ? new double[]{1 - p[0], 1 - p[1], p[2]}
                : new double[]{p[0], 1 - p[1], 1 - p[2]};
    }

    /** Outline and collision equal the exported cuboids in all four facings, floor and ceiling. */
    static void assertOutlineFollowsModel(BlockState base, String name) throws Exception {
        var model = model(name);
        for (var facing : Direction.Plane.HORIZONTAL) for (boolean upsideDown : new boolean[]{false, true}) {
            var volume = Shapes.empty();
            for (var entry : model.getAsJsonArray("elements")) {
                var cube = entry.getAsJsonObject();
                var from = vector(cube.getAsJsonArray("from"));
                var to = vector(cube.getAsJsonArray("to"));
                var a = place(from[0] / 16, from[1] / 16, from[2] / 16, facing, upsideDown);
                var b = place(to[0] / 16, to[1] / 16, to[2] / 16, facing, upsideDown);
                volume = Shapes.or(volume, Shapes.box(Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]),
                        Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])));
            }
            var state = base.setValue(AbstractTerrainScannerBlock.FACING, facing)
                    .setValue(AbstractTerrainScannerBlock.UPSIDE_DOWN, upsideDown);
            String where = name + " " + facing + (upsideDown ? " ceiling" : " floor");
            assertFalse(state.canOcclude(), where);
            assertFalse(Shapes.joinIsNotEmpty(volume, state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                    BooleanOp.NOT_SAME), where);
            assertFalse(Shapes.joinIsNotEmpty(volume, state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO,
                    CollisionContext.empty()), BooleanOp.NOT_SAME), where);
        }
    }

    /** The front lens faces the cast: centred at (8, 8), flush with z 0, front face on the optics texture. */
    static void assertFrontLens(String name) throws Exception {
        var model = model(name);
        var lens = element(model, "forward_antenna");
        var from = vector(lens.getAsJsonArray("from"));
        var to = vector(lens.getAsJsonArray("to"));
        assertEquals(0, from[2], "The lens is the frontmost part");
        assertEquals(8, (from[0] + to[0]) / 2);
        assertEquals(8, (from[1] + to[1]) / 2);
        for (var entry : model.getAsJsonArray("elements")) {
            assertTrue(vector(entry.getAsJsonObject().getAsJsonArray("from"))[2] >= 0);
        }
        var front = lens.getAsJsonObject("faces").getAsJsonObject("north").get("texture").getAsString();
        assertEquals(NS + ":block/" + name + "_optics",
                model.getAsJsonObject("textures").get(front.substring(1)).getAsString());
    }

    /** Floor variants rotate on Y; ceiling variants flip on X and add 180 degrees on Y. */
    static void assertBlockstateAndItem(String name) throws Exception {
        var variants = json(NS, "blockstates/" + name + ".json").getAsJsonObject("variants");
        for (var facing : Direction.Plane.HORIZONTAL) {
            int rotation = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
            var floor = variants.getAsJsonObject("facing=" + facing.getSerializedName() + ",upside_down=false");
            assertNotNull(floor, facing + " floor variant");
            assertEquals(NS + ":block/" + name, floor.get("model").getAsString());
            assertEquals(rotation, floor.has("y") ? floor.get("y").getAsInt() : 0);
            assertFalse(floor.has("x"));
            var ceiling = variants.getAsJsonObject("facing=" + facing.getSerializedName() + ",upside_down=true");
            assertNotNull(ceiling, facing + " ceiling variant");
            assertEquals(NS + ":block/" + name, ceiling.get("model").getAsString());
            assertEquals(180, ceiling.get("x").getAsInt());
            assertEquals((rotation + 180) % 360, ceiling.has("y") ? ceiling.get("y").getAsInt() : 0);
        }
        assertEquals(NS + ":block/" + name, json(NS, "models/item/" + name + ".json").get("parent").getAsString());
    }
}
