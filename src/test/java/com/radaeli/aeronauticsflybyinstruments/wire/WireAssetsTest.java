package com.radaeli.aeronauticsflybyinstruments.wire;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static com.radaeli.aeronauticsflybyinstruments.sensor.SensorModelChecks.*;
import static org.junit.jupiter.api.Assertions.*;

/** Avionics Wire: brass connector, industrial iron support, black wire strip, spool sprite. */
class WireAssetsTest {
    private static final String NS = "aeronautics_fly_by_instruments";
    private static final String ROOT = "/assets/" + NS + "/";
    private static final Map<String, int[][]> NODES = Map.of(
            "wire_connector", WireConnectorBlock.PARTS, "wire_support", WireSupportBlock.PARTS);

    @Test
    void nodeModelsFollowTheGuideAndMatchTheirShapes() throws Exception {
        for (var entry : NODES.entrySet()) {
            String name = entry.getKey();
            assertGuideRules(name, -WireConnectorBlock.STEM[2]);
            assertEquals(Set.of(NS + ":block/" + name), textureIds(model(name)));
        }
        // Connector: flange, body and gland are the shape; the stem reaches into the host.
        var connector = model("wire_connector").getAsJsonArray("elements");
        int[][] parts = WireConnectorBlock.PARTS;
        assertEquals(parts.length + 1, connector.size());
        for (int i = 0; i < parts.length; i++) assertSquare(connector.get(i).getAsJsonObject(), parts[i], "connector " + i);
        assertSquare(connector.get(parts.length).getAsJsonObject(), WireConnectorBlock.STEM, "stem");
        assertEquals(-2, WireConnectorBlock.STEM[2], "the stem reaches 2 px into the host");
        assertEquals(WireConnectorBlock.PARTS[1][2], WireConnectorBlock.STEM[3], "the stem carries the body without a flange");
        // Slim: the same model without the flange.
        assertGuideRules("wire_connector_slim", 0);
        var slim = model("wire_connector_slim").getAsJsonArray("elements");
        assertEquals(connector.size() - 1, slim.size());
        for (var element : slim) assertNotEquals("flange", element.getAsJsonObject().get("name").getAsString());
        // Nothing enters the table: inside it, faces would lie on its top and bottom planes.
        assertSquare(slim.get(slim.size() - 1).getAsJsonObject(), new int[]{6, 10, 0, 1}, "slim stem");
        // Support: base, four corner posts and a cap, all inside the hull the shape uses.
        var support = model("wire_support").getAsJsonArray("elements");
        int[] hull = WireSupportBlock.PARTS[0];
        assertEquals(6, support.size());
        double[] min = {99, 99, 99}, max = {-99, -99, -99};
        for (var element : support) {
            var from = vector(element.getAsJsonObject().getAsJsonArray("from"));
            var to = vector(element.getAsJsonObject().getAsJsonArray("to"));
            for (int axis = 0; axis < 3; axis++) {
                min[axis] = Math.min(min[axis], from[axis]);
                max[axis] = Math.max(max[axis], to[axis]);
            }
        }
        assertArrayEquals(new double[]{hull[0], hull[2], hull[0]}, min);
        assertArrayEquals(new double[]{hull[1], hull[3], hull[1]}, max);
    }

    private static void assertSquare(JsonObject element, int[] part, String message) {
        assertArrayEquals(new double[]{part[0], part[2], part[0]}, vector(element.getAsJsonArray("from")), message);
        assertArrayEquals(new double[]{part[1], part[3], part[1]}, vector(element.getAsJsonArray("to")), message);
    }

    @Test
    void wireMeetsTheConnectorGlandAndPassesThroughTheSupport() throws Exception {
        int[] gland = WireConnectorBlock.PARTS[2];
        assertEquals(WireAttachPoints.CONNECTOR_MOUTH, gland[3]);
        var support = model("wire_support").getAsJsonArray("elements");
        double baseTop = vector(support.get(0).getAsJsonObject().getAsJsonArray("to"))[1];
        double capBottom = vector(support.get(5).getAsJsonObject().getAsJsonArray("from"))[1];
        assertEquals((baseTop + capBottom) / 2, WireAttachPoints.SUPPORT_WIRE, "the wire runs between base and cap");
        assertTrue(WireAttachPoints.SUPPORT_WIRE - 0.5 >= baseTop && WireAttachPoints.SUPPORT_WIRE + 0.5 <= capBottom);
        // Outer lanes clear the corner posts.
        double postInner = vector(support.get(1).getAsJsonObject().getAsJsonArray("to"))[0];
        double edge = 8 + WireAttachPoints.LANE_SPACING + 0.5;
        assertTrue(8 - (edge - 8) >= postInner && edge <= 16 - postInner, "lanes clear the posts");
    }

