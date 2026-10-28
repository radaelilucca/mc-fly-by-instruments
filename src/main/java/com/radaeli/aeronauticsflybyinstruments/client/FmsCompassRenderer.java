package com.radaeli.aeronauticsflybyinstruments.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

/**
 * Draws {@link FmsCompass} on the FMS lens: black lens, white card and letters, brass waypoint dot,
 * all in one flat pass (the letters too, so they never fight the lens for depth).
 * Unlit (reads like a lit display in any light), no allocation per frame, and every size is fixed
 * in model units so the card keeps the same proportions as the brass frame around it.
 */
final class FmsCompassRenderer {
    /** Unlit flat colour, alpha blended, no depth write, offset above the lens to avoid z-fighting. */
    private static final RenderType FLAT = RenderType.create("aeronautics_fly_by_instruments_fms_compass",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                    .createCompositeState(false));

    // Lens of fms_module_2x3 (model units): X 5..11, Z 3..9, top at Y 3.
    private static final float CENTER_X = 8, CENTER_Z = 6, SURFACE = 3.02f, LENS_HALF = 3;
    /** Card radius: leaves room above the ring for the fixed heading index. */
    private static final float RADIUS = 2.45f;
    private static final float INDEX_GAP = 0.08f, INDEX_HEIGHT = 0.32f, INDEX_HALF_WIDTH = 0.17f;
    /** Ownship triangle (outline) and its stroke. */
    private static final float TRIANGLE_TIP = 0.42f, TRIANGLE_BASE = -0.3f, TRIANGLE_HALF_WIDTH = 0.26f,
            STROKE = 0.07f;
    /** Waypoint dot: a marker on the ring, wider than a tick but not a disc. */
    private static final float DOT_RADIUS = 0.17f;
    private static final int DOT_SIDES = 14;
    /** Letter cap height and the radius of the letter centres. */
    private static final float LETTER_HEIGHT = 0.5f, LETTER_RADIUS = 1.5f;

    private static final int WHITE = 0xF2F2EA, BRASS = 0xE4B763;

    private FmsCompassRenderer() {}

