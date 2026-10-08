package com.radaeli.aeronauticsflybyinstruments.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock;
import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.panel.PanelCoordinates;
import com.radaeli.aeronauticsflybyinstruments.panel.PanelGrid;
import com.radaeli.aeronauticsflybyinstruments.network.PanelNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;

public final class ModularTableRenderer implements BlockEntityRenderer<ModularTableBlockEntity> {
    private static final double SURFACE = 3.0 / 16;
    private static final double CELL = 1.0 / PanelGrid.SIZE;
    private final BlockRenderDispatcher blocks;
    private final LampModuleRenderer lamps1x1 = new LampModuleRenderer(ModBlocks.LAMP_MODULE.get());
    private final LampModuleRenderer lamps2x1 = new LampModuleRenderer(ModBlocks.LAMP_MODULE_2X1.get());
    private final ItemStack buzzerStack = new ItemStack(ModBlocks.BUZZER_MODULE.get());
    private final ItemStack fmsStack = new ItemStack(ModBlocks.FMS_MODULE.get());

    public ModularTableRenderer(BlockEntityRendererProvider.Context context) {
        blocks = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(ModularTableBlockEntity table, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int light, int overlay) {
        var facing = table.getBlockState().getValue(ModularTableBaseBlock.FACING);
        poses.pushPose();
        orient(poses, facing);
        for (var module : table.grid().modules()) {
            if (ModularTableBlockEntity.isLampModule(module)) {
                renderLamp(table, module, poses, buffers, light, overlay);
            } else if (module.type().equals("buzzer")) {
                renderBuzzer(table, module, poses, buffers, light, overlay);
            } else if (module.type().equals("fms")) {
                renderFms(table, module, poses, buffers, light, overlay);
            } else {
                // Keep a fallback for future module types without authored renderers.
                var lampState = Blocks.REDSTONE_LAMP.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, table.isModuleActive(module));
                cube(poses, buffers, lampState,
                        module.column() * CELL + 0.06, SURFACE, module.row() * CELL + 0.06,
                        module.width() * CELL - 0.12, 2.0 / 16, module.height() * CELL - 0.12, light, overlay);
            }
        }

        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        boolean holdingSmallLamp = player != null && (player.getMainHandItem().is(ModBlocks.LAMP_MODULE.get())
                || player.getOffhandItem().is(ModBlocks.LAMP_MODULE.get()));
        boolean holdingLongLamp = player != null && (player.getMainHandItem().is(ModBlocks.LAMP_MODULE_2X1.get())
                || player.getOffhandItem().is(ModBlocks.LAMP_MODULE_2X1.get()));
        boolean holdingBuzzer = player != null && (player.getMainHandItem().is(ModBlocks.BUZZER_MODULE.get())
                || player.getOffhandItem().is(ModBlocks.BUZZER_MODULE.get()));
        boolean holdingFms = player != null && (player.getMainHandItem().is(ModBlocks.FMS_MODULE.get())
                || player.getOffhandItem().is(ModBlocks.FMS_MODULE.get()));
        boolean removing = player != null && (ModularTableBaseBlock.isCreateWrench(player.getMainHandItem())
                || ModularTableBaseBlock.isCreateWrench(player.getOffhandItem()));
        if ((holdingSmallLamp || holdingLongLamp || holdingBuzzer || holdingFms || removing) && minecraft.hitResult instanceof BlockHitResult hit
                && hit.getBlockPos().equals(table.getBlockPos()) && hit.getDirection() == facing) {
            for (int i = 0; i <= PanelGrid.SIZE; i++) {
                double line = Math.min(1 - 0.012, Math.max(0, i * CELL - 0.006));
                cube(poses, buffers, Blocks.GRAY_CONCRETE.defaultBlockState(),
                        line, SURFACE + 0.001, 0, 0.012, 0.004, 1, light, overlay);
                cube(poses, buffers, Blocks.GRAY_CONCRETE.defaultBlockState(),
                        0, SURFACE + 0.001, line, 1, 0.004, 0.012, light, overlay);
            }
            var point = PanelCoordinates.toPanel(facing, hit.getLocation().subtract(Vec3.atLowerCornerOf(table.getBlockPos())));
            int column = PanelCoordinates.cell(point.x);
            int row = PanelCoordinates.cell(point.z);
            if (column >= 0 && row >= 0) {
                int width = holdingFms ? 2 : holdingLongLamp && PanelNetwork.isVertical(player) ? 1 : holdingLongLamp ? 2 : 1;
                int height = holdingFms ? 3 : holdingLongLamp && PanelNetwork.isVertical(player) ? 2 : 1;
                int outlineColumn = column;
                int outlineRow = row;
                boolean valid = false;
                if (removing) {
                    var module = table.grid().at(column, row);
                    if (module.isPresent()) {
                        var installed = module.get();
                        outlineColumn = installed.column();
                        outlineRow = installed.row();
                        width = installed.width();
                        height = installed.height();
                        valid = true;
                    }
                } else {
                    valid = table.grid().canFit(column, row, width, height);
                }
                if (holdingSmallLamp || holdingLongLamp || holdingBuzzer || holdingFms || valid) {
                    var color = (valid ? Blocks.LIME_CONCRETE : Blocks.RED_CONCRETE).defaultBlockState();
                    outline(poses, buffers, color, outlineColumn, outlineRow, width, height, light, overlay);
                }
            }
        }
        poses.popPose();
    }

