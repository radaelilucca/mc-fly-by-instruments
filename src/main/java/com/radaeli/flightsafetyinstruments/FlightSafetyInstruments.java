package com.radaeli.flightsafetyinstruments;

import com.mojang.logging.LogUtils;
import com.radaeli.flightsafetyinstruments.block.ModBlocks;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import com.radaeli.flightsafetyinstruments.client.PanelClient;

/** Main entry point for the Flight Safety Instruments addon. */
@Mod(FlightSafetyInstruments.MOD_ID)
public final class FlightSafetyInstruments {
    public static final String MOD_ID = "flightsafetyinstruments";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FlightSafetyInstruments(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);
        modEventBus.addListener(this::registerCapabilities);
        if (FMLEnvironment.dist == Dist.CLIENT) PanelClient.register(modEventBus);
        modEventBus.addListener(this::addCreativeContents);
        LOGGER.info("Flight Safety Instruments loaded");
    }

    private void addCreativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.MODULAR_TABLE_BASE_ITEM);
            event.accept(ModBlocks.LAMP_MODULE);
        }
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), ModBlocks.MODULAR_TABLE_ENTITY.get(),
                (table, side) -> table.peripheral());
    }
}
