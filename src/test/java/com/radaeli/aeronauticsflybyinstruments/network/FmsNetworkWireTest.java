package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.fms.FmsModuleData;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsWaypoint;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FmsNetworkWireTest {
    @Test
    void screenDataCodecRoundTripsWaypointStateAndNavigatorBindingData() {
        var waypoint = new FmsWaypoint(UUID.randomUUID(), "Landing Site", "minecraft:overworld", 3.25, 82, -9.5);
        var data = new FmsModuleData(UUID.randomUUID(), List.of(waypoint), waypoint.id());
        var buffer = Unpooled.buffer();

        FmsNetworkWire.writeData(buffer, data);
        var decoded = FmsNetworkWire.readData(buffer);

        assertEquals(data.id(), decoded.id());
        assertEquals(data.waypoints(), decoded.waypoints());
        assertEquals(data.selectedWaypointId(), decoded.selectedWaypointId());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void actionPayloadCodecRoundTripsCoordinatesAndConfirmation() {
        var payload = new FmsActionPayload(new net.minecraft.core.BlockPos(-10, 64, 24), 2, 1,
                "update", UUID.randomUUID().toString(), "Mountain", -14.5, 88, 120.25, 34, true);
        var buffer = Unpooled.buffer();

        FmsActionPayload.STREAM_CODEC.encode(buffer, payload);
        var decoded = FmsActionPayload.STREAM_CODEC.decode(buffer);

        assertEquals(payload.position(), decoded.position());
        assertEquals(payload.column(), decoded.column());
        assertEquals(payload.row(), decoded.row());
        assertEquals(payload.action(), decoded.action());
        assertEquals(payload.waypointId(), decoded.waypointId());
        assertEquals(payload.name(), decoded.name());
        assertEquals(payload.x(), decoded.x());
        assertEquals(payload.y(), decoded.y());
        assertEquals(payload.z(), decoded.z());
        assertEquals(payload.navigatorDisplaySlot(), decoded.navigatorDisplaySlot());
        assertTrue(decoded.confirmRebind());
    }
}
