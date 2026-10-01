package com.radaeli.flightsafetyinstruments.avionics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bounded, six-neighbor graph traversal. It never queries blocks in unloaded chunks. */
public final class NetworkDiscovery {
    private NetworkDiscovery() {}

    public static NetworkSnapshot discover(BlockPos origin, NetworkAccess world, int maxVisitedBlocks) {
        if (maxVisitedBlocks < 1) throw new IllegalArgumentException("maxVisitedBlocks must be positive");
        Set<BlockPos> cables = new HashSet<>();
        Set<BlockPos> endpoints = new HashSet<>();
        ArrayDeque<BlockPos> pending = new ArrayDeque<>();
        boolean incomplete = false;

        for (Direction direction : Direction.values()) {
            BlockPos adjacent = origin.relative(direction);
            if (!world.isLoaded(adjacent)) {
                incomplete = true;
            } else if (world.isCable(adjacent) && cables.add(adjacent)) {
                pending.add(adjacent);
            }
        }
        if (pending.isEmpty()) {
            return incomplete
                    ? new NetworkSnapshot(NetworkStatus.INCOMPLETE, 0, 0, List.of())
                    : NetworkSnapshot.disconnected();
        }

        while (!pending.isEmpty()) {
            BlockPos cable = pending.removeFirst();
            if (cables.size() + endpoints.size() > maxVisitedBlocks) {
                return new NetworkSnapshot(NetworkStatus.LIMIT_EXCEEDED, cables.size(), endpoints.size(), List.of());
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = cable.relative(direction);
                if (!world.isLoaded(neighbor)) {
                    incomplete = true;
                    continue;
                }
                if (world.isCable(neighbor)) {
                    if (cables.add(neighbor)) {
                        if (cables.size() + endpoints.size() > maxVisitedBlocks) {
                            return new NetworkSnapshot(NetworkStatus.LIMIT_EXCEEDED, cables.size(), endpoints.size(), List.of());
                        }
                        pending.addLast(neighbor);
                    }
                    continue;
                }
                String endpointType = world.endpointType(neighbor);
                if (endpointType != null && !neighbor.equals(origin)) endpoints.add(neighbor);
                if (cables.size() + endpoints.size() > maxVisitedBlocks) {
                    return new NetworkSnapshot(NetworkStatus.LIMIT_EXCEEDED, cables.size(), endpoints.size(), List.of());
                }
            }
        }

        var status = incomplete ? NetworkStatus.INCOMPLETE : NetworkStatus.CONNECTED;
        List<NetworkDevice> devices = new ArrayList<>();
        if (!incomplete) {
            for (BlockPos pos : endpoints) {
                String type = world.endpointType(pos);
                if (type != null) devices.add(new NetworkDevice(type,
                        pos.getX() - origin.getX(), pos.getY() - origin.getY(), pos.getZ() - origin.getZ()));
            }
        }
        return new NetworkSnapshot(status, cables.size(), endpoints.size(), devices);
    }
}
