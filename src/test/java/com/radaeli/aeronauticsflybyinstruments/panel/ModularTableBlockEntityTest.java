package com.radaeli.aeronauticsflybyinstruments.panel;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class ModularTableBlockEntityTest {
    private static final HolderLookup.Provider REGISTRIES = HolderLookup.Provider.create(Stream.empty());

    public static ModularTableBlockEntity table() {
        return new ModularTableBlockEntity(BlockPos.ZERO, ModBlocks.MODULAR_TABLE_BASE.get().defaultBlockState());
    }

    @Test
    void savedModulesSurviveReloadAndClientUpdateTags() {
        var original = table();
        assertTrue(original.placeLamp(0, 0));
        assertTrue(original.placeLamp(2, 2));
        assertTrue(original.configureModule(0, 0, "terrain", 20));
        assertTrue(original.setTestSignal("terrain", 25));
        var restored = table();
        restored.loadCustomOnly(original.saveWithoutMetadata(REGISTRIES), REGISTRIES);
        assertEquals(original.grid().modules(), restored.grid().modules());
        assertEquals(25, restored.testSignal("terrain"));
        assertTrue(restored.isModuleActive(restored.grid().at(0, 0).orElseThrow()));
        var client = table();
        client.loadCustomOnly(original.getUpdateTag(REGISTRIES), REGISTRIES);
        assertEquals(original.grid().modules(), client.grid().modules());
        assertEquals(25, client.testSignal("terrain"));
        assertTrue(restored.removeModule(0, 0));
        assertEquals(1, restored.grid().modules().size());
    }

    @Test
    void independentLampStatesReachTheClientAndCanBeSwapped() {
        var original = table();
        assertTrue(original.placeLamp(0, 0));
        assertTrue(original.placeLamp(1, 0));
        assertTrue(original.configureModule(0, 0, "lamp_a", 0.5));
        assertTrue(original.configureModule(1, 0, "lamp_b", 0.5));
        assertTrue(original.setTestSignal("lamp_a", 1));
        assertTrue(original.setTestSignal("lamp_b", 0));

        var client = table();
        client.loadCustomOnly(original.getUpdateTag(REGISTRIES), REGISTRIES);
        assertTrue(client.isModuleActive(client.grid().at(0, 0).orElseThrow()));
        assertFalse(client.isModuleActive(client.grid().at(1, 0).orElseThrow()));

        assertTrue(original.setTestSignal("lamp_a", 0));
        assertTrue(original.setTestSignal("lamp_b", 1));
        client.loadCustomOnly(original.getUpdateTag(REGISTRIES), REGISTRIES);
        assertFalse(client.isModuleActive(client.grid().at(0, 0).orElseThrow()));
        assertTrue(client.isModuleActive(client.grid().at(1, 0).orElseThrow()));
    }

    @Test
    void lampTestIntensityIsTransientAndSyncedOnlyToClientUpdates() {
        var original = table();
        assertTrue(original.placeLamp(0, 0));
        var lamp = original.grid().at(0, 0).orElseThrow();
        assertTrue(original.setLampTestIntensity(0, 0, 7));
        assertEquals(7, original.lampRenderIntensity(lamp));

        var client = table();
        client.loadCustomOnly(original.getUpdateTag(REGISTRIES), REGISTRIES);
        assertEquals(7, client.lampRenderIntensity(client.grid().at(0, 0).orElseThrow()));

        var saved = original.saveWithoutMetadata(REGISTRIES);
        assertFalse(saved.contains("LampTestIntensities"));
        var restored = table();
        restored.loadCustomOnly(saved, REGISTRIES);
        assertEquals(0, restored.lampRenderIntensity(restored.grid().at(0, 0).orElseThrow()));

        assertTrue(original.configureModule(0, 0, "active", 0.5));
        assertTrue(original.setTestSignal("active", 1));
        assertTrue(original.clearLampTestIntensity(0, 0));
        assertEquals(15, original.lampRenderIntensity(original.grid().at(0, 0).orElseThrow()));
    }

    @Test
    void corruptedDuplicateOutOfBoundsAndUnknownModulesAreIgnored() {
        var data = new CompoundTag();
        var modules = new ListTag();
        for (String type : new String[]{"lamp", "lamp", "unknown"}) {
            var entry = new CompoundTag();
            entry.putString("Type", type);
            entry.putInt("Width", 1);
            entry.putInt("Height", 1);
            modules.add(entry);
        }
        var bad = new CompoundTag();
        bad.putString("Type", "lamp");
        bad.putInt("Column", 3);
        bad.putInt("Width", 1);
        bad.putInt("Height", 1);
        modules.add(bad);
        data.put("Modules", modules);
        var restored = table();
        restored.loadCustomOnly(data, REGISTRIES);
        assertEquals(1, restored.grid().modules().size());
    }
}
