package com.radaeli.aeronauticsflybyinstruments.wire;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Centre line of a wire. Pure math, computed once per route change: a short straight stub out of each connector
 * that curves into the span, a shallow parabolic sag on every span (close to a catenary at these lengths) and
 * rounded bends instead of kinks.
 */
public final class WireGeometry {
    /** Only the part of the wire held by the gland is straight, along the face normal. */
    public static final double STRAIGHT = 1 / 16.0;
    /** Corner of the curve out of a connector: the wire never stands further off the face than this. */
    public static final double LEAD = 2 / 16.0;
    /** How far along the span the curve out of a connector ends. */
    public static final double LEAD_SWEEP = 3 / 16.0;
    /** Bend radius at supports. */
    public static final double BEND = 2 / 16.0;
    /** Shallow enough that a wire along a row of blocks stays within the blocks' height. */
    public static final double SAG_PER_BLOCK = 0.015;
    public static final double MAX_SAG = 0.3;
    public static final double STEP = 0.25;
    public static final int MAX_STEPS_PER_SPAN = 64;
    private static final int BEND_STEPS = 4;

    private WireGeometry() {}

    /** A point the wire passes through; {@code exit} is the face normal for connectors, null for supports. */
    public record Node(Vec3 point, @Nullable Vec3 exit) {}

    public static List<Vec3> centreLine(List<Node> nodes) {
        if (nodes.size() < 2) throw new IllegalArgumentException("A wire needs two nodes");
        var controls = new ArrayList<Vec3>();
        for (int i = 0; i < nodes.size(); i++) {
            var node = nodes.get(i);
            Vec3 lead = node.exit() == null ? null : node.point().add(node.exit().normalize().scale(LEAD));
            if (lead != null && i == nodes.size() - 1) controls.add(lead);
            controls.add(node.point());
            if (lead != null && i == 0) controls.add(lead);
        }
        boolean startLead = nodes.getFirst().exit() != null, endLead = nodes.getLast().exit() != null;

        // Leads are straight; every other span sags.
        var spans = new ArrayList<List<Vec3>>();
        for (int i = 0; i < controls.size() - 1; i++) {
            spans.add(isLead(i, controls.size(), startLead, endLead) ? List.of(controls.get(i), controls.get(i + 1))
                    : sagSpan(controls.get(i), controls.get(i + 1)));
        }

        var result = new ArrayList<Vec3>();
        List<Vec3> current = spans.getFirst();
        for (int i = 1; i < spans.size(); i++) {
            List<Vec3> next = spans.get(i);
            boolean leadIn = isLead(i - 1, controls.size(), startLead, endLead);
            boolean leadOut = isLead(i, controls.size(), startLead, endLead);
            double in, out;
            if (leadIn || leadOut) {
                // Out of a connector: straight only inside the gland, then a wide sweep into the span.
                double sweep = Math.min(LEAD_SWEEP, 0.45 * length(leadIn ? next : current));
                in = leadIn ? LEAD - STRAIGHT : sweep;
                out = leadOut ? LEAD - STRAIGHT : sweep;
            } else {
                in = out = Math.min(BEND, 0.45 * Math.min(length(current), length(next)));
            }
            var incoming = trimEnd(current, in);
            var outgoing = trimStart(next, out);
            result.addAll(incoming.subList(0, incoming.size() - 1));
            Vec3 from = incoming.getLast(), corner = controls.get(i), to = outgoing.getFirst();
            for (int step = 0; step < BEND_STEPS; step++) {
                double t = step / (double) BEND_STEPS;
                result.add(bezier(from, corner, to, t));
            }
            current = outgoing;
        }
        result.addAll(current);
        return dedupe(result);
    }

    private static boolean isLead(int span, int controls, boolean startLead, boolean endLead) {
        return (startLead && span == 0) || (endLead && span == controls - 2);
    }

    /** Sag depth for a span: proportional to its horizontal reach, zero when vertical. */
    public static double sag(Vec3 a, Vec3 b) {
        double horizontal = Math.hypot(b.x - a.x, b.z - a.z);
        return Math.min(MAX_SAG, SAG_PER_BLOCK * horizontal);
    }

    private static List<Vec3> sagSpan(Vec3 a, Vec3 b) {
        double sag = sag(a, b);
        double length = a.distanceTo(b);
        int steps = sag < 1e-4 ? 1 : Math.max(2, Math.min(MAX_STEPS_PER_SPAN, (int) Math.ceil(length / STEP)));
        var points = new ArrayList<Vec3>(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double t = i / (double) steps;
            points.add(a.lerp(b, t).subtract(0, 4 * sag * t * (1 - t), 0));
        }
        return points;
    }

    private static Vec3 bezier(Vec3 a, Vec3 control, Vec3 b, double t) {
        double u = 1 - t;
        return a.scale(u * u).add(control.scale(2 * u * t)).add(b.scale(t * t));
    }

    static double length(List<Vec3> points) {
        double total = 0;
        for (int i = 1; i < points.size(); i++) total += points.get(i - 1).distanceTo(points.get(i));
        return total;
    }

    /** Removes {@code distance} of arc length from the start of a polyline. */
    private static List<Vec3> trimStart(List<Vec3> points, double distance) {
        double remaining = distance;
        for (int i = 1; i < points.size(); i++) {
            double segment = points.get(i - 1).distanceTo(points.get(i));
            if (segment >= remaining && segment > 0) {
                var result = new ArrayList<Vec3>();
                result.add(points.get(i - 1).lerp(points.get(i), remaining / segment));
                result.addAll(points.subList(i, points.size()));
                return result;
            }
            remaining -= segment;
        }
        return List.of(points.getLast(), points.getLast());
    }

    private static List<Vec3> trimEnd(List<Vec3> points, double distance) {
        var reversed = new ArrayList<>(points);
        java.util.Collections.reverse(reversed);
        var trimmed = new ArrayList<>(trimStart(reversed, distance));
        java.util.Collections.reverse(trimmed);
        return trimmed;
    }

    private static List<Vec3> dedupe(List<Vec3> points) {
        var result = new ArrayList<Vec3>(points.size());
        for (var point : points) {
            if (result.isEmpty() || result.getLast().distanceToSqr(point) > 1e-10) result.add(point);
        }
        return List.copyOf(result);
    }
}
