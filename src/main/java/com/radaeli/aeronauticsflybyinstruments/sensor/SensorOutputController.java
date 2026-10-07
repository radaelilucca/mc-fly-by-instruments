package com.radaeli.aeronauticsflybyinstruments.sensor;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/** Owns signal transitions and Create Link endpoint lifecycle for one sensor block entity. */
public final class SensorOutputController {
    private final BlockEntity owner;
    private final boolean altimeter;
    private final boolean advanced;
    private SensorOutputConfig config;
    private EnumMap<SensorOutputConfig.Channel, Integer> outputs = new EnumMap<>(SensorOutputConfig.Channel.class);
    private final EnumMap<SensorOutputConfig.Channel, Endpoint> endpoints = new EnumMap<>(SensorOutputConfig.Channel.class);

    public SensorOutputController(BlockEntity owner, boolean altimeter, boolean advanced, int maximumRange) {
        this.owner = owner;
        this.altimeter = altimeter;
        this.advanced = advanced;
        this.config = new SensorOutputConfig(advanced, altimeter);
        config.clampToMaximum(maximumRange);
        config.ranges().keySet().forEach(channel -> outputs.put(channel, 0));
    }

    public SensorOutputConfig config() { return config.copy(); }
    public int power(SensorOutputConfig.Channel channel) { return outputs.getOrDefault(channel, 0); }
    public Map<SensorOutputConfig.Channel, Integer> outputs() { return Map.copyOf(outputs); }

    public boolean apply(SensorOutputConfig updated, int maximumRange) {
        if (!valid(updated, maximumRange)) return false;
        updated = updated.copy();
        updated.selectAutomaticMode();
        var old = config;
        if (same(old, updated)) return false;
        unregisterAll();
        config = updated.copy();
        owner.setChanged();
        syncBlock();
        refreshLinks();
        return true;
    }

    public boolean setRange(SensorOutputConfig.Channel channel, int min, int max, int maximumRange) {
        if (!config.ranges().containsKey(channel)) return false;
        SensorOutputConfig.Range range = new SensorOutputConfig.Range(min, max);
        if (!range.valid(maximumRange)) return false;
        if (range.equals(config.range(channel))) return true;
        SensorOutputConfig next = config.copy(); next.setRange(channel, range);
        return apply(next, maximumRange);
    }

    public boolean setMode(String name, int maximumRange) {
        try { SensorOutputConfig.Mode.valueOf(name.toUpperCase(java.util.Locale.ROOT)); }
        catch (RuntimeException exception) { return false; }
        // Retain the legacy CC method without allowing it to override automatic routing.
        return true;
    }

    public boolean setFrequencies(SensorOutputConfig.Channel channel, ItemStack first, ItemStack second,
                                  int maximumRange) {
        if (!config.ranges().containsKey(channel)) return false;
        var replacement = new SensorOutputConfig.FrequencyPair(first, second);
        if (replacement.sameIdentity(config.frequencies(channel))) return true;
        SensorOutputConfig next = config.copy();
        next.setFrequencies(channel, replacement);
        return apply(next, maximumRange);
    }

    public boolean valid(SensorOutputConfig value, int maximumRange) {
        if (value == null || value.mode() == null || !value.ranges().keySet().equals(config.ranges().keySet())) return false;
        for (var entry : value.ranges().entrySet()) {
            if (entry.getValue() == null || !entry.getValue().valid(maximumRange)) return false;
            if (value.frequencies(entry.getKey()) == null) return false;
        }
        return true;
    }

    public boolean acceptAltitude(SensorOutputLogic.SensorReadingView reading) {
        if (!altimeter) return false;
        return transition(SensorOutputLogic.altitude(reading, config));
    }

    public boolean acceptTerrain(TerrainScanSnapshot reading, int openingAngle) {
        if (altimeter) return false;
        return transition(SensorOutputLogic.terrain(reading, config, advanced, openingAngle));
    }

    public int weakPower(Direction face) {
        Direction facing = owner.getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                ? owner.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        return SensorOutputLogic.weakPower(outputs, config, altimeter, advanced, facing, face);
    }

    public void onLoad() { refreshLinks(); }
    public void onChunkUnloaded() { unregisterAll(); }
    public void onRemoved() { unregisterAll(); }

