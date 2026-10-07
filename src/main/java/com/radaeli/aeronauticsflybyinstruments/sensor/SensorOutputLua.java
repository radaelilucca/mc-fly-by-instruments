package com.radaeli.aeronauticsflybyinstruments.sensor;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** CC:Tweaked table conversion shared by all redstone-capable sensor peripherals. */
final class SensorOutputLua {
    private SensorOutputLua() {}

    static Map<String, Object> configuration(SensorOutputConfig config) {
        Map<String, Object> ranges = new HashMap<>();
        Map<String, Object> frequencies = new HashMap<>();
        for (var channel : config.ranges().keySet()) {
            var range = config.range(channel);
            ranges.put(name(channel), Map.of("min", range.min(), "max", range.max()));
            var pair = config.frequencies(channel);
            frequencies.put(name(channel), Map.of("first", frequency(pair.first()), "second", frequency(pair.second())));
        }
        return Map.of("mode", config.mode().name().toLowerCase(Locale.ROOT), "ranges", Map.copyOf(ranges),
                "frequencies", Map.copyOf(frequencies));
    }

    static Map<String, Object> outputs(Map<SensorOutputConfig.Channel, Integer> outputs) {
        Map<String, Object> result = new HashMap<>();
        outputs.forEach((channel, power) -> result.put(name(channel), power));
        return Map.copyOf(result);
    }

    static String name(SensorOutputConfig.Channel channel) { return channel.name().toLowerCase(Locale.ROOT); }

    static SensorOutputConfig.Channel channel(String value) {
        try { return SensorOutputConfig.Channel.valueOf(value.toUpperCase(Locale.ROOT)); }
        catch (RuntimeException exception) { return null; }
    }

    static ItemStack frequency(Object value) {
        if (!(value instanceof Map<?, ?> table)) return null;
        Object item = table.get("item");
        if (item == null && table.isEmpty()) return ItemStack.EMPTY;
        if (!(item instanceof String id)) return null;
        if (id.isBlank()) return ItemStack.EMPTY;
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null) return null;
        var holder = BuiltInRegistries.ITEM.getHolder(key);
        if (holder.isEmpty()) return null;
        ItemStack stack = new ItemStack(holder.get());
        Object colorValue = table.get("color");
        if (colorValue != null) {
            if (!(colorValue instanceof Number number)) return null;
            int rgb = number.intValue();
            if (rgb < 0 || rgb > 0xFFFFFF) return null;
            stack.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(rgb, false));
        }
        return stack;
    }

    private static Map<String, Object> frequency(ItemStack stack) {
        if (stack.isEmpty()) return Map.of("item", "");
        Map<String, Object> result = new HashMap<>();
        result.put("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        Optional.ofNullable(stack.get(DataComponents.DYED_COLOR)).ifPresent(color -> result.put("color", color.rgb()));
        return Map.copyOf(result);
    }
}