    private void renderLamp(ModularTableBlockEntity table, PanelGrid.Module module, PoseStack poses,
                            MultiBufferSource buffers, int light, int overlay) {
        poses.pushPose();
        // Center the model on its full footprint, then turn the long axis for a vertical 1x2 placement.
        var offset = PanelCoordinates.modelOffset(module);
        poses.translate(offset.x, offset.y, offset.z);
        if (module.height() > module.width()) {
            poses.translate(0.5, 0, 0.5);
            poses.mulPose(Axis.YP.rotationDegrees(90));
            poses.translate(-0.5, 0, -0.5);
        }
        var renderer = module.type().equals("lamp_2x1") ? lamps2x1 : lamps1x1;
        renderer.render(table.lampRenderIntensity(module), table.getLevel(), poses, buffers, light, overlay);
        poses.popPose();
    }

    private void renderBuzzer(ModularTableBlockEntity table, PanelGrid.Module module, PoseStack poses,
                              MultiBufferSource buffers, int light, int overlay) {
        poses.pushPose();
        // Render raw model quads: item display transforms must not shift mounted modules.
        var offset = PanelCoordinates.modelOffset(module);
        poses.translate(offset.x, offset.y, offset.z);
        var items = Minecraft.getInstance().getItemRenderer();
        var model = items.getModel(buzzerStack, table.getLevel(), null, 0);
        items.renderModelLists(model, buzzerStack, light, overlay, poses,
                buffers.getBuffer(Sheets.cutoutBlockSheet()));
        poses.popPose();
    }

    private void renderFms(ModularTableBlockEntity table, PanelGrid.Module module, PoseStack poses,
                           MultiBufferSource buffers, int light, int overlay) {
        poses.pushPose();
        var offset = PanelCoordinates.modelOffset(module);
        poses.translate(offset.x, offset.y, offset.z);
        // Old occupied 2x2 saves retain their footprint; fit the visual inside it.
        if (module.height() != 3) {
            poses.translate(0.5, 0, 0.5);
            poses.scale(1, 1, module.height() / 3.0f);
            poses.translate(-0.5, 0, -0.5);
        }
        var items = Minecraft.getInstance().getItemRenderer();
        var model = items.getModel(fmsStack, table.getLevel(), null, 0);
        items.renderModelLists(model, fmsStack, light, overlay, poses,
                buffers.getBuffer(Sheets.solidBlockSheet()));
        poses.popPose();
    }

    private void outline(PoseStack poses, MultiBufferSource buffers, BlockState color,
                        int column, int row, int width, int height, int light, int overlay) {
        width = Math.min(width, PanelGrid.SIZE - column);
        height = Math.min(height, PanelGrid.SIZE - row);
        double x = column * CELL + 0.012;
        double z = row * CELL + 0.012;
        double w = width * CELL - 0.024;
        double d = height * CELL - 0.024;
        cube(poses, buffers, color, x, SURFACE + 0.005, z, w, 0.006, 0.016, light, overlay);
        cube(poses, buffers, color, x, SURFACE + 0.005, z + d - 0.016, w, 0.006, 0.016, light, overlay);
        cube(poses, buffers, color, x, SURFACE + 0.005, z, 0.016, 0.006, d, light, overlay);
        cube(poses, buffers, color, x + w - 0.016, SURFACE + 0.005, z, 0.016, 0.006, d, light, overlay);
    }

    private void cube(PoseStack poses, MultiBufferSource buffers, BlockState state,
                      double x, double y, double z, double width, double height, double depth, int light, int overlay) {
        poses.pushPose();
        poses.translate(x, y, z);
        poses.scale((float) width, (float) height, (float) depth);
        blocks.renderSingleBlock(state, poses, buffers, light, overlay);
        poses.popPose();
    }

    private static void orient(PoseStack poses, Direction facing) {
        poses.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case UP -> { }
            case DOWN -> poses.mulPose(Axis.XP.rotationDegrees(180));
            case NORTH -> poses.mulPose(Axis.XP.rotationDegrees(-90));
            case SOUTH -> {
                poses.mulPose(Axis.YP.rotationDegrees(180));
                poses.mulPose(Axis.XP.rotationDegrees(-90));
            }
            case WEST -> {
                poses.mulPose(Axis.YP.rotationDegrees(90));
                poses.mulPose(Axis.XP.rotationDegrees(-90));
            }
            case EAST -> {
                poses.mulPose(Axis.YP.rotationDegrees(-90));
                poses.mulPose(Axis.XP.rotationDegrees(-90));
            }
        }
        poses.translate(-0.5, -0.5, -0.5);
    }
}
