package com.radaeli.aeronauticsflybyinstruments.client;

import com.radaeli.aeronauticsflybyinstruments.network.SensorOutputScreenPayload;
import net.minecraft.client.Minecraft;

public final class SensorOutputClient {
    private SensorOutputClient() {}
    public static void openOrUpdate(SensorOutputScreenPayload payload) {
        Minecraft.getInstance().setScreen(new SensorOutputConfigScreen(payload));
    }
}
