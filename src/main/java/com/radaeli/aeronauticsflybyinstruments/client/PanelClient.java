package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class PanelClient {
    private PanelClient() {}

    public static void register(IEventBus bus) {
        bus.addListener(PanelClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MODULAR_TABLE_ENTITY.get(), ModularTableRenderer::new);
    }
}
