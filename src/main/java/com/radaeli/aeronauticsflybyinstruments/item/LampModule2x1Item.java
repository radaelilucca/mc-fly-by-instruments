package com.radaeli.aeronauticsflybyinstruments.item;

import com.radaeli.aeronauticsflybyinstruments.network.PanelNetwork;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import java.util.List;

/** A two-cell lamp whose footprint orientation is selected with the mouse wheel. */
public final class LampModule2x1Item extends Item {
    public LampModule2x1Item(Properties properties) { super(properties); }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        boolean vertical = PanelNetwork.isVertical(context.getPlayer());
        return LampModuleItem.place(context, "lamp_2x1", vertical ? 1 : 2, vertical ? 2 : 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.aeronautics_fly_by_instruments.lamp_module_2x1.description"));
    }
}
