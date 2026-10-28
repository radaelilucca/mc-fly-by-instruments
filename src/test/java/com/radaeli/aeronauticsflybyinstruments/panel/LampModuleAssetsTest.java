package com.radaeli.aeronauticsflybyinstruments.panel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Lamp modules 1x1 and 2x1 v2.1: guide §9 (table modules) and the nixie-style glass/filament split. */
class LampModuleAssetsTest {
    private static final String ROOT = "/assets/aeronautics_fly_by_instruments/";
    private static final String NS = "aeronautics_fly_by_instruments:block/";
    /** Model name -> carcass bounds {minX, maxX, minZ, maxZ} (guide §9.2: 4 per cell, 10 for two). */
    private static final Map<String, int[]> FOOTPRINTS = Map.of(
            "lamp_module_1x1", new int[]{6, 10, 6, 10}, "lamp_module_2x1", new int[]{3, 13, 6, 10});
    private static final Map<String, int[]> FACE_AXES = Map.of(
            "north", new int[]{0, 1}, "south", new int[]{0, 1}, "east", new int[]{2, 1},
            "west", new int[]{2, 1}, "up", new int[]{0, 2}, "down", new int[]{0, 2});

    private JsonObject model(String name) throws Exception {
        try (var input = getClass().getResourceAsStream(ROOT + "models/block/" + name + ".json")) {
            assertNotNull(input, name);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private BufferedImage texture(String id) throws Exception {
        assertTrue(id.startsWith(NS), id);
        try (var input = getClass().getResourceAsStream(ROOT + "textures/block/" + id.substring(NS.length()) + ".png")) {
            assertNotNull(input, id);
            return ImageIO.read(input);
        }
    }

    private static double[] vector(JsonObject cube, String key) {
        var array = cube.getAsJsonArray(key);
        return new double[]{array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()};
    }

    @Test
    void fitsItsFootprintOnTheGuideGridWithOneTexelPerUnit() throws Exception {
        for (var footprint : FOOTPRINTS.entrySet()) for (var state : new String[]{"_off", "_on"}) {
            var name = footprint.getKey() + state;
            int[] bounds = footprint.getValue();
            var model = model(name);
            assertEquals("minecraft:translucent", model.get("render_type").getAsString());
            var textures = model.getAsJsonObject("textures");
            Map<String, BufferedImage> images = new HashMap<>();
            for (var entry : textures.entrySet()) images.put(entry.getKey(), texture(entry.getValue().getAsString()));
            double minX = 16, maxX = 0, minZ = 16, maxZ = 0, minY = 16, maxY = 0;
            for (var element : model.getAsJsonArray("elements")) {
                var cube = element.getAsJsonObject();
                assertFalse(cube.has("rotation"), name + ": unrotated");
                var from = vector(cube, "from");
                var to = vector(cube, "to");
                for (int axis = 0; axis < 3; axis++) {
                    assertEquals(Math.rint(from[axis]), from[axis], name + ": integer coordinates");
                    assertEquals(Math.rint(to[axis]), to[axis], name + ": integer coordinates");
                }
                minX = Math.min(minX, from[0]); maxX = Math.max(maxX, to[0]);
                minY = Math.min(minY, from[1]); maxY = Math.max(maxY, to[1]);
                minZ = Math.min(minZ, from[2]); maxZ = Math.max(maxZ, to[2]);
                for (var face : cube.getAsJsonObject("faces").entrySet()) {
                    var json = face.getValue().getAsJsonObject();
                    assertFalse(json.has("cullface"), "Modules never cull against neighbouring blocks");
                    var image = images.get(json.get("texture").getAsString().substring(1));
                    assertEquals(16, image.getWidth());
                    var uv = json.getAsJsonArray("uv");
                    int[] axes = FACE_AXES.get(face.getKey());
                    assertEquals(to[axes[0]] - from[axes[0]], Math.abs(uv.get(2).getAsDouble() - uv.get(0).getAsDouble()),
                            name + " " + cube.get("name") + "." + face.getKey() + ": 1 texel per unit");
                    assertEquals(to[axes[1]] - from[axes[1]], Math.abs(uv.get(3).getAsDouble() - uv.get(1).getAsDouble()),
                            name + " " + cube.get("name") + "." + face.getKey() + ": 1 texel per unit");
                }
            }
            assertEquals(bounds[0], minX, name); assertEquals(bounds[1], maxX, name);
            assertEquals(bounds[2], minZ, name); assertEquals(bounds[3], maxZ, name);
            assertEquals(0, minY, "Rests on the table surface");
            assertEquals(4, maxY, "Guide §9.2: 2-4 units tall");
            assertTrue(maxY / 16 <= PanelCoordinates.MODULE_TOP - PanelCoordinates.SURFACE + 1e-9, "Selection volume covers the lamp");
        }
    }

    @Test
    void nixieTubeLayersAreSeparateTexturesAndOnlyTheFilamentSwaps() throws Exception {
        for (var lamp : FOOTPRINTS.keySet()) assertNixieLayers(model(lamp + "_off"), model(lamp + "_on"));
    }

    private static void assertNixieLayers(JsonObject off, JsonObject on) {
        assertEquals(off.get("elements"), on.get("elements"), "Lit and unlit keep the geometry");
        Map<String, String> textures = new HashMap<>();
        for (var element : off.getAsJsonArray("elements")) {
            var cube = element.getAsJsonObject();
            var faces = cube.getAsJsonObject("faces");
            String key = faces.entrySet().iterator().next().getValue().getAsJsonObject().get("texture").getAsString().substring(1);
            textures.put(cube.get("name").getAsString(), key);
        }
        assertEquals(Set.of("socket", "glass", "filament"), textures.keySet());
        var offTextures = off.getAsJsonObject("textures");
        var onTextures = on.getAsJsonObject("textures");
        assertEquals(NS + "lamp_socket", offTextures.get(textures.get("socket")).getAsString());
        assertEquals(NS + "lamp_glass_red", offTextures.get(textures.get("glass")).getAsString());
        assertEquals(NS + "lamp_filament_off", offTextures.get(textures.get("filament")).getAsString());
        assertEquals(NS + "lamp_filament_on", onTextures.get(textures.get("filament")).getAsString());
        assertEquals(offTextures.get(textures.get("glass")), onTextures.get(textures.get("glass")));
        // Like the nixie tube, the glass has no bottom: the socket top shows through it.
        for (var element : off.getAsJsonArray("elements")) {
            var cube = element.getAsJsonObject();
            if (!cube.get("name").getAsString().equals("socket"))
                assertFalse(cube.getAsJsonObject("faces").has("down"), cube.get("name") + " has no bottom face");
        }
        var glass = vector(element(off, "glass"), "from");
        var filament = element(off, "filament");
        assertTrue(vector(filament, "from")[0] > glass[0] && vector(filament, "to")[1] < vector(element(off, "glass"), "to")[1],
                "The filament sits inside the glass without sharing its faces");
    }

    @Test
    void amberVariantsOnlySwapTheGlassAndTheLitFilament() throws Exception {
        for (var lamp : FOOTPRINTS.keySet()) {
            var red = model(lamp + "_off");
            for (var lit : new boolean[]{false, true}) {
                var amber = model(lamp + (lit ? "_amber_on" : "_amber_off"));
                assertEquals(red.get("elements"), amber.get("elements"), "Amber keeps the geometry");
                var textures = amber.getAsJsonObject("textures");
                var redTextures = red.getAsJsonObject("textures");
                for (var key : redTextures.keySet()) {
                    String expected = redTextures.get(key).getAsString()
                            .replace("lamp_glass_red", "lamp_glass_amber")
                            .replace("lamp_filament_off", lit ? "lamp_filament_on_amber" : "lamp_filament_off");
                    assertEquals(expected, textures.get(key).getAsString(), lamp + " " + key);
                }
            }
        }
        // Same alpha map: red and amber read as one object in two colours (plan §6).
        var red = texture(NS + "lamp_glass_red");
        var amber = texture(NS + "lamp_glass_amber");
        for (int y = 0; y < red.getHeight(); y++) for (int x = 0; x < red.getWidth(); x++)
            assertEquals(red.getRGB(x, y) >>> 24, amber.getRGB(x, y) >>> 24, "alpha at " + x + "," + y);
    }

    @Test
    void onlyTheGlassIsTranslucent() throws Exception {
        for (var name : new String[]{"lamp_socket", "lamp_filament_off", "lamp_filament_on", "lamp_filament_on_amber",
                "lamp_glass_red", "lamp_glass_amber"}) {
            var image = texture(NS + name);
            boolean glass = name.startsWith("lamp_glass_");
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (glass) assertTrue(alpha > 0 && alpha < 255, name + ": tinted glass");
                else assertEquals(255, alpha, name + ": opaque");
            }
        }
    }

    private static JsonObject element(JsonObject model, String name) {
        for (var element : model.getAsJsonArray("elements"))
            if (element.getAsJsonObject().get("name").getAsString().equals(name)) return element.getAsJsonObject();
        throw new AssertionError(name);
    }
}
