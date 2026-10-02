package com.radaeli.aeronauticsflybyinstruments.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class TerrainScannerClient {
    private TerrainScannerClient() {}

    public static void openOrUpdate(BlockPos position, int openingAngle) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AdvancedTerrainScannerScreen screen
                && screen.position().equals(position)) {
            screen.acceptServerAngle(openingAngle);
        } else {
            minecraft.setScreen(new AdvancedTerrainScannerScreen(position, openingAngle));
        }
    }
}
