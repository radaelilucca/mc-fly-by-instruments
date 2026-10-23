package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.ruilib.ui.Bounds;
import com.radaeli.ruilib.ui.Divider;
import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiContainer;
import com.radaeli.ruilib.ui.UiContext;
import com.radaeli.ruilib.ui.UiSize;
import com.radaeli.ruilib.ui.UiTheme;
import net.minecraft.client.gui.Font;

import java.util.HashSet;
import java.util.Set;

/**
 * Responsive body of the sensor output screen.
 *
 * <ul>
 *   <li>{@link Mode#WIDE}: settings on the left, frequency picker on the right, actions under the picker.</li>
 *   <li>{@link Mode#STACKED}: one column with header divider and picker hint.</li>
 *   <li>{@link Mode#COMPACT}: one column without the decorative divider and hint, for 240 px tall GUIs.</li>
 * </ul>
 *
 * The mode depends only on the available size, so measuring and laying out always agree.
 */
final class SensorScreenLayout extends UiContainer {
    enum Mode { WIDE, STACKED, COMPACT }

    static final int COLUMN_WIDTH = 9 * 16 + 8;
    static final int COLUMN_GAP = 12;
    static final int WIDE_WIDTH = COLUMN_WIDTH * 2 + COLUMN_GAP;
    private static final int HEADER_GAP = 4;
    private static final int WIDE_SECTION_GAP = 8;
    private static final int STACKED_SECTION_GAP = 6;
    private static final int COMPACT_SECTION_GAP = 5;
    private static final int HINT_GAP = 3;

    private final UiComponent header;
    private final Divider headerDivider = new Divider();
    private final Divider columnDivider = new Divider(Divider.Orientation.VERTICAL);
    private final UiComponent settings;
    private final UiComponent pickerHint;
    private final UiComponent picker;
    private final UiComponent actions;
    private final Set<UiComponent> hidden = new HashSet<>();
    private Mode mode = Mode.STACKED;

    SensorScreenLayout(UiComponent header, UiComponent settings, UiComponent pickerHint,
                       UiComponent picker, UiComponent actions) {
        this.header = add(header);
        add(headerDivider);
        this.settings = add(settings);
        add(columnDivider);
        this.pickerHint = add(pickerHint);
        this.picker = add(picker);
        this.actions = add(actions);
    }

    Mode mode() {
        return mode;
    }

    private Mode modeFor(Font font, UiTheme theme, int width, int height) {
        if (width >= WIDE_WIDTH) return Mode.WIDE;
        return height(font, theme, Mode.STACKED) <= height ? Mode.STACKED : Mode.COMPACT;
    }

    private static int heightOf(UiComponent component, Font font, UiTheme theme) {
        return component.measure(font, theme, COLUMN_WIDTH, Integer.MAX_VALUE / 4).height();
    }

    private int height(Font font, UiTheme theme, Mode mode) {
        int header = heightOf(this.header, font, theme);
        int settings = heightOf(this.settings, font, theme);
        int hint = heightOf(pickerHint, font, theme);
        int picker = heightOf(this.picker, font, theme);
        int actions = heightOf(this.actions, font, theme);
        return switch (mode) {
            case WIDE -> header + HEADER_GAP + 1 + WIDE_SECTION_GAP
                    + Math.max(settings, hint + HINT_GAP + picker) + WIDE_SECTION_GAP + actions;
            case STACKED -> header + HEADER_GAP + 1 + STACKED_SECTION_GAP + settings + STACKED_SECTION_GAP
                    + hint + HINT_GAP + picker + STACKED_SECTION_GAP + actions;
            case COMPACT -> header + COMPACT_SECTION_GAP + settings + COMPACT_SECTION_GAP
                    + picker + COMPACT_SECTION_GAP + actions;
        };
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        Mode measured = modeFor(font, theme, maxWidth, maxHeight);
        int width = measured == Mode.WIDE ? WIDE_WIDTH : COLUMN_WIDTH;
        return new UiSize(width, height(font, theme, measured)).constrain(maxWidth, maxHeight);
    }

    @Override
    protected void layoutChildren(Font font, UiTheme theme) {
        mode = modeFor(font, theme, bounds().width(), bounds().height());
        hidden.clear();
        int x = bounds().x();
        int y = bounds().y();
        int width = mode == Mode.WIDE ? WIDE_WIDTH : COLUMN_WIDTH;
        int headerHeight = heightOf(header, font, theme);
        header.layout(font, theme, new Bounds(x, y, width, headerHeight));
        y += headerHeight;

        if (mode == Mode.COMPACT) {
            hide(font, theme, headerDivider, columnDivider, pickerHint);
            y += COMPACT_SECTION_GAP;
            y = place(settings, font, theme, x, y) + COMPACT_SECTION_GAP;
            y = place(picker, font, theme, x, y) + COMPACT_SECTION_GAP;
            place(actions, font, theme, x, y);
            return;
        }

        y += HEADER_GAP;
        headerDivider.layout(font, theme, new Bounds(x, y, width, 1));
        y += 1;
        if (mode == Mode.STACKED) {
            hide(font, theme, columnDivider);
            y += STACKED_SECTION_GAP;
            y = place(settings, font, theme, x, y) + STACKED_SECTION_GAP;
            y = place(pickerHint, font, theme, x, y) + HINT_GAP;
            y = place(picker, font, theme, x, y) + STACKED_SECTION_GAP;
            place(actions, font, theme, x, y);
            return;
        }

        y += WIDE_SECTION_GAP;
        int top = y;
        int left = place(settings, font, theme, x, top);
        int rightX = x + COLUMN_WIDTH + COLUMN_GAP;
        int right = place(pickerHint, font, theme, rightX, top) + HINT_GAP;
        right = place(picker, font, theme, rightX, right);
        int bottom = Math.max(left, right);
        columnDivider.layout(font, theme, new Bounds(x + COLUMN_WIDTH + COLUMN_GAP / 2, top, 1, bottom - top));
        place(actions, font, theme, rightX, bottom + WIDE_SECTION_GAP);
    }

    private int place(UiComponent component, Font font, UiTheme theme, int x, int y) {
        int height = heightOf(component, font, theme);
        component.layout(font, theme, new Bounds(x, y, COLUMN_WIDTH, height));
        return y + height;
    }

    private void hide(Font font, UiTheme theme, UiComponent... components) {
        for (UiComponent component : components) {
            component.layout(font, theme, Bounds.EMPTY);
            hidden.add(component);
        }
    }

    @Override
    public void render(UiContext context) {
        for (UiComponent child : children()) {
            if (!hidden.contains(child)) child.render(context);
        }
    }
}
