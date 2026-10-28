package com.radaeli.aeronauticsflybyinstruments.wire;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

/**
 * Where the wire meets each node, in block units relative to the node's own block. Must match the
 * Blockbench models: both are authored facing UP with the host block below.
 */
public final class WireAttachPoints {
    /** Height of the connector's gland (wire entry) above the host face, in pixels. */
    public static final int CONNECTOR_MOUTH = 4;
    /** Height of the wire centre above the host face on a support: midway between its base and cap. */
    public static final double SUPPORT_WIRE = 2;
    /** Lane spacing inside the support, in pixels. */
    public static final int LANE_SPACING = 2;

    private WireAttachPoints() {}

    /** Mouth of a connector on {@code facing} (pointing away from its host), shifted onto the host's socket. */
    public static Vec3 connector(Direction facing, Vec3 socketOffset) {
        return alongNormal(facing, CONNECTOR_MOUTH).add(socketOffset);
    }

    /** Wire centre on a support lane; lanes spread across the wire, centre first then either side. */
    public static Vec3 support(Direction facing, int lane, Vec3 wireDirection) {
        Vec3 point = alongNormal(facing, SUPPORT_WIRE);
        if (lane == 0) return point;
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 side = normal.cross(wireDirection);
        if (side.lengthSqr() < 1e-8) {
            // Wire runs along the support's own axis: spread the lanes on a fixed in-plane axis.
            side = facing.getAxis() == Direction.Axis.X ? new Vec3(0, 0, 1) : new Vec3(1, 0, 0);
        }
        double sign = lane == 1 ? -1 : 1;
        return point.add(side.normalize().scale(sign * LANE_SPACING / 16.0));
    }

    /**
     * In-face offset that moves a connector from the face centre onto a device socket. {@code socket} is in
     * host-local block units, as given by {@code CableSocketProvider} for the face toward the connector.
     */
    public static Vec3 socketOffset(Direction facing, Vec3 socket) {
        Vec3 faceCentre = new Vec3(.5 + .5 * facing.getStepX(), .5 + .5 * facing.getStepY(), .5 + .5 * facing.getStepZ());
        return socket.subtract(faceCentre);
    }

    private static Vec3 alongNormal(Direction facing, double pixels) {
        double distance = pixels / 16.0 - 0.5;
        return new Vec3(.5 + facing.getStepX() * distance, .5 + facing.getStepY() * distance,
                .5 + facing.getStepZ() * distance);
    }
}
