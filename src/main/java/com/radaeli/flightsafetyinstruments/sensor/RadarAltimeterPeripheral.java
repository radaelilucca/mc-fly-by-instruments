package com.radaeli.flightsafetyinstruments.sensor;

import com.radaeli.flightsafetyinstruments.avionics.SensorReading;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Independent CC:Tweaked peripheral for one Radar Altimeter block. */
public final class RadarAltimeterPeripheral implements IPeripheral {
    public static final String EVENT_NAME = "radar_altimeter_reading_changed";
    private final RadarAltimeterBlockEntity sensor;
    private final Set<IComputerAccess> computers = ConcurrentHashMap.newKeySet();

    public RadarAltimeterPeripheral(RadarAltimeterBlockEntity sensor) {
        this.sensor = sensor;
    }

    @Override
    public String getType() {
        return "radar_altimeter";
    }

    @Override
    public Object getTarget() {
        return sensor;
    }

    @Override
    public boolean equals(IPeripheral other) {
        return other instanceof RadarAltimeterPeripheral peripheral && peripheral.sensor == sensor;
    }

    @Override
    public void attach(IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.remove(computer);
    }

    @LuaFunction(mainThread = true)
    public Map<String, Object> getReading() {
        return toLua(sensor.reading());
    }

    public void publishReading(SensorReading<Double> reading) {
        Map<String, Object> snapshot = toLua(reading);
        for (IComputerAccess computer : computers) {
            computer.queueEvent(EVENT_NAME, computer.getAttachmentName(), snapshot);
        }
    }

    public static Map<String, Object> toLua(SensorReading<Double> reading) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("status", reading.status().name().toLowerCase(java.util.Locale.ROOT));
        reading.value().ifPresent(value -> snapshot.put("value", value));
        snapshot.put("unit", reading.unit());
        snapshot.put("gameTime", reading.gameTime());
        return Map.copyOf(snapshot);
    }
}
