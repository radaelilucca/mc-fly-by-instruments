package com.radaeli.aeronauticsflybyinstruments.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Physical 1x1 panel module; audio/control behavior is reserved for a later phase. */
public final class BuzzerModuleItem extends Item {
    public BuzzerModuleItem(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return LampModuleItem.place(context, "buzzer", 1, 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.aeronautics_fly_by_instruments.buzzer_module.description"));
    }
}
