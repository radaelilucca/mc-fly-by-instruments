package com.radaeli.aeronauticsflybyinstruments.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import static com.radaeli.aeronauticsflybyinstruments.sensor.SensorModelChecks.*;
import static org.junit.jupiter.api.Assertions.*;

/** GPWS Computer v2.1: industrial iron cabinet with brass trim, no wood (docs/visual-design-guide.md). */
class GpwsAssetsTest {
    private static final String ROOT = "/assets/aeronautics_fly_by_instruments/";
    private static final String NAME = "gpws_computer";
    private static final String NS = "aeronautics_fly_by_instruments";

    @Test void followsTheGuideRulesWithIronAndBrassOnly() throws Exception {
        assertGuideRules(NAME);
        assertGuideRules(NAME + "_armed");
        var model = model(NAME);
        var names = new java.util.HashSet<String>();
        for (var value : model.getAsJsonArray("elements")) names.add(value.getAsJsonObject().get("name").getAsString());
        assertEquals(Set.of("base", "body", "lid", "screen", "test_button"), names);
        assertEquals(Set.of(NS + ":block/gpws_computer", NS + ":block/gpws_computer_top",
                NS + ":block/gpws_computer_screen", "create:block/industrial_iron_block", "create:block/brass_casing"),
                textureIds(model));
        assertEquals("create:block/industrial_iron_block", model.getAsJsonObject("textures").get("particle").getAsString());
    }

    @Test void panelIsTheFrontmostPartAndCentred() throws Exception {
        var model = model(NAME);
        for (String part : new String[]{"screen", "test_button"}) {
            var from = vector(element(model, part).getAsJsonArray("from"));
            var to = vector(element(model, part).getAsJsonArray("to"));
            assertEquals(0, from[2], part + " sits on the NORTH face");
            assertEquals(8, (from[0] + to[0]) / 2, part + " is centred");
        }
        var body = vector(element(model, "body").getAsJsonArray("from"));
        assertEquals(1, body[2], "The body is inset 1 behind the panel");
    }

    @Test void armedModelOnlySwapsTheScreenForTheBlinkingCursor() throws Exception {
        var off = model(NAME);
        var armed = model(NAME + "_armed");
        assertEquals(off.get("elements"), armed.get("elements"), "Arming must not change the geometry");
        var offTextures = off.getAsJsonObject("textures");
        var armedTextures = armed.getAsJsonObject("textures");
        assertEquals(offTextures.keySet(), armedTextures.keySet());
        var changed = new java.util.HashSet<String>();
        for (var key : offTextures.keySet()) {
            if (!offTextures.get(key).equals(armedTextures.get(key))) changed.add(key);
        }
        assertEquals(1, changed.size(), "Only the screen texture changes");
        var key = changed.iterator().next();
        assertEquals(NS + ":block/gpws_computer_screen", offTextures.get(key).getAsString());
        assertEquals(NS + ":block/gpws_computer_screen_armed", armedTextures.get(key).getAsString());
        // The screen front face is the one that carries the swapped texture.
        var front = element(off, "screen").getAsJsonObject("faces").getAsJsonObject("north");
        assertEquals("#" + key, front.get("texture").getAsString());
    }

    @Test void itemAndEightBlockStatesReferenceModels() throws Exception {
        var item = json("models/item/gpws_computer.json");
        assertEquals(NS + ":block/gpws_computer", item.get("parent").getAsString());
        for (String context : new String[]{"gui", "firstperson_righthand", "firstperson_lefthand",
                "thirdperson_righthand", "thirdperson_lefthand", "ground", "fixed"}) {
            assertTrue(item.getAsJsonObject("display").has(context));
        }
        var variants = json("blockstates/gpws_computer.json").getAsJsonObject("variants");
        assertEquals(8, variants.size());
        int rotation = 0;
        for (String facing : new String[]{"north", "east", "south", "west"}) {
            for (boolean armed : new boolean[]{false, true}) {
                var variant = variants.getAsJsonObject("facing=" + facing + ",armed=" + armed);
                assertNotNull(variant, facing + " armed=" + armed);
                assertEquals(item.get("parent").getAsString() + (armed ? "_armed" : ""), variant.get("model").getAsString());
                assertEquals(rotation, variant.has("y") ? variant.get("y").getAsInt() : 0);
            }
            rotation += 90;
        }
        assertFalse(ModBlocks.GPWS_COMPUTER.get().defaultBlockState().getValue(GpwsComputerBlock.ARMED),
                "A placed computer starts in STANDBY: no cursor");
    }

    @Test void outlineAndCollisionFollowTheModelInEveryFacing() throws Exception {
        var model = model(NAME);
        for (var facing : Direction.Plane.HORIZONTAL) {
            var volume = Shapes.empty();
            for (var entry : model.getAsJsonArray("elements")) {
                var from = vector(entry.getAsJsonObject().getAsJsonArray("from"));
                var to = vector(entry.getAsJsonObject().getAsJsonArray("to"));
                var a = place(from[0] / 16, from[2] / 16, facing);
                var b = place(to[0] / 16, to[2] / 16, facing);
                volume = Shapes.or(volume, Shapes.box(Math.min(a[0], b[0]), from[1] / 16, Math.min(a[1], b[1]),
                        Math.max(a[0], b[0]), to[1] / 16, Math.max(a[1], b[1])));
            }
            for (boolean armed : new boolean[]{false, true}) {
                var state = ModBlocks.GPWS_COMPUTER.get().defaultBlockState().setValue(GpwsComputerBlock.FACING, facing)
                        .setValue(GpwsComputerBlock.ARMED, armed);
                assertFalse(Shapes.joinIsNotEmpty(volume, state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                        BooleanOp.NOT_SAME), facing.toString());
                assertFalse(Shapes.joinIsNotEmpty(volume, state.getCollisionShape(EmptyBlockGetter.INSTANCE,
                        BlockPos.ZERO, CollisionContext.empty()), BooleanOp.NOT_SAME), facing.toString());
            }
        }
    }

    /** Horizontal point (x, z) of the NORTH-facing source as placed with the given facing. */
    private static double[] place(double x, double z, Direction facing) {
        return switch (facing) {
            case NORTH -> new double[]{x, z};
            case EAST -> new double[]{1 - z, x};
            case SOUTH -> new double[]{1 - x, 1 - z};
            case WEST -> new double[]{z, 1 - x};
            default -> throw new IllegalArgumentException();
        };
    }

    private static JsonObject json(String path) throws Exception {
        try (var stream = GpwsAssetsTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
