package com.radaeli.aeronauticsflybyinstruments.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.radaeli.aeronauticsflybyinstruments.block.TableConnections.Tile.*;
import static org.junit.jupiter.api.Assertions.*;

/** Table base v2.1: brass rim + dark-wood top (guide §9.3) with compact connected textures. */
class ModularTableAssetsTest {
    private static final String ROOT = "/assets/aeronautics_fly_by_instruments/";
    /** Brass and dark-wood ramps (guide §5): the base uses nothing else. */
    private static final Set<Integer> PALETTE = Set.of(0x592424, 0x724731, 0x9E6947, 0xBF8E55, 0xCEA05A,
            0xE4B763, 0xF7CB6C, 0xFFEB8C, 0x301E0E, 0x3A2411, 0x442B15, 0x4F3118, 0x53381A, 0x5A3E1E);

    private static BufferedImage texture(String name) throws Exception {
        try (var input = ModularTableAssetsTest.class.getResourceAsStream(ROOT + "textures/block/" + name + ".png")) {
            assertNotNull(input, name);
            return ImageIO.read(input);
        }
    }

    private static JsonObject model() throws Exception {
        try (var input = ModularTableAssetsTest.class.getResourceAsStream(ROOT + "models/block/modular_table_base.json")) {
            assertNotNull(input);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    void slabMatchesTheCollisionAndKeepsOneTexelPerUnit() throws Exception {
        var model = model();
        var elements = model.getAsJsonArray("elements");
        assertEquals(1, elements.size());
        var cube = elements.get(0).getAsJsonObject();
        assertEquals("[0,0,0]", cube.get("from").toString());
        assertEquals("[16,3,16]", cube.get("to").toString(), "Same 3/16 slab as ModularTableBaseBlock.UP");
        var textures = model.getAsJsonObject("textures");
        Map<String, String> faceTexture = new HashMap<>();
        for (var face : cube.getAsJsonObject("faces").entrySet()) {
            var json = face.getValue().getAsJsonObject();
            var id = textures.get(json.get("texture").getAsString().substring(1)).getAsString();
            faceTexture.put(face.getKey(), id.substring(id.indexOf('/') + 1));
            var uv = json.getAsJsonArray("uv");
            double width = uv.get(2).getAsDouble() - uv.get(0).getAsDouble();
            double height = uv.get(3).getAsDouble() - uv.get(1).getAsDouble();
            boolean flat = face.getKey().equals("up") || face.getKey().equals("down");
            assertEquals(16, width, face.getKey());
            assertEquals(flat ? 16 : 3, height, face.getKey() + ": 1 texel per unit");
            assertEquals(face.getKey().equals("down"), json.has("cullface"), "Only the bottom culls, against the support");
        }
        // The client model finds the top and the sides by these sprites.
        assertEquals("modular_table_top", faceTexture.get("up"));
        assertEquals("modular_table_top_wood", faceTexture.get("down"));
        for (var side : new String[]{"north", "south", "east", "west"}) {
            assertEquals("modular_table_side", faceTexture.get(side));
        }
        assertEquals("aeronautics_fly_by_instruments:block/modular_table_top", textures.get("particle").getAsString());
    }

    @Test
    void everyTextureIsOpaqueSixteenPixelsAndInTheBrassAndWoodRamps() throws Exception {
        for (var tile : TableConnections.Tile.values()) check("modular_table_top" + tile.suffix);
        check("modular_table_side");
    }

    private static void check(String name) throws Exception {
        var image = texture(name);
        assertEquals(16, image.getWidth(), name);
        assertEquals(16, image.getHeight(), name);
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int argb = image.getRGB(x, y);
            assertEquals(0xFF, argb >>> 24, name + " opaque");
            assertTrue(PALETTE.contains(argb & 0xFFFFFF), name + " palette at " + x + "," + y);
        }
    }

    @Test
    void tilesShareTheWoodAndRunTheRimAcrossJoins() throws Exception {
        var frame = texture("modular_table_top");
        var horizontal = texture("modular_table_top_horizontal");
        var vertical = texture("modular_table_top_vertical");
        var inner = texture("modular_table_top_inner");
        var wood = texture("modular_table_top_wood");
        // Wood is a function of the pixel alone, so quarters from different tiles meet seamlessly.
        for (int y = 2; y < 14; y++) for (int x = 2; x < 14; x++) {
            for (var tile : new BufferedImage[]{frame, horizontal, vertical, inner}) {
                assertEquals(wood.getRGB(x, y), tile.getRGB(x, y), "wood at " + x + "," + y);
            }
        }
        // Rims of joined tiles have no end caps: each rim row/column is one colour edge to edge.
        for (int line : new int[]{0, 1, 14, 15}) for (int i = 0; i < 16; i++) {
            assertEquals(horizontal.getRGB(0, line), horizontal.getRGB(i, line), "horizontal rim row " + line);
            assertEquals(vertical.getRGB(line, 0), vertical.getRGB(line, i), "vertical rim column " + line);
            assertEquals(horizontal.getRGB(i, line), frame.getRGB(Math.clamp(i, 2, 13), line), "frame rim row");
        }
        // The rim of the frame continues into the horizontal tile at the same rows.
        for (int x = 0; x < 16; x++) assertEquals(frame.getRGB(8, 0), horizontal.getRGB(x, 0));
    }

    @Test
    void quartersPickTheirTileFromTwoEdgesAndTheDiagonal() {
        assertEquals(FRAME, TableConnections.tile(false, false, false));
        assertEquals(FRAME, TableConnections.tile(false, false, true));
        assertEquals(HORIZONTAL, TableConnections.tile(true, false, true));
        assertEquals(VERTICAL, TableConnections.tile(false, true, false));
        assertEquals(INNER, TableConnections.tile(true, true, false));
        assertEquals(WOOD, TableConnections.tile(true, true, true));
    }

    @Test
    void tablesJoinOnlyWithTablesOnTheSameFaceInTheirPlane() {
        var up = ModBlocks.MODULAR_TABLE_BASE.get().defaultBlockState();
        var wall = up.setValue(ModularTableBaseBlock.FACING, Direction.NORTH);
        Map<BlockPos, BlockState> world = new HashMap<>();
        world.put(BlockPos.ZERO, up);
        world.put(new BlockPos(1, 0, 0), up);                                     // east, joins
        world.put(new BlockPos(1, 0, 1), up.setValue(ModularTableBaseBlock.LIGHT, 7)); // diagonal, lit, joins
        world.put(new BlockPos(0, 0, 1), wall);                                   // south, other face
        world.put(new BlockPos(0, 1, 0), up);                                     // above: out of plane
        world.put(new BlockPos(-1, 0, 0), Blocks.OAK_PLANKS.defaultBlockState());
        int mask = TableConnections.mask(getter(world), BlockPos.ZERO, up);
        assertEquals(TableConnections.bit(1, 0, 0) | TableConnections.bit(1, 0, 1), mask);
        assertTrue(TableConnections.joined(mask, Direction.EAST));
        assertFalse(TableConnections.joined(mask, Direction.SOUTH));
        assertFalse(TableConnections.joined(mask, Direction.UP));
    }

    private static BlockGetter getter(Map<BlockPos, BlockState> world) {
        return new BlockGetter() {
            @Override public BlockEntity getBlockEntity(BlockPos pos) { return null; }
            @Override public BlockState getBlockState(BlockPos pos) {
                return world.getOrDefault(pos, Blocks.AIR.defaultBlockState());
            }
            @Override public FluidState getFluidState(BlockPos pos) { return getBlockState(pos).getFluidState(); }
            @Override public int getHeight() { return 384; }
            @Override public int getMinBuildHeight() { return -64; }
        };
    }
}
