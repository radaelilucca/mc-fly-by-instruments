package com.radaeli.aeronauticsflybyinstruments.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WireGeometryTest {
    private static final Vec3 UP = new Vec3(0, 1, 0);

    @Test
    void endsExactlyAtBothConnectorMouths() {
        var a = new Vec3(0.5, 0.25, 0.5);
        var b = new Vec3(8.5, 0.25, 0.5);
        var line = WireGeometry.centreLine(List.of(new WireGeometry.Node(a, UP), new WireGeometry.Node(b, UP)));
        assertEquals(a, line.getFirst());
        assertEquals(b, line.getLast());
    }

    @Test
    void leavesTheConnectorAlongTheFaceNormal() {
        var a = new Vec3(0.5, 0.25, 0.5);
        var line = WireGeometry.centreLine(List.of(new WireGeometry.Node(a, new Vec3(1, 0, 0)),
                new WireGeometry.Node(new Vec3(6, -2, 3), UP)));
        var tangent = line.get(1).subtract(line.get(0)).normalize();
        assertEquals(1.0, tangent.x, 1e-9);
    }

    @Test
    void onlyTheGlandHoldsTheWireStraight() {
        var mouth = new Vec3(0.5, 0.5, 0.5);
        var line = WireGeometry.centreLine(List.of(new WireGeometry.Node(mouth, new Vec3(1, 0, 0)),
                new WireGeometry.Node(new Vec3(0.5, -3, 0.5), UP)));
        assertEquals(mouth.add(WireGeometry.STRAIGHT, 0, 0), line.get(1));
        // The curve that follows never stands off the face further than the lead corner.
        line.forEach(point -> assertTrue(point.x <= mouth.x + WireGeometry.LEAD + 1e-9));
        assertTrue(line.get(2).y < mouth.y, "already turning right after the gland");
    }

    @Test
    void aWireAlongARowOfBlocksStaysWithinTheirHeight() {
        // Connectors mid-height on the side faces of a 1-block-tall beam, the longest span allowed.
        var a = new Vec3(0, 0.5, 0.5);
        var b = new Vec3(24, 0.5, 0.5);
        var line = WireGeometry.centreLine(List.of(new WireGeometry.Node(a, new Vec3(-1, 0, 0)),
                new WireGeometry.Node(b, new Vec3(1, 0, 0))));
        double lowest = line.stream().mapToDouble(Vec3::y).min().orElseThrow();
        assertTrue(lowest - 0.5 / 16 > 0, "belly " + lowest + " drops below the beam");
    }

    @Test
    void sagsBelowTheStraightLineAndStaysBounded() {
        var a = new Vec3(0, 0, 0);
        var b = new Vec3(10, 0, 0);
        var line = WireGeometry.centreLine(List.of(new WireGeometry.Node(a, null), new WireGeometry.Node(b, null)));
        double lowest = line.stream().mapToDouble(Vec3::y).min().orElseThrow();
        assertEquals(-WireGeometry.sag(a, b), lowest, 0.02);
        assertTrue(lowest >= -WireGeometry.MAX_SAG - 1e-9);
        assertTrue(line.size() <= WireGeometry.MAX_STEPS_PER_SPAN + 1);
    }

    @Test
    void verticalSpanIsStraightAndFinite() {
        var line = WireGeometry.centreLine(List.of(new WireGeometry.Node(new Vec3(0, 0, 0), null),
                new WireGeometry.Node(new Vec3(0, 5, 0), null)));
        assertEquals(0, WireGeometry.sag(new Vec3(0, 0, 0), new Vec3(0, 5, 0)));
        line.forEach(point -> assertTrue(Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z)));
        line.forEach(point -> assertEquals(0, point.x, 1e-9));
    }

    @Test
    void roundsTheBendAtASupportWithoutKinks() {
        var line = WireGeometry.centreLine(List.of(
                new WireGeometry.Node(new Vec3(0, 0, 0), UP),
                new WireGeometry.Node(new Vec3(4, 0, 0), null),
                new WireGeometry.Node(new Vec3(4, 0, 4), UP)));
        for (int i = 2; i < line.size(); i++) {
            var before = line.get(i - 1).subtract(line.get(i - 2)).normalize();
            var after = line.get(i).subtract(line.get(i - 1)).normalize();
            assertTrue(before.dot(after) > Math.cos(Math.toRadians(50)), "Turn too sharp at point " + i);
        }
    }

    @Test
    void supportLanesSpreadAcrossTheWireAndNeverOverlap() {
        var direction = new Vec3(1, 0, 0);
        var centre = WireAttachPoints.support(Direction.UP, 0, direction);
        var left = WireAttachPoints.support(Direction.UP, 1, direction);
        var right = WireAttachPoints.support(Direction.UP, 2, direction);
        assertEquals(2 / 16.0, centre.distanceTo(left), 1e-9);
        assertEquals(2 / 16.0, centre.distanceTo(right), 1e-9);
        assertEquals(0, left.subtract(centre).dot(direction), 1e-9, "lanes are perpendicular to the wire");
        var alongAxis = WireAttachPoints.support(Direction.UP, 1, new Vec3(0, 1, 0));
        assertTrue(Double.isFinite(alongAxis.x) && alongAxis.distanceTo(centre) > 0);
    }

    @Test
    void attachPointsMatchTheModelsOnEveryFace() {
        assertEquals(new Vec3(.5, 4 / 16.0, .5), WireAttachPoints.connector(Direction.UP, Vec3.ZERO));
        assertEquals(new Vec3(.5, 1 - 4 / 16.0, .5), WireAttachPoints.connector(Direction.DOWN, Vec3.ZERO));
        assertEquals(new Vec3(.5, .5, 1 - 4 / 16.0), WireAttachPoints.connector(Direction.NORTH, Vec3.ZERO));
        assertEquals(new Vec3(4 / 16.0, .5, .5), WireAttachPoints.connector(Direction.EAST, Vec3.ZERO));
        assertEquals(new Vec3(.5, 2 / 16.0, .5), WireAttachPoints.support(Direction.UP, 0, new Vec3(1, 0, 0)));
    }

    @Test
    void socketOffsetMovesTheConnectorWithinTheFace() {
        // Radar Altimeter NORTH socket: (8, 9, 0) px on the device.
        var offset = WireAttachPoints.socketOffset(Direction.NORTH, new Vec3(8, 9, 0).scale(1 / 16.0));
        assertEquals(new Vec3(0, 1 / 16.0, 0), offset);
    }

    @Test
    void layoutUsesBlockCentresForUnloadedNodes() {
        var nodes = WireLayout.nodes(List.of(
                new WireLayout.Anchor(BlockPos.ZERO, Direction.UP, true, 0, Vec3.ZERO, true),
                WireLayout.Anchor.unknown(new BlockPos(5, 0, 0), true)));
        assertEquals(new Vec3(5.5, .5, .5), nodes.getLast().point());
        assertNull(nodes.getLast().exit());
        assertEquals(UP, nodes.getFirst().exit());
    }
}
