package com.radaeli.flightsafetyinstruments.sensor;

import com.radaeli.flightsafetyinstruments.block.ModBlocks;
import com.radaeli.flightsafetyinstruments.avionics.SensorReading;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RadarAltimeterPeripheralTest {
    @Test
    void exposesTypedReadingAndLuaAnnotation() throws Exception {
        var sensor = sensor();
        var peripheral = sensor.peripheral();
        assertEquals("radar_altimeter", peripheral.getType());
        assertEquals(Map.of("status", "unavailable", "unit", "blocks", "gameTime", 0L),
                peripheral.getReading());
        assertNotNull(RadarAltimeterPeripheral.class.getMethod("getReading")
                .getAnnotation(dan200.computercraft.api.lua.LuaFunction.class));
    }

    @Test
    void sendsSnapshotOnStatusOrValueChangeButNotTimestampOnly() {
        var sensor = sensor();
        var events = new ArrayList<Object[]>();
        var computer = (IComputerAccess) Proxy.newProxyInstance(IComputerAccess.class.getClassLoader(),
                new Class<?>[]{IComputerAccess.class}, (proxy, method, arguments) -> switch (method.getName()) {
                    case "getAttachmentName" -> "ralt";
                    case "queueEvent" -> { events.add(arguments); yield null; }
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> null;
                });
        sensor.peripheral().attach(computer);

        sensor.acceptReading(SensorReading.valid(12.5, "blocks", 5));
        assertEquals(1, events.size());
        assertEquals(RadarAltimeterPeripheral.EVENT_NAME, events.getFirst()[0]);
        Object[] arguments = (Object[]) events.getFirst()[1];
        assertEquals("ralt", arguments[0]);
        assertEquals(Map.of("status", "valid", "value", 12.5, "unit", "blocks", "gameTime", 5L),
                arguments[1]);

        sensor.acceptReading(SensorReading.valid(12.5, "blocks", 10));
        assertEquals(1, events.size());
        sensor.acceptReading(SensorReading.valid(11.0, "blocks", 15));
        assertEquals(2, events.size());
        sensor.acceptReading(SensorReading.unavailable("blocks", 20));
        assertEquals(3, events.size());
        sensor.peripheral().detach(computer);
    }

    private static RadarAltimeterBlockEntity sensor() {
        return new RadarAltimeterBlockEntity(BlockPos.ZERO, ModBlocks.RADAR_ALTIMETER.get().defaultBlockState());
    }
}
