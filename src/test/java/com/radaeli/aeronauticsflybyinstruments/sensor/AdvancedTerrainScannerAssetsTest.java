package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radaeli.aeronauticsflybyinstruments.block.CableSocketProvider;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedTerrainScannerAssetsTest {
    private static final String ROOT = "/assets/aeronautics_fly_by_instruments/";

    private JsonObject json(String path) throws Exception {
        try (var input = getClass().getResourceAsStream(ROOT + path)) {
            assertNotNull(input, path);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private BufferedImage image(String resource) throws Exception {
        try (var input = getClass().getResourceAsStream(ROOT + "textures/" + resource.split(":", 2)[1] + ".png")) {
            assertNotNull(input, resource);
            return ImageIO.read(input);
        }
    }

    private JsonObject cube(JsonObject model, String name) {
        for (var element : model.getAsJsonArray("elements")) {
            if (element.getAsJsonObject().get("name").getAsString().equals(name)) return element.getAsJsonObject();
        }
        throw new AssertionError(name);
    }

    private String texture(JsonObject model, String cube, String side) {
        String key = cube(model, cube).getAsJsonObject("faces").getAsJsonObject(side).get("texture").getAsString();
        return model.getAsJsonObject("textures").get(key.substring(1)).getAsString();
    }

    @Test
    void housingAndSocketGeometryRemainIdenticalToBasicInAllFourOrientations() throws Exception {
        var basic = json("models/block/terrain_scanner.json");
        var advanced = json("models/block/terrain_scanner_advanced.json");
        assertEquals(basic.getAsJsonArray("elements").size(), advanced.getAsJsonArray("elements").size());
        for (var element : advanced.getAsJsonArray("elements")) {
            var part = element.getAsJsonObject();
            var original = cube(basic, part.get("name").getAsString());
            assertEquals(original.get("from"), part.get("from"));
            assertEquals(original.get("to"), part.get("to"));
            assertFalse(part.has("rotation"));
        }
        var variants = json("blockstates/terrain_scanner_advanced.json").getAsJsonObject("variants");
        for (var facing : Direction.Plane.HORIZONTAL) {
            var a = ModBlocks.TERRAIN_SCANNER_ADVANCED.get().defaultBlockState().setValue(AbstractTerrainScannerBlock.FACING, facing);
            var b = ModBlocks.TERRAIN_SCANNER.get().defaultBlockState().setValue(AbstractTerrainScannerBlock.FACING, facing);
            assertFalse(a.canOcclude());
            assertFalse(Shapes.joinIsNotEmpty(b.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                    a.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO), BooleanOp.NOT_SAME));
            assertFalse(Shapes.joinIsNotEmpty(b.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()),
                    a.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty()), BooleanOp.NOT_SAME));
            for (var face : Direction.values()) assertEquals(((CableSocketProvider) b.getBlock()).cableSocket(b, face),
                    ((CableSocketProvider) a.getBlock()).cableSocket(a, face));
            var variant = variants.getAsJsonObject("facing=" + facing.getSerializedName());
            assertEquals("aeronautics_fly_by_instruments:block/terrain_scanner_advanced", variant.get("model").getAsString());
            int rotation = switch (facing) { case EAST -> 90; case SOUTH -> 180; case WEST -> 270; default -> 0; };
            assertEquals(rotation, variant.has("y") ? variant.get("y").getAsInt() : 0);
        }
        assertEquals("aeronautics_fly_by_instruments:block/terrain_scanner_advanced",
                json("models/item/terrain_scanner_advanced.json").get("parent").getAsString());
    }

    @Test
    void antennaHasTwentyFiveSeparateGoldPadsAndOtherFacesHaveDoubleBlueIdentification() throws Exception {
        var model = json("models/block/terrain_scanner_advanced.json");
        assertEquals(.25, cube(model, "forward_antenna").getAsJsonArray("from").get(2).getAsDouble());
        var pixels = image(texture(model, "forward_antenna", "north"));
        boolean[][] visited = new boolean[32][32];
        int pads = 0;
        for (int y = 0; y < 32; y++) for (int x = 0; x < 32; x++) {
            if (visited[x][y] || !gold(pixels.getRGB(x, y))) continue;
            pads++;
            var queue = new ArrayDeque<int[]>(); queue.add(new int[]{x, y}); visited[x][y] = true;
            int area = 0;
            while (!queue.isEmpty()) {
                var p = queue.removeFirst(); area++;
                for (var offset : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = p[0] + offset[0], ny = p[1] + offset[1];
                    if (nx >= 0 && nx < 32 && ny >= 0 && ny < 32 && !visited[nx][ny] && gold(pixels.getRGB(nx, ny))) {
                        visited[nx][ny] = true; queue.add(new int[]{nx, ny});
                    }
                }
            }
            assertEquals(16, area, "Four by four texels per pad, separated by two texels");
        }
        assertEquals(25, pads);
        for (var face : new String[][]{{"metal_case", "west"}, {"metal_case", "east"},
                {"metal_case", "south"}, {"metal_case", "down"}, {"mounting_lid", "up"}}) {
            var texture = image(texture(model, face[0], face[1]));
            int blue = 0;
            for (int x = 0; x < 32; x++) for (int y = 0; y < 32; y++) if ((texture.getRGB(x, y) & 0xffffff) == 0x31657e) blue++;
            assertTrue(blue >= 40, face[0] + " " + face[1]);
        }
        // Direction arrows remain yellow, not a substitute blue state indicator.
        assertEquals(0xe4a92f, image(texture(model, "metal_case", "west")).getRGB(16, 20) & 0xffffff);
    }

    private boolean gold(int argb) {
        return ((argb >>> 16) & 255) > 120 && ((argb >>> 8) & 255) > 80 && (argb & 255) < 200;
    }

    @Test
    void assetsStayOpaqueThirtyTwoPixelsWithValidUvsAndNoEmissiveOrCulledFaces() throws Exception {
        var model = json("models/block/terrain_scanner_advanced.json");
        assertFalse(model.has("render_type"));
        for (var entry : model.getAsJsonObject("textures").entrySet()) {
            assertTrue(entry.getValue().getAsString().startsWith("aeronautics_fly_by_instruments:block/"));
            var pixels = image(entry.getValue().getAsString());
            assertEquals(32, pixels.getWidth()); assertEquals(32, pixels.getHeight());
            for (int x = 0; x < 32; x++) for (int y = 0; y < 32; y++) assertEquals(255, pixels.getRGB(x, y) >>> 24);
        }
        for (var element : model.getAsJsonArray("elements")) {
            assertFalse(element.getAsJsonObject().has("light_emission"));
            for (var entry : element.getAsJsonObject().getAsJsonObject("faces").entrySet()) {
                var face = entry.getValue().getAsJsonObject();
                assertFalse(face.has("cullface"));
                assertTrue(model.getAsJsonObject("textures").has(face.get("texture").getAsString().substring(1)));
                for (var uv : face.getAsJsonArray("uv")) assertTrue(uv.getAsDouble() >= 0 && uv.getAsDouble() <= 16);
            }
        }
    }
}
