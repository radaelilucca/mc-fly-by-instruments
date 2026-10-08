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
