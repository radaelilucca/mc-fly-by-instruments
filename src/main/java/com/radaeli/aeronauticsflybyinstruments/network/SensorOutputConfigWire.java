package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.sensor.SensorOutputConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.DyedItemColor;


/** Compact config codec: Create Link frequency identity consists only of item id and dyed RGB. */
final class SensorOutputConfigWire {
    private SensorOutputConfigWire() {}

    static void encode(ByteBuf buf, SensorOutputConfig config) {
        ByteBufCodecs.STRING_UTF8.encode(buf, config.mode().name());
        for (var channel : SensorOutputConfig.Channel.values()) {
            var range = config.range(channel);
            ByteBufCodecs.VAR_INT.encode(buf, range == null ? -1 : range.min());
            ByteBufCodecs.VAR_INT.encode(buf, range == null ? -1 : range.max());
            var pair = config.frequencies(channel);
            writeStackIdentity(buf, pair == null ? ItemStack.EMPTY : pair.first());
            writeStackIdentity(buf, pair == null ? ItemStack.EMPTY : pair.second());
        }
    }

    static SensorOutputConfig decode(ByteBuf buf, String kind) {
        boolean altimeter = kind.equals("altimeter");
        boolean advanced = kind.equals("advanced");
        SensorOutputConfig config = new SensorOutputConfig(advanced, altimeter);
        try { config.mode(SensorOutputConfig.Mode.valueOf(ByteBufCodecs.STRING_UTF8.decode(buf))); }
        catch (IllegalArgumentException ignored) { config.mode(SensorOutputConfig.Mode.FACES); }
        for (var channel : SensorOutputConfig.Channel.values()) {
            int min = ByteBufCodecs.VAR_INT.decode(buf), max = ByteBufCodecs.VAR_INT.decode(buf);
            ItemStack first = readStackIdentity(buf), second = readStackIdentity(buf);
            if (config.ranges().containsKey(channel) && min >= 0 && max > min) {
                config.setRange(channel, new SensorOutputConfig.Range(min, max));
                config.setFrequencies(channel, new SensorOutputConfig.FrequencyPair(first, second));
            }
        }
        return config;
    }

    static void encodePayload(ByteBuf buf, BlockPos position, String kind, int angle, String side,
                              SensorOutputConfig config) {
        BlockPos.STREAM_CODEC.encode(buf, position);
        ByteBufCodecs.STRING_UTF8.encode(buf, kind);
        ByteBufCodecs.VAR_INT.encode(buf, angle);
        ByteBufCodecs.STRING_UTF8.encode(buf, side);
        encode(buf, config);
    }

    static PayloadFields decodePayload(ByteBuf buf) {
        BlockPos position = BlockPos.STREAM_CODEC.decode(buf);
        String kind = ByteBufCodecs.STRING_UTF8.decode(buf);
        int angle = ByteBufCodecs.VAR_INT.decode(buf);
        String side = ByteBufCodecs.STRING_UTF8.decode(buf);
        return new PayloadFields(position, kind, angle, side, decode(buf, kind));
    }

    /** {@code side} is the scanner's aircraft side (see ScannerSide), empty for the altimeter. */
    record PayloadFields(BlockPos position, String kind, int angle, String side, SensorOutputConfig config) {}

    private static void writeStackIdentity(ByteBuf buf, ItemStack stack) {
        String item = stack.isEmpty() ? "" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        ByteBufCodecs.STRING_UTF8.encode(buf, item);
        var color = stack.get(DataComponents.DYED_COLOR);
        ByteBufCodecs.VAR_INT.encode(buf, color == null ? -1 : color.rgb());
    }

    private static ItemStack readStackIdentity(ByteBuf buf) {
        String item = ByteBufCodecs.STRING_UTF8.decode(buf);
        int color = ByteBufCodecs.VAR_INT.decode(buf);
        ResourceLocation id = ResourceLocation.tryParse(item);
        if (id == null || BuiltInRegistries.ITEM.getHolder(id).isEmpty()) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getHolder(id).orElseThrow());
        if (color >= 0 && color <= 0xFFFFFF) stack.set(DataComponents.DYED_COLOR, new DyedItemColor(color, false));
        return stack;
    }
}
