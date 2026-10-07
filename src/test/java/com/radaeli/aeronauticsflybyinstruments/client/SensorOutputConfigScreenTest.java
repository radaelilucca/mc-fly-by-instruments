package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig;
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
    void allSensorsShareInventoryWidthAndKeepActionsInsideThePanel() {
        for (String kind : List.of("altimeter", "terrain", "advanced")) {
            var screen = screen(kind);
            var root = screen.root();
            var size = root.measure(font, UiTheme.DARK, 168, 228);
            root.layout(font, UiTheme.DARK, new Bounds(40, 10, size.width(), size.height()));
            assertEquals(168, size.width());
            assertTrue(size.height() <= 228);
            for (Button button : components(root, Button.class)) {
                assertEquals(20, button.bounds().height(), kind + " action must not be compressed");
                assertTrue(button.bounds().bottom() <= root.bounds().bottom() - 8);
                assertSame(button, root.hitTest(button.bounds().centerX(), button.bounds().centerY()));
            }
            InventoryGrid inventory = components(root, InventoryGrid.class).getFirst();
            assertEquals(152, inventory.bounds().width());
            assertEquals(root.bounds().x() + 8, inventory.bounds().x());
            assertEquals(root.bounds(), screen.jeiGuiBounds());
        }
    }

    @Test
    void advancedTabsKeepIndependentDraftRangesAndGhostTargets() {
        var screen = screen("advanced");
        var root = screen.root();
        var size = root.measure(font, UiTheme.DARK, 168, 228);
        root.layout(font, UiTheme.DARK, new Bounds(40, 10, size.width(), size.height()));
        Tabs tabs = components(root, Tabs.class).getFirst();
        List<Slider> sliders = components(root, Slider.class);
        assertEquals(3, sliders.size());
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
    }

    private SensorOutputConfigScreen screen(String kind) {
        return new SensorOutputConfigScreen(BlockPos.ZERO, kind, 90,
                new SensorOutputConfig(kind.equals("advanced"), kind.equals("altimeter")), 128);
    }

    private static Map<String, String> translations() {
        Map<String, String> result = new HashMap<>();
        try (var stream = SensorOutputConfigScreenTest.class.getResourceAsStream(
                "/assets/aeronautics_fly_by_instruments/lang/en_us.json")) {
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
