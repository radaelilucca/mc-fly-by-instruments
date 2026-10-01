package com.radaeli.flightsafetyinstruments.panel;

import com.radaeli.flightsafetyinstruments.block.ModBlocks;
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
        var restored = table();
        restored.loadCustomOnly(original.saveWithoutMetadata(REGISTRIES), REGISTRIES);
        assertEquals(original.grid().modules(), restored.grid().modules());
        var client = table();
        client.loadCustomOnly(original.getUpdateTag(REGISTRIES), REGISTRIES);
        assertEquals(original.grid().modules(), client.grid().modules());
        assertTrue(restored.removeModule(0, 0));
        assertEquals(1, restored.grid().modules().size());
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