    @Test
    void connectorSitsMidThicknessOnATableEdge() {
        var table = com.radaeli.aeronauticsflybyinstruments.block.ModBlocks.MODULAR_TABLE_BASE.get().defaultBlockState();
        var facing = com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock.FACING;
        int[] body = WireConnectorBlock.PARTS[1];
        assertEquals(com.radaeli.aeronauticsflybyinstruments.panel.PanelCoordinates.SURFACE * 16, body[1] - body[0], 1e-9,
                "the body is exactly as wide as the table is thick");
        // Floor table, connector on its north edge: centred on the 4 px edge.
        assertEquals(new Vec3(.5, 2 / 16.0, 0), WireNodeBlock.tableEdgeSocket(table.setValue(facing, Direction.UP), Direction.NORTH));
        // Ceiling table, east edge.
        assertEquals(new Vec3(1, 14 / 16.0, .5), WireNodeBlock.tableEdgeSocket(table.setValue(facing, Direction.DOWN), Direction.EAST));
        // Wall table (exposed face west), connector on its top edge.
        assertEquals(new Vec3(14 / 16.0, 1, .5), WireNodeBlock.tableEdgeSocket(table.setValue(facing, Direction.WEST), Direction.UP));
        // On the table's own face: an ordinary connector.
        assertNull(WireNodeBlock.tableEdgeSocket(table.setValue(facing, Direction.UP), Direction.UP));
        assertNull(WireNodeBlock.tableEdgeSocket(
                com.radaeli.aeronauticsflybyinstruments.block.ModBlocks.NETWORK_BRIDGE.get().defaultBlockState(), Direction.NORTH));
    }

    @Test
    void shapesTurnWithTheFacingAndStartAtTheHostFace() {
        for (Direction facing : Direction.values()) {
            var bounds = WireNodeBlock.box(facing, 5, 11, 0, 4).bounds();
            Vec3 mouth = WireAttachPoints.connector(facing, Vec3.ZERO);
            assertTrue(bounds.inflate(1e-9).contains(mouth), facing + ": mouth on the connector");
            double hostFace = facing.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? bounds.min(facing.getAxis()) : 1 - bounds.max(facing.getAxis());
            assertEquals(0, hostFace, 1e-9, facing + ": sits on the host face");
            assertEquals(4 / 16.0, bounds.max(facing.getAxis()) - bounds.min(facing.getAxis()), 1e-9);
        }
    }

    @Test
    void blockstatesRotateLikeAnEndRodAndItemsUseTheBlockModel() throws Exception {
        Map<String, int[]> expected = Map.of("up", new int[]{0, 0}, "down", new int[]{180, 0},
                "north", new int[]{90, 0}, "south", new int[]{90, 180}, "west", new int[]{90, 270}, "east", new int[]{90, 90});
        Map<String, Map<String, String>> keys = Map.of(
                "wire_support", Map.of("", "wire_support"),
                "wire_connector", Map.of(",slim=false", "wire_connector", ",slim=true", "wire_connector_slim"));
        for (String name : NODES.keySet()) {
            var variants = json("blockstates/" + name + ".json").getAsJsonObject("variants");
            assertEquals(6 * keys.get(name).size(), variants.size());
            for (var entry : expected.entrySet()) {
                for (var key : keys.get(name).entrySet()) {
                    var variant = variants.getAsJsonObject("facing=" + entry.getKey() + key.getKey());
                    String label = name + " " + entry.getKey() + key.getKey();
                    assertEquals(NS + ":block/" + key.getValue(), variant.get("model").getAsString(), label);
                    assertEquals(entry.getValue()[0], variant.has("x") ? variant.get("x").getAsInt() : 0, label);
                    assertEquals(entry.getValue()[1], variant.has("y") ? variant.get("y").getAsInt() : 0, label);
                }
            }
            assertEquals(NS + ":block/" + name, json("models/item/" + name + ".json").get("parent").getAsString());
            assertNotNull(getClass().getResource("/data/" + NS + "/loot_table/blocks/" + name + ".json"), name + " loot");
        }
        assertEquals(NS + ":item/avionics_wire",
                json("models/item/avionics_wire.json").getAsJsonObject("textures").get("layer0").getAsString());
    }

    @Test
    void wireStripIsOpaqueBlackAndTheSpoolUsesThePalette() throws Exception {
        Set<Integer> wire = Set.of(0x141416, 0x1B1B1E, 0x232326, 0x2B2B2B);
        var strip = ImageIO.read(getClass().getResourceAsStream(ROOT + "textures/entity/avionics_wire.png"));
        assertEquals(16, strip.getWidth());
        assertEquals(16, strip.getHeight());
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
            int argb = strip.getRGB(x, y);
            assertEquals(255, argb >>> 24);
            assertTrue(wire.contains(argb & 0xFFFFFF), "wire pixel " + x + "," + y);
        }
        Set<Integer> spoolColours = Set.of(0x141416, 0x1B1B1E, 0x232326, 0x2B2B2B,
                0x592424, 0x724731, 0x9E6947, 0xBF8E55, 0xCEA05A, 0xE4B763, 0xF7CB6C, 0xFFEB8C,
                0x353438, 0x3F3E42, 0x4F4F4F, 0x595858, 0x676161, 0x757171);
        var spool = ImageIO.read(getClass().getResourceAsStream(ROOT + "textures/item/avionics_wire.png"));
        for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) {
            int argb = spool.getRGB(x, y);
            if ((argb >>> 24) == 0) continue;
            assertEquals(255, argb >>> 24, "no partial alpha");
            assertTrue(spoolColours.contains(argb & 0xFFFFFF), "spool pixel " + x + "," + y);
        }
    }

    @Test
    void everyWireStringIsTranslated() throws Exception {
        var english = json("lang/en_us.json");
        var portuguese = json("lang/pt_br.json");
        for (var key : english.keySet()) {
            if (key.contains(".wire") || key.contains("wiring_view") || key.contains("avionics_wire")) {
                assertTrue(portuguese.has(key), key);
            }
        }
    }

    private JsonObject json(String path) throws Exception {
        try (var reader = new InputStreamReader(getClass().getResourceAsStream(ROOT + path), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
