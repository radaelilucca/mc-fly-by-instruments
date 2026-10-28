package com.radaeli.aeronauticsflybyinstruments.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Lays the FMS Compass flat on the Navigation Table's brass top plate and turns it so its needle
 * follows the table's pointer, i.e. the active FMS waypoint.
 *
 * <p>The table hands over a pose already tipped 90° onto its top (pivot 0.55 above the block
 * centre, scale 0.5, made for item sprites). This undoes the tip and draws the compass model
 * without display transforms, so item frames keep their own {@code fixed} transform.
 */
public final class FmsNavigatorTableRenderer {
    /** Lifts the model base (Y = 0) onto the top plate (Y = 15 of the table) in the 0.5-scaled pose. */
    private static final float LIFT = 0.278f;
    /** The painted needle points north-east (45° off the model's +X); the table angle is measured from +X. */
    private static final float NEEDLE_OFFSET = (float) (Math.PI / 4);

    private FmsNavigatorTableRenderer() {}

    public static void render(ItemStack stack, NavTableBlockEntity table, float partialTicks,
                              PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        pose.pushPose();
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.translate(0, LIFT, 0);
        pose.mulPose(Axis.YP.rotation(table.getClientTargetAngle(partialTicks) - NEEDLE_OFFSET));
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.NONE, light, overlay,
                pose, buffer, Minecraft.getInstance().level, 0);
        pose.popPose();
    }
}
