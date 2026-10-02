package com.radaeli.flightsafetyinstruments.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.radaeli.flightsafetyinstruments.config.ModConfig;
import com.radaeli.flightsafetyinstruments.sensor.TerrainScanDebugRay;
import com.radaeli.flightsafetyinstruments.sensor.RadarAltimeterBlockEntity;
import com.radaeli.flightsafetyinstruments.sensor.RadarAltimeterScanner;
import com.radaeli.flightsafetyinstruments.sensor.TerrainScannerBlockEntity;
import com.radaeli.flightsafetyinstruments.sensor.TerrainScannerScanner;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.bus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Development-only client visualization for Terrain Scanner ray paths. */
public final class TerrainScannerDebug {
    private static final double SCANNER_RADIUS = 96.0;
    private static final KeyMapping TOGGLE = new KeyMapping(
            "key.aeronautics_fly_by_instruments.raycast_debug",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F7,
            "key.categories.aeronautics_fly_by_instruments");

    private static boolean active;
    private static int ticksUntilRefresh;
    private static Object previousLevel;
    private static List<TerrainScanDebugRay> rays = List.of();

    private TerrainScannerDebug() {}

    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (TOGGLE.consumeClick()) {
            active = !active;
            ticksUntilRefresh = 0;
            if (!active) rays = List.of();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(active
                        ? "debug.aeronautics_fly_by_instruments.raycast_debug_enabled"
                        : "debug.aeronautics_fly_by_instruments.raycast_debug_disabled"), true);
            }
        }

        if (!active || minecraft.level == null || minecraft.player == null) {
            if (minecraft.level != previousLevel) rays = List.of();
            previousLevel = minecraft.level;
            return;
        }
        if (minecraft.level != previousLevel) {
            rays = List.of();
            ticksUntilRefresh = 0;
            previousLevel = minecraft.level;
        }
        if (ticksUntilRefresh-- > 0) return;
        ticksUntilRefresh = Math.max(1, ModConfig.SENSOR_SCAN_INTERVAL_TICKS.get()) - 1;

        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double radiusSqr = SCANNER_RADIUS * SCANNER_RADIUS;
        int maxRange = ModConfig.TERRAIN_SCANNER_MAX_RANGE.get();
        List<TerrainScanDebugRay> updated = new ArrayList<>();
        Set<BlockEntity> scanned = Collections.newSetFromMap(new IdentityHashMap<>());
        int centerChunkX = BlockPos.containing(camera).getX() >> 4;
        int centerChunkZ = BlockPos.containing(camera).getZ() >> 4;
        int chunkRadius = (int) Math.ceil(SCANNER_RADIUS / 16.0);
        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                ChunkAccess chunk = minecraft.level.getChunkSource()
                        .getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk instanceof LevelChunk levelChunk)
                    scanChunk(levelChunk, minecraft.level, camera, radiusSqr, maxRange, scanned, updated);
            }
        }

        AABB debugBounds = new AABB(camera.x, camera.y, camera.z,
                camera.x, camera.y, camera.z).inflate(SCANNER_RADIUS);
        for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(minecraft.level, new BoundingBox3d(debugBounds))) {
            for (var chunkHolder : subLevel.getPlot().getLoadedChunks()) {
                scanChunk(chunkHolder.getChunk(), minecraft.level, camera, radiusSqr, maxRange, scanned, updated);
            }
        }
        rays = List.copyOf(updated);
    }

    private static void scanChunk(LevelChunk chunk, net.minecraft.client.multiplayer.ClientLevel level,
                                  Vec3 camera, double radiusSqr, int maxRange,
                                  Set<BlockEntity> scanned, List<TerrainScanDebugRay> output) {
        for (var blockEntity : chunk.getBlockEntities().values()) {
            if (!(blockEntity instanceof TerrainScannerBlockEntity)
                    && !(blockEntity instanceof RadarAltimeterBlockEntity)) continue;
            if (!scanned.add(blockEntity)) continue;
            Vec3 scannerOrigin = Vec3.atCenterOf(blockEntity.getBlockPos());
            SubLevel host = Sable.HELPER.getContaining(blockEntity);
            if (host != null) scannerOrigin = host.logicalPose().transformPosition(scannerOrigin);
            if (scannerOrigin.distanceToSqr(camera) > radiusSqr) continue;
            try {
                if (blockEntity instanceof TerrainScannerBlockEntity scanner) {
                    TerrainScannerScanner.DebugScan scan = TerrainScannerScanner.scanDetailed(
                            level, scanner, maxRange, scanner.advanced(), scanner.openingAngle());
                    output.addAll(scan.rays());
                } else if (blockEntity instanceof RadarAltimeterBlockEntity altimeter) {
                    RadarAltimeterScanner.DebugScan scan = RadarAltimeterScanner.scanDetailed(
                            level, altimeter, ModConfig.RADAR_ALTIMETER_MAX_RANGE.get());
                    output.addAll(scan.rays());
                }
            } catch (RuntimeException ignored) {
                // Debug rendering must not crash the client if a world/contraption is in a transient state.
            }
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!active || rays.isEmpty()
                || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        VertexConsumer lines = minecraft.renderBuffers().bufferSource().getBuffer(RenderType.lines());
        for (TerrainScanDebugRay ray : rays) {
            int color = switch (ray.result()) {
                case HIT -> 0xFFFF4040;
                case CLEAR -> 0xFF40E0FF;
                case INCOMPLETE -> 0xFFFFC040;
            };
            addLine(lines, poseStack, ray.start(), ray.end(), color);
        }
        minecraft.renderBuffers().bufferSource().endBatch(RenderType.lines());
        poseStack.popPose();
    }

    private static void addLine(VertexConsumer lines, PoseStack poseStack, Vec3 start, Vec3 end, int color) {
        PoseStack.Pose pose = poseStack.last();
        lines.addVertex(pose, (float) start.x, (float) start.y, (float) start.z)
                .setColor(color).setNormal(pose, 0.0F, 1.0F, 0.0F);
        lines.addVertex(pose, (float) end.x, (float) end.y, (float) end.z)
                .setColor(color).setNormal(pose, 0.0F, 1.0F, 0.0F);
    }
}
