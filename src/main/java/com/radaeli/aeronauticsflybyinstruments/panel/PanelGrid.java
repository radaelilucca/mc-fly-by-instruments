package com.radaeli.aeronauticsflybyinstruments.panel;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Occupancy is owned by an anchor; a footprint never spans adjacent tables. */
public final class PanelGrid {
    public static final int SIZE = 3;
    private final List<Module> modules = new ArrayList<>();

    public record Module(String type, int column, int row, int width, int height, String channel, double threshold,
                         String outputSource, String alertChannel, int minimumSeverity, int buzzerVolume) {
        public Module(String type, int column, int row, int width, int height) {
            this(type, column, row, width, height, "test", 0.5, "test", "gpws.any", 2, 100);
        }
        public Module(String type, int column, int row, int width, int height, String channel, double threshold) {
            this(type, column, row, width, height, channel, threshold, "test", "gpws.any", 2, 100);
        }

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

    public boolean configureAt(int column, int row, String channel, double threshold) {
        var existing = at(column, row);
        if (existing.isEmpty()) return false;
        var module = existing.get();
        modules.set(modules.indexOf(module), new Module(module.type(), module.column(), module.row(),
                module.width(), module.height(), channel, threshold, "test", module.alertChannel(),
                module.minimumSeverity(), module.buzzerVolume()));
        return true;
    }

    public boolean configureAlertAt(int column, int row, String source, String alertChannel, int minimumSeverity) {
        var existing = at(column, row);
        if (existing.isEmpty()) return false;
        return configureAlertAt(column, row, source, alertChannel, minimumSeverity,
                existing.get().channel(), existing.get().threshold());
    }

    public boolean configureAlertAt(int column, int row, String source, String alertChannel, int minimumSeverity,
                                    String localChannel, double localThreshold) {
        var existing = at(column, row);
        boolean validLocalSignal = source.equals("gpws") || localChannel != null && !localChannel.isBlank()
                && localChannel.length() <= 64 && Double.isFinite(localThreshold);
        if (existing.isEmpty() || !(source.equals("gpws") || source.equals("test"))
                || alertChannel == null || alertChannel.isBlank() || alertChannel.length() > 64
                || minimumSeverity < 1 || minimumSeverity > 3 || !validLocalSignal) return false;
        var module = existing.get();
        modules.set(modules.indexOf(module), new Module(module.type(), module.column(), module.row(),
                module.width(), module.height(), source.equals("test") ? localChannel : module.channel(),
                source.equals("test") ? localThreshold : module.threshold(), source, alertChannel,
                minimumSeverity, module.buzzerVolume()));
        return true;
    }

    public boolean setBuzzerVolumeAt(int column, int row, int volume) {
        var existing = at(column, row);
        if (existing.isEmpty() || !existing.get().type().equals("buzzer") || volume < 0 || volume > 100) return false;
        var module = existing.get();
        modules.set(modules.indexOf(module), new Module(module.type(), module.column(), module.row(),
                module.width(), module.height(), module.channel(), module.threshold(), module.outputSource(),
                module.alertChannel(), module.minimumSeverity(), volume));
        return true;
    }

    public List<Module> modules() {
        return List.copyOf(modules);
    }
}
