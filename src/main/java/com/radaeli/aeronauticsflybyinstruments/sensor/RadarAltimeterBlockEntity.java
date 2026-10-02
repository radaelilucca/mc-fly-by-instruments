package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.mojang.logging.LogUtils;
import com.radaeli.aeronauticsflybyinstruments.avionics.AvionicsEndpoint;
import com.radaeli.aeronauticsflybyinstruments.avionics.ReadingStatus;
import com.radaeli.aeronauticsflybyinstruments.avionics.SensorReading;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

/** Periodically samples clearance beneath its lower face and exposes the reading to CC:Tweaked. */
public final class RadarAltimeterBlockEntity extends BlockEntity implements AvionicsEndpoint {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final RadarAltimeterPeripheral peripheral = new RadarAltimeterPeripheral(this);
    private SensorReading<Double> reading = SensorReading.unavailable("blocks", 0L);
    private boolean loggedScanFailure;

    public RadarAltimeterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RADAR_ALTIMETER_ENTITY.get(), pos, state);
    }

    public RadarAltimeterPeripheral peripheral() {
        return peripheral;
    }

    public SensorReading<Double> reading() {
        return reading;
    }

    @Override
    public String avionicsType() {
        return "radar_altimeter";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RadarAltimeterBlockEntity sensor) {
        if (level.isClientSide) return;
        int interval = ModConfig.SENSOR_SCAN_INTERVAL_TICKS.get();
        long offset = Math.floorMod(pos.asLong(), (long) interval);
        if (Math.floorMod(level.getGameTime(), interval) != offset) return;
        sensor.scan();
    }

    private void scan() {
        if (level == null || level.isClientSide) return;
        try {
            acceptReading(RadarAltimeterScanner.scan(level, this, ModConfig.RADAR_ALTIMETER_MAX_RANGE.get()));
        } catch (RuntimeException exception) {
            acceptReading(SensorReading.failed("blocks", level.getGameTime()));
            if (!loggedScanFailure) {
                LOGGER.warn("Radar Altimeter scan failed at {}", worldPosition, exception);
                loggedScanFailure = true;
            }
        }
    }

    void acceptReading(SensorReading<Double> updated) {
        SensorReading<Double> previous = reading;
        reading = updated;
        if (changed(previous, updated)) peripheral.publishReading(updated);
    }

    private static boolean changed(SensorReading<Double> previous, SensorReading<Double> updated) {
        if (previous.status() != updated.status()) return true;
        if (updated.status() != ReadingStatus.VALID) return false;
        return Double.compare(previous.value().orElseThrow(), updated.value().orElseThrow()) != 0;
    }
}
