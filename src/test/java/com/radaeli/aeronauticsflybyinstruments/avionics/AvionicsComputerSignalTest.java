package com.radaeli.aeronauticsflybyinstruments.avionics;

import com.radaeli.aeronauticsflybyinstruments.block.ModBlocks;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AvionicsComputerSignalTest {
    @Test
    void gpwsPublishesEveryAlertAndTheAggregateChannel() {
        var gpws = new AvionicsComputerBlockEntity(BlockPos.ZERO,
                ModBlocks.AVIONICS_COMPUTER.get().defaultBlockState());
        var signals = gpws.avionicsSignals();
        assertEquals(AlertChannel.values().length + 1, signals.size());
        assertTrue(signals.stream().anyMatch(signal -> signal.name().equals("gpws.any")));
        for (AlertChannel channel : AlertChannel.values())
            assertTrue(signals.stream().anyMatch(signal -> signal.name().equals("gpws." + channel.id())));
        var aggregate = signals.stream().filter(signal -> signal.name().equals("gpws.any")).findFirst().orElseThrow();
        assertEquals(ReadingStatus.UNAVAILABLE, aggregate.reading().status());
    }
}
