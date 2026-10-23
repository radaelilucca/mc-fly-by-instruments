package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.client.DynamicText.Tone;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig;
import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig.Channel;
import com.radaeli.ruilib.ui.Bounds;
import com.radaeli.ruilib.ui.Button;
import com.radaeli.ruilib.ui.InventoryGrid;
import com.radaeli.ruilib.ui.ItemSlot;
import com.radaeli.ruilib.ui.Slider;
import com.radaeli.ruilib.ui.Tabs;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SensorOutputConfigScreenTest {
    private static final Map<String, String> TRANSLATIONS = translations();
    /** Smallest GUI an automatic GUI scale produces is 320×240; the screen keeps a 12 px margin. */
    private static final int SMALL_GUI_WIDTH = 320 - 24;
    private static final int SMALL_GUI_HEIGHT = 240 - 12;
    private static final int WIDE_GUI_WIDTH = 426 - 24;
    // Deterministic text metrics, without loading an atlas or starting the Minecraft client.
    private final Font font = new Font(ignored -> null, false) {
        @Override public int width(String text) { return TRANSLATIONS.getOrDefault(text, text).length() * 5; }
        @Override public int width(FormattedText text) { return width(text.getString()); }
        @Override public String plainSubstrByWidth(String text, int width) {
            return text.substring(0, Math.min(text.length(), Math.max(0, width / 5)));
        }
        @Override public String plainSubstrByWidth(String text, int width, boolean reverse) {
            int count = Math.min(text.length(), Math.max(0, width / 5));
            return reverse ? text.substring(text.length() - count) : text.substring(0, count);
        }
    };

    @Test
    void smallGuiStacksEverySensorInOneColumnWithUncompressedActions() {
        for (String kind : List.of("altimeter", "terrain", "advanced")) {
            var screen = screen(kind, 90);
            var root = layout(screen, SMALL_GUI_WIDTH, SMALL_GUI_HEIGHT);
            assertEquals(168, root.bounds().width(), kind);
            assertTrue(root.bounds().height() <= SMALL_GUI_HEIGHT, kind + " fits a 240 px GUI");
            assertNotEquals(SensorScreenLayout.Mode.WIDE, screen.layoutMode());
            assertActionsInside(root, kind);
            InventoryGrid inventory = components(root, InventoryGrid.class).getFirst();
            assertEquals(152, inventory.bounds().width());
            assertEquals(root.bounds().x() + 8, inventory.bounds().x());
            assertEquals(root.bounds(), screen.jeiGuiBounds());
        }
        assertEquals(SensorScreenLayout.Mode.STACKED, screenAt("altimeter", SMALL_GUI_WIDTH, SMALL_GUI_HEIGHT).layoutMode(),
                "Single-channel sensors keep the header divider and the picker hint");
        assertEquals(SensorScreenLayout.Mode.COMPACT, screenAt("advanced", SMALL_GUI_WIDTH, SMALL_GUI_HEIGHT).layoutMode(),
                "The advanced scanner drops only decoration to fit");
        assertEquals(SensorScreenLayout.Mode.STACKED, screenAt("advanced", SMALL_GUI_WIDTH, 300).layoutMode());
    }

    @Test
    void wideGuiPutsThePickerBesideTheSettingsAndTheActionsUnderIt() {
        for (String kind : List.of("altimeter", "terrain", "advanced")) {
            var screen = screen(kind, 90);
            var root = layout(screen, WIDE_GUI_WIDTH, SMALL_GUI_HEIGHT);
            assertEquals(SensorScreenLayout.Mode.WIDE, screen.layoutMode(), kind);
            assertEquals(SensorScreenLayout.WIDE_WIDTH + 16, root.bounds().width());
            assertTrue(root.bounds().height() <= 180, kind + " leaves room on short screens: " + root.bounds());
            assertActionsInside(root, kind);
            InventoryGrid inventory = components(root, InventoryGrid.class).getFirst();
            int pickerX = root.bounds().x() + 8 + SensorScreenLayout.COLUMN_WIDTH + SensorScreenLayout.COLUMN_GAP;
            assertEquals(pickerX, inventory.bounds().x());
            for (Button button : components(root, Button.class)) {
                assertTrue(button.bounds().x() >= pickerX, "Actions sit under the picker column");
                assertTrue(button.bounds().y() > inventory.bounds().bottom());
            }
            for (Slider slider : components(root, Slider.class)) {
                assertTrue(slider.bounds().right() <= pickerX - SensorScreenLayout.COLUMN_GAP,
                        "Settings never run into the picker column");
            }
        }
    }

    @Test
    void advancedTabsKeepIndependentDraftRangesAndGhostTargets() {
        var screen = screen("advanced", 90);
        var root = layout(screen, SMALL_GUI_WIDTH, SMALL_GUI_HEIGHT);
        Tabs tabs = components(root, Tabs.class).getFirst();
        List<Slider> allSliders = components(root, Slider.class);
        assertEquals(4, allSliders.size(), "Opening angle plus one range slider per sector");
        Slider angle = allSliders.getFirst();
        assertFalse(angle.hasUpperValue(), "Opening angle is a single-value slider");
        assertEquals(90, angle.value());
        List<Slider> sliders = allSliders.subList(1, 4);
        assertTrue(sliders.stream().allMatch(Slider::hasUpperValue), "Sector sliders select a range");
        sliders.get(0).setValues(5, 25);
        tabs.select(1);
        sliders.get(1).setValues(6, 40);
        tabs.select(2);
        assertEquals(5, sliders.get(0).value());
        assertEquals(25, sliders.get(0).upperValue());
        assertEquals(6, sliders.get(1).value());
        assertEquals(40, sliders.get(1).upperValue());
        assertEquals(4, sliders.get(2).value());
        assertEquals(32, sliders.get(2).upperValue());
        assertEquals(6, components(root, ItemSlot.class).size());
        long activeSlots = components(root, ItemSlot.class).stream().filter(slot ->
                root.hitTest(slot.bounds().centerX(), slot.bounds().centerY()) == slot).count();
        assertEquals(2, activeSlots, "JEI must only expose the selected sector's slots");
        Bounds strip = tabs.bounds();
        assertTrue(sliders.get(2).bounds().y() >= strip.y() + 17 + 5, "Sector controls keep a gap below the tab strip");
        List<DynamicText> texts = components(root, DynamicText.class);
        assertTrue(texts.getFirst().line().text().getStyle().isBold(), "The block name is the only bold heading");
        assertTrue(texts.stream().skip(1).noneMatch(text -> text.line().text().getStyle().isBold()));
    }

    @Test
    void sideSectorsAreInertAndExplainWhyWhileTheApertureIsTooNarrow() {
        var screen = screen("advanced", 40);
        var root = layout(screen, SMALL_GUI_WIDTH, SMALL_GUI_HEIGHT);
        List<Slider> sliders = components(root, Slider.class);
        List<ItemSlot> slots = components(root, ItemSlot.class);
        assertTrue(sliders.get(1).isEnabled(), "FRONT works at any aperture");
        assertFalse(sliders.get(2).isEnabled());
        assertFalse(sliders.get(3).isEnabled());
        assertTrue(slots.subList(2, 6).stream().noneMatch(UiComponent::isEnabled));
        assertEquals(Tone.WARNING, screen.draft().caption(Channel.LEFT).tone());
        assertEquals(Tone.MUTED, screen.draft().caption(Channel.FRONT).tone());

        sliders.getFirst().setValue(51);
        assertTrue(sliders.get(2).isEnabled());
        assertTrue(slots.subList(2, 6).stream().allMatch(UiComponent::isEnabled));
        assertEquals(Tone.MUTED, screen.draft().caption(Channel.LEFT).tone());
    }

    @Test
    void captionsFollowTheRangeAndStateEachSensorsPolarity() {
        var altimeter = screen("altimeter", 90);
        var altitude = altimeter.draft().caption(Channel.ALTITUDE).text();
        assertEquals("gui.aeronautics_fly_by_instruments.sensor_output.caption.altitude", key(altitude));
        assertArrayEquals(new Object[]{0, 32}, args(altitude));

        var terrain = screen("terrain", 90);
        layout(terrain, SMALL_GUI_WIDTH, SMALL_GUI_HEIGHT);
        components(terrain.root(), Slider.class).getFirst().setValues(6, 20);
        var distance = terrain.draft().caption(Channel.FRONT).text();
        assertEquals("gui.aeronautics_fly_by_instruments.sensor_output.caption.distance", key(distance));
        assertArrayEquals(new Object[]{6, 20}, args(distance));
    }

    @Test
    void linkStatusShowsTheRoutingTheServerWillApply() {
        var screen = screen("advanced", 90);
        var root = layout(screen, SMALL_GUI_WIDTH, SMALL_GUI_HEIGHT);
        var draft = screen.draft();
        List<ItemSlot> slots = components(root, ItemSlot.class);
        assertStatus("faces", Tone.MUTED, draft.linkStatus(Channel.FRONT));

        slots.get(0).setItemStack(new ItemStack(Items.REDSTONE));
        assertStatus("incomplete", Tone.WARNING, draft.linkStatus(Channel.FRONT));

        slots.get(1).setItemStack(new ItemStack(Items.LAPIS_LAZULI));
        assertStatus("link", Tone.SUCCESS, draft.linkStatus(Channel.FRONT));
        assertStatus("off", Tone.WARNING, draft.linkStatus(Channel.LEFT),
                "With Link selected for the sensor, an unpaired sector has no output");
    }

    @Test
    void everyKeyTheScreenUsesIsTranslatedInBothLanguages() throws Exception {
        Map<String, String> portuguese = translations("pt_br");
        for (String key : List.of("aperture", "altitude_range", "detection_range", "caption.altitude",
                "caption.distance", "side_off", "link", "status.faces", "status.link", "status.incomplete",
                "status.off", "picker_hint", "apply", "cancel", "sector.front", "sector.left", "sector.right")) {
            String full = "gui.aeronautics_fly_by_instruments.sensor_output." + key;
            assertTrue(TRANSLATIONS.containsKey(full), "en_us " + full);
            assertTrue(portuguese.containsKey(full), "pt_br " + full);
        }
        for (String kind : List.of("altimeter", "terrain", "advanced")) {
            String title = SensorOutputConfigScreen.blockKey(kind);
            assertTrue(TRANSLATIONS.containsKey(title) && portuguese.containsKey(title),
                    "The heading reuses the block name: " + title);
        }
    }

    private static void assertStatus(String expected, Tone tone, DynamicText.Line line) {
        assertStatus(expected, tone, line, expected);
    }

    private static void assertStatus(String expected, Tone tone, DynamicText.Line line, String message) {
        assertEquals("gui.aeronautics_fly_by_instruments.sensor_output.status." + expected, key(line.text()), message);
        assertEquals(tone, line.tone(), message);
    }

    private void assertActionsInside(UiComponent root, String kind) {
        List<Button> buttons = components(root, Button.class);
        assertEquals(2, buttons.size());
        for (Button button : buttons) {
            assertEquals(20, button.bounds().height(), kind + " action must not be compressed");
            assertTrue(button.bounds().bottom() <= root.bounds().bottom() - 8, kind + " " + button.bounds());
            assertSame(button, root.hitTest(button.bounds().centerX(), button.bounds().centerY()));
        }
    }

    private SensorOutputConfigScreen screenAt(String kind, int width, int height) {
        var screen = screen(kind, 90);
        layout(screen, width, height);
        return screen;
    }

    private UiComponent layout(SensorOutputConfigScreen screen, int width, int height) {
        var root = screen.root();
        var size = root.measure(font, UiTheme.DARK, width, height);
        root.layout(font, UiTheme.DARK, new Bounds(12, 6, size.width(), size.height()));
        return root;
    }

    private static String key(net.minecraft.network.chat.Component component) {
        return ((net.minecraft.network.chat.contents.TranslatableContents) component.getContents()).getKey();
    }

    private static Object[] args(net.minecraft.network.chat.Component component) {
        return ((net.minecraft.network.chat.contents.TranslatableContents) component.getContents()).getArgs();
    }

    private SensorOutputConfigScreen screen(String kind, int angle) {
        return new SensorOutputConfigScreen(BlockPos.ZERO, kind, angle,
                new SensorOutputConfig(kind.equals("advanced"), kind.equals("altimeter")), 128);
    }

    private static Map<String, String> translations() {
        return translations("en_us");
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

    private static <T extends UiComponent> List<T> components(UiComponent root, Class<T> type) {
        List<T> result = new ArrayList<>();
        root.visitComponents(component -> { if (type.isInstance(component)) result.add(type.cast(component)); });
        return result;
    }
}
