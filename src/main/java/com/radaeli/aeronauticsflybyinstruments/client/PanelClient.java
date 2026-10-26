package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.PanelScreenPayload;
import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;
import net.minecraft.client.Minecraft;

public final class PanelClient {
    private PanelClient() { }
    public static void register(IEventBus bus) {
        bus.addListener(PanelClient::registerRenderers);
        bus.addListener(PanelClient::registerRenderBuffers);
    }
    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MODULAR_TABLE_ENTITY.get(), ModularTableRenderer::new);
    }
    private static void registerRenderBuffers(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(LampModuleRenderer.EMISSIVE);
    }
    public static void open(PanelScreenPayload payload) {
        Minecraft.getInstance().setScreen(new PanelScreen(payload));
    }
}
