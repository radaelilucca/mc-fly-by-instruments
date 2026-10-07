package com.radaeli.aeronauticsflybyinstruments.client;

import com.google.gson.JsonParser;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsModuleData;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsWaypoint;
import com.radaeli.aeronauticsflybyinstruments.network.FmsScreenPayload;
import com.radaeli.ruilib.ui.Bounds;
import com.radaeli.ruilib.ui.Alignment;
import com.radaeli.ruilib.ui.Button;
import com.radaeli.ruilib.ui.Label;
import com.radaeli.ruilib.ui.ScrollView;
import com.radaeli.ruilib.ui.Tabs;
import com.radaeli.ruilib.ui.TextField;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiTheme;
import com.radaeli.ruilib.ui.UiText;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FmsScreenTest {
    private static final Map<String, String> TRANSLATIONS = translations();
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
    void waypointFormConvertsTypedFractionalCoordinatesToContainingBlockPositions() {
        assertEquals(3.0, FmsScreen.parseBlockCoordinate("3.25"), 0.0);
        assertEquals(-2.0, FmsScreen.parseBlockCoordinate("-1.3278908072976996"), 0.0);
        assertEquals(-11.0, FmsScreen.parseBlockCoordinate("-10.671499778056285"), 0.0);
        assertNull(FmsScreen.parseBlockCoordinate("NaN"));
        assertNull(FmsScreen.parseBlockCoordinate("not a coordinate"));
    }

    @Test
    void waypointCrudStartsWithoutAnOpenFormAndFitsInsideThePanel() {
        var data = new FmsModuleData();
        data.add(new FmsWaypoint(UUID.randomUUID(), "Landing", "minecraft:overworld", 3.25, 82, -9.5));
        var screen = new FmsScreen(new FmsScreenPayload(BlockPos.ZERO, 0, 0, data,
                "minecraft:overworld", 0, 64, 0, -1, ""));
        var root = screen.root();
        var measured = root.measure(font, UiTheme.DARK, 168, 240);
        root.layout(font, UiTheme.DARK, new Bounds(30, 8, measured.width(), measured.height()));

        assertEquals(168, measured.width());
        assertTrue(measured.height() <= 240, "waypoint CRUD should fit a normal game viewport");
        assertEquals(root.bounds(), screen.jeiGuiBounds());
        var tabs = components(root, Tabs.class).getFirst();
        assertTrue(components(root, TextField.class).isEmpty(), "CRUD form should be hidden initially");
        assertEquals(1, components(root, ScrollView.class).size());
        assertTrue(components(root, com.radaeli.ruilib.ui.Table.class).isEmpty());
        assertEquals(3, components(root, Button.class).size(),
                "New, Select destination and FMS Compass actions are available");
        ScrollView list = components(root, ScrollView.class).getFirst();
        assertEquals(152, list.bounds().width());
        assertEquals(1, list.children().size());
        assertInstanceOf(FmsScreen.WaypointListItem.class, list.children().getFirst());
        assertEquals(list.bounds().x(), list.children().getFirst().bounds().x());
        assertEquals(list.bounds().width(), list.children().getFirst().bounds().width());
        assertTrue(list.bounds().y() >= tabs.bounds().y() + 22,
                "waypoint content should be separated from the tab strip");
        assertEquals("x: 3, z: -10", FmsScreen.waypointCoordinatesLabel(data.waypoints().getFirst()));
        assertEquals("Landing - x: 3, z: -10, y: 82",
                FmsScreen.waypointTooltipLabel(data.waypoints().getFirst()));

        for (Button button : components(root, Button.class)) {
            if (button.bounds().width() == 0 || button.bounds().height() == 0) continue;
            assertTrue(button.bounds().x() >= root.bounds().x());
            assertTrue(button.bounds().right() <= root.bounds().right());
            assertTrue(button.bounds().y() >= root.bounds().y());
            assertTrue(button.bounds().bottom() <= root.bounds().bottom());
        }
    }

    @Test
    void noTargetDoesNotReserveBlankCoordinateAndDistanceRows() {
        var screen = new FmsScreen(new FmsScreenPayload(BlockPos.ZERO, 0, 0, new FmsModuleData(),
                "minecraft:overworld", 0, 64, 0, -1, ""));
        var root = screen.root();
        root.layout(font, UiTheme.DARK, new Bounds(30, 8, 168, 180));

        var tabs = components(root, Tabs.class).getFirst();
        tabs.select(1);
        var visibleButtons = components(root, Button.class).stream()
                .filter(button -> button.bounds().width() > 0 && button.bounds().height() > 0)
                .toList();
        assertEquals(2, visibleButtons.size(), "Navigator should show destination and FMS Compass actions");
        assertTrue(visibleButtons.getFirst().bounds().y() >= tabs.bounds().y() + 22,
                "Navigator content should have breathing room below the tabs");
        assertTrue(visibleButtons.getLast().bounds().y() - visibleButtons.getFirst().bounds().bottom() < 60,
                "empty target coordinates must not leave a large gap above FMS Compass");
    }

    @Test
    void clearTargetActionAppearsAfterTargetDetails() {
        var data = new FmsModuleData();
        var target = new FmsWaypoint(UUID.randomUUID(), "Landing", "minecraft:overworld", -2, 60, -11);
        data.add(target);
        data.select(target.id());
        var screen = new FmsScreen(new FmsScreenPayload(BlockPos.ZERO, 0, 0, data,
                "minecraft:overworld", -2, 60, -9, -1, ""));
        var root = screen.root();
        root.layout(font, UiTheme.DARK, new Bounds(30, 8, 168, 240));
        components(root, Tabs.class).getFirst().select(1);

        var visibleButtons = components(root, Button.class).stream()
                .filter(button -> button.bounds().width() > 0 && button.bounds().height() > 0)
                .toList();
        var targetLabels = components(root, UiComponent.class).stream()
                .filter(component -> component.getClass().getSimpleName().equals("MutableLabel"))
                .filter(component -> component.bounds().width() > 0)
                .toList();
        assertEquals(2, visibleButtons.size());
        assertEquals(4, targetLabels.size());
        assertTrue(targetLabels.get(2).bounds().bottom() <= visibleButtons.getFirst().bounds().y(),
                "Clear target should follow the distance row");
        assertTrue(visibleButtons.getFirst().bounds().bottom() <= targetLabels.get(3).bounds().y(),
                "Clear target should remain before the FMS Compass status");
    }

    @Test
    void centeredTabStripKeepsItsClickTargetsCentered() {
        var tabs = new Tabs().setTabAlignment(Alignment.CENTER)
                .addTab(Component.literal("Waypoints"), new Label(Component.literal("List")))
                .addTab(Component.literal("Navigator"), new Label(Component.literal("Nav")));
        tabs.layout(font, UiTheme.DARK, new Bounds(10, 10, 200, 60));

        int firstWidth = UiText.width(font, "Waypoints", UiTheme.DARK) + 16;
        int secondWidth = UiText.width(font, "Navigator", UiTheme.DARK) + 16;
        int offset = (200 - firstWidth - secondWidth) / 2;
        assertTrue(offset > 0);
        tabs.mouseClicked(11, 11, 0);
        assertEquals(0, tabs.selectedIndex(), "Empty space before the centered strip is not a tab");
        tabs.mouseClicked(10 + offset + firstWidth + 1, 11, 0);
        assertEquals(1, tabs.selectedIndex(), "The centered second tab remains clickable");
    }

    private static Map<String, String> translations() {
        Map<String, String> result = new HashMap<>();
        try (var stream = FmsScreenTest.class.getResourceAsStream(
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
