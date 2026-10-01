package com.radaeli.flightsafetyinstruments.avionics;

import com.radaeli.flightsafetyinstruments.block.ModBlocks;
import com.radaeli.flightsafetyinstruments.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Event-invalidated, per-level topology cache. World traversal only occurs on query or dirty events. */
public final class AvionicsNetworkManager {
    private static final Map<Level, Map<BlockPos, NetworkSnapshot>> CACHE = new WeakHashMap<>();
    private static final Map<Level, Set<NetworkBridgePeripheral>> ATTACHED_PERIPHERALS = new WeakHashMap<>();
    private static final Set<Level> DIRTY_LEVELS = new HashSet<>();

    private AvionicsNetworkManager() {}

    public static NetworkSnapshot snapshot(Level level, BlockPos position) {
        if (level == null || level.isClientSide) return NetworkSnapshot.disconnected();
        BlockPos origin = position.immutable();
        synchronized (CACHE) {
            return CACHE.computeIfAbsent(level, ignored -> new HashMap<>()).computeIfAbsent(origin,
                    ignored -> NetworkDiscovery.discover(origin, new LevelAccess(level), ModConfig.MAX_AVIONICS_NETWORK_BLOCKS.get()));
        }
    }

    public static void register(NetworkBridgePeripheral peripheral, Level level) {
        if (level == null || level.isClientSide) return;
        synchronized (ATTACHED_PERIPHERALS) {
            ATTACHED_PERIPHERALS.computeIfAbsent(level, ignored -> new HashSet<>()).add(peripheral);
        }
    }

    public static void unregister(NetworkBridgePeripheral peripheral, Level level) {
        if (level == null || level.isClientSide) return;
        synchronized (ATTACHED_PERIPHERALS) {
            Set<NetworkBridgePeripheral> set = ATTACHED_PERIPHERALS.get(level);
            if (set != null) {
                set.remove(peripheral);
                if (set.isEmpty()) ATTACHED_PERIPHERALS.remove(level);
            }
        }
    }

    public static void markDirty(Level level) {
        if (level == null || level.isClientSide) return;
        synchronized (DIRTY_LEVELS) {
            DIRTY_LEVELS.add(level);
        }
    }

    public static void clearLevel(Level level) {
        synchronized (CACHE) { CACHE.remove(level); }
        synchronized (ATTACHED_PERIPHERALS) { ATTACHED_PERIPHERALS.remove(level); }
        synchronized (DIRTY_LEVELS) { DIRTY_LEVELS.remove(level); }
    }

    /** Called at server tick end; multiple block/chunk events are coalesced into one refresh. */
    public static void flushDirty() {
        Set<Level> dirty;
        synchronized (DIRTY_LEVELS) {
            dirty = Set.copyOf(DIRTY_LEVELS);
            DIRTY_LEVELS.clear();
        }
        for (Level level : dirty) {
            synchronized (CACHE) {
                CACHE.remove(level);
            }
            Set<NetworkBridgePeripheral> peripherals;
            synchronized (ATTACHED_PERIPHERALS) {
                peripherals = new HashSet<>(ATTACHED_PERIPHERALS.getOrDefault(level, Set.of()));
            }
            peripherals.forEach(NetworkBridgePeripheral::refreshNetworkAndNotify);
        }
    }

    private record LevelAccess(Level level) implements NetworkAccess {
        @Override public boolean isLoaded(BlockPos pos) { return level.hasChunkAt(pos); }
        @Override public boolean isCable(BlockPos pos) {
            return isLoaded(pos) && level.getBlockState(pos).is(ModBlocks.AVIONICS_CABLE.get());
        }
        @Override public String endpointType(BlockPos pos) {
            if (!isLoaded(pos)) return null;
            return level.getBlockEntity(pos) instanceof AvionicsEndpoint endpoint ? endpoint.avionicsType() : null;
        }
    }
}
