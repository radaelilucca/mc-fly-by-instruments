package com.radaeli.aeronauticsflybyinstruments.sensor;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;

import java.util.EnumMap;
import java.util.Map;

/** Persistent player-selected redstone ranges and Create Link frequency pairs for a sensor. */
public final class SensorOutputConfig {
    public enum Mode { FACES, LINK }
    public enum Channel { ALTITUDE, FRONT, LEFT, RIGHT }

    public record Range(int min, int max) {
        public boolean valid(int sensorMaxRange) {
            return min >= 0 && max <= sensorMaxRange && min < max;
        }
    }

    public record FrequencyPair(ItemStack first, ItemStack second) {
        public FrequencyPair {
            first = first == null ? ItemStack.EMPTY : first.copyWithCount(1);
            second = second == null ? ItemStack.EMPTY : second.copyWithCount(1);
        }
        @Override public ItemStack first() { return first.copy(); }
        @Override public ItemStack second() { return second.copy(); }
        public boolean enabled() { return !first.isEmpty() && !second.isEmpty(); }
        public boolean sameIdentity(FrequencyPair other) {
            return other != null && Frequency.of(first).equals(Frequency.of(other.first))
                    && Frequency.of(second).equals(Frequency.of(other.second));
        }
    }

    private Mode mode = Mode.FACES;
    private final EnumMap<Channel, Range> ranges = new EnumMap<>(Channel.class);
    private final EnumMap<Channel, FrequencyPair> frequencies = new EnumMap<>(Channel.class);

    public SensorOutputConfig(boolean advanced, boolean altimeter) {
        if (altimeter) ranges.put(Channel.ALTITUDE, new Range(0, 32));
        else {
            ranges.put(Channel.FRONT, new Range(4, 32));
            if (advanced) {
                ranges.put(Channel.LEFT, new Range(4, 32));
                ranges.put(Channel.RIGHT, new Range(4, 32));
            }
        }
        for (Channel channel : ranges.keySet()) frequencies.put(channel, new FrequencyPair(ItemStack.EMPTY, ItemStack.EMPTY));
    }

    public Mode mode() { return mode; }
    public void mode(Mode value) { mode = value; }
    /** Routing is global per sensor: any complete pair selects Link, otherwise local faces. */
    public void selectAutomaticMode() {
        mode = frequencies.values().stream().anyMatch(FrequencyPair::enabled) ? Mode.LINK : Mode.FACES;
    }
    public Range range(Channel channel) { return ranges.get(channel); }
    public Map<Channel, Range> ranges() { return Map.copyOf(ranges); }
    public FrequencyPair frequencies(Channel channel) { return frequencies.get(channel); }
    public void setRange(Channel channel, Range range) { if (ranges.containsKey(channel)) ranges.put(channel, range); }
    public void setFrequencies(Channel channel, FrequencyPair pair) { if (ranges.containsKey(channel)) frequencies.put(channel, pair); }

    public void clampToMaximum(int maximumRange) {
        int farLimit = Math.max(1, maximumRange);
        for (Channel channel : ranges.keySet()) {
            Range current = ranges.get(channel);
            int far = Math.min(current.max(), farLimit);
            int near = Math.min(current.min(), far - 1);
            ranges.put(channel, new Range(Math.max(0, near), far));
        }
    }

    public SensorOutputConfig copy() {
        SensorOutputConfig copy = new SensorOutputConfig(ranges.containsKey(Channel.LEFT), ranges.containsKey(Channel.ALTITUDE));
        copy.mode = mode;
        copy.ranges.clear(); copy.ranges.putAll(ranges);
        copy.frequencies.clear();
        frequencies.forEach((channel, pair) -> copy.frequencies.put(channel, new FrequencyPair(pair.first, pair.second)));
        return copy;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        root.putString("Mode", mode.name());
        CompoundTag channels = new CompoundTag();
        for (Channel channel : ranges.keySet()) {
            CompoundTag entry = new CompoundTag();
            Range range = ranges.get(channel);
            entry.putInt("Min", range.min()); entry.putInt("Max", range.max());
            FrequencyPair pair = frequencies.get(channel);
            if (!pair.first.isEmpty()) entry.put("FrequencyA", pair.first.save(registries));
            if (!pair.second.isEmpty()) entry.put("FrequencyB", pair.second.save(registries));
            channels.put(channel.name(), entry);
        }
        root.put("Channels", channels);
        return root;
    }

    public static SensorOutputConfig load(CompoundTag root, HolderLookup.Provider registries,
                                          boolean advanced, boolean altimeter, int maximumRange) {
        SensorOutputConfig result = new SensorOutputConfig(advanced, altimeter);
        if (root.contains("Mode", Tag.TAG_STRING)) {
            try { result.mode = Mode.valueOf(root.getString("Mode")); }
            catch (IllegalArgumentException ignored) { result.mode = Mode.FACES; }
        }
        CompoundTag channels = root.getCompound("Channels");
        for (Channel channel : result.ranges.keySet()) {
            if (!channels.contains(channel.name(), Tag.TAG_COMPOUND)) continue;
            CompoundTag entry = channels.getCompound(channel.name());
            Range loaded = new Range(entry.getInt("Min"), entry.getInt("Max"));
            if (loaded.valid(maximumRange)) result.ranges.put(channel, loaded);
            ItemStack first = readStack(entry, "FrequencyA", registries);
            ItemStack second = readStack(entry, "FrequencyB", registries);
            result.frequencies.put(channel, new FrequencyPair(first, second));
        }
        result.clampToMaximum(maximumRange);
        return result;
    }

    private static ItemStack readStack(CompoundTag entry, String key, HolderLookup.Provider registries) {
        if (!entry.contains(key, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        return ItemStack.parse(registries, entry.getCompound(key)).orElse(ItemStack.EMPTY);
    }

    public static int altitudePower(double altitude, Range range) {
        if (!Double.isFinite(altitude) || altitude <= range.min()) return 0;
        if (altitude >= range.max()) return 15;
        return clampPower((altitude - range.min()) * 15.0 / (range.max() - range.min()));
    }

    public static int proximityPower(double distance, Range range) {
        if (!Double.isFinite(distance) || distance >= range.max()) return 0;
        if (distance <= range.min()) return 15;
        return clampPower((range.max() - distance) * 15.0 / (range.max() - range.min()));
    }

    private static int clampPower(double value) { return Math.clamp((int) Math.round(value), 0, 15); }

    /** Maps the front/left/right channels to the outward redstone faces for the block's horizontal facing. */
    public static Channel channelForFace(Direction facing, Direction face) {
        if (face == facing) return Channel.FRONT;
        if (face == facing.getCounterClockWise()) return Channel.LEFT;
        if (face == facing.getClockWise()) return Channel.RIGHT;
        return null;
    }
}
