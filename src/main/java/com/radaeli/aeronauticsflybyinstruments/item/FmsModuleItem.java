package com.radaeli.aeronauticsflybyinstruments.item;

import com.radaeli.aeronauticsflybyinstruments.fms.FmsModuleData;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

/** 2x3 virtual FMS module. Its identity and waypoint database remain on the item when removed. */
public final class FmsModuleItem extends Item {
    public FmsModuleItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return LampModuleItem.place(context, "fms", 2, 3);
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
