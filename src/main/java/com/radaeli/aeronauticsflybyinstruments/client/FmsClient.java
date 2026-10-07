package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.FmsScreenPayload;
import net.minecraft.client.Minecraft;

public final class FmsClient {
    private FmsClient() {}

    public static void openOrUpdate(FmsScreenPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof FmsScreen screen && screen.matches(payload)) {
            screen.acceptServerState(payload);
        } else {
            minecraft.setScreen(new FmsScreen(payload));
        }
    }
}
