package com.radaeli.aeronauticsflybyinstruments.fms;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FmsAssetsTest {
    @Test
    void moduleUsesCenteredModelAndNavigatorKeepsCompassPlaceholder() throws Exception {
        var module = readJson("/assets/aeronautics_fly_by_instruments/models/item/fms_module.json");
        var navigator = readJson("/assets/aeronautics_fly_by_instruments/models/item/fms_navigator.json");
        assertEquals("aeronautics_fly_by_instruments:block/fms_module_2x3", module.get("parent").getAsString());
        var model = readJson("/assets/aeronautics_fly_by_instruments/models/block/fms_module_2x3.json");
        double[] low = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] high = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (var element : model.getAsJsonArray("elements")) {
            var cube = element.getAsJsonObject();
            for (int axis = 0; axis < 3; axis++) {
                low[axis] = Math.min(low[axis], cube.getAsJsonArray("from").get(axis).getAsDouble());
                high[axis] = Math.max(high[axis], cube.getAsJsonArray("to").get(axis).getAsDouble());
            }
        }
        assertEquals(8, (low[0] + high[0]) / 2);
        assertEquals(8, (low[2] + high[2]) / 2);
        assertEquals(0, low[1]);
        assertTrue(high[0] - low[0] < 16 * 2.0 / 3);
        assertTrue(high[2] - low[2] < 16);
        assertEquals("minecraft:item/generated", navigator.get("parent").getAsString());
        assertEquals("minecraft:item/compass_00", navigator.getAsJsonObject("textures")
                .get("layer0").getAsString());
    }

    /** v2.1 (guide §9): 10 x 14 carcass, integer grid, 1 texel per unit, industrial iron housing. */
    @Test
    void moduleFollowsTheTableModuleGuide() throws Exception {
        var model = readJson("/assets/aeronautics_fly_by_instruments/models/block/fms_module_2x3.json");
        var textures = model.getAsJsonObject("textures");
        assertEquals("create:block/industrial_iron_block", textures.get("particle").getAsString());
        var elements = model.getAsJsonArray("elements");
        assertTrue(elements.size() <= 15, "Screen module budget is ~15 cubes");
        var names = new java.util.HashSet<String>();
        double[] low = {16, 16, 16};
        double[] high = {0, 0, 0};
        for (var element : elements) {
            var cube = element.getAsJsonObject();
            names.add(cube.get("name").getAsString());
            assertFalse(cube.has("rotation"), cube.get("name") + ": unrotated");
            double[] size = new double[3];
            for (int axis = 0; axis < 3; axis++) {
                double from = cube.getAsJsonArray("from").get(axis).getAsDouble();
                double to = cube.getAsJsonArray("to").get(axis).getAsDouble();
                assertEquals(Math.rint(from), from, "integer coordinates");
                assertEquals(Math.rint(to), to, "integer coordinates");
                low[axis] = Math.min(low[axis], from);
                high[axis] = Math.max(high[axis], to);
                size[axis] = to - from;
            }
            for (var face : cube.getAsJsonObject("faces").entrySet()) {
                var json = face.getValue().getAsJsonObject();
                assertFalse(json.has("cullface"), "Modules never cull against neighbouring blocks");
                var id = textures.get(json.get("texture").getAsString().substring(1)).getAsString();
                int width = id.startsWith("create:") ? 16 : readImage(id).getWidth();
                int[] axes = switch (face.getKey()) {
                    case "north", "south" -> new int[]{0, 1};
                    case "east", "west" -> new int[]{2, 1};
                    default -> new int[]{0, 2};
                };
                var uv = json.getAsJsonArray("uv");
                assertEquals(size[axes[0]], Math.abs(uv.get(2).getAsDouble() - uv.get(0).getAsDouble()) * width / 16,
                        cube.get("name") + "." + face.getKey() + ": 1 texel per unit (u)");
                assertEquals(size[axes[1]], Math.abs(uv.get(3).getAsDouble() - uv.get(1).getAsDouble()) * width / 16,
                        cube.get("name") + "." + face.getKey() + ": 1 texel per unit (v)");
            }
        }
        assertArrayEquals(new double[]{3, 0, 1}, low);
        assertArrayEquals(new double[]{13, 3, 15}, high);
        assertTrue(names.containsAll(java.util.List.of("housing", "screen", "knob_left", "knob_right")), names.toString());
        assertEquals(32, readImage("aeronautics_fly_by_instruments:block/fms_module").getWidth());
    }

    private static java.awt.image.BufferedImage readImage(String id) throws Exception {
        String path = "/assets/aeronautics_fly_by_instruments/textures/block/"
                + id.substring("aeronautics_fly_by_instruments:block/".length()) + ".png";
        try (var stream = FmsAssetsTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing resource " + path);
            return javax.imageio.ImageIO.read(stream);
        }
    }

    @Test
    void portugueseAndEnglishCoverEveryFmsVisibleString() throws Exception {
        var required = new String[]{"title", "waypoints", "navigator", "name", "name_hint", "dimension",
                "coordinate_x", "coordinate_y", "coordinate_z", "current_position", "save_waypoint",
                "delete", "set_target", "clear_target", "navigator_help", "confirm_rebind",
                "navigator_missing", "navigator_linked", "navigator_unbound", "navigator_other_link"};
        var english = readJson("/assets/aeronautics_fly_by_instruments/lang/en_us.json");
        var portuguese = readJson("/assets/aeronautics_fly_by_instruments/lang/pt_br.json");
        for (String key : required) {
            String fullKey = "gui.aeronautics_fly_by_instruments.fms." + key;
            assertTrue(english.has(fullKey), "Missing en_us key " + fullKey);
            assertTrue(portuguese.has(fullKey), "Missing pt_br key " + fullKey);
        }
    }

    private static com.google.gson.JsonObject readJson(String path) throws Exception {
        try (var stream = FmsAssetsTest.class.getResourceAsStream(path)) {
            assertNotNull(stream, "Missing resource " + path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
