package com.radaeli.flightsafetyinstruments.integration;

import com.radaeli.flightsafetyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.flightsafetyinstruments.panel.PanelGrid;
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
    @Override public void attach(IComputerAccess computer) { computers.add(computer); }
    @Override public void detach(IComputerAccess computer) { computers.remove(computer); }

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

    private static void validateCell(int column, int row) throws LuaException {
        if (column < 1 || column > PanelGrid.SIZE || row < 1 || row > PanelGrid.SIZE) {
            throw new LuaException("Column and row must be between 1 and 3");
        }
    }

    public static Map<String, Object> describe(PanelGrid.Module module) {
        return Map.of("occupied", true, "type", module.type(), "column", module.column() + 1,
                "row", module.row() + 1, "width", module.width(), "height", module.height());
    }

    public void moduleChanged(String action, PanelGrid.Module module) {
        for (var computer : computers) {
            computer.queueEvent("modular_table_changed", computer.getAttachmentName(), action, describe(module));
        }
    }
}
