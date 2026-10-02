package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.GpwsScreenPayload;
import net.minecraft.client.Minecraft;

public final class GpwsClient {
    private GpwsClient() {}

    public static void openOrUpdate(GpwsScreenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GpwsComputerScreen screen && screen.position().equals(payload.position())) {
            screen.accept(payload);
        } else {
            minecraft.setScreen(new GpwsComputerScreen(payload));
        }
    }
}
