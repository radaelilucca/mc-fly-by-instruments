package com.radaeli.aeronauticsflybyinstruments.panel;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TableIndicationTest {
    private static final HolderLookup.Provider REGISTRIES = HolderLookup.Provider.create(Stream.empty());

    @Test
    void newLampsShowGpwsCautionInAmberAndBuzzersSoundOnEveryAlert() {
        var table = ModularTableBlockEntityTest.table();
        assertTrue(table.placeLamp(0, 0));
        assertTrue(table.placeBuzzer(1, 0));
        var lamp = table.grid().at(0, 0).orElseThrow();
        assertEquals("caution", table.indication(lamp));
        assertEquals(LampIndication.Color.AMBER, table.lampColor(lamp));
        assertEquals("caution", table.indication(table.grid().at(1, 0).orElseThrow()));
    }

    @Test
    void indicationsApplyOnlyWhenOfferedForTheModuleType() {
        var table = ModularTableBlockEntityTest.table();
        assertTrue(table.placeLamp(0, 0));
        assertTrue(table.placeBuzzer(1, 0));
        assertTrue(table.setIndication(0, 0, "warning"));
        assertEquals(LampIndication.Color.RED, table.lampColor(table.grid().at(0, 0).orElseThrow()));
        assertTrue(table.setIndication(0, 0, "function:pull_up"));
        assertEquals("gpws.pull_up", table.grid().at(0, 0).orElseThrow().alertChannel());
        assertFalse(table.setIndication(0, 0, "redstone_link"), "Reserved, not offered yet");
        assertFalse(table.setIndication(1, 0, "inop"), "Buzzers only pick all alerts or red only");
        assertTrue(table.setIndication(1, 0, "warning"));
        assertFalse(table.setIndication(2, 2, "warning"), "Empty cell");
    }

    @Test
    void inopLampLightsWhileNoComputerAnswers() {
        var table = ModularTableBlockEntityTest.table();
        assertTrue(table.placeLamp(0, 0));
        assertTrue(table.setIndication(0, 0, "inop"));
        var lamp = table.grid().at(0, 0).orElseThrow();
        assertTrue(table.isModuleActive(lamp), "No cable, no computer: GPWS INOP");
        assertEquals(2, table.moduleSeverity(lamp));
        assertEquals("disconnected", table.panelStatusId());
    }

    @Test
    void clientsReceiveTheAlertStateThatLightsTheLamps() {
        var server = ModularTableBlockEntityTest.table();
        assertTrue(server.placeLamp(0, 0));
        server.setMuted(true);
        CompoundTag update = server.getUpdateTag(REGISTRIES);
        assertTrue(update.contains("AlertSeverities"), "The lamp state must reach the client");
        var client = ModularTableBlockEntityTest.table();
        client.loadCustomOnly(update, REGISTRIES);
        assertTrue(client.muted());
        assertEquals(server.alertNetworkStatus(), client.alertNetworkStatus());
        assertFalse(server.saveWithoutMetadata(REGISTRIES).contains("AlertSeverities"),
                "Runtime alert state is never saved to disk");
    }

    @Test
    void reservedRedstoneLinkModulesLoadButNeverLight() {
        var table = ModularTableBlockEntityTest.table();
        assertTrue(table.placeLamp(0, 0));
        assertTrue(table.grid().configureAlertAt(0, 0, LampIndication.SOURCE_REDSTONE_LINK, "gpws.any", 2));
        var saved = table.saveWithoutMetadata(REGISTRIES);
        var restored = ModularTableBlockEntityTest.table();
        restored.loadCustomOnly(saved, REGISTRIES);
        var lamp = restored.grid().at(0, 0).orElseThrow();
        assertEquals("redstone_link", restored.indication(lamp));
        assertFalse(restored.isModuleActive(lamp));
    }
}
