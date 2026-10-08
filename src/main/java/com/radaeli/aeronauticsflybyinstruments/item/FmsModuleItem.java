package com.radaeli.aeronauticsflybyinstruments.item;

import com.radaeli.aeronauticsflybyinstruments.block.ModularTableBaseBlock;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsModuleData;
import com.radaeli.aeronauticsflybyinstruments.panel.ModularTableBlockEntity;
import com.radaeli.aeronauticsflybyinstruments.panel.PanelCoordinates;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** 2x3 virtual FMS module. Its identity and waypoint database remain on the item when removed. */
public final class FmsModuleItem extends Item {
    public FmsModuleItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof ModularTableBlockEntity table)) return InteractionResult.PASS;
        var state = level.getBlockState(pos);
        var facing = state.getValue(ModularTableBaseBlock.FACING);
        if (context.getClickedFace() != facing) return InteractionResult.FAIL;
        var point = PanelCoordinates.toPanel(facing,
                context.getClickLocation().subtract(Vec3.atLowerCornerOf(pos)));
        int column = PanelCoordinates.cell(point.x);
        int row = PanelCoordinates.cell(point.z);
        if (!table.grid().canFit(column, row, 2, 3)) return InteractionResult.FAIL;
        if (!level.isClientSide) {
            FmsModuleData data = load(context.getItemInHand());
            if (!table.placeFmsModule(column, row, data)) return InteractionResult.FAIL;
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static FmsModuleData load(net.minecraft.world.item.ItemStack stack) {
        var custom = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (custom == null) return new FmsModuleData();
        var root = custom.copyTag();
        return root.contains("FmsModule", net.minecraft.nbt.Tag.TAG_COMPOUND)
                ? FmsModuleData.load(root.getCompound("FmsModule")) : new FmsModuleData();
    }

    public static void store(net.minecraft.world.item.ItemStack stack, FmsModuleData data) {
        var custom = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        var root = custom == null ? new net.minecraft.nbt.CompoundTag() : custom.copyTag();
        root.put("FmsModule", data.save());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(root));
    }
}
