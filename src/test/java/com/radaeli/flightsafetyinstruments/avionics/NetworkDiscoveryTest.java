package com.radaeli.flightsafetyinstruments.avionics;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NetworkDiscoveryTest {
    private static final BlockPos ORIGIN = new BlockPos(0, 0, 0);

    @Test
    void discoversLinearNetworkAndRelativeEndpoint() {
        var world = new FakeAccess();
        world.cables(1, 2, 3);
        world.endpoint(new BlockPos(4, 0, 0), "modular_table");

        var snapshot = NetworkDiscovery.discover(ORIGIN, world, 4096);

        assertEquals(NetworkStatus.CONNECTED, snapshot.status());
        assertEquals(3, snapshot.cableCount());
        assertEquals(1, snapshot.endpointCount());
        assertEquals(new NetworkDevice("modular_table", 4, 0, 0), snapshot.devices().getFirst());
    }

    @Test
    void visitsBranchesAndCyclesOnlyOnce() {
        var world = new FakeAccess();
        world.cable(1, 0, 0);
        world.cable(2, 0, 0);
        world.cable(2, 1, 0);
        world.cable(1, 1, 0);
        world.cable(2, -1, 0);
        world.endpoint(new BlockPos(3, 0, 0), "modular_table");
        world.endpoint(new BlockPos(2, -2, 0), "modular_table");

        var snapshot = NetworkDiscovery.discover(ORIGIN, world, 4096);

        assertEquals(NetworkStatus.CONNECTED, snapshot.status());
        assertEquals(5, snapshot.cableCount());
        assertEquals(2, snapshot.endpointCount());
        assertEquals(2, snapshot.devices().size());
    }

    @Test
    void reportsDisconnectedWhenNoCableTouchesEndpoint() {
        var snapshot = NetworkDiscovery.discover(ORIGIN, new FakeAccess(), 4096);
        assertEquals(NetworkSnapshot.disconnected(), snapshot);
    }

    @Test
    void unloadedBoundaryMakesResultIncompleteWithoutReturningPartialDevices() {
        var world = new FakeAccess();
        world.cable(1, 0, 0);
        world.endpoint(new BlockPos(2, 0, 0), "modular_table");
        world.unloaded.add(new BlockPos(1, 1, 0));

        var snapshot = NetworkDiscovery.discover(ORIGIN, world, 4096);

        assertEquals(NetworkStatus.INCOMPLETE, snapshot.status());
        assertFalse(snapshot.complete());
        assertEquals(1, snapshot.endpointCount());
        assertTrue(snapshot.devices().isEmpty());
    }

    @Test
    void neverQueriesBlocksInsideAnUnloadedChunk() {
        var world = new FakeAccess();
        world.cable(1, 0, 0);
        world.unloaded.add(new BlockPos(1, 1, 0));
        world.throwWhenQueryingUnloaded = true;

        var snapshot = NetworkDiscovery.discover(ORIGIN, world, 4096);

        assertEquals(NetworkStatus.INCOMPLETE, snapshot.status());
        assertEquals(0, world.unloadedBlockQueries);
    }

    @Test
    void stopsAtConfiguredVisitedBlockLimit() {
        var world = new FakeAccess();
        world.cable(1, 0, 0);
        world.endpoint(new BlockPos(2, 0, 0), "modular_table");

        var snapshot = NetworkDiscovery.discover(ORIGIN, world, 1);

        assertEquals(NetworkStatus.LIMIT_EXCEEDED, snapshot.status());
        assertFalse(snapshot.complete());
        assertTrue(snapshot.devices().isEmpty());
    }

    @Test
    void aFreshDiscoveryReflectsCableRemovalAndReconnection() {
        var world = new FakeAccess();
        world.cable(1, 0, 0);
        world.cable(2, 0, 0);
        world.endpoint(new BlockPos(3, 0, 0), "modular_table");
        assertEquals(NetworkStatus.CONNECTED, NetworkDiscovery.discover(ORIGIN, world, 4096).status());

        world.cables.clear();
        assertEquals(NetworkStatus.DISCONNECTED, NetworkDiscovery.discover(ORIGIN, world, 4096).status());

        world.cable(1, 0, 0);
        world.cable(2, 0, 0);
        assertEquals(NetworkStatus.CONNECTED, NetworkDiscovery.discover(ORIGIN, world, 4096).status());
    }

    private static final class FakeAccess implements NetworkAccess {
        private final Set<BlockPos> cables = new HashSet<>();
        private final Set<BlockPos> unloaded = new HashSet<>();
        private final Map<BlockPos, String> endpoints = new HashMap<>();
        private boolean throwWhenQueryingUnloaded;
        private int unloadedBlockQueries;

        void cable(int x, int y, int z) { cables.add(new BlockPos(x, y, z)); }
        void cables(int... xs) { for (int x : xs) cable(x, 0, 0); }
        void endpoint(BlockPos pos, String type) { endpoints.put(pos, type); }
        @Override public boolean isLoaded(BlockPos pos) { return !unloaded.contains(pos); }
        @Override public boolean isCable(BlockPos pos) {
            assertLoadedBeforeBlockQuery(pos);
            return cables.contains(pos);
        }
        @Override public String endpointType(BlockPos pos) {
            assertLoadedBeforeBlockQuery(pos);
            return endpoints.get(pos);
        }
        private void assertLoadedBeforeBlockQuery(BlockPos pos) {
            if (unloaded.contains(pos)) {
                unloadedBlockQueries++;
                if (throwWhenQueryingUnloaded) throw new AssertionError("Queried block in unloaded chunk: " + pos);
            }
        }
    }
}
