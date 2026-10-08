package com.radaeli.aeronauticsflybyinstruments.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;

class GpwsAssetsTest {
    private static final String ROOT = "/assets/aeronautics_fly_by_instruments/";

    @Test void modelUsesOpaqueAtlasAndFitsItsCell() throws Exception {
        var model = json("models/block/gpws_computer.json");
        assertEquals("minecraft:solid", model.get("render_type").getAsString());
        assertEquals("aeronautics_fly_by_instruments:block/gpws_computer",
                model.getAsJsonObject("textures").get("0").getAsString());
        assertEquals(33, model.getAsJsonArray("elements").size());
        var sockets = new java.util.HashSet<String>();
        for (var value : model.getAsJsonArray("elements")) {
            var cube = value.getAsJsonObject();
            String name = cube.get("name").getAsString();
            if (name.startsWith("socket_")) sockets.add(name);
            for (int axis = 0; axis < 3; axis++) {
                double min = cube.getAsJsonArray("from").get(axis).getAsDouble();
                double max = cube.getAsJsonArray("to").get(axis).getAsDouble();
                assertTrue(min >= 0 && min < max && max <= 16, cube.get("name").getAsString());
            }
            for (var face : cube.getAsJsonObject("faces").entrySet()) {
                assertEquals("#0", face.getValue().getAsJsonObject().get("texture").getAsString());
                assertFalse(face.getValue().getAsJsonObject().has("cullface"));
                for (var uv : face.getValue().getAsJsonObject().getAsJsonArray("uv")) {
                    assertTrue(uv.getAsDouble() >= 0 && uv.getAsDouble() <= 16);
                }
            }
        }
        assertEquals(java.util.Set.of("socket_north", "socket_up", "socket_down"), sockets);
        try (var stream = getClass().getResourceAsStream(ROOT + "textures/block/gpws_computer.png")) {
            assertNotNull(stream);
            var image = ImageIO.read(stream);
            assertEquals(64, image.getWidth()); assertEquals(64, image.getHeight());
            for (int y=0;y<64;y++) for (int x=0;x<64;x++) assertEquals(255, image.getRGB(x,y) >>> 24);
        }
    }

    @Test void itemAndFourBlockStatesReferenceModel() throws Exception {
        var item = json("models/item/gpws_computer.json");
        assertEquals("aeronautics_fly_by_instruments:block/gpws_computer", item.get("parent").getAsString());
        for (String context : new String[]{"gui", "firstperson_righthand", "firstperson_lefthand",
                "thirdperson_righthand", "thirdperson_lefthand", "ground", "fixed"}) {
            assertTrue(item.getAsJsonObject("display").has(context));
        }
        var variants = json("blockstates/gpws_computer.json").getAsJsonObject("variants");
        assertEquals(4, variants.size());
        for (String facing : new String[]{"north", "east", "south", "west"}) {
            assertEquals(item.get("parent").getAsString(),
                    variants.getAsJsonObject("facing=" + facing).get("model").getAsString());
        }
    }

    @Test void rotatedShapesPreserveSocketEdgesAndHousingRecesses() {
        var block = ModBlocks.GPWS_COMPUTER.get();
        var north = block.defaultBlockState();
        var northShape = north.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        assertFalse(northShape.toAabbs().stream().anyMatch(b -> b.contains(.01, .1, .01)));
        double volume = northShape.toAabbs().stream().mapToDouble(b -> b.getXsize()*b.getYsize()*b.getZsize()).sum();
        for (Rotation rotation : Rotation.values()) {
            var state = north.rotate(rotation);
            var shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            assertEquals(volume, shape.toAabbs().stream().mapToDouble(b -> b.getXsize()*b.getYsize()*b.getZsize()).sum(), 1e-8);
            for (Direction.Axis axis : Direction.Axis.values()) {
                if (axis == Direction.Axis.Y) {
                    assertEquals(0, shape.min(axis)); assertEquals(1, shape.max(axis));
                }
            }
            for (Direction face : new Direction[]{state.getValue(GpwsComputerBlock.FACING).getOpposite(),
                    state.getValue(GpwsComputerBlock.FACING).getClockWise(),
                    state.getValue(GpwsComputerBlock.FACING).getCounterClockWise()}) {
                if (face.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                    assertTrue(shape.max(face.getAxis()) < 1);
                } else {
                    assertTrue(shape.min(face.getAxis()) > 0);
                }
            }
            assertEquals(shape, state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
        }
    }

    private static JsonObject json(String path) throws Exception {
        try (var stream = GpwsAssetsTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
