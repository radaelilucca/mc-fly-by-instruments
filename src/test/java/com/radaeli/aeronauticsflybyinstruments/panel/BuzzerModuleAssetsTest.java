package com.radaeli.aeronauticsflybyinstruments.panel;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class BuzzerModuleAssetsTest {
    private static final String ROOT = "/assets/aeronautics_fly_by_instruments/";

    @Test
    void geometryFitsOneSlotAndHasNoUnsupportedRotationsOrNeighbourCulling() throws Exception {
        try (var input = getClass().getResourceAsStream(ROOT + "models/block/buzzer_module_1x1.json")) {
            assertNotNull(input);
            var model = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("minecraft:cutout", model.get("render_type").getAsString());
            var elements = model.getAsJsonArray("elements");
            double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
            double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
            for (var element : elements) {
                var cube = element.getAsJsonObject();
                var from = cube.getAsJsonArray("from");
                var to = cube.getAsJsonArray("to");
                double angle = 0, originX = 0, originZ = 0;
                if (cube.has("rotation")) {
                    var rotation = cube.getAsJsonObject("rotation");
                    assertEquals("y", rotation.get("axis").getAsString());
                    angle = Math.toRadians(rotation.get("angle").getAsDouble());
                    assertTrue(Math.abs(angle) <= Math.PI / 4);
                    assertEquals(0, Math.IEEEremainder(rotation.get("angle").getAsDouble(), 22.5), 1e-6);
                    originX = rotation.getAsJsonArray("origin").get(0).getAsDouble();
                    originZ = rotation.getAsJsonArray("origin").get(2).getAsDouble();
                    assertFalse(rotation.get("rescale").getAsBoolean());
                }
                for (int xCorner = 0; xCorner < 2; xCorner++) {
                    for (int zCorner = 0; zCorner < 2; zCorner++) {
                        double x = (xCorner == 0 ? from : to).get(0).getAsDouble() - originX;
                        double z = (zCorner == 0 ? from : to).get(2).getAsDouble() - originZ;
                        double rx = originX + x * Math.cos(angle) + z * Math.sin(angle);
                        double rz = originZ - x * Math.sin(angle) + z * Math.cos(angle);
                        minX = Math.min(minX, rx); maxX = Math.max(maxX, rx);
                        minZ = Math.min(minZ, rz); maxZ = Math.max(maxZ, rz);
                    }
                }
                minY = Math.min(minY, from.get(1).getAsDouble());
                maxY = Math.max(maxY, to.get(1).getAsDouble());
                for (var face : cube.getAsJsonObject("faces").entrySet()) {
                    assertFalse(face.getValue().getAsJsonObject().has("cullface"));
                }
            }
            assertEquals(8, (minX + maxX) / 2, 1e-6);
            assertEquals(8, (minZ + maxZ) / 2, 1e-6);
            assertTrue(maxX - minX < 3.4, "Buzzer must be slightly smaller than the 1x1 lamp");
            assertTrue(maxZ - minZ < 3.4);
            assertEquals(0, minY, 1e-6, "Mounting face must start at Y=0");
            assertEquals(1.9, maxY, 1e-6);
        }
    }

    @Test
    void topHasTransparentSoundHoleButMountingBaseIsOpaque() throws Exception {
        try (var topInput = getClass().getResourceAsStream(ROOT + "textures/block/buzzer_top.png");
             var baseInput = getClass().getResourceAsStream(ROOT + "textures/block/buzzer_base.png")) {
            assertNotNull(topInput); assertNotNull(baseInput);
            var top = ImageIO.read(topInput);
            var base = ImageIO.read(baseInput);
            assertEquals(64, top.getWidth());
            assertEquals(64, top.getHeight());
            assertEquals(0, top.getRGB(32, 32) >>> 24);
            assertEquals(255, base.getRGB(32, 32) >>> 24);
            assertEquals(255, top.getRGB(20, 32) >>> 24);
            assertNotEquals(top.getRGB(20, 32), top.getRGB(21, 32), "Porous surface must retain texture variation");
        }
    }
}
