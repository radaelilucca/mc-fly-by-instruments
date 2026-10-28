package com.radaeli.aeronauticsflybyinstruments.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Turns a route plus the state of each node block into the nodes {@link WireGeometry} draws through. */
public final class WireLayout {
    private WireLayout() {}

    /**
     * A node block on the route: where it is (relative to the drawing origin), which way it faces, and
     * for connectors the socket shift. Unknown nodes (unloaded) fall back to the block centre.
     */
    public record Anchor(BlockPos offset, Direction facing, boolean connector, int lane, Vec3 socketOffset,
                         boolean known) {
        public static Anchor unknown(BlockPos offset, boolean connector) {
            return new Anchor(offset, Direction.UP, connector, 0, Vec3.ZERO, false);
        }
    }

    public static List<WireGeometry.Node> nodes(List<Anchor> anchors) {
        var nodes = new ArrayList<WireGeometry.Node>(anchors.size());
        for (int i = 0; i < anchors.size(); i++) {
            var anchor = anchors.get(i);
            Vec3 base = Vec3.atLowerCornerOf(anchor.offset());
            if (!anchor.known()) {
                nodes.add(new WireGeometry.Node(base.add(.5, .5, .5), null));
                continue;
            }
            if (anchor.connector()) {
                nodes.add(new WireGeometry.Node(base.add(WireAttachPoints.connector(anchor.facing(), anchor.socketOffset())),
                        Vec3.atLowerCornerOf(anchor.facing().getNormal())));
            } else {
                Vec3 previous = Vec3.atLowerCornerOf(anchors.get(Math.max(0, i - 1)).offset());
                Vec3 next = Vec3.atLowerCornerOf(anchors.get(Math.min(anchors.size() - 1, i + 1)).offset());
                nodes.add(new WireGeometry.Node(base.add(WireAttachPoints.support(anchor.facing(), anchor.lane(),
                        next.subtract(previous))), null));
            }
        }
        return nodes;
    }
}
