package com.radaeli.aeronauticsflybyinstruments.sensor;

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
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class RadarAltimeterAssetsTest {
    private static final String ROOT = "/assets/aeronautics_fly_by_instruments/";

    private JsonObject model(String relativePath) throws Exception {
        try (var input = getClass().getResourceAsStream(ROOT + relativePath)) {
            assertNotNull(input, relativePath);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void exportedCubesMatchOutlineAndCollisionWithoutMovingToAnotherBlock() throws Exception {
        var model = model("models/block/radar_altimeter.json");
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
            volume = Shapes.or(volume, Block.box(from.get(0).getAsDouble(), from.get(1).getAsDouble(),
                    from.get(2).getAsDouble(), to.get(0).getAsDouble(), to.get(1).getAsDouble(),
                    to.get(2).getAsDouble()));
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
    void distinctiveAntennaIsOnLocalDownAndCentredOnTheCastOrigin() throws Exception {
        var model = model("models/block/radar_altimeter.json");
        JsonObject antenna = null;
        for (var entry : model.getAsJsonArray("elements")) {
            var cube = entry.getAsJsonObject();
            if (cube.get("name").getAsString().equals("downward_antenna")) antenna = cube;
        }
        assertNotNull(antenna);
        var from = antenna.getAsJsonArray("from");
        var to = antenna.getAsJsonArray("to");
        assertEquals(0, from.get(1).getAsDouble());
        assertEquals(8, (from.get(0).getAsDouble() + to.get(0).getAsDouble()) / 2);
        assertEquals(8, (from.get(2).getAsDouble() + to.get(2).getAsDouble()) / 2);
        var faces = antenna.getAsJsonObject("faces");
        var downTexture = faces.getAsJsonObject("down").get("texture").getAsString();
        assertEquals("aeronautics_fly_by_instruments:block/radar_altimeter_antenna",
                model.getAsJsonObject("textures").get(downTexture.substring(1)).getAsString());
        for (String side : new String[]{"up", "north", "south", "east", "west"}) {
            assertNotEquals(downTexture, faces.getAsJsonObject(side).get("texture").getAsString());
        }
    }

    @Test
    void allTexturesArePresentOpaqueAndUseLegalUvs() throws Exception {
        var model = model("models/block/radar_altimeter.json");
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
                var json = face.getValue().getAsJsonObject();
                assertFalse(json.has("cullface"), "Inset faces must not disappear against neighbouring blocks");
                assertTrue(model.getAsJsonObject("textures").has(json.get("texture").getAsString().substring(1)));
                for (var uv : json.getAsJsonArray("uv")) assertTrue(uv.getAsDouble() >= 0 && uv.getAsDouble() <= 16);
            }
        }
        assertEquals("aeronautics_fly_by_instruments:block/radar_altimeter",
                model("models/item/radar_altimeter.json").get("parent").getAsString());
        assertEquals("aeronautics_fly_by_instruments:block/radar_altimeter",
                model("blockstates/radar_altimeter.json").getAsJsonObject("variants").getAsJsonObject("")
                        .get("model").getAsString());
    }
}
