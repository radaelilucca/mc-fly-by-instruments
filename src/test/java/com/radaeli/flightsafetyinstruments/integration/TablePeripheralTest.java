package com.radaeli.flightsafetyinstruments.integration;

import com.radaeli.flightsafetyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.flightsafetyinstruments.block.ModBlocks;
import net.minecraft.core.BlockPos;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TablePeripheralTest {
    private ModularTableBlockEntity table() {
        return new ModularTableBlockEntity(BlockPos.ZERO, ModBlocks.MODULAR_TABLE_BASE.get().defaultBlockState());
    }

    @Test
    void exposesOneBasedSlotsAndFootprintQueries() throws LuaException {
        var table = table();
        var peripheral = table.peripheral();
        assertEquals("modular_table", peripheral.getType());
        assertEquals(Map.of("columns", 3, "rows", 3), peripheral.getGridSize());
        assertEquals(false, peripheral.getSlot(1, 1).get("occupied"));
        table.placeLamp(0, 0);
        assertEquals(true, peripheral.getSlot(1, 1).get("occupied"));
        assertEquals(1, peripheral.getSlot(1, 1).get("column"));
        assertEquals("lamp", peripheral.getSlot(1, 1).get("type"));
        assertEquals(1, peripheral.getModules().size());
        assertFalse(peripheral.canFit(1, 1, 1, 1));
        assertTrue(peripheral.canFit(2, 2, 2, 2));
        assertFalse(peripheral.canFit(3, 3, 2, 2));
        assertThrows(LuaException.class, () -> peripheral.getSlot(0, 1));
        assertThrows(LuaException.class, () -> peripheral.getSlot(1, 4));
        assertThrows(LuaException.class, () -> peripheral.canFit(1, 1, 0, 1));
    }

    @Test
    void publishesAttachmentActionAndModuleAndStopsAfterDetach() {
        var table = table();
        var peripheral = table.peripheral();
        var events = new ArrayList<Object[]>();
        var computer = (IComputerAccess) Proxy.newProxyInstance(IComputerAccess.class.getClassLoader(),
                new Class<?>[]{IComputerAccess.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "getAttachmentName" -> "back";
                    case "queueEvent" -> { events.add(arguments); yield null; }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> null;
                });
        table.placeLamp(1, 2);
        var module = table.grid().at(1, 2).orElseThrow();
        peripheral.attach(computer);
        peripheral.moduleChanged("placed", module);
        assertEquals(1, events.size());
        assertEquals("modular_table_changed", events.getFirst()[0]);
        var arguments = (Object[]) events.getFirst()[1];
        assertEquals("back", arguments[0]);
        assertEquals("placed", arguments[1]);
        assertEquals(TablePeripheral.describe(module), arguments[2]);
        peripheral.detach(computer);
        peripheral.moduleChanged("removed", module);
        assertEquals(1, events.size());
    }
}
