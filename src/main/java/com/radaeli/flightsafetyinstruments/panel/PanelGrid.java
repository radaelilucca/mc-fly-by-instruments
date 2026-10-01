package com.radaeli.flightsafetyinstruments.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Occupancy is owned by an anchor; a footprint never spans adjacent tables. */
public final class PanelGrid {
    public static final int SIZE = 3;
    private final List<Module> modules = new ArrayList<>();

    public record Module(String type, int column, int row, int width, int height) {
        public boolean contains(int x, int y) {
            return x >= column && x < column + width && y >= row && y < row + height;
        }
    }

    public boolean canFit(int column, int row, int width, int height) {
        if (column < 0 || row < 0 || width < 1 || height < 1
                || width > SIZE || height > SIZE || column > SIZE - width || row > SIZE - height) {
            return false;
        }
        for (int x = column; x < column + width; x++) {
            for (int y = row; y < row + height; y++) {
                if (at(x, y).isPresent()) return false;
            }
        }
        return true;
    }

    public boolean place(Module module) {
        if (module.type() == null || module.type().isBlank()
                || !canFit(module.column(), module.row(), module.width(), module.height())) return false;
        modules.add(module);
        return true;
    }

    public Optional<Module> at(int column, int row) {
        return modules.stream().filter(module -> module.contains(column, row)).findFirst();
    }

    public Optional<Module> removeAt(int column, int row) {
        var module = at(column, row);
        module.ifPresent(modules::remove);
        return module;
    }

    public List<Module> modules() {
        return List.copyOf(modules);
    }
}
