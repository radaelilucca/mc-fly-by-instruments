package com.radaeli.aeronauticsflybyinstruments.item;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

/** Compass-like token bound to one FMS module identity. */
public final class FmsNavigatorItem extends Item {
    private static final String BOUND_ID = "FmsModuleId";

    public FmsNavigatorItem(Properties properties) { super(properties.stacksTo(1)); }

    public static UUID boundFmsId(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return null;
        var tag = custom.copyTag();
        return tag.hasUUID(BOUND_ID) ? tag.getUUID(BOUND_ID) : null;
    }

    public static void bind(ItemStack stack, UUID id) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        var tag = custom == null ? new net.minecraft.nbt.CompoundTag() : custom.copyTag();
        if (id == null) tag.remove(BOUND_ID);
        else tag.putUUID(BOUND_ID, id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        UUID id = boundFmsId(stack);
        tooltip.add(Component.translatable(id == null
                ? "item." + AeronauticsFlyByInstruments.MOD_ID + ".fms_navigator.unbound"
                : "item." + AeronauticsFlyByInstruments.MOD_ID + ".fms_navigator.bound"));
    }
}
