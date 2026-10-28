package com.radaeli.aeronauticsflybyinstruments.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.radaeli.aeronauticsflybyinstruments.avionics.AvionicsComputerStatus;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import static com.radaeli.aeronauticsflybyinstruments.sensor.SensorModelChecks.*;
import static org.junit.jupiter.api.Assertions.*;

/** Avionics Computer v2.1 (ex-GPWS Computer): iron cabinet, brass trim, one screen per state. */
class AvionicsComputerAssetsTest {
    private static final String ROOT = "/assets/aeronautics_fly_by_instruments/";
    private static final String NAME = "avionics_computer";
    private static final String NS = "aeronautics_fly_by_instruments";
    private static final String[] STATES = {"normal", "caution", "warning", "inop"};

    @Test void followsTheGuideRulesWithIronAndBrassOnly() throws Exception {
        assertGuideRules(NAME);
        for (String state : STATES) assertGuideRules(NAME + "_" + state);
        var model = model(NAME);
        var names = new java.util.HashSet<String>();
        for (var value : model.getAsJsonArray("elements")) names.add(value.getAsJsonObject().get("name").getAsString());
        assertEquals(Set.of("base", "body", "lid", "screen", "test_button"), names);
        assertEquals(Set.of(NS + ":block/avionics_computer", NS + ":block/avionics_computer_top",
                NS + ":block/avionics_computer_screen", "create:block/industrial_iron_block", "create:block/brass_casing"),
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

    @Test void everyStateOnlySwapsTheScreenTexture() throws Exception {
        var base = model(NAME);
        for (String state : STATES) {
            var variant = model(NAME + "_" + state);
            assertEquals(base.get("elements"), variant.get("elements"), state + " must not change the geometry");
            var baseTextures = base.getAsJsonObject("textures");
            var variantTextures = variant.getAsJsonObject("textures");
            assertEquals(baseTextures.keySet(), variantTextures.keySet());
            var changed = new java.util.HashSet<String>();
            for (var key : baseTextures.keySet()) {
                if (!baseTextures.get(key).equals(variantTextures.get(key))) changed.add(key);
            }
            assertEquals(1, changed.size(), "Only the screen texture changes");
            var key = changed.iterator().next();
            assertEquals(NS + ":block/avionics_computer_screen", baseTextures.get(key).getAsString());
            assertEquals(NS + ":block/avionics_computer_screen_" + state, variantTextures.get(key).getAsString());
            var front = element(base, "screen").getAsJsonObject("faces").getAsJsonObject("north");
            assertEquals("#" + key, front.get("texture").getAsString());
        }
    }

    @Test void itemAndBlockStatesReferenceModels() throws Exception {
        var item = json("models/item/avionics_computer.json");
        assertEquals(NS + ":block/avionics_computer", item.get("parent").getAsString());
        for (String context : new String[]{"gui", "firstperson_righthand", "firstperson_lefthand",
                "thirdperson_righthand", "thirdperson_lefthand", "ground", "fixed"}) {
            assertTrue(item.getAsJsonObject("display").has(context));
        }
        var variants = json("blockstates/avionics_computer.json").getAsJsonObject("variants");
        assertEquals(16, variants.size());
        int rotation = 0;
        for (String facing : new String[]{"north", "east", "south", "west"}) {
            for (String state : STATES) {
                var variant = variants.getAsJsonObject("facing=" + facing + ",screen=" + state);
                assertNotNull(variant, facing + " " + state);
                assertEquals(item.get("parent").getAsString() + "_" + state, variant.get("model").getAsString());
                assertEquals(rotation, variant.has("y") ? variant.get("y").getAsInt() : 0);
            }
            rotation += 90;
        }
        for (var level : AvionicsComputerStatus.Level.values()) {
            assertNotNull(AvionicsComputerBlock.Screen.of(level), level.name());
        }
        assertEquals(AvionicsComputerBlock.Screen.INOP,
                ModBlocks.AVIONICS_COMPUTER.get().defaultBlockState().getValue(AvionicsComputerBlock.SCREEN),
                "A placed computer shows INOP until its first evaluation");
    }

    @Test void testButtonIsPressableFromTheFrontInEveryFacing() {
        for (var facing : Direction.Plane.HORIZONTAL) {
            // The button centre (model 8, 3, 0) placed with this facing.
            var centre = place(8 / 16.0, 0, facing);
            var hit = new Vec3(centre[0], 3 / 16.0, centre[1]);
            assertTrue(AvionicsComputerBlock.hitsTestButton(facing, facing, hit), facing.toString());
            assertFalse(AvionicsComputerBlock.hitsTestButton(facing, facing.getOpposite(), hit), facing.toString());
            assertFalse(AvionicsComputerBlock.hitsTestButton(facing, facing, hit.add(0, 6 / 16.0, 0)),
                    "The screen above the button opens the computer");
        }
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
            for (var screen : AvionicsComputerBlock.Screen.values()) {
                var state = ModBlocks.AVIONICS_COMPUTER.get().defaultBlockState()
                        .setValue(AvionicsComputerBlock.FACING, facing).setValue(AvionicsComputerBlock.SCREEN, screen);
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
        try (var stream = AvionicsComputerAssetsTest.class.getResourceAsStream(ROOT + path)) {
            assertNotNull(stream, path);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
