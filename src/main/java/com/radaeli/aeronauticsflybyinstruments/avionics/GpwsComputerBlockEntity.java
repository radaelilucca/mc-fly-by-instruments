package com.radaeli.aeronauticsflybyinstruments.avionics;

import com.mojang.logging.LogUtils;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.config.ModConfig;
import com.radaeli.aeronauticsflybyinstruments.sensor.AbstractTerrainScannerBlock;
import com.radaeli.aeronauticsflybyinstruments.sensor.RadarAltimeterBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.sensor.TerrainScanContact;
import com.radaeli.aeronauticsflybyinstruments.sensor.TerrainScanGeometry;
import com.radaeli.aeronauticsflybyinstruments.sensor.TerrainScanSnapshot;
import com.radaeli.aeronauticsflybyinstruments.sensor.TerrainScannerBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.HashMap;

/** GPWS endpoint: fuses cached network snapshots and evaluates them without running new scans. */
public final class GpwsComputerBlockEntity extends BlockEntity implements AvionicsEndpoint, AvionicsSignalProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private GpwsConfiguration configuration = new GpwsConfiguration();
    private GpwsMode mode = GpwsMode.STANDBY;
    private final GpwsEngine engine = new GpwsEngine();
    private final GpwsComputerPeripheral peripheral = new GpwsComputerPeripheral(this);
    private GpwsSample latestSample = emptySample(0, NetworkStatus.DISCONNECTED);
    private GpwsEngine.Evaluation latestEvaluation = initialEvaluation();
    private boolean loggedEvaluationFailure;

    public GpwsComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.GPWS_COMPUTER_ENTITY.get(), pos, state);
    }

    @Override public String avionicsType() { return "gpws"; }
    public GpwsComputerPeripheral peripheral() { return peripheral; }
    public GpwsConfiguration configuration() { return configuration; }
    public GpwsMode mode() { return mode; }
    public GpwsEngine.Evaluation evaluation() { return latestEvaluation; }
    public GpwsSample latestSample() { return latestSample; }

    @Override
    public List<AvionicsDataChannel<?>> avionicsSignals() {
        List<AvionicsDataChannel<?>> result = new ArrayList<>();
        int maximum = 0;
        boolean anyUsable = false;
        for (AlertChannel channel : AlertChannel.values()) {
            AlertReading reading = latestEvaluation.alerts().get(channel);
            int rank = com.radaeli.aeronauticsflybyinstruments.panel.PanelAlertLogic.severity(reading);
            maximum = Math.max(maximum, rank);
            anyUsable |= reading != null && reading.state() != AlertState.UNAVAILABLE;
            var value = reading == null || reading.state() == AlertState.UNAVAILABLE
                    ? SensorReading.<Integer>unavailable("severity", latestSample.gameTime())
                    : SensorReading.valid(rank, "severity", reading.gameTime());
            result.add(new AvionicsDataChannel<>("gpws." + channel.id(), value));
        }
        result.add(new AvionicsDataChannel<>("gpws.any", anyUsable
                ? SensorReading.valid(maximum, "severity", latestSample.gameTime())
                : SensorReading.unavailable("severity", latestSample.gameTime())));
        return List.copyOf(result);
    }

    public Map<String, Object> statusSnapshot() {
        Map<String, Object> result = new HashMap<>();
        result.put("mode", lower(mode));
        result.put("profile", lower(configuration.profile()));
        result.put("sensitivity", lower(configuration.sensitivity()));
        result.put("flightPhase", lower(latestEvaluation.phase()));
        result.put("automaticFlightPhase", lower(latestEvaluation.automaticPhase()));
        result.put("phaseOverride", configuration.phaseOverride().map(GpwsComputerBlockEntity::lower).orElse("auto"));
        result.put("networkStatus", lower(latestSample.networkStatus()));
        result.put("networkComplete", latestSample.networkStatus() == NetworkStatus.CONNECTED);
        result.put("cableCount", latestSample.cableCount());
        result.put("endpointCount", latestSample.endpointCount());
        result.put("radarAltimeterCount", latestSample.altimeterCount());
        result.put("terrainScannerCount", latestSample.terrainScannerCount());
        result.put("radarAltimeterFault", latestSample.altimeterFault());
        result.put("terrainScannerFault", latestSample.terrainScannerFault());
        result.put("terrainCoverageComplete", latestSample.terrainCoverageComplete());
        result.put("pullUpTerrainCoverageComplete", latestSample.pullUpTerrainCoverageComplete());
        result.put("gameTime", latestSample.gameTime());
        result.put("clearance", readingSnapshot(latestSample.clearance(), latestSample.altimeterFault(),
                latestSample.clearanceGameTime(), "blocks"));
        result.put("verticalSpeed", readingSnapshot(latestEvaluation.verticalSpeed(),
                false, latestSample.gameTime(), "blocks_per_second"));
        result.put("bankAngle", readingSnapshot(latestSample.bankAngleDegrees(), false,
                latestSample.gameTime(), "degrees"));
        return Map.copyOf(result);
    }

    private static Map<String, Object> readingSnapshot(OptionalDouble value, boolean failed, long gameTime,
                                                        String unit) {
        Map<String, Object> result = new HashMap<>();
        result.put("status", value.isPresent() ? "valid" : failed ? "failed" : "unavailable");
        value.ifPresent(reading -> result.put("value", reading));
        result.put("unit", unit);
        result.put("gameTime", gameTime);
        return Map.copyOf(result);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GpwsComputerBlockEntity computer) {
        if (level.isClientSide) return;
        // The screen's blinking cursor (gpws_computer_armed model) follows the runtime mode.
        boolean armed = computer.mode == GpwsMode.ARMED;
        if (state.getValue(com.radaeli.aeronauticsflybyinstruments.block.GpwsComputerBlock.ARMED) != armed) {
            level.setBlock(pos, state.setValue(com.radaeli.aeronauticsflybyinstruments.block.GpwsComputerBlock.ARMED, armed),
                    Block.UPDATE_CLIENTS);
        }
        int interval = ModConfig.SENSOR_SCAN_INTERVAL_TICKS.get();
        long offset = Math.floorMod(pos.asLong(), (long) interval);
        if (Math.floorMod(level.getGameTime(), interval) != offset) return;
        computer.evaluate();
    }

    private void evaluate() {
        if (level == null || level.isClientSide) return;
        GpwsEngine.Evaluation previousEvaluation = latestEvaluation;
        try {
            latestSample = gatherSample();
            if (GpwsEngine.shouldAutoArm(configuration.profile(), mode, latestSample)) {
                setModeInternal(GpwsMode.ARMED);
            }
            latestEvaluation = engine.evaluate(latestSample, configuration, mode,
                    ModConfig.SENSOR_SCAN_INTERVAL_TICKS.get());
            loggedEvaluationFailure = false;
        } catch (RuntimeException exception) {
            latestSample = emptySample(level.getGameTime(), NetworkStatus.INCOMPLETE);
            latestEvaluation = engine.evaluate(latestSample, configuration, mode,
                    ModConfig.SENSOR_SCAN_INTERVAL_TICKS.get());
            if (!loggedEvaluationFailure) {
                LOGGER.warn("GPWS evaluation failed at {}", worldPosition, exception);
                loggedEvaluationFailure = true;
            }
        }
        publishEvaluationChanges(previousEvaluation, latestEvaluation);
    }

    private void publishEvaluationChanges(GpwsEngine.Evaluation previous, GpwsEngine.Evaluation updated) {
        if (previous.phase() != updated.phase()) {
            peripheral.publishPhaseChanged(updated.phase(), latestSample.gameTime());
        }
        EnumMap<AlertChannel, AlertReading> changed = new EnumMap<>(AlertChannel.class);
        for (AlertChannel channel : AlertChannel.values()) {
            AlertReading oldReading = previous.alerts().get(channel);
            AlertReading newReading = updated.alerts().get(channel);
            if (GpwsComputerPeripheral.alertStateChanged(oldReading, newReading)) changed.put(channel, newReading);
        }
        if (!changed.isEmpty()) peripheral.publishAlertsChanged(changed);
    }

    private GpwsSample gatherSample() {
        long now = level.getGameTime();
        int interval = ModConfig.SENSOR_SCAN_INTERVAL_TICKS.get();
        long maxAge = interval * 2L;
        NetworkSnapshot network = AvionicsNetworkManager.snapshot(level, worldPosition);
        if (network.status() != NetworkStatus.CONNECTED) {
            return new GpwsSample(now, network.status(), network.cableCount(), network.endpointCount(),
                    0, false, OptionalDouble.empty(), now, 0, false, false, false, List.of(), bankAngle());
        }

        int altimeterCount = 0;
        boolean altimeterFault = false;
        Double minimumClearance = null;
        long minimumTime = now;
        int scannerCount = 0;
        boolean scannerFault = false;
        boolean allScannerCoverageComplete = true;
        boolean pullUpCoverageComplete = false;
        List<GpwsSample.TerrainContact> contacts = new ArrayList<>();
        SubLevel host = containingSubLevel(this);
        Vec3 gpwsForward = worldDirection(this,
                getBlockState().getValue(com.radaeli.aeronauticsflybyinstruments.block.GpwsComputerBlock.FACING));

        for (NetworkDevice device : network.devices()) {
            BlockPos sourcePos = worldPosition.offset(device.x(), device.y(), device.z());
            if (!level.hasChunkAt(sourcePos)) {
                if (device.type().equals("radar_altimeter")) altimeterFault = true;
                if (device.type().startsWith("terrain_scanner")) scannerFault = true;
                allScannerCoverageComplete = false;
                continue;
            }
            BlockEntity source = level.getBlockEntity(sourcePos);
            if (device.type().equals("radar_altimeter")) {
                altimeterCount++;
                if (!(source instanceof RadarAltimeterBlockEntity altimeter)
                        || containingSubLevel(altimeter) != host) {
                    altimeterFault = true;
                    continue;
                }
                SensorReading<Double> reading = altimeter.reading();
                if (!fresh(reading.status(), reading.gameTime(), now, maxAge)) {
                    altimeterFault = true;
                    continue;
                }
                double value = reading.value().orElseThrow();
                if (!Double.isFinite(value)) {
                    altimeterFault = true;
                    continue;
                }
                if (minimumClearance == null || value < minimumClearance
                        || (Double.compare(value, minimumClearance) == 0 && reading.gameTime() > minimumTime)) {
                    minimumClearance = value;
                    minimumTime = reading.gameTime();
                }
            } else if (device.type().equals("terrain_scanner")
                    || device.type().equals("terrain_scanner_advanced")) {
                scannerCount++;
                if (!(source instanceof TerrainScannerBlockEntity scanner)
                        || containingSubLevel(scanner) != host) {
                    scannerFault = true;
                    allScannerCoverageComplete = false;
                    continue;
                }
                TerrainScanSnapshot reading = scanner.reading();
                if (!fresh(reading.status(), reading.gameTime(), now, maxAge)) {
                    scannerFault = true;
                    allScannerCoverageComplete = false;
                    continue;
                }
                boolean complete = reading.sampledRays() == reading.totalRays();
                allScannerCoverageComplete &= complete;
                Direction facing = scanner.getBlockState().getValue(AbstractTerrainScannerBlock.FACING);
                Vec3 scannerForward = worldDirection(scanner, facing);
                boolean aligned = scannerForward.dot(gpwsForward) >= Math.cos(Math.toRadians(0.5));
                int aperture = scanner.advanced() ? scanner.openingAngle()
                        : TerrainScanGeometry.SIMPLE_APERTURE_DEGREES;
                if (aligned && complete && aperture >= 45) pullUpCoverageComplete = true;
                for (TerrainScanContact contact : reading.contacts()) {
                    Vec3 direction = TerrainScanGeometry.toLevelLocal(facing,
                            new TerrainScanGeometry.DirectionVector(contact.directionX(), contact.directionY(),
                                    contact.directionZ()));
                    if (host != null) direction = host.logicalPose().transformNormal(direction).normalize();
                    double angle = Math.toDegrees(Math.acos(Math.clamp(direction.dot(gpwsForward), -1.0, 1.0)));
                    contacts.add(new GpwsSample.TerrainContact(contact.distance(), angle));
                }
            }
        }

        return new GpwsSample(now, network.status(), network.cableCount(), network.endpointCount(),
                altimeterCount, altimeterFault,
                minimumClearance == null ? OptionalDouble.empty() : OptionalDouble.of(minimumClearance),
                minimumTime, scannerCount, scannerFault,
                scannerCount > 0 && allScannerCoverageComplete, scannerCount > 0 && pullUpCoverageComplete,
                contacts, bankAngle());
    }

    private OptionalDouble bankAngle() {
        if (level == null) return OptionalDouble.empty();
        SubLevel host = containingSubLevel(this);
        if (host == null) return OptionalDouble.of(0);
        Direction facing = getBlockState().getValue(com.radaeli.aeronauticsflybyinstruments.block.GpwsComputerBlock.FACING);
        Vec3 localForward = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        Vec3 worldForward = host.logicalPose().transformNormal(localForward).normalize();
        Vec3 worldUp = host.logicalPose().transformNormal(new Vec3(0, 1, 0)).normalize();
        return GpwsAttitude.bankDegrees(worldForward, worldUp);
    }

    private static Vec3 worldDirection(BlockEntity entity, Direction facing) {
        Vec3 direction = new Vec3(facing.getStepX(), facing.getStepY(), facing.getStepZ());
        SubLevel host = containingSubLevel(entity);
        return host == null ? direction : host.logicalPose().transformNormal(direction).normalize();
    }

    private static SubLevel containingSubLevel(BlockEntity entity) {
        var containing = Sable.HELPER.getContaining(entity);
        return containing instanceof SubLevel subLevel ? subLevel : null;
    }

    private static boolean fresh(ReadingStatus status, long readingTime, long now, long maxAge) {
        return status == ReadingStatus.VALID && readingTime <= now && now - readingTime <= maxAge;
    }

    private static GpwsSample emptySample(long gameTime, NetworkStatus status) {
        return new GpwsSample(gameTime, status, 0, 0, 0, false, OptionalDouble.empty(), gameTime,
                0, false, false, false, List.of(), OptionalDouble.empty());
    }

    private static GpwsEngine.Evaluation initialEvaluation() {
        EnumMap<AlertChannel, AlertReading> alerts = new EnumMap<>(AlertChannel.class);
        for (AlertChannel channel : AlertChannel.values()) {
            alerts.put(channel, new AlertReading(channel, AlertState.UNAVAILABLE, AlertSeverity.ADVISORY, 0));
        }
        return new GpwsEngine.Evaluation(FlightPhase.UNKNOWN, FlightPhase.UNKNOWN, OptionalDouble.empty(),
                Map.copyOf(alerts));
    }

    public boolean setMode(GpwsMode updated) {
        if (updated == null || updated == GpwsMode.ARMED && configuration.profile() == GpwsProfile.BASIC) return false;
        return setModeInternal(updated);
    }

    private boolean setModeInternal(GpwsMode updated) {
        if (mode == updated) return false;
        mode = updated;
        setChangedAndSync();
        peripheral.publishModeChanged(mode, configuration.profile());
        return true;
    }

    public boolean setProfile(GpwsProfile updated) {
        if (!configuration.setProfile(updated)) return false;
        setModeInternal(GpwsMode.STANDBY);
        configurationChanged();
        return true;
    }

    public boolean setSensitivity(GpwsSensitivity updated) {
        if (!configuration.setSensitivity(updated)) return false;
        configurationChanged();
        return true;
    }

    public boolean setPhaseOverride(FlightPhase updated) {
        if (configuration.profile() != GpwsProfile.ADVANCED && updated != null) return false;
        if (!configuration.setPhaseOverride(updated)) return false;
        configurationChanged();
        return true;
    }

    public boolean setAlertRule(AlertChannel channel, FlightPhase phase, GpwsRule rule) {
        if (configuration.profile() != GpwsProfile.ADVANCED || !configuration.setRule(channel, phase, rule)) {
            return false;
        }
        configurationChanged();
        return true;
    }

    private void configurationChanged() {
        setChangedAndSync();
        peripheral.publishConfigurationChanged();
    }

    private void setChangedAndSync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        configuration.save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        configuration = GpwsConfiguration.load(tag);
        // Arming is a runtime state: every world load starts safely in STANDBY.
        mode = GpwsMode.STANDBY;
    }

    private static String lower(Enum<?> value) { return value.name().toLowerCase(Locale.ROOT); }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
