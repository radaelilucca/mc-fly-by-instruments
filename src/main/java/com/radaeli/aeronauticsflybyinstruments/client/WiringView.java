package com.radaeli.aeronauticsflybyinstruments.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radaeli.aeronauticsflybyinstruments.avionics.NetworkStatus;
import com.radaeli.aeronauticsflybyinstruments.network.CutWirePayload;
import com.radaeli.aeronauticsflybyinstruments.network.WireNetwork;
import com.radaeli.aeronauticsflybyinstruments.network.WiringStatusPayload;
import com.radaeli.aeronauticsflybyinstruments.network.WiringStatusRequestPayload;
import com.radaeli.aeronauticsflybyinstruments.wire.AvionicsWireItem;
import com.radaeli.aeronauticsflybyinstruments.wire.ClientWireIndex;
import com.radaeli.aeronauticsflybyinstruments.wire.WireNodeBlock;
import com.radaeli.aeronauticsflybyinstruments.wire.WireNodeBlockEntity;
import com.simibubi.create.content.equipment.wrench.WrenchItem;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Wiring View: a toggle (V by default) that dims the world and draws every nearby wire, connector and support
 * through blocks, coloured by the state of its network. Sneak + use with a Create wrench cuts the wire under
 * the crosshair.
 */
public final class WiringView {
    static final KeyMapping TOGGLE = new KeyMapping("key.aeronautics_fly_by_instruments.wiring_view",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V,
            "key.categories.aeronautics_fly_by_instruments");
    private static final double RANGE = WireNetwork.VIEW_RANGE;
    private static final double PICK_RANGE = 24;
    private static final int BRASS = 0xFFE4B763, LONE = 0xFF8F9691, BROKEN = 0xFFCD0000, PENDING = 0xA0E4B763;
    private static final int HOVER = 0xFFFFFFFF, IRON = 0xFF757171, GLAND = 0xFF2B2B2B, DEVICE = 0xFFF6EDD8;
    private static final Map<BlockPos, WiringStatusPayload.Entry> STATUS = new HashMap<>();

    private static boolean active;
    private static int ticksUntilRequest;
    private static Object lastLevel;
    private static Hovered hovered;

    private record Drawn(WireNodeBlockEntity holder, UUID route, List<Vec3> world, double length) {}

    private record Hovered(BlockPos holder, UUID route, double length, int color, int endpoints, NetworkStatus status) {}

    private WiringView() {}

    public static boolean active() {
        return active;
    }

