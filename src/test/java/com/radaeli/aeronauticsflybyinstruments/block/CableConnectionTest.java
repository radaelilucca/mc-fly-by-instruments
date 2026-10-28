package com.radaeli.aeronauticsflybyinstruments.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radaeli.aeronauticsflybyinstruments.sensor.AbstractTerrainScannerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CableConnectionTest {
    private static final String ROOT = "/assets/aeronautics_fly_by_instruments/";
    private static final double EPS = 1e-8;

    private JsonObject model(String name) throws Exception {
        try (var input = getClass().getResourceAsStream(ROOT + "models/block/" + name + ".json")) {
            assertNotNull(input, name);
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private void equalVector(Vec3 expected, Vec3 actual) {
        assertEquals(expected.x, actual.x, EPS);
        assertEquals(expected.y, actual.y, EPS);
        assertEquals(expected.z, actual.z, EPS);
    }

    @Test
    void tipEndsAtActualSocketAndFlexibleJacketMeetsConnectorInEveryOrientation() throws Exception {
        var states = new java.util.ArrayList<BlockState>();
        states.add(ModBlocks.RADAR_ALTIMETER.get().defaultBlockState());
        for (var facing : Direction.Plane.HORIZONTAL) {
            states.add(ModBlocks.TERRAIN_SCANNER.get().defaultBlockState()
                    .setValue(AbstractTerrainScannerBlock.FACING, facing));
            states.add(ModBlocks.TERRAIN_SCANNER_ADVANCED.get().defaultBlockState()
                    .setValue(AbstractTerrainScannerBlock.FACING, facing));
        }
        for (var state : states) for (var face : Direction.values()) {
            var sockets = (CableSocketProvider) state.getBlock();
            var connection = new CableConnection(face.getOpposite(), sockets.cableSocket(state, face));
            equalVector(connection.target(), connection.transform(new Vec3(.5, .5, 1), true));
            equalVector(new Vec3(.5, .5, .5), connection.transform(new Vec3(.5, .5, .5), false));
            equalVector(connection.transform(new Vec3(.5, .5, CableConnection.FLEX_END), false),
                    connection.transform(new Vec3(.5, .5, CableConnection.FLEX_END), true));
            var axis = new Vec3(connection.direction().getStepX(), connection.direction().getStepY(),
                    connection.direction().getStepZ());
            double surface = connection.target().dot(axis);
            var outline = connection.outline();
            for (String part : List.of("avionics_cable_flex", "avionics_cable_terminal")) {
                for (var entry : model(part).getAsJsonArray("elements")) {
                    var cube = entry.getAsJsonObject();
                    var from = cube.getAsJsonArray("from");
                    var to = cube.getAsJsonArray("to");
                    for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) for (int z = 0; z < 2; z++) {
                        var p = connection.transform(new Vec3((x == 0 ? from : to).get(0).getAsDouble() / 16,
                                (y == 0 ? from : to).get(1).getAsDouble() / 16,
                                (z == 0 ? from : to).get(2).getAsDouble() / 16), part.endsWith("terminal"));
                        assertTrue(p.dot(axis) <= surface + EPS, state + " " + face + " penetrates surface");
                        assertTrue(outline.toAabbs().stream().anyMatch(box ->
                                p.x >= box.minX - EPS && p.x <= box.maxX + EPS &&
                                p.y >= box.minY - EPS && p.y <= box.maxY + EPS &&
                                p.z >= box.minZ - EPS && p.z <= box.maxZ + EPS), "Outline must enclose model");
                    }
                }
            }
        }
    }

    @Test
    void radarUsesCentredSocketsAndScannerRearSocketRotatesWithBlock() {
        var radar = ModBlocks.RADAR_ALTIMETER.get();
        equalVector(new Vec3(.5, 9.0 / 16, 0), radar.cableSocket(radar.defaultBlockState(), Direction.NORTH));
        equalVector(new Vec3(.5, 0, .5), radar.cableSocket(radar.defaultBlockState(), Direction.DOWN));
        var scanner = ModBlocks.TERRAIN_SCANNER.get();
        for (var facing : Direction.Plane.HORIZONTAL) {
            var rear = facing.getOpposite();
            equalVector(new Vec3(.5 + .5 * rear.getStepX(), .5, .5 + .5 * rear.getStepZ()),
                    scanner.cableSocket(scanner.defaultBlockState().setValue(AbstractTerrainScannerBlock.FACING,
                            facing), rear));
        }
        assertFalse(CableSocketProvider.class.isInstance(ModBlocks.MODULAR_TABLE_BASE.get()));
        assertTrue(CableSocketProvider.class.isInstance(ModBlocks.TERRAIN_SCANNER_ADVANCED.get()));
        assertFalse(CableSocketProvider.class.isInstance(ModBlocks.NETWORK_BRIDGE.get()));
        assertFalse(CableSocketProvider.class.isInstance(ModBlocks.AVIONICS_COMPUTER.get()));
    }

    @Test
    void socketCoordinatesAreOnTheExportedSensorSurface() throws Exception {
        for (var state : List.of(ModBlocks.RADAR_ALTIMETER.get().defaultBlockState(),
                ModBlocks.TERRAIN_SCANNER.get().defaultBlockState(),
                ModBlocks.TERRAIN_SCANNER_ADVANCED.get().defaultBlockState())) {
            var model = model(state.getBlock() == ModBlocks.RADAR_ALTIMETER.get() ? "radar_altimeter"
                    : state.getBlock() == ModBlocks.TERRAIN_SCANNER_ADVANCED.get()
                    ? "terrain_scanner_advanced" : "terrain_scanner");
            for (var face : Direction.values()) {
                var point = ((CableSocketProvider) state.getBlock()).cableSocket(state, face).scale(16);
                int axis = face.getAxis() == Direction.Axis.X ? 0 : face.getAxis() == Direction.Axis.Y ? 1 : 2;
                double[] p = {point.x, point.y, point.z};
                boolean onSurface = false;
                for (var entry : model.getAsJsonArray("elements")) {
                    var cube = entry.getAsJsonObject();
                    var from = cube.getAsJsonArray("from");
                    var to = cube.getAsJsonArray("to");
                    double plane = (face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? to : from)
                            .get(axis).getAsDouble();
                    boolean inside = Math.abs(p[axis] - plane) < EPS;
                    for (int i = 0; i < 3; i++) if (i != axis) inside &=
                            p[i] >= from.get(i).getAsDouble() && p[i] <= to.get(i).getAsDouble();
                    onSurface |= inside;
                }
                assertTrue(onSurface, state + " " + face);
            }
        }
    }

    @Test
    void shapeTracksNeighborOrientationWithoutLookingUpBlockEntities() {
        var scanner = ModBlocks.TERRAIN_SCANNER.get();
        var neighbor = scanner.defaultBlockState();
        var cable = ModBlocks.AVIONICS_CABLE.get().defaultBlockState()
                .setValue(AvionicsCableBlock.CONNECTIONS.get(Direction.SOUTH), true);
        BlockGetter world = new BlockGetter() {
            @Override public BlockEntity getBlockEntity(BlockPos pos) { fail("No entity lookup for visual routing"); return null; }
            @Override public BlockState getBlockState(BlockPos pos) { return neighbor; }
            @Override public FluidState getFluidState(BlockPos pos) { return Fluids.EMPTY.defaultFluidState(); }
            @Override public int getHeight() { return 384; }
            @Override public int getMinBuildHeight() { return -64; }
        };
        // The cable stops at the neighbour's own socket. The v2 scanner's front lens ends on the block
        // boundary, so the arm reaches exactly z = 1 instead of entering a recessed face.
        double socketDepth = ((CableSocketProvider) scanner).cableSocket(neighbor, Direction.NORTH).z;
        assertEquals(0, socketDepth, EPS);
        assertEquals(1 + socketDepth, cable.getShape(world, BlockPos.ZERO).bounds().maxZ, EPS);
        assertSame(SoundType.NETHERITE_BLOCK, cable.getSoundType());
    }

    @Test
    void cableAssetsHaveOwnOpaqueTexturesAndNoCoplanarMatingCaps() throws Exception {
        for (String name : List.of("core", "flex", "terminal", "north", "south", "east", "west", "up", "down", "item")) {
            var model = model("avionics_cable_" + name);
            assertFalse(model.has("render_type"));
            for (var texture : model.getAsJsonObject("textures").entrySet()) {
                String path = texture.getValue().getAsString();
                assertTrue(path.startsWith("aeronautics_fly_by_instruments:block/avionics_cable_"));
                try (var input = getClass().getResourceAsStream(ROOT + "textures/" + path.split(":", 2)[1] + ".png")) {
                    assertNotNull(input, path);
                    var image = ImageIO.read(input);
                    assertEquals(16, image.getWidth());
                    assertEquals(16, image.getHeight());
                    for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) assertEquals(255, image.getRGB(x, y) >>> 24);
                }
            }
            for (var entry : model.getAsJsonArray("elements")) {
                var cube = entry.getAsJsonObject();
                var faces = cube.getAsJsonObject("faces");
                for (var face : faces.entrySet()) {
                    var f = face.getValue().getAsJsonObject();
                    assertFalse(f.has("cullface"));
                    assertTrue(model.getAsJsonObject("textures").has(f.get("texture").getAsString().substring(1)));
                    for (var uv : f.getAsJsonArray("uv")) assertTrue(uv.getAsDouble() >= 0 && uv.getAsDouble() <= 16);
                }
                String cubeName = cube.get("name").getAsString();
                for (int i = 0; i < 3; i++) {
                    assertEquals(Math.rint(cube.getAsJsonArray("from").get(i).getAsDouble()),
                            cube.getAsJsonArray("from").get(i).getAsDouble(), EPS, cubeName + " integer from");
                    assertEquals(Math.rint(cube.getAsJsonArray("to").get(i).getAsDouble()),
                            cube.getAsJsonArray("to").get(i).getAsDouble(), EPS, cubeName + " integer to");
                }
                // Only the item shows the plug face; a placed tip mates the socket.
                if (cubeName.equals("socket_tip")) assertFalse(faces.has("north") || (!name.equals("item") && faces.has("south")));
                if (cubeName.equals("flexible_jacket")) assertFalse(faces.has("south"));
                if (cubeName.equals("strain_relief")) assertFalse(faces.has("south"));
                if (cubeName.equals("metal_ferrule")) assertFalse(faces.has("north"));
            }
        }
    }
}