    private boolean transition(Map<SensorOutputConfig.Channel, Integer> nextValues) {
        boolean changed = false;
        Set<LinkKey> refresh = new HashSet<>();
        for (SensorOutputConfig.Channel channel : nextValues.keySet()) {
            int next = nextValues.get(channel);
            int previous = outputs.getOrDefault(channel, 0);
            if (previous == next) continue;
            outputs.put(channel, next);
            changed = true;
            Endpoint endpoint = endpoints.get(channel);
            if (endpoint != null) refresh.add(endpoint.key);
        }
        if (!changed) return false;
        notifyRedstoneNeighbors();
        Level level = owner.getLevel();
        if (level != null && !level.isClientSide) {
            for (LinkKey key : refresh) {
                Endpoint representative = endpoints.values().stream()
                        .filter(endpoint -> endpoint.key.equals(key)).findFirst().orElse(null);
                if (representative != null) Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, representative.linkable);
            }
        }
        return true;
    }

    private void refreshLinks() {
        if (owner.getLevel() == null || owner.getLevel().isClientSide) return;
        Set<LinkKey> addedKeys = new HashSet<>();
        for (var entry : config.ranges().entrySet()) {
            var pair = config.frequencies(entry.getKey());
            if (config.mode() != SensorOutputConfig.Mode.LINK || pair == null || !pair.enabled()) continue;
            Endpoint endpoint = new Endpoint(entry.getKey(), pair);
            endpoints.put(entry.getKey(), endpoint);
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(owner.getLevel(), endpoint.linkable);
            addedKeys.add(endpoint.key);
        }
        for (LinkKey key : addedKeys) updateNetwork(key);
    }

    private void unregisterAll() {
        Level level = owner.getLevel();
        if (level != null && !level.isClientSide) {
            Set<LinkKey> keys = new HashSet<>();
            for (Endpoint endpoint : endpoints.values()) {
                keys.add(endpoint.key);
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, endpoint.linkable);
            }
            for (LinkKey key : keys) updateNetwork(key);
        }
        endpoints.clear();
    }

    private void updateNetwork(LinkKey key) {
        Endpoint representative = endpoints.values().stream()
                .filter(endpoint -> endpoint.key.equals(key)).findFirst().orElse(null);
        if (representative != null && owner.getLevel() != null && !owner.getLevel().isClientSide) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(owner.getLevel(), representative.linkable);
        }
    }

    private void syncBlock() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) return;
        BlockPos pos = owner.getBlockPos();
        level.updateNeighborsAt(pos, owner.getBlockState().getBlock());
        level.sendBlockUpdated(pos, owner.getBlockState(), owner.getBlockState(), 3);
    }

    private void notifyRedstoneNeighbors() {
        Level level = owner.getLevel();
        if (level != null && !level.isClientSide && config.mode() == SensorOutputConfig.Mode.FACES) {
            BlockPos pos = owner.getBlockPos();
            level.updateNeighborsAt(pos, owner.getBlockState().getBlock());
        }
    }

    private static boolean same(SensorOutputConfig a, SensorOutputConfig b) {
        if (a.mode() != b.mode() || !a.ranges().equals(b.ranges())) return false;
        for (var channel : a.ranges().keySet()) if (!a.frequencies(channel).sameIdentity(b.frequencies(channel))) return false;
        return true;
    }

    private record LinkKey(Frequency first, Frequency second) {}

    private final class Endpoint {
        private final SensorOutputConfig.Channel channel;
        private final LinkKey key;
        private final IRedstoneLinkable linkable;
        private final Object networkKey;
        private Endpoint(SensorOutputConfig.Channel channel, SensorOutputConfig.FrequencyPair pair) {
            this.channel = channel;
            key = new LinkKey(Frequency.of(pair.first()), Frequency.of(pair.second()));
            networkKey = createCouple(key.first, key.second);
            linkable = (IRedstoneLinkable) Proxy.newProxyInstance(IRedstoneLinkable.class.getClassLoader(),
                    new Class<?>[]{IRedstoneLinkable.class}, this::invoke);
        }
        private Object invoke(Object proxy, Method method, Object[] args) {
            return switch (method.getName()) {
                case "getTransmittedStrength" -> outputs.getOrDefault(channel, 0);
                case "setReceivedStrength" -> null;
                case "isListening" -> false;
                case "isAlive" -> owner.getLevel() != null && !owner.isRemoved();
                case "getNetworkKey" -> networkKey;
                case "getLocation" -> owner.getBlockPos();
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == (args == null ? null : args[0]);
                case "toString" -> "AeronauticsSensorLink[" + owner.getBlockPos() + "," + channel + "]";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }
        private Object createCouple(Frequency first, Frequency second) {
            try {
                Class<?> couple = Class.forName("net.createmod.catnip.data.Couple");
                for (Method method : couple.getMethods()) {
                    if (method.getName().equals("create") && java.lang.reflect.Modifier.isStatic(method.getModifiers())
                            && method.getParameterCount() == 2) return method.invoke(null, first, second);
                }
                throw new NoSuchMethodException("Couple.create");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Create Catnip Couple API is unavailable", exception);
            }
        }
    }
}
