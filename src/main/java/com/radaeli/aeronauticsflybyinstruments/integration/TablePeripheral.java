package com.radaeli.aeronauticsflybyinstruments.integration;

import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.panel.PanelGrid;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Lua coordinates are 1-based. Modules are installed physically, not created through Lua. */
public final class TablePeripheral implements IPeripheral {
    private final ModularTableBlockEntity table;
    private final Set<IComputerAccess> computers = ConcurrentHashMap.newKeySet();

    public TablePeripheral(ModularTableBlockEntity table) { this.table = table; }
    @Override public String getType() { return "modular_table"; }
    @Override public Object getTarget() { return table; }
    @Override public boolean equals(IPeripheral other) {
        return other instanceof TablePeripheral peripheral && peripheral.table == table;
    }
    @Override public void attach(IComputerAccess computer) {
        computers.add(computer);
    }
    @Override public void detach(IComputerAccess computer) {
        computers.remove(computer);
    }

    @LuaFunction(mainThread = true)
    public Map<String, Object> getGridSize() {
        return Map.of("columns", PanelGrid.SIZE, "rows", PanelGrid.SIZE);
    }

    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> getModules() {
        return table.grid().modules().stream().map(TablePeripheral::describe).toList();
    }

    @LuaFunction(mainThread = true)
    public Map<String, Object> getSlot(int column, int row) throws LuaException {
        validateCell(column, row);
        return table.grid().at(column - 1, row - 1).map(TablePeripheral::describe)
                .orElseGet(() -> Map.of("occupied", false, "column", column, "row", row));
    }

    @LuaFunction(mainThread = true)
    public boolean canFit(int column, int row, int width, int height) throws LuaException {
        validateCell(column, row);
        if (width < 1 || height < 1 || width > PanelGrid.SIZE || height > PanelGrid.SIZE) {
            throw new LuaException("Footprint dimensions must be between 1 and 3");
        }
        return table.grid().canFit(column - 1, row - 1, width, height);
    }

    @LuaFunction(mainThread = true)
    public boolean configureModule(int column, int row, String channel, double threshold) throws LuaException {
        validateCell(column, row);
        if (channel == null || channel.isBlank() || channel.length() > 64) throw new LuaException("Channel must contain 1 to 64 characters");
        if (!Double.isFinite(threshold)) throw new LuaException("Threshold must be finite");
        if (table.grid().at(column - 1, row - 1).filter(ModularTableBlockEntity::isLampModule).isEmpty()) {
            throw new LuaException("Only installed lamp modules can be configured");
        }
        return table.configureModule(column - 1, row - 1, channel, threshold);
    }

    @LuaFunction(mainThread = true)
    public boolean setTestSignal(String channel, double value) throws LuaException {
        if (channel == null || channel.isBlank() || channel.length() > 64) throw new LuaException("Channel must contain 1 to 64 characters");
        if (!Double.isFinite(value)) throw new LuaException("Signal must be finite");
        return table.setTestSignal(channel, value);
    }

    @LuaFunction(mainThread = true)
    public boolean setLampTestIntensity(int column, int row, int intensity) throws LuaException {
        validateCell(column, row);
        if (intensity < 0 || intensity > 15) throw new LuaException("Lamp intensity must be between 0 and 15");
        if (table.grid().at(column - 1, row - 1).filter(ModularTableBlockEntity::isLampModule).isEmpty()) {
            throw new LuaException("Test intensity is available only for installed lamps");
        }
        return table.setLampTestIntensity(column - 1, row - 1, intensity);
    }

    @LuaFunction(mainThread = true)
    public boolean clearLampTestIntensity(int column, int row) throws LuaException {
        validateCell(column, row);
        if (table.grid().at(column - 1, row - 1).filter(ModularTableBlockEntity::isLampModule).isEmpty()) {
            throw new LuaException("Test intensity is available only for installed lamps");
        }
        return table.clearLampTestIntensity(column - 1, row - 1);
    }

    @LuaFunction(mainThread = true)
    public List<Map<String, Object>> getOutputs() {
        return table.grid().modules().stream().map(module -> {
            var output = new java.util.HashMap<>(describe(module));
            output.put("active", table.isModuleActive(module));
            output.put("signal", table.testSignal(module.channel()));
            return Map.copyOf(output);
        }).toList();
    }

    private static void validateCell(int column, int row) throws LuaException {
        if (column < 1 || column > PanelGrid.SIZE || row < 1 || row > PanelGrid.SIZE) {
            throw new LuaException("Column and row must be between 1 and 3");
        }
    }

    public static Map<String, Object> describe(PanelGrid.Module module) {
        return Map.of("occupied", true, "type", module.type(), "column", module.column() + 1,
                "row", module.row() + 1, "width", module.width(), "height", module.height(),
                "channel", module.channel(), "threshold", module.threshold());
    }

    public void moduleChanged(String action, PanelGrid.Module module) {
        for (var computer : computers) {
            computer.queueEvent("modular_table_changed", computer.getAttachmentName(), action, describe(module));
        }
    }

    public void testSignalChanged(String channel, double value) {
        for (var computer : computers) {
            computer.queueEvent("modular_table_signal_changed", computer.getAttachmentName(), channel, value);
        }
    }
}
