package com.radaeli.aeronauticsflybyinstruments.fms;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import java.util.UUID;

/** Persistent dimension-qualified, block-aligned destination stored by an FMS module. */
public record FmsWaypoint(UUID id, String name, String dimension, double x, double y, double z) {
    public static final int MAX_NAME_LENGTH = 32;
    public static final double MAX_COORDINATE = 30_000_000;

    public FmsWaypoint {
        if (id == null || name == null || dimension == null) throw new IllegalArgumentException("Waypoint fields are required");
        name = name.trim();
        if (name.isEmpty() || name.length() > MAX_NAME_LENGTH) throw new IllegalArgumentException("Waypoint name must contain 1 to 32 characters");
        if (dimension.isBlank() || dimension.length() > 256) throw new IllegalArgumentException("Invalid dimension id");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > MAX_COORDINATE || Math.abs(y) > MAX_COORDINATE || Math.abs(z) > MAX_COORDINATE) {
            throw new IllegalArgumentException("Waypoint coordinates are outside the supported range");
        }
        // Waypoints target Minecraft block positions. Floor (rather than truncate) so negative
        // coordinates resolve to the block containing that world position.
        x = Math.floor(x);
        y = Math.floor(y);
        z = Math.floor(z);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putString("Dimension", dimension);
        tag.putDouble("X", x);
        tag.putDouble("Y", y);
        tag.putDouble("Z", z);
        return tag;
    }

    public static FmsWaypoint load(CompoundTag tag) {
        if (!tag.hasUUID("Id") || !tag.contains("Name", Tag.TAG_STRING)
                || !tag.contains("Dimension", Tag.TAG_STRING)) return null;
        try {
            return new FmsWaypoint(tag.getUUID("Id"), tag.getString("Name"), tag.getString("Dimension"),
                    tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z"));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
