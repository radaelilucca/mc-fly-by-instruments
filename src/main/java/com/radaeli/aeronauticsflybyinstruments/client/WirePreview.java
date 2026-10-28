package com.radaeli.aeronauticsflybyinstruments.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radaeli.aeronauticsflybyinstruments.config.ModConfig;
import com.radaeli.aeronauticsflybyinstruments.wire.AvionicsWireItem;
import com.radaeli.aeronauticsflybyinstruments.wire.WireNodeBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.wire.WirePath;
import com.radaeli.aeronauticsflybyinstruments.wire.WireRoute;
import com.radaeli.aeronauticsflybyinstruments.wire.WireRouteInProgress;
import com.radaeli.aeronauticsflybyinstruments.wire.WireRules;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Ghost of the wire being routed: the chosen points plus a loose end under the crosshair, already sagging.
 * White when the next click would be accepted, red when it would be refused (with the reason under the
 * crosshair). The server re-checks everything.
 */
final class WirePreview {
    private static final int VALID = 0xE6FFFFFF, INVALID = 0xE6FF4040;
    private static final double FREE_REACH = 4.5;

    private WirePreview() {}

    private record Target(List<BlockPos> positions, List<Integer> lanes, @Nullable Vec3 freeEnd,
                          WireRules.Check check, @Nullable String problem, double length) {}

    @Nullable
    private static WireRouteInProgress heldRoute() {
        var player = Minecraft.getInstance().player;
        if (player == null) return null;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof AvionicsWireItem) return AvionicsWireItem.route(stack);
        }
        return null;
    }

    @Nullable
    private static Target target(WireRouteInProgress route) {
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft.level;
        var player = minecraft.player;
        if (level == null || player == null) return null;
        var positions = new ArrayList<>(route.positions());
        var lanes = new ArrayList<>(route.lanes());
        HitResult hit = minecraft.hitResult;
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                && level.getBlockEntity(blockHit.getBlockPos()) instanceof WireNodeBlockEntity node
                && !blockHit.getBlockPos().equals(route.last())) {
            BlockPos pos = blockHit.getBlockPos();
            var check = WireRules.canAppend(route.positions(), pos, ModConfig.WIRE_MAX_SPAN.get(), ModConfig.WIRE_MAX_POINTS.get());
            String problem = check.translationKey();
            if (check == WireRules.Check.OK && !AvionicsWireItem.sameStructure(level, route.positions().getFirst(), pos)) {
                problem = WireRules.Check.OTHER_STRUCTURE.translationKey();
            } else if (check == WireRules.Check.OK && node.isFull()) {
                problem = WireRules.Check.FULL.translationKey();
            }
            int lane = node.isConnector() ? 0 : Math.max(0, WireRules.freeLane(node.usedLanes()));
            positions.add(pos);
            lanes.add(lane);
            return new Target(positions, lanes, null, check, problem, WireRoute.length(positions));
        }
        // Loose end: only outside sublevels, where crosshair and level coordinates agree.
        if (WireSpace.containing(route.last()) != null) {
            return new Target(positions, lanes, null, WireRules.Check.OK, null, WireRoute.length(positions));
        }
        Vec3 end = hit != null && hit.getType() != HitResult.Type.MISS ? hit.getLocation()
                : player.getEyePosition().add(player.getLookAngle().scale(FREE_REACH));
        double span = Vec3.atCenterOf(route.last()).distanceTo(end);
        boolean tooLong = span > ModConfig.WIRE_MAX_SPAN.get();
        return new Target(positions, lanes, end, tooLong ? WireRules.Check.TOO_LONG : WireRules.Check.OK,
                tooLong ? WireRules.Check.TOO_LONG.translationKey() : null,
                WireRoute.length(positions) + span);
    }

    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        var route = heldRoute();
        Minecraft minecraft = Minecraft.getInstance();
        if (route == null || minecraft.level == null) return;
        var target = target(route);
        if (target == null) return;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        List<Vec3> line;
        try {
            line = target.freeEnd() != null
                    ? WirePath.previewLine(minecraft.level, target.positions(), target.lanes(), target.freeEnd())
                    : target.positions().size() >= 2
                    ? WirePath.centreLine(minecraft.level, target.positions(), target.lanes()) : List.of();
        } catch (IllegalArgumentException degenerate) {
            return;
        }
        if (line.size() < 2) return;
        var subLevel = WireSpace.containing(route.positions().getFirst());
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        poses.translate(-camera.x, -camera.y, -camera.z);
        var pose = poses.last();
        var buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        int color = target.problem() == null ? VALID : INVALID;
        Vec3 previous = null;
        for (var point : line) {
            Vec3 world = target.freeEnd() != null ? point : WireSpace.toWorld(subLevel, point, partialTick);
            if (previous != null) {
                Vec3 normal = world.subtract(previous).normalize();
                lines.addVertex(pose, (float) previous.x, (float) previous.y, (float) previous.z).setColor(color)
                        .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
                lines.addVertex(pose, (float) world.x, (float) world.y, (float) world.z).setColor(color)
                        .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
            }
            previous = world;
        }
        buffers.endBatch(RenderType.lines());
        poses.popPose();
    }

    static void onRenderGui(RenderGuiEvent.Post event) {
        var route = heldRoute();
        Minecraft minecraft = Minecraft.getInstance();
        if (route == null || minecraft.options.hideGui || WiringView.active()) return;
        var target = target(route);
        if (target == null) return;
        var graphics = event.getGuiGraphics();
        var font = minecraft.font;
        int cost = WireRules.cost(target.length(), ModConfig.WIRE_BLOCKS_PER_ITEM.get());
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("gui.aeronautics_fly_by_instruments.wire.preview",
                AvionicsWireItem.meters(target.length()), cost));
        if (target.problem() != null) {
            lines.add(Component.translatable(target.problem(), ModConfig.WIRE_MAX_SPAN.get()));
        } else {
            lines.add(Component.translatable("gui.aeronautics_fly_by_instruments.wire.preview_hint"));
        }
        int y = graphics.guiHeight() / 2 + 10;
        for (int i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            int color = i == 0 ? 0xFFFFFFFF : target.problem() != null ? 0xFFFF6060 : 0xFFAAAAAA;
            graphics.drawString(font, line, (graphics.guiWidth() - font.width(line)) / 2, y, color, true);
            y += font.lineHeight + 1;
        }
    }
}
