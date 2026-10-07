package com.radaeli.aeronauticsflybyinstruments.network;

import com.radaeli.aeronauticsflybyinstruments.fms.FmsModuleData;
import com.radaeli.aeronauticsflybyinstruments.fms.FmsWaypoint;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.ArrayList;
import java.util.UUID;

final class FmsNetworkWire {
    private FmsNetworkWire() {}

    static void writeData(ByteBuf buf, FmsModuleData data) {
        writeUuid(buf, data.id());
        writeUuid(buf, data.selectedWaypointId().orElse(null));
        ByteBufCodecs.VAR_INT.encode(buf, data.waypoints().size());
        for (FmsWaypoint waypoint : data.waypoints()) {
            writeUuid(buf, waypoint.id());
            ByteBufCodecs.STRING_UTF8.encode(buf, waypoint.name());
            ByteBufCodecs.STRING_UTF8.encode(buf, waypoint.dimension());
            buf.writeDouble(waypoint.x());
            buf.writeDouble(waypoint.y());
            buf.writeDouble(waypoint.z());
        }
    }

    static FmsModuleData readData(ByteBuf buf) {
        UUID id = readUuid(buf);
        UUID selected = readUuid(buf);
        int count = ByteBufCodecs.VAR_INT.decode(buf);
        if (count < 0) throw new io.netty.handler.codec.DecoderException("Negative FMS waypoint count");
        // Do not preallocate from a packet-supplied count; payload contents remain the bound.
        var waypoints = new ArrayList<FmsWaypoint>();
        for (int i = 0; i < count; i++) {
            UUID waypointId = readUuid(buf);
            String name = ByteBufCodecs.STRING_UTF8.decode(buf);
            String dimension = ByteBufCodecs.STRING_UTF8.decode(buf);
            var waypoint = new FmsWaypoint(waypointId, name, dimension,
                    buf.readDouble(), buf.readDouble(), buf.readDouble());
            waypoints.add(waypoint);
        }
        return new FmsModuleData(id, waypoints, selected);
    }

    static void writeUuid(ByteBuf buf, UUID id) {
        buf.writeBoolean(id != null);
        if (id != null) {
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());
        }
    }

    static UUID readUuid(ByteBuf buf) {
        return buf.readBoolean() ? new UUID(buf.readLong(), buf.readLong()) : null;
    }

    static void writePosition(ByteBuf buf, BlockPos position) {
        BlockPos.STREAM_CODEC.encode(buf, position);
    }

    static BlockPos readPosition(ByteBuf buf) {
        return BlockPos.STREAM_CODEC.decode(buf);
    }
}
