package com.radaeli.flightsafetyinstruments.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ModConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec.IntValue MAX_AVIONICS_NETWORK_BLOCKS = BUILDER
            .comment("Maximum cable and endpoint blocks visited while discovering one avionics network.")
            .defineInRange("maxAvionicsNetworkBlocks", 4096, 1, 1_000_000);
    public static final ModConfigSpec.IntValue SENSOR_SCAN_INTERVAL_TICKS = BUILDER
            .comment("Ticks between server-side acquisition sensor scans.")
            .defineInRange("sensorScanIntervalTicks", 5, 1, 1200);
    public static final ModConfigSpec.IntValue RADAR_ALTIMETER_MAX_RANGE = BUILDER
            .comment("Maximum Radar Altimeter range in blocks.")
            .defineInRange("radarAltimeterMaxRange", 128, 1, 4096);
    public static final ModConfigSpec.IntValue TERRAIN_SCANNER_MAX_RANGE = BUILDER
            .comment("Maximum Terrain Scanner range in blocks.")
            .defineInRange("terrainScannerMaxRange", 128, 1, 4096);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private ModConfig() {}
}
