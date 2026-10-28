package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client wiring for the Avionics Wire: renderer, connector socket model, preview and Wiring View. */
public final class WireClient {
    private WireClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(ModBlocks.WIRE_NODE_ENTITY.get(), WireRenderer::new));
        modBus.addListener(WireConnectorModels::wrapModels);
        modBus.addListener(WiringView::registerKeyMapping);
        NeoForge.EVENT_BUS.addListener(WiringView::onClientTick);
        NeoForge.EVENT_BUS.addListener(WiringView::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(WiringView::onRenderGui);
        NeoForge.EVENT_BUS.addListener(WiringView::onInteraction);
        NeoForge.EVENT_BUS.addListener(WirePreview::onRenderLevel);
        NeoForge.EVENT_BUS.addListener(WirePreview::onRenderGui);
    }
}
