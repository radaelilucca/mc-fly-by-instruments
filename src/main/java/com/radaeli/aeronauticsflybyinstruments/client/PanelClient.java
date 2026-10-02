package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;

public final class PanelClient {
    private PanelClient() {}

    public static void register(IEventBus bus) {
        bus.addListener(PanelClient::registerRenderers);
        bus.addListener(PanelClient::registerRenderBuffers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MODULAR_TABLE_ENTITY.get(), ModularTableRenderer::new);
    }

    private static void registerRenderBuffers(RegisterRenderBuffersEvent event) {
        // Batch and sort all luminous surfaces after the opaque geometry, without flushing
        // the shared buffer once per module or letting the lens hide its own core.
        event.registerRenderBuffer(LampModuleRenderer.EMISSIVE);
    }
}
