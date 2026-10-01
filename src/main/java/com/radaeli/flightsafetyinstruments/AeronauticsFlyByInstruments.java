package com.radaeli.flightsafetyinstruments;

import com.mojang.logging.LogUtils;
import com.radaeli.flightsafetyinstruments.block.ModBlocks;
import com.radaeli.flightsafetyinstruments.avionics.NetworkBridgeBlockEntity;
import com.radaeli.flightsafetyinstruments.sensor.RadarAltimeterBlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import com.radaeli.flightsafetyinstruments.config.ModConfig;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import dan200.computercraft.api.peripheral.PeripheralCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import com.radaeli.flightsafetyinstruments.client.PanelClient;
import com.radaeli.flightsafetyinstruments.client.PanelInputHandler;
import com.radaeli.flightsafetyinstruments.network.PanelNetwork;
import net.neoforged.neoforge.common.NeoForge;

/** Main entry point for Aeronautics: Fly by Instruments. */
@Mod(AeronauticsFlyByInstruments.MOD_ID)
public final class AeronauticsFlyByInstruments {
    public static final String MOD_ID = "aeronautics_fly_by_instruments";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AeronauticsFlyByInstruments(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlocks.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modContainer.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER, ModConfig.SPEC);
        modEventBus.addListener(PanelNetwork::registerPayloads);
        modEventBus.addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.addListener(PanelNetwork::onPlayerLoggedIn);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            PanelClient.register(modEventBus);
            NeoForge.EVENT_BUS.addListener(PanelInputHandler::onMouseScroll);
        }
        LOGGER.info("Aeronautics: Fly by Instruments loaded");
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(PeripheralCapability.get(), ModBlocks.MODULAR_TABLE_ENTITY.get(),
                (table, side) -> table.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModBlocks.NETWORK_BRIDGE_ENTITY.get(),
                (bridge, side) -> bridge.peripheral());
        event.registerBlockEntity(PeripheralCapability.get(), ModBlocks.RADAR_ALTIMETER_ENTITY.get(),
                (altimeter, side) -> altimeter.peripheral());
    }
}
