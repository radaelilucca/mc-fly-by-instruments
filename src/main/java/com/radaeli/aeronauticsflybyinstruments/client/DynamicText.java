package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.ruilib.ui.UiComponent;
import com.radaeli.ruilib.ui.UiContext;
import com.radaeli.ruilib.ui.UiPaint;
import com.radaeli.ruilib.ui.UiPalette;
import com.radaeli.ruilib.ui.UiSize;
import com.radaeli.ruilib.ui.UiText;
import com.radaeli.ruilib.ui.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Single-line text whose content and semantic tone are read on every frame, so captions and
 * status lines follow the draft without rebuilding the tree. Text that does not fit is cut with
 * an ellipsis instead of overflowing the panel.
 */
final class DynamicText extends UiComponent {
    /** Semantic colors from the active theme; the hierarchy never uses raw ARGB values. */
    enum Tone {
        TEXT, MUTED, SUCCESS, WARNING;

        int color(UiPalette palette) {
            return switch (this) {
                case TEXT -> palette.text();
                case MUTED -> palette.mutedText();
                case SUCCESS -> palette.success();
                case WARNING -> palette.warning();
            };
        }
    }

    record Line(Component text, Tone tone) {
        Line {
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(tone, "tone");
        }
    }

    private static final String ELLIPSIS = "…";
    private final Supplier<Line> line;
    private boolean fillWidth;

    DynamicText(Supplier<Line> line) {
        this.line = Objects.requireNonNull(line, "line");
    }

    static DynamicText of(Component text, Tone tone) {
        Line fixed = new Line(text, tone);
        return new DynamicText(() -> fixed);
    }

    /** Takes the remaining width of a row, so a changing status never re-flows its siblings. */
    DynamicText fillWidth() {
        fillWidth = true;
        return this;
    }

    Line line() {
        return line.get();
    }

    @Override
    protected UiSize measureIntrinsic(Font font, UiTheme theme, int maxWidth, int maxHeight) {
        int width = fillWidth ? maxWidth : UiText.width(font, line.get().text(), theme);
        return new UiSize(width, UiText.height(font, theme)).constrain(maxWidth, maxHeight);
    }

    @Override
    public void render(UiContext context) {
        if (bounds().width() <= 0 || bounds().height() <= 0) return;
        Line current = line.get();
        UiPalette palette = context.theme().palette();
        int color = isEnabled() ? current.tone().color(palette) : palette.mutedText();
        Font font = context.font();
        int available = bounds().width();
        if (font.width(current.text()) <= available) {
            UiPaint.text(context, current.text(), bounds().x(), bounds().y(), color);
            return;
        }
        String clipped = font.plainSubstrByWidth(current.text().getString(),
                Math.max(0, available - font.width(ELLIPSIS))) + ELLIPSIS;
        UiPaint.text(context, clipped, bounds().x(), bounds().y(), color);
    }
}
