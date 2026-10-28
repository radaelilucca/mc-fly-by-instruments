package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.AvionicsComputerScreenPayload;
import net.minecraft.client.Minecraft;

public final class AvionicsComputerClient {
    private AvionicsComputerClient() {}

    public static void openOrUpdate(AvionicsComputerScreenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof AvionicsComputerScreen screen && screen.position().equals(payload.position())) {
            screen.accept(payload);
        } else {
            minecraft.setScreen(new AvionicsComputerScreen(payload));
        }
    }
}
