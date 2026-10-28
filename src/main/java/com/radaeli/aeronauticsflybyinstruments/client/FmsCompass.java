package com.radaeli.aeronauticsflybyinstruments.client;

import net.minecraft.world.phys.Vec3;

/**
 * Compass rose shown on the FMS lens while a waypoint is locked, in the style of a heading
 * indicator: the card turns so the FMS heading sits at the top index, the centre triangle is the
 * FMS itself, and a brass dot on the ring marks the bearing to the waypoint.
 * Pure geometry in lens units (x right, y up, card radius 1), precomputed once; no game state.
 */
final class FmsCompass {
    /** Degree ticks every 10 degrees, plus the four intercardinal points (45, 135, ...). */
    static final int DEGREE_STEP = 10;
    static final int TICKS = 360 / DEGREE_STEP + 4;
    /** Cardinal letters at 0, 90, 180 and 270 degrees; intercardinals only get the long tick. */
    static final String LETTERS = "NESW";
    /** Seconds the boot-in takes after the lock. */
    static final double INTRO_SECONDS = 0.5;

    // Tick lengths and widths as a fraction of the card radius.
    static final float DEGREE_LENGTH = 0.065f, POINT_LENGTH = 0.2f;
    static final float DEGREE_WIDTH = 0.028f, POINT_WIDTH = 0.045f;

    /** Precomputed ticks: card angle (sin, cos clockwise from north), inner radius and width. */
    static final float[] TICK_SIN = new float[TICKS], TICK_COS = new float[TICKS];
    static final float[] TICK_INNER = new float[TICKS], TICK_WIDTH = new float[TICKS];

    /**
     * Letters as pixel glyphs in the style of the Minecraft font (5 x 7), drawn as geometry with the
     * ticks: the font's own text pipeline z-fights with the lens at some camera angles and shaders.
     */
    static final int GLYPH_WIDTH = 5, GLYPH_HEIGHT = 7;
    private static final String[][] GLYPHS = {
            {"X...X", "XX..X", "X.X.X", "X..XX", "X...X", "X...X", "X...X"},
            {"XXXXX", "X....", "XXXX.", "X....", "X....", "X....", "XXXXX"},
            {".XXXX", "X....", ".XXX.", "....X", "....X", "X...X", ".XXX."},
            {"X...X", "X...X", "X...X", "X.X.X", "X.X.X", "XX.XX", "X...X"},
    };
    /** Per letter: runs of lit pixels as rectangles {u0, v0, u1, v1} (u right, v down), merged by row. */
    static final float[][] LETTER_RECTS = new float[LETTERS.length()][];

    static {
        int tick = 0;
        for (int degrees = 0; degrees < 360; degrees += 5) {
            boolean point = degrees % 45 == 0;
            if (degrees % DEGREE_STEP != 0 && !point) continue;
            double angle = Math.toRadians(degrees);
            TICK_SIN[tick] = (float) Math.sin(angle);
            TICK_COS[tick] = (float) Math.cos(angle);
            TICK_INNER[tick] = 1 - (point ? POINT_LENGTH : DEGREE_LENGTH);
            TICK_WIDTH[tick] = point ? POINT_WIDTH : DEGREE_WIDTH;
            tick++;
        }
        for (int letter = 0; letter < GLYPHS.length; letter++) {
            var rects = new java.util.ArrayList<Float>();
            for (int v = 0; v < GLYPH_HEIGHT; v++) {
                String row = GLYPHS[letter][v];
                for (int u = 0; u < GLYPH_WIDTH; u++) {
                    if (row.charAt(u) != 'X') continue;
                    int end = u;
                    while (end + 1 < GLYPH_WIDTH && row.charAt(end + 1) == 'X') end++;
                    rects.add((float) u); rects.add((float) v); rects.add((float) end + 1); rects.add((float) v + 1);
                    u = end;
                }
            }
            LETTER_RECTS[letter] = new float[rects.size()];
            for (int i = 0; i < rects.size(); i++) LETTER_RECTS[letter][i] = rects.get(i);
        }
    }

    private FmsCompass() {}

    /**
     * Heading in radians, clockwise from north (Minecraft: north is -Z, east is +X).
     * @param screenUp     world direction of the top of the FMS screen
     * @param screenNormal world direction the screen faces
     * Uses the top of the screen when the panel lies flat; on a wall or a steep tilt the top of the
     * screen points up, so the heading is the way into the panel, where a pilot facing it looks.
     */
    static double heading(Vec3 screenUp, Vec3 screenNormal) {
        double x = screenUp.x, z = screenUp.z;
        if (x * x + z * z < 0.25) {
            x = -screenNormal.x;
            z = -screenNormal.z;
        }
        return Math.atan2(x, -z);
    }

    /** Bearing in radians from one world point to another, clockwise from north. */
    static double bearing(double fromX, double fromZ, double toX, double toZ) {
        return Math.atan2(toX - fromX, -(toZ - fromZ));
    }

    /** Boot-in progress 0..1 with an ease-out, from the moment the waypoint was locked. */
    static float intro(double secondsSinceLock) {
        double t = Math.max(0, Math.min(1, secondsSinceLock / INTRO_SECONDS));
        return (float) (1 - Math.pow(1 - t, 3));
    }
}
