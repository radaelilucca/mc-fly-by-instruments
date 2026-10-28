package com.radaeli.aeronauticsflybyinstruments.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Reads the node blocks of a route from the level and builds its centre line, in block units from the holder. */
public final class WirePath {
    private WirePath() {}

    public static List<Vec3> centreLine(Level level, BlockPos holder, WireRoute route) {
        var anchors = new ArrayList<WireLayout.Anchor>(route.anchors().size());
        for (int i = 0; i < route.anchors().size(); i++) {
            var anchor = route.anchors().get(i);
            boolean end = i == 0 || i == route.anchors().size() - 1;
            anchors.add(anchor(level, holder, anchor.offset(), end, anchor.lane()));
        }
        return WireGeometry.centreLine(WireLayout.nodes(anchors));
    }

    /** Centre line through absolute positions (route in progress), in absolute block coordinates. */
    public static List<Vec3> centreLine(Level level, List<BlockPos> positions, List<Integer> lanes) {
        var anchors = new ArrayList<WireLayout.Anchor>(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            boolean end = i == 0 || i == positions.size() - 1;
            anchors.add(anchor(level, BlockPos.ZERO, positions.get(i), end, lanes.get(i)));
        }
        return WireGeometry.centreLine(WireLayout.nodes(anchors));
    }

    /** Route in progress plus a loose end at {@code freeEnd} (absolute coordinates), for the placement preview. */
    public static List<Vec3> previewLine(Level level, List<BlockPos> positions, List<Integer> lanes, Vec3 freeEnd) {
        var anchors = new ArrayList<WireLayout.Anchor>(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            anchors.add(anchor(level, BlockPos.ZERO, positions.get(i), i == 0, lanes.get(i)));
        }
        var nodes = new ArrayList<>(WireLayout.nodes(anchors));
        nodes.add(new WireGeometry.Node(freeEnd, null));
        return WireGeometry.centreLine(nodes);
    }

    static WireLayout.Anchor anchor(Level level, BlockPos origin, BlockPos offset, boolean connector, int lane) {
        BlockPos pos = origin.offset(offset);
        if (!level.isLoaded(pos)) return WireLayout.Anchor.unknown(offset, connector);
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof WireNodeBlock)) return WireLayout.Anchor.unknown(offset, connector);
        boolean isConnector = state.getBlock() instanceof WireConnectorBlock;
        return new WireLayout.Anchor(offset, state.getValue(WireNodeBlock.FACING), isConnector, lane,
                WireNodeBlock.socketOffset(level, pos, state), true);
    }
}
