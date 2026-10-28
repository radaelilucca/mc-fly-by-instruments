package com.radaeli.aeronauticsflybyinstruments.client;

import com.google.gson.JsonParser;
import com.radaeli.aeronauticsflybyinstruments.avionics.ScannerSide;
import com.radaeli.aeronauticsflybyinstruments.network.ApplySensorOutputConfigPayload;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig.Channel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SensorOutputConfigScreenTest {
    private static final String GUI = "gui.aeronautics_fly_by_instruments.sensor_output.";
    private final List<ApplySensorOutputConfigPayload> sent = new ArrayList<>();
    private List<ItemStack> inventory = List.of();

    @Test
    void rangeStepsApplyImmediatelyAndNeverCrossOrLeaveTheSensorReach() {
        var screen = screen("terrain", 90);
        screen.stepRange(Channel.FRONT, false, 3);
        assertEquals(1, sent.size(), "Every change is sent at once: no Save button");
        assertEquals(new SensorOutputConfig.Range(7, 32), sent.getLast().config().range(Channel.FRONT));

        screen.stepRange(Channel.FRONT, false, 100);
        assertEquals(new SensorOutputConfig.Range(31, 32), screen.config().range(Channel.FRONT));
        screen.stepRange(Channel.FRONT, true, -100);
        assertEquals(new SensorOutputConfig.Range(31, 32), screen.config().range(Channel.FRONT),
                "The far end cannot pass the near end");
        int before = sent.size();
        screen.stepRange(Channel.FRONT, true, -1);
        assertEquals(before, sent.size(), "A step that changes nothing sends nothing");
        screen.stepRange(Channel.FRONT, true, 1000);
        assertEquals(128, screen.config().range(Channel.FRONT).max());
    }

    @Test
    void apertureStepsOnlyExistOnTheAdvancedScannerAndGateTheSides() {
        var simple = screen("terrain", 90);
        simple.stepAngle(5);
        assertTrue(sent.isEmpty());

        var advanced = screen("advanced", 55);
        assertTrue(advanced.channelActive(Channel.LEFT));
        advanced.stepAngle(-5);
        assertEquals(50, sent.getLast().openingAngle());
        assertFalse(advanced.channelActive(Channel.LEFT), "Sides need an aperture above 50°");
        assertTrue(advanced.channelActive(Channel.FRONT));
        advanced.stepAngle(-1000);
        assertEquals(30, advanced.angle());
        advanced.stepAngle(1000);
        assertEquals(180, advanced.angle());
    }

    @Test
    void pickedItemsFillTheArmedSlotThenItsPairAndShowTheRouting() {
        var screen = screen("advanced", 90);
        assertEquals(GUI + "status.faces", screen.linkStatusKey(Channel.FRONT));

        screen.selectSlot(Channel.LEFT, true);
        screen.pick(Channel.LEFT, new ItemStack(Items.REDSTONE, 12));
        assertEquals(Items.REDSTONE, screen.config().frequencies(Channel.LEFT).first().getItem());
        assertEquals(1, screen.config().frequencies(Channel.LEFT).first().getCount(), "Items are copied, not taken");
        assertTrue(screen.selected(Channel.LEFT, false), "The pair's empty slot is armed next");
        assertEquals(GUI + "status.incomplete", screen.linkStatusKey(Channel.LEFT));

        screen.pick(Channel.LEFT, new ItemStack(Items.LAPIS_LAZULI));
        assertEquals(GUI + "status.link", screen.linkStatusKey(Channel.LEFT));
        assertEquals(GUI + "status.off", screen.linkStatusKey(Channel.FRONT),
                "With Link selected for the sensor, an unpaired sector has no output");
        assertEquals(SensorOutputConfig.Mode.LINK, sent.getLast().config().mode());

        int before = sent.size();
        screen.pick(Channel.LEFT, new ItemStack(Items.GOLD_INGOT));
        assertEquals(before, sent.size(), "A full pair without an armed slot ignores the click");
        screen.pick(Channel.FRONT, new ItemStack(Items.DIAMOND));
        assertEquals(Items.DIAMOND, screen.config().frequencies(Channel.FRONT).first().getItem(),
                "Without an armed slot the channel's first empty one is used");
        assertTrue(screen.config().frequencies(Channel.LEFT).sameIdentity(new SensorOutputConfig.FrequencyPair(
                new ItemStack(Items.REDSTONE), new ItemStack(Items.LAPIS_LAZULI))), "Other channels are untouched");

        screen.clearSlot(Channel.LEFT, false);
        assertEquals(GUI + "status.incomplete", screen.linkStatusKey(Channel.LEFT));
        assertEquals(SensorOutputConfig.Mode.FACES, sent.getLast().config().mode());
    }

    @Test
    void clickingTheArmedSlotAgainDisarmsIt() {
        var screen = screen("terrain", 90);
        screen.selectSlot(Channel.FRONT, false);
        assertTrue(screen.selected(Channel.FRONT, false));
        screen.selectSlot(Channel.FRONT, false);
        assertFalse(screen.selected(Channel.FRONT, false));
    }

    @Test
    void aDraggedItemGoesIntoTheSlotItIsDroppedOn() {
        var screen = screen("terrain", 90);
        screen.selectSlot(Channel.FRONT, true);
        screen.dropFrequency(Channel.FRONT, false, new ItemStack(Items.REDSTONE, 5));
        var pair = screen.config().frequencies(Channel.FRONT);
        assertTrue(pair.first().isEmpty(), "The drop target wins over the armed slot");
        assertEquals(Items.REDSTONE, pair.second().getItem());
        assertEquals(1, pair.second().getCount());
        assertTrue(screen.selected(Channel.FRONT, true), "The empty slot of the pair is armed next");
        screen.dropFrequency(Channel.FRONT, false, new ItemStack(Items.DIAMOND));
        assertEquals(Items.DIAMOND, screen.config().frequencies(Channel.FRONT).second().getItem(),
                "Dropping on a filled slot replaces it");
        assertEquals(2, sent.size());
    }

    @Test
    void sideChannelsIgnorePicksWhileTheApertureIsTooNarrow() {
        var screen = screen("advanced", 40);
        screen.pick(Channel.LEFT, new ItemStack(Items.REDSTONE));
        assertTrue(sent.isEmpty());
        assertTrue(screen.config().frequencies(Channel.LEFT).first().isEmpty());
    }

    @Test
    void theSideStepperStartsAtTheFrontAndCyclesBothWays() {
        var screen = screen("terrain", 90);
        screen.stepSide(1);
        assertEquals("front", screen.side());
        screen.stepSide(1);
        assertEquals("rear", screen.side());
        screen.stepSide(-1);
        screen.stepSide(-1);
        assertEquals("right", screen.side(), "Going back from the front wraps to the right side");
        var backwards = screen("terrain", 90);
        backwards.stepSide(-1);
        assertEquals("right", backwards.side());
    }

    @Test
    void theScannerSideIsChosenInItsOwnScreenAndSentAtOnce() {
        var screen = screen("terrain", 90);
        assertEquals("unset", screen.side(), "A new scanner has no side until the player picks one");
        screen.chooseSide(ScannerSide.REAR);
        assertEquals("rear", screen.side());
        assertEquals("rear", sent.getLast().side());
        screen.chooseSide(ScannerSide.REAR);
        screen.chooseSide(ScannerSide.UNSET);
        assertEquals(1, sent.size(), "Re-picking the same side or going back to unset sends nothing");

        assertEquals("left", screen("advanced", 90, "left").side());
        assertEquals("unset", screen("terrain", 90, "sideways").side());
        var altimeter = screen("altimeter", 90, "front");
        assertEquals("", altimeter.side(), "The altimeter has no side");
        altimeter.chooseSide(ScannerSide.FRONT);
        assertTrue(sent.size() == 1);
    }

    @Test
    void channelsFollowTheSensor() {
        assertEquals(List.of(Channel.ALTITUDE), screen("altimeter", 90).channels());
        assertEquals(List.of(Channel.FRONT), screen("terrain", 90).channels());
        assertEquals(List.of(Channel.FRONT, Channel.LEFT, Channel.RIGHT), screen("advanced", 90).channels());
    }

    @Test
    void everyKeyTheScreenUsesIsTranslatedInBothLanguages() {
        Map<String, String> english = translations("en_us");
        Map<String, String> portuguese = translations("pt_br");
        for (String key : List.of("title", "scan", "aperture", "angle_tooltip", "channel.altitude_only",
                "channel.front_only", "channel.front", "channel.left", "channel.right", "output", "side_off",
                "near", "far", "low", "high", "range_tooltip", "terrain_range_tooltip", "range_output_tooltip",
                "terrain_output_tooltip", "step_tooltip", "link", "slot_tooltip", "frequency_tooltip",
                "advanced_frequency_tooltip", "status.faces", "status.link", "status.incomplete", "status.off",
                "picker", "inventory", "done", "picker_slot_tooltip", "frequency_picker_tooltip", "side",
                "side_unset", "side.unset", "side.front", "side.rear", "side.left", "side.right", "side_tooltip")) {
            assertTrue(english.containsKey(GUI + key), "en_us " + key);
            assertTrue(portuguese.containsKey(GUI + key), "pt_br " + key);
        }
        for (String kind : List.of("altimeter", "terrain", "advanced")) {
            String title = SensorOutputConfigScreen.blockKey(kind);
            assertTrue(english.containsKey(title) && portuguese.containsKey(title),
                    "The heading reuses the block name: " + title);
        }
        String computer = "gui.aeronautics_fly_by_instruments.avionics_computer.";
        for (String key : List.of("issue.no_side", "no_side_tooltip", "role.terrain_scanner.unset",
                "role.terrain_scanner_advanced.unset", "role.terrain_scanner.rear",
                "role.terrain_scanner_advanced.left")) {
            assertTrue(english.containsKey(computer + key), "en_us " + key);
            assertTrue(portuguese.containsKey(computer + key), "pt_br " + key);
        }
    }

    private SensorOutputConfigScreen screen(String kind, int angle) {
        return screen(kind, angle, "unset");
    }

    private SensorOutputConfigScreen screen(String kind, int angle, String side) {
        return new SensorOutputConfigScreen(BlockPos.ZERO, kind, angle, side,
                new SensorOutputConfig(kind.equals("advanced"), kind.equals("altimeter")), 128, sent::add,
                () -> inventory);
    }

    private static Map<String, String> translations(String language) {
        Map<String, String> result = new HashMap<>();
        try (var stream = SensorOutputConfigScreenTest.class.getResourceAsStream(
                "/assets/aeronautics_fly_by_instruments/lang/" + language + ".json")) {
            assertNotNull(stream);
            JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject()
                    .entrySet().forEach(entry -> result.put(entry.getKey(), entry.getValue().getAsString()));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException(exception);
        }
        return result;
    }
}
