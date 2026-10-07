package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.ruilib.ui.Alignment;
import com.radaeli.ruilib.ui.Bounds;
import com.radaeli.ruilib.ui.Column;
import com.radaeli.ruilib.ui.Insets;
import com.radaeli.ruilib.ui.Panel;
import com.radaeli.ruilib.ui.Row;
import com.radaeli.ruilib.ui.Slider;
import com.radaeli.ruilib.ui.Spacer;
import com.radaeli.ruilib.ui.UiTheme;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Regressions against the locally shipped R-UI library; fixed spacers require no font/GPU. */
class RUiLayoutTest {
    @Test
    void naturalColumnIncludesGapsWithoutMovingItsContentOutsideThePanel() {
        Column column = new Column(3, Alignment.CENTER, Alignment.STRETCH);
        Spacer title = Spacer.fixed(152, 9);
        Spacer controls = Spacer.fixed(152, 22);
        Spacer buttons = Spacer.fixed(152, 20);
        column.add(title); column.add(controls); column.add(buttons);
        Panel panel = new Panel(column, new Insets(2, 8, 8, 8));
        var size = panel.measure(null, UiTheme.DARK, 168, 300);
        panel.layout(null, UiTheme.DARK, new Bounds(50, 40, size.width(), size.height()));
        assertEquals(168, size.width());
        assertEquals(67, size.height());
        assertEquals(42, title.bounds().y());
        assertEquals(panel.bounds().bottom() - 8, buttons.bounds().bottom());
    }

    @Test
    void centerAndEndAlignRowsWithoutFlexibleSpacers() {
        for (Alignment alignment : List.of(Alignment.CENTER, Alignment.END)) {
            Row row = new Row(4, alignment, Alignment.CENTER);
            Spacer first = Spacer.fixed(20, 10);
            Spacer second = Spacer.fixed(20, 10);
            row.add(first); row.add(second);
            row.layout(null, UiTheme.DARK, new Bounds(10, 10, 100, 20));
            assertEquals(alignment == Alignment.CENTER ? 38 : 66, first.bounds().x());
            assertEquals(first.bounds().x() + 24, second.bounds().x());
        }
    }

    @Test
    void centerAndEndAlignColumnsWithoutFlexibleSpacers() {
        for (Alignment alignment : List.of(Alignment.CENTER, Alignment.END)) {
            Column column = new Column(4, alignment, Alignment.START);
            Spacer first = Spacer.fixed(10, 20);
            Spacer second = Spacer.fixed(10, 20);
            column.add(first); column.add(second);
            column.layout(null, UiTheme.DARK, new Bounds(10, 10, 20, 100));
            assertEquals(alignment == Alignment.CENTER ? 38 : 66, first.bounds().y());
            assertEquals(first.bounds().y() + 24, second.bounds().y());
        }
    }

    @Test
    void flexibleSpacersStillFillRemainingSpaceIncludingGaps() {
        Row row = new Row(4, Alignment.CENTER, Alignment.CENTER);
        Spacer first = Spacer.fixed(20, 10);
        Spacer space = Spacer.flex();
        Spacer last = Spacer.fixed(20, 10);
        row.add(first); row.add(space); row.add(last);
        row.layout(null, UiTheme.DARK, new Bounds(10, 10, 100, 20));
        assertEquals(10, first.bounds().x());
        assertEquals(52, space.bounds().width());
        assertEquals(110, last.bounds().right());

        Column column = new Column(4, Alignment.CENTER, Alignment.CENTER);
        Spacer top = Spacer.fixed(10, 20);
        Spacer flexible = Spacer.flex();
        Spacer bottom = Spacer.fixed(10, 20);
        column.add(top); column.add(flexible); column.add(bottom);
        column.layout(null, UiTheme.DARK, new Bounds(10, 10, 20, 100));
        assertEquals(10, top.bounds().y());
        assertEquals(52, flexible.bounds().height());
        assertEquals(110, bottom.bounds().bottom());
    }

    @Test
    void dualSliderReportsBothLimitsAndRejectsCrossedValues() {
        List<List<Double>> changes = new ArrayList<>();
        Slider slider = new Slider(0, 128, 0, 1, ignored -> { });
        slider.setValues(0, 32);
        slider.setOnRangeChange((lower, upper) -> changes.add(List.of(lower, upper)));
        slider.setValue(4);
        slider.setUpperValue(77);
        assertEquals(List.of(List.of(4.0, 32.0), List.of(4.0, 77.0)), changes);
        assertThrows(IllegalArgumentException.class, () -> slider.setValues(77, 77));
        assertEquals(4, slider.value());
        assertEquals(77, slider.upperValue());
    }

    @Test
    void smallestSensorRangeKeepsDistinctHandles() {
        Slider slider = new Slider(0, 1, 0, 1, ignored -> { });
        slider.setValues(0, 1);
        slider.setValue(1);
        assertEquals(0, slider.value());
        assertEquals(1, slider.upperValue());
    }
}
