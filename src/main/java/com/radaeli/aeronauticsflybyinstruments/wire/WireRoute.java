package com.radaeli.aeronauticsflybyinstruments.wire;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;

/**
 * A placed wire: connector, zero or more supports, connector. Every node on the route keeps its own copy with
 * offsets relative to itself, so the data survives Sable assembly and contraption moves unchanged.
 */
public record WireRoute(UUID id, List<WireAnchor> anchors) {
    public static final Codec<WireRoute> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(WireRoute::id),
            WireAnchor.CODEC.listOf().fieldOf("anchors").forGetter(WireRoute::anchors)).apply(instance, WireRoute::new));

    public WireRoute {
        anchors = List.copyOf(anchors);
        if (anchors.size() < 2) throw new IllegalArgumentException("A wire needs two connectors");
        var seen = new HashSet<BlockPos>();
        for (var anchor : anchors) {
            if (!seen.add(anchor.offset())) throw new IllegalArgumentException("Repeated anchor " + anchor.offset());
        }
    }

    /** Builds a route from absolute positions, stored relative to its first connector. */
    public static WireRoute of(UUID id, List<BlockPos> positions, List<Integer> lanes) {
        BlockPos origin = positions.getFirst();
        var anchors = new ArrayList<WireAnchor>(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            anchors.add(new WireAnchor(positions.get(i).subtract(origin), lanes.get(i)));
        }
        return new WireRoute(id, anchors);
    }

    /** The same route as seen from the anchor at {@code offset} (relative to the current holder). */
    public WireRoute rebasedTo(BlockPos offset) {
        return new WireRoute(id, anchors.stream()
                .map(anchor -> new WireAnchor(anchor.offset().subtract(offset), anchor.lane())).toList());
    }

    /** Rotates/mirrors offsets when a contraption is placed back in a different orientation. */
    public WireRoute transformed(UnaryOperator<BlockPos> rotation) {
        return new WireRoute(id, anchors.stream()
                .map(anchor -> new WireAnchor(rotation.apply(anchor.offset()), anchor.lane())).toList());
    }

    public boolean ownedByHolder() {
        return anchors.getFirst().offset().equals(BlockPos.ZERO);
    }

    @Nullable
    public WireAnchor self() {
        for (var anchor : anchors) if (anchor.offset().equals(BlockPos.ZERO)) return anchor;
        return null;
    }

    /** Offset of the opposite connector when the holder is one of the two ends; supports have none. */
    @Nullable
    public BlockPos farEnd() {
        if (anchors.getFirst().offset().equals(BlockPos.ZERO)) return anchors.getLast().offset();
        if (anchors.getLast().offset().equals(BlockPos.ZERO)) return anchors.getFirst().offset();
        return null;
    }

    /** Straight-line length between anchor block centres, used for cost and limits. */
    public double length() {
        return length(anchors.stream().map(WireAnchor::offset).toList());
    }

    public static double length(List<BlockPos> positions) {
        double total = 0;
        for (int i = 1; i < positions.size(); i++) {
            total += Math.sqrt(positions.get(i - 1).distSqr(positions.get(i)));
        }
        return total;
    }
}
