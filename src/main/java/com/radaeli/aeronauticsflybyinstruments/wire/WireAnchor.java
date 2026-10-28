package com.radaeli.aeronauticsflybyinstruments.wire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

/** One point a wire passes through: a connector (route ends) or a support lane, relative to the holder. */
public record WireAnchor(BlockPos offset, int lane) {
    public static final Codec<WireAnchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("offset").forGetter(WireAnchor::offset),
            Codec.INT.optionalFieldOf("lane", 0).forGetter(WireAnchor::lane)).apply(instance, WireAnchor::new));

    public WireAnchor {
        offset = offset.immutable();
        if (lane < 0 || lane >= WireRules.SUPPORT_LANES) throw new IllegalArgumentException("lane " + lane);
    }
}
