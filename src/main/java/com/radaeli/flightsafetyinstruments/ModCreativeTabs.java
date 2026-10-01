package com.radaeli.flightsafetyinstruments;

import com.radaeli.flightsafetyinstruments.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AeronauticsFlyByInstruments.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> AERONAUTICS_FLY_BY_INSTRUMENTS =
            TABS.register("aeronautics_fly_by_instruments", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.aeronautics_fly_by_instruments"))
                    .icon(() -> new ItemStack(ModBlocks.MODULAR_TABLE_BASE_ITEM.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.MODULAR_TABLE_BASE_ITEM);
                        output.accept(ModBlocks.LAMP_MODULE);
                        output.accept(ModBlocks.LAMP_MODULE_2X1);
                    })
                    .build());

    private ModCreativeTabs() {}
}
