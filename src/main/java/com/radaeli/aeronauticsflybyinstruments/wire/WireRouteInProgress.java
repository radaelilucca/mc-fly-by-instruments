package com.radaeli.aeronauticsflybyinstruments.wire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/** Item component on the Avionics Wire while the player is routing: absolute node positions and lanes. */
public record WireRouteInProgress(List<BlockPos> positions, List<Integer> lanes) {
    public static final Codec<WireRouteInProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.listOf().fieldOf("positions").forGetter(WireRouteInProgress::positions),
            Codec.INT.listOf().fieldOf("lanes").forGetter(WireRouteInProgress::lanes))
            .apply(instance, WireRouteInProgress::new));
    public static final StreamCodec<ByteBuf, WireRouteInProgress> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), WireRouteInProgress::positions,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), WireRouteInProgress::lanes,
            WireRouteInProgress::new);

    public WireRouteInProgress {
        positions = List.copyOf(positions);
        lanes = List.copyOf(lanes);
        if (positions.size() != lanes.size() || positions.isEmpty()) throw new IllegalArgumentException("route");
    }

    public static WireRouteInProgress start(BlockPos connector) {
        return new WireRouteInProgress(List.of(connector.immutable()), List.of(0));
    }

    public WireRouteInProgress append(BlockPos pos, int lane) {
        var p = new ArrayList<>(positions);
        var l = new ArrayList<>(lanes);
        p.add(pos.immutable());
        l.add(lane);
        return new WireRouteInProgress(p, l);
    }

    /** Without the last point, or null when only the start connector was left. */
    public WireRouteInProgress undo() {
        if (positions.size() <= 1) return null;
        return new WireRouteInProgress(positions.subList(0, positions.size() - 1), lanes.subList(0, lanes.size() - 1));
    }

    public BlockPos last() {
        return positions.getLast();
    }
}
