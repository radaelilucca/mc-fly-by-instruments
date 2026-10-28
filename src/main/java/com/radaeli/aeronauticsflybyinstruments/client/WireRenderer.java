package com.radaeli.aeronauticsflybyinstruments.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import com.radaeli.aeronauticsflybyinstruments.wire.WireNodeBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.wire.WirePath;
import com.radaeli.aeronauticsflybyinstruments.wire.WireRoute;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws every wire whose first connector is this block entity: a 1×1 texel black tube along the sagging centre
 * line, lit per vertex like a lead. Inside a Sable sublevel the pose stack is already in sublevel space.
 */
public final class WireRenderer implements BlockEntityRenderer<WireNodeBlockEntity> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            AeronauticsFlyByInstruments.MOD_ID, "textures/entity/avionics_wire.png");
    /** Half the wire thickness: one texel square. */
    public static final float HALF = 0.5f / 16;
    private static final int REFRESH_TICKS = 40;

    public WireRenderer(BlockEntityRendererProvider.Context context) {}

    /** Centre lines of the owned routes, relative to the block entity, with the AABB that covers them. */
    record Cache(List<WireRoute> routes, List<List<Vec3>> lines, AABB bounds, long builtAt) {}

    static Cache cache(WireNodeBlockEntity node) {
        Level level = node.getLevel();
        long now = level == null ? 0 : level.getGameTime();
        if (node.renderCache instanceof Cache cache && now - cache.builtAt() < REFRESH_TICKS && now >= cache.builtAt()) {
            return cache;
        }
        var owned = new ArrayList<WireRoute>();
        var lines = new ArrayList<List<Vec3>>();
        AABB bounds = new AABB(BlockPos.ZERO);
        if (level != null) {
            for (WireRoute route : node.routes()) {
                if (!route.ownedByHolder()) continue;
                var line = WirePath.centreLine(level, node.getBlockPos(), route);
                owned.add(route);
                lines.add(line);
                for (var point : line) bounds = bounds.minmax(new AABB(point, point));
            }
        }
        var cache = new Cache(List.copyOf(owned), List.copyOf(lines), bounds.inflate(0.5), now);
        node.renderCache = cache;
        return cache;
    }

    @Override
    public void render(WireNodeBlockEntity node, float partialTick, PoseStack poses, MultiBufferSource buffers,
                       int packedLight, int packedOverlay) {
        var cache = cache(node);
        if (cache.lines().isEmpty()) return;
        Level level = node.getLevel();
        BlockPos origin = node.getBlockPos();
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        for (var line : cache.lines()) {
            tube(consumer, poses.last(), line, HALF,
                    point -> level == null ? packedLight : LevelRenderer.getLightColor(level, BlockPos.containing(
                            origin.getX() + point.x, origin.getY() + point.y, origin.getZ() + point.z)));
        }
    }

    @Override
    public AABB getRenderBoundingBox(WireNodeBlockEntity node) {
        return cache(node).bounds().move(node.getBlockPos());
    }

    @Override
    public boolean shouldRenderOffScreen(WireNodeBlockEntity node) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    interface Light {
        int at(Vec3 point);
    }

    /**
     * Square tube with a parallel-transported frame (no twisting at bends). Faces sample one texel column
     * each; v follows arc length at 16 texels per block, so the texture repeats instead of stretching.
     */
    static void tube(VertexConsumer consumer, PoseStack.Pose pose, List<Vec3> line, float half, Light light) {
        int n = line.size();
        if (n < 2) return;
        Vec3[] sides = new Vec3[n], ups = new Vec3[n];
        double[] distance = new double[n];
        Vec3 side = null;
        for (int i = 0; i < n; i++) {
            Vec3 tangent = line.get(Math.min(n - 1, i + 1)).subtract(line.get(Math.max(0, i - 1))).normalize();
            if (side == null) {
                side = tangent.cross(new Vec3(0, 1, 0));
                if (side.lengthSqr() < 1e-6) side = tangent.cross(new Vec3(1, 0, 0));
            } else {
                side = side.subtract(tangent.scale(side.dot(tangent)));
                if (side.lengthSqr() < 1e-8) side = tangent.cross(new Vec3(0, 1, 0));
            }
            side = side.normalize();
            sides[i] = side;
            ups[i] = side.cross(tangent).normalize();
            if (i > 0) distance[i] = distance[i - 1] + line.get(i).distanceTo(line.get(i - 1));
        }
        // Faces in corner order: +side, -up (underside), -side, +up (top); texture columns 1, 5, 9 and 13.
        double[][] corners = {{1, 1}, {1, -1}, {-1, -1}, {-1, 1}};
        for (int face = 0; face < 4; face++) {
            double[] a = corners[face], b = corners[(face + 1) % 4];
            float u0 = (4 * face + 1) / 16f, u1 = (4 * face + 2) / 16f;
            for (int i = 0; i < n - 1; i++) {
                int lightA = light.at(line.get(i)), lightB = light.at(line.get(i + 1));
                Vec3 normalA = ups[i].scale((a[1] + b[1]) / 2).add(sides[i].scale((a[0] + b[0]) / 2)).normalize();
                float v0 = (float) distance[i], v1 = (float) distance[i + 1];
                vertex(consumer, pose, corner(line.get(i), sides[i], ups[i], a, half), u0, v0, lightA, normalA);
                vertex(consumer, pose, corner(line.get(i), sides[i], ups[i], b, half), u1, v0, lightA, normalA);
                vertex(consumer, pose, corner(line.get(i + 1), sides[i + 1], ups[i + 1], b, half), u1, v1, lightB, normalA);
                vertex(consumer, pose, corner(line.get(i + 1), sides[i + 1], ups[i + 1], a, half), u0, v1, lightB, normalA);
            }
        }
    }

    private static Vec3 corner(Vec3 centre, Vec3 side, Vec3 up, double[] signs, float half) {
        return centre.add(side.scale(signs[0] * half)).add(up.scale(signs[1] * half));
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 p, float u, float v, int light, Vec3 normal) {
        consumer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(0xFFFFFFFF).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
