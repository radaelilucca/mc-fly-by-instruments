package com.radaeli.aeronauticsflybyinstruments.sensor;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensorAutomaticOutputTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void onlyCompletePairsSelectLinkForEverySensorKind() {
        for (SensorOutputConfig config : new SensorOutputConfig[]{new SensorOutputConfig(false, true),
                new SensorOutputConfig(false, false), new SensorOutputConfig(true, false)}) {
            var channel = config.ranges().keySet().iterator().next();
            config.mode(SensorOutputConfig.Mode.LINK);
            config.selectAutomaticMode();
            assertEquals(SensorOutputConfig.Mode.FACES, config.mode());
            config.setFrequencies(channel, new SensorOutputConfig.FrequencyPair(new ItemStack(Items.STONE), ItemStack.EMPTY));
            config.selectAutomaticMode();
            assertEquals(SensorOutputConfig.Mode.FACES, config.mode());
            config.setFrequencies(channel, pair());
            config.selectAutomaticMode();
            assertEquals(SensorOutputConfig.Mode.LINK, config.mode());
            config.setFrequencies(channel, new SensorOutputConfig.FrequencyPair(ItemStack.EMPTY, ItemStack.EMPTY));
            config.selectAutomaticMode();
            assertEquals(SensorOutputConfig.Mode.FACES, config.mode());
        }
    }

    @Test void serverControllerNormalizesDraftsAndLegacyModeSetterCannotOverrideRouting() {
        var owner = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        var controller = new SensorOutputController(owner, false, true, 128);
        var draft = controller.config();
        draft.setFrequencies(SensorOutputConfig.Channel.LEFT, pair());
        draft.mode(SensorOutputConfig.Mode.FACES);
        assertTrue(controller.apply(draft, 128));
        assertEquals(SensorOutputConfig.Mode.LINK, controller.config().mode());
        assertTrue(controller.setMode("faces", 128));
        assertEquals(SensorOutputConfig.Mode.LINK, controller.config().mode());
        assertFalse(controller.setMode("invalid", 128));
        assertTrue(controller.setFrequencies(SensorOutputConfig.Channel.LEFT, ItemStack.EMPTY, ItemStack.EMPTY, 128));
        assertEquals(SensorOutputConfig.Mode.FACES, controller.config().mode());
    }

    private static SensorOutputConfig.FrequencyPair pair() {
        return new SensorOutputConfig.FrequencyPair(new ItemStack(Items.STONE), new ItemStack(Items.DIRT));
    }
}
