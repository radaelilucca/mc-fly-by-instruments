package com.radaeli.aeronauticsflybyinstruments.fms;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import com.radaeli.aeronauticsflybyinstruments.AeronauticsFlyByInstruments;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.registrate.SimulatedRegistrate;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

public final class FmsRegistries {
    public static final FmsNavigationTarget NAVIGATOR_TARGET = new FmsNavigationTarget();
    private static final SimulatedRegistrate REGISTRATE = new SimulatedRegistrate(
            ResourceLocation.fromNamespaceAndPath(AeronauticsFlyByInstruments.MOD_ID, "fms"),
            AeronauticsFlyByInstruments.MOD_ID);
    public static final Supplier<FmsNavigationTarget> FMS_NAVIGATOR_TARGET;

    static {
        FMS_NAVIGATOR_TARGET = REGISTRATE.navTarget("fms_navigator", () -> NAVIGATOR_TARGET,
                () -> ModBlocks.FMS_NAVIGATOR.get());
    }

    public static void register(IEventBus modEventBus) {
        REGISTRATE.registerEventListeners(modEventBus);
    }

    private FmsRegistries() {}
}
