package com.radaeli.aeronauticsflybyinstruments.avionics;

import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = AeronauticsFlyByInstruments.MOD_ID)
public final class AvionicsNetworkEvents {
    private AvionicsNetworkEvents() {}

    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof Level level) AvionicsNetworkManager.markDirty(level);
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level) AvionicsNetworkManager.markDirty(level);
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        AvionicsNetworkManager.markDirty(event.getPlayer().level());
    }

    @SubscribeEvent
    public static void onChunkLoaded(ChunkEvent.Load event) {
        if (event.getLevel() instanceof Level level) AvionicsNetworkManager.markDirty(level);
    }

    @SubscribeEvent
    public static void onChunkUnloaded(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof Level level) AvionicsNetworkManager.markDirty(level);
    }

    @SubscribeEvent
    public static void onLevelUnloaded(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) AvionicsNetworkManager.clearLevel(level);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        AvionicsNetworkManager.flushDirty();
    }
}
