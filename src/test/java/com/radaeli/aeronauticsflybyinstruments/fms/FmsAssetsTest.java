package com.radaeli.aeronauticsflybyinstruments.fms;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class FmsAssetsTest {
    @Test
    void moduleAndNavigatorUseTheApprovedPlaceholders() throws Exception {
        var module = readJson("/assets/aeronautics_fly_by_instruments/models/item/fms_module.json");
        var navigator = readJson("/assets/aeronautics_fly_by_instruments/models/item/fms_navigator.json");
        assertEquals("minecraft:block/iron_block", module.get("parent").getAsString());
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