    static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    public static void acceptStatus(WiringStatusPayload payload) {
        for (var entry : payload.entries()) STATUS.put(entry.connector(), entry);
    }

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != lastLevel) {
            lastLevel = minecraft.level;
            STATUS.clear();
            hovered = null;
            if (minecraft.level == null) ClientWireIndex.clear();
        }
        while (TOGGLE.consumeClick()) {
            active = !active;
            STATUS.clear();
            ticksUntilRequest = 0;
            if (minecraft.player != null) minecraft.player.displayClientMessage(Component.translatable(
                    "message.aeronautics_fly_by_instruments.wiring_view." + (active ? "on" : "off")), true);
        }
        if (!active || minecraft.level == null || minecraft.player == null) {
            hovered = null;
            return;
        }
        if (ticksUntilRequest-- > 0) return;
        ticksUntilRequest = 20;
        Vec3 eye = minecraft.player.getEyePosition();
        var connectors = new ArrayList<BlockPos>();
        for (var node : ClientWireIndex.nodes(minecraft.level)) {
            if (node.isConnector() && !node.routes().isEmpty()) connectors.add(node.getBlockPos());
        }
        connectors.sort(Comparator.comparingDouble(pos -> worldCentre(pos, 0).distanceToSqr(eye)));
        if (!connectors.isEmpty()) PacketDistributor.sendToServer(new WiringStatusRequestPayload(connectors));
    }

    static void onRenderLevel(RenderLevelStageEvent event) {
        if (!active || event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        Vec3 look = new Vec3(camera.getLookVector());
        Vector3f left = camera.getLeftVector(), up = camera.getUpVector();

        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(-eye.x, -eye.y, -eye.z);
        var pose = poses.last();
        var buffers = minecraft.renderBuffers().bufferSource();

        // Dim the world with a quad just in front of the eye.
        VertexConsumer shade = buffers.getBuffer(WiringViewRenderTypes.SHADE);
        billboard(shade, pose, eye.add(look.scale(0.3)), left, up, 8, 0x8C000000);
        buffers.endBatch(WiringViewRenderTypes.SHADE);

        var drawn = new ArrayList<Drawn>();
        var nodes = new ArrayList<WireNodeBlockEntity>();
        for (var node : ClientWireIndex.nodes(minecraft.level)) {
            var subLevel = WireSpace.containing(node.getBlockPos());
            if (WireSpace.toWorld(subLevel, Vec3.atCenterOf(node.getBlockPos()), partialTick).distanceTo(eye) > RANGE) continue;
            nodes.add(node);
            var cache = WireRenderer.cache(node);
            for (int i = 0; i < cache.routes().size(); i++) {
                var world = new ArrayList<Vec3>();
                for (var point : cache.lines().get(i)) {
                    world.add(WireSpace.toWorld(subLevel, Vec3.atLowerCornerOf(node.getBlockPos()).add(point), partialTick));
                }
                drawn.add(new Drawn(node, cache.routes().get(i).id(), world, cache.routes().get(i).length()));
            }
        }
        hovered = pick(drawn, eye, look);

        VertexConsumer lines = buffers.getBuffer(WiringViewRenderTypes.LINES);
        for (var wire : drawn) {
            boolean isHovered = hovered != null && hovered.route().equals(wire.route());
            int color = isHovered ? HOVER : colour(wire.holder().getBlockPos());
            for (int i = 1; i < wire.world().size(); i++) line(lines, pose, wire.world().get(i - 1), wire.world().get(i), color);
        }
        buffers.endBatch(WiringViewRenderTypes.LINES);

        VertexConsumer icons = buffers.getBuffer(WiringViewRenderTypes.ICONS);
        for (var node : nodes) {
            var subLevel = WireSpace.containing(node.getBlockPos());
            Vec3 centre = WireSpace.toWorld(subLevel, Vec3.atCenterOf(node.getBlockPos()), partialTick);
            double scale = Math.max(1, centre.distanceTo(eye) / 8);
            if (node.isConnector()) {
                billboard(icons, pose, centre, left, up, 0.09 * scale, BRASS);
                billboard(icons, pose, centre, left, up, 0.04 * scale, GLAND);
                if (WireNetwork.hostIsDevice(minecraft.level, node)) {
                    var host = WireNodeBlock.host(node.getBlockState(), node.getBlockPos());
                    diamond(icons, pose, WireSpace.toWorld(subLevel, Vec3.atCenterOf(host), partialTick), left, up,
                            0.12 * scale, DEVICE);
                }
            } else {
                billboard(icons, pose, centre, left, up, 0.06 * scale, IRON);
            }
        }
        buffers.endBatch(WiringViewRenderTypes.ICONS);
        poses.popPose();
    }

    static void onRenderGui(RenderGuiEvent.Post event) {
        if (!active) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) return;
        var graphics = event.getGuiGraphics();
        var font = minecraft.font;
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        Component title = Component.translatable("gui.aeronautics_fly_by_instruments.wiring_view.title");
        graphics.drawString(font, title, (width - font.width(title)) / 2, 6, BRASS, true);
        if (hovered == null) return;
        String key = "gui.aeronautics_fly_by_instruments.wiring_view.";
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable(key + "wire", AvionicsWireItem.meters(hovered.length())));
        lines.add(hovered.status() == null ? Component.translatable(key + "status.pending")
                : Component.translatable(key + "status." + hovered.status().name().toLowerCase(java.util.Locale.ROOT),
                hovered.endpoints()));
        lines.add(Component.translatable(key + "cut_hint"));
        int y = height / 2 + 10;
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            int color = i == 0 ? HOVER : i == 1 ? hovered.color() : 0xFFAAAAAA;
            graphics.drawString(font, line, (width - font.width(line)) / 2, y, color, true);
            y += font.lineHeight + 1;
        }
    }

    static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!active || hovered == null || !event.isUseItem() || minecraft.player == null
                || !minecraft.player.isShiftKeyDown()
                || !(minecraft.player.getItemInHand(event.getHand()).getItem() instanceof WrenchItem)) return;
        PacketDistributor.sendToServer(new CutWirePayload(hovered.holder(), hovered.route()));
        event.setSwingHand(true);
        event.setCanceled(true);
        hovered = null;
    }

    private static Hovered pick(List<Drawn> drawn, Vec3 eye, Vec3 look) {
        Drawn best = null;
        double bestScore = Double.MAX_VALUE;
        for (var wire : drawn) {
            for (int i = 1; i < wire.world().size(); i++) {
                double[] hit = rayToSegment(eye, look, wire.world().get(i - 1), wire.world().get(i));
                double along = hit[0], gap = hit[1];
                if (along <= 0 || along > PICK_RANGE || gap > 0.12 + 0.01 * along) continue;
                double score = gap + along * 0.001;
                if (score < bestScore) {
                    bestScore = score;
                    best = wire;
                }
            }
        }
        if (best == null) return null;
        BlockPos holder = best.holder().getBlockPos();
        var entry = STATUS.get(holder);
        NetworkStatus status = entry == null ? null : NetworkStatus.values()[entry.status()];
        return new Hovered(holder, best.route(), best.length(), colour(holder), entry == null ? 0 : entry.endpoints(), status);
    }

    /** {distance along the ray, closest distance between ray and segment}. */
    private static double[] rayToSegment(Vec3 origin, Vec3 direction, Vec3 a, Vec3 b) {
        Vec3 segment = b.subtract(a), w = origin.subtract(a);
        double aa = direction.dot(direction), bb = direction.dot(segment), cc = segment.dot(segment);
        double dd = direction.dot(w), ee = segment.dot(w), denominator = aa * cc - bb * bb;
        double t, s;
        if (denominator < 1e-9) {
            s = 0;
            t = -dd / aa;
        } else {
            s = Math.max(0, Math.min(1, (aa * ee - bb * dd) / denominator));
            t = (bb * s - dd) / aa;
        }
        Vec3 onRay = origin.add(direction.scale(t)), onSegment = a.add(segment.scale(s));
        return new double[]{t, onRay.distanceTo(onSegment)};
    }

    private static int colour(BlockPos holder) {
        var entry = STATUS.get(holder);
        if (entry == null) return PENDING;
        var status = NetworkStatus.values()[entry.status()];
        return switch (status) {
            case INCOMPLETE, LIMIT_EXCEEDED -> BROKEN;
            case CONNECTED -> entry.endpoints() >= 2 ? BRASS : LONE;
            default -> LONE;
        };
    }

    private static Vec3 worldCentre(BlockPos pos, float partialTick) {
        return WireSpace.toWorld(WireSpace.containing(pos), Vec3.atCenterOf(pos), partialTick);
    }

    private static void line(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, int color) {
        Vec3 normal = b.subtract(a).normalize();
        consumer.addVertex(pose, (float) a.x, (float) a.y, (float) a.z).setColor(color)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(pose, (float) b.x, (float) b.y, (float) b.z).setColor(color)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static void billboard(VertexConsumer consumer, PoseStack.Pose pose, Vec3 centre, Vector3f left, Vector3f up,
                                  double half, int color) {
        quad(consumer, pose, centre, new Vec3(left).scale(half), new Vec3(up).scale(half), color);
    }

    private static void diamond(VertexConsumer consumer, PoseStack.Pose pose, Vec3 centre, Vector3f left, Vector3f up,
                                double half, int color) {
        Vec3 l = new Vec3(left).scale(half), u = new Vec3(up).scale(half);
        vertex(consumer, pose, centre.add(u), color);
        vertex(consumer, pose, centre.add(l), color);
        vertex(consumer, pose, centre.subtract(u), color);
        vertex(consumer, pose, centre.subtract(l), color);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 centre, Vec3 l, Vec3 u, int color) {
        vertex(consumer, pose, centre.add(l).add(u), color);
        vertex(consumer, pose, centre.add(l).subtract(u), color);
        vertex(consumer, pose, centre.subtract(l).subtract(u), color);
        vertex(consumer, pose, centre.subtract(l).add(u), color);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 p, int color) {
        consumer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(color);
    }
}
