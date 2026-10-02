package com.radaeli.aeronauticsflybyinstruments.integration;

import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
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
        assertTrue(table.placeLamp2x1(1, 0, false));
        assertTrue(peripheral.configureModule(2, 1, "long_lamp", 0.5));
        assertTrue(peripheral.setTestSignal("long_lamp", 1));
        var longOutput = peripheral.getOutputs().stream().filter(output -> output.get("type").equals("lamp_2x1"))
                .findFirst().orElseThrow();
        assertEquals(true, longOutput.get("active"));
        assertTrue(peripheral.configureModule(1, 1, "terrain", 30));
        assertTrue(peripheral.setTestSignal("terrain", 35));
        var output = peripheral.getOutputs().getFirst();
        assertEquals(true, output.get("active"));
        assertEquals("terrain", output.get("channel"));
        assertEquals(35.0, output.get("signal"));
        assertTrue(peripheral.setTestSignal("terrain", 10));
        assertEquals(false, peripheral.getOutputs().getFirst().get("active"));
        assertThrows(LuaException.class, () -> peripheral.configureModule(1, 1, "", 1));
        assertThrows(LuaException.class, () -> peripheral.setTestSignal("test", Double.NaN));
        assertThrows(LuaException.class, () -> peripheral.getSlot(0, 1));
        assertThrows(LuaException.class, () -> peripheral.getSlot(1, 4));
        assertThrows(LuaException.class, () -> peripheral.canFit(1, 1, 0, 1));
    }

    @Test
    void tablePeripheralDoesNotExposeNetworkTopology() throws Exception {
        var peripheral = table().peripheral();
        assertThrows(NoSuchMethodException.class, () -> TablePeripheral.class.getMethod("getNetworkStatus"));
        assertThrows(NoSuchMethodException.class, () -> TablePeripheral.class.getMethod("getConnectedDevices"));
        assertNotNull(TablePeripheral.class.getMethod("getGridSize")
                .getAnnotation(dan200.computercraft.api.lua.LuaFunction.class));
    }

    @Test
    void lampIntensityTestApiValidatesAndClearsTransientOverrides() throws Exception {
        var table = table();
        var peripheral = table.peripheral();
        table.placeLamp(0, 0);
        assertTrue(peripheral.setLampTestIntensity(1, 1, 8));
        assertEquals(8, table.lampRenderIntensity(table.grid().at(0, 0).orElseThrow()));
        assertThrows(LuaException.class, () -> peripheral.setLampTestIntensity(1, 1, 16));
        assertThrows(LuaException.class, () -> peripheral.setLampTestIntensity(2, 1, 4));
        assertThrows(LuaException.class, () -> peripheral.clearLampTestIntensity(4, 1));
        assertTrue(peripheral.clearLampTestIntensity(1, 1));
        assertEquals(0, table.lampRenderIntensity(table.grid().at(0, 0).orElseThrow()));

        assertNotNull(TablePeripheral.class.getMethod("setLampTestIntensity", int.class, int.class, int.class)
                .getAnnotation(dan200.computercraft.api.lua.LuaFunction.class));
        assertNotNull(TablePeripheral.class.getMethod("clearLampTestIntensity", int.class, int.class)
                .getAnnotation(dan200.computercraft.api.lua.LuaFunction.class));
    }

    @Test
    void longLampHasIndependentSignalAndVisualTestControlsAcrossItsFootprint() throws LuaException {
        var table = table();
        var peripheral = table.peripheral();
        assertTrue(table.placeLamp2x1(0, 0, false));
        assertTrue(peripheral.configureModule(1, 1, "warning", 2));
        assertTrue(peripheral.setTestSignal("warning", 2));
        var longLamp = table.grid().modules().getFirst();
        assertTrue(table.isModuleActive(longLamp));
        assertTrue(peripheral.setLampTestIntensity(2, 1, 11));
        assertEquals(11, table.lampRenderIntensity(longLamp));
        assertTrue(peripheral.clearLampTestIntensity(2, 1));
        assertEquals(15, table.lampRenderIntensity(longLamp));
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