    /**
     * @param poses    positioned at the module origin, in blocks
     * @param depth    lens depth scale (1, or below 1 for legacy 2x2 saves squashed by the renderer)
     * @param heading  FMS heading, radians clockwise from north
     * @param bearing  bearing to the waypoint in the same frame, or NaN to hide the dot
     * @param intro    boot-in progress 0..1 ({@link FmsCompass#intro})
     */
    static void render(PoseStack poses, MultiBufferSource buffers, float depth, double heading, double bearing,
                       float intro) {
        if (intro <= 0) return;
        poses.pushPose();
        poses.scale(1 / 16f, 1 / 16f, 1 / 16f);
        float centerZ = 8 + (CENTER_Z - 8) * depth;
        float radius = RADIUS * Math.min(1, depth) * (0.85f + 0.15f * intro);
        float size = radius / RADIUS;
        int alpha = Math.round(intro * 255);
        var pose = poses.last();
        var flat = buffers.getBuffer(FLAT);

        // Blank the lens to black, hiding its painted glint, as the compass boots in.
        float lensNorth = 8 + (CENTER_Z - LENS_HALF - 8) * depth, lensSouth = 8 + (CENTER_Z + LENS_HALF - 8) * depth;
        quad(flat, pose, CENTER_X - LENS_HALF, lensNorth, CENTER_X - LENS_HALF, lensSouth,
                CENTER_X + LENS_HALF, lensSouth, CENTER_X + LENS_HALF, lensNorth, 0, alpha);

        // The card turns against the heading, so the heading reads at the top index.
        float headingSin = (float) Math.sin(heading), headingCos = (float) Math.cos(heading);
        for (int i = 0; i < FmsCompass.TICKS; i++) {
            // Screen angle = card angle - heading, clockwise from the top.
            float sin = FmsCompass.TICK_SIN[i] * headingCos - FmsCompass.TICK_COS[i] * headingSin;
            float cos = FmsCompass.TICK_COS[i] * headingCos + FmsCompass.TICK_SIN[i] * headingSin;
            stroke(flat, pose, centerZ, sin * radius, cos * radius,
                    sin * radius * FmsCompass.TICK_INNER[i], cos * radius * FmsCompass.TICK_INNER[i],
                    FmsCompass.TICK_WIDTH[i] * RADIUS * size, WHITE, alpha);
        }

        // Fixed heading index above the ring, pointing down at it.
        float indexBase = radius + INDEX_GAP + INDEX_HEIGHT;
        quad(flat, pose, CENTER_X - INDEX_HALF_WIDTH * size, centerZ - indexBase,
                CENTER_X, centerZ - radius - INDEX_GAP, CENTER_X, centerZ - radius - INDEX_GAP,
                CENTER_X + INDEX_HALF_WIDTH * size, centerZ - indexBase, WHITE, alpha);

        // Ownship: hollow triangle at the centre, always pointing at the index.
        float tipY = TRIANGLE_TIP * size, baseY = TRIANGLE_BASE * size, half = TRIANGLE_HALF_WIDTH * size;
        stroke(flat, pose, centerZ, 0, tipY, -half, baseY, STROKE * size, WHITE, alpha);
        stroke(flat, pose, centerZ, 0, tipY, half, baseY, STROKE * size, WHITE, alpha);
        stroke(flat, pose, centerZ, -half, baseY, half, baseY, STROKE * size, WHITE, alpha);

        if (!Double.isNaN(bearing)) {
            double relative = bearing - heading;
            // Centred on the band of the degree ticks, so it marks the ring and not a new angle.
            float ring = radius * (1 - FmsCompass.DEGREE_LENGTH / 2);
            float x = CENTER_X + (float) Math.sin(relative) * ring, z = centerZ - (float) Math.cos(relative) * ring;
            float dot = DOT_RADIUS * size;
            for (int i = 0; i < DOT_SIDES; i++) {
                double a = i * Math.PI * 2 / DOT_SIDES, b = (i + 1) * Math.PI * 2 / DOT_SIDES;
                quad(flat, pose, x, z, x, z, x + dot * (float) Math.cos(a), z + dot * (float) Math.sin(a),
                        x + dot * (float) Math.cos(b), z + dot * (float) Math.sin(b), BRASS, alpha);
            }
        }

        // Letters turn with the card, tops pointing outward like on a heading indicator. They are
        // geometry in this same pass, so they sort and depth-test exactly like the ticks.
        float pixel = LETTER_HEIGHT * size / FmsCompass.GLYPH_HEIGHT;
        float halfWidth = FmsCompass.GLYPH_WIDTH / 2f, halfHeight = FmsCompass.GLYPH_HEIGHT / 2f;
        for (int letter = 0; letter < FmsCompass.LETTER_RECTS.length; letter++) {
            double angle = letter * Math.PI / 2 - heading;
            // Outward (letter up) and rightward unit vectors on the lens, x right and y up.
            float outX = (float) Math.sin(angle), outY = (float) Math.cos(angle);
            float rightX = outY, rightY = -outX;
            float centerX = outX * LETTER_RADIUS * size, centerY = outY * LETTER_RADIUS * size;
            float[] rects = FmsCompass.LETTER_RECTS[letter];
            for (int i = 0; i < rects.length; i += 4) {
                float u0 = (rects[i] - halfWidth) * pixel, v0 = (halfHeight - rects[i + 1]) * pixel;
                float u1 = (rects[i + 2] - halfWidth) * pixel, v1 = (halfHeight - rects[i + 3]) * pixel;
                quad(flat, pose,
                        lensX(centerX, rightX, outX, u0, v0), lensZ(centerZ, centerY, rightY, outY, u0, v0),
                        lensX(centerX, rightX, outX, u0, v1), lensZ(centerZ, centerY, rightY, outY, u0, v1),
                        lensX(centerX, rightX, outX, u1, v1), lensZ(centerZ, centerY, rightY, outY, u1, v1),
                        lensX(centerX, rightX, outX, u1, v0), lensZ(centerZ, centerY, rightY, outY, u1, v0),
                        WHITE, alpha);
            }
        }
        poses.popPose();
    }

    /** Model X of a letter point given in letter space (u right, v up). */
    private static float lensX(float centerX, float rightX, float outX, float u, float v) {
        return CENTER_X + centerX + rightX * u + outX * v;
    }

    /** Model Z of a letter point given in letter space (u right, v up); screen up is -Z. */
    private static float lensZ(float centerZ, float centerY, float rightY, float outY, float u, float v) {
        return centerZ - (centerY + rightY * u + outY * v);
    }

    /** A straight stroke between two lens points (x right, y up from the centre). */
    private static void stroke(VertexConsumer buffer, PoseStack.Pose pose, float centerZ,
                               float x0, float y0, float x1, float y1, float width, int color, int alpha) {
        float dx = x1 - x0, dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1e-5f) return;
        // Ends are squared off by half the width so the triangle corners close.
        float ux = dx / length * width / 2, uy = dy / length * width / 2;
        float nx = -uy, ny = ux;
        quad(buffer, pose,
                CENTER_X + x0 - ux + nx, centerZ - (y0 - uy + ny), CENTER_X + x0 - ux - nx, centerZ - (y0 - uy - ny),
                CENTER_X + x1 + ux - nx, centerZ - (y1 + uy - ny), CENTER_X + x1 + ux + nx, centerZ - (y1 + uy + ny),
                color, alpha);
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, float x0, float z0, float x1, float z1,
                             float x2, float z2, float x3, float z3, int color, int alpha) {
        int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
        buffer.addVertex(pose, x0, SURFACE, z0).setColor(r, g, b, alpha);
        buffer.addVertex(pose, x1, SURFACE, z1).setColor(r, g, b, alpha);
        buffer.addVertex(pose, x2, SURFACE, z2).setColor(r, g, b, alpha);
        buffer.addVertex(pose, x3, SURFACE, z3).setColor(r, g, b, alpha);
    }
}
