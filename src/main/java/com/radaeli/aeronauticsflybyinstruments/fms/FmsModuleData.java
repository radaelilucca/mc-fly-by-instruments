package com.radaeli.aeronauticsflybyinstruments.fms;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Module identity, waypoint database, and selected destination. */
public final class FmsModuleData {
    private UUID id;
    private final List<FmsWaypoint> waypoints;
    private UUID selectedWaypoint;

    public FmsModuleData() {
        this(UUID.randomUUID(), List.of(), null);
    }

    public FmsModuleData(UUID id, List<FmsWaypoint> waypoints, UUID selectedWaypoint) {
        this.id = id == null ? UUID.randomUUID() : id;
        this.waypoints = new ArrayList<>(waypoints);
        this.selectedWaypoint = selectedWaypoint;
        if (selectedWaypoint != null && find(selectedWaypoint).isEmpty()) this.selectedWaypoint = null;
    }

    public UUID id() { return id; }
    public List<FmsWaypoint> waypoints() { return List.copyOf(waypoints); }
    public Optional<UUID> selectedWaypointId() { return Optional.ofNullable(selectedWaypoint); }
    public Optional<FmsWaypoint> selectedWaypoint() {
        return selectedWaypoint == null ? Optional.empty() : find(selectedWaypoint);
    }

    public Optional<FmsWaypoint> find(UUID waypointId) {
        if (waypointId == null) return Optional.empty();
        return waypoints.stream().filter(waypoint -> waypoint.id().equals(waypointId)).findFirst();
    }

    public FmsModuleData copy() { return new FmsModuleData(id, waypoints, selectedWaypoint); }

    public boolean add(FmsWaypoint waypoint) {
        if (waypoint == null || find(waypoint.id()).isPresent()) return false;
        waypoints.add(waypoint);
        return true;
    }

    public boolean update(FmsWaypoint waypoint) {
        if (waypoint == null) return false;
        for (int i = 0; i < waypoints.size(); i++) {
            if (waypoints.get(i).id().equals(waypoint.id())) {
                waypoints.set(i, waypoint);
                return true;
            }
        }
        return false;
    }

    public boolean delete(UUID waypointId) {
        boolean removed = waypoints.removeIf(waypoint -> waypoint.id().equals(waypointId));
        if (removed && waypointId.equals(selectedWaypoint)) selectedWaypoint = null;
        return removed;
    }

    public boolean select(UUID waypointId) {
        if (waypointId != null && find(waypointId).isEmpty()) return false;
        selectedWaypoint = waypointId;
        return true;
    }

    public void replaceIdentity(UUID replacement) {
        if (replacement == null) throw new IllegalArgumentException("Module identity is required");
        id = replacement;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        if (selectedWaypoint != null) tag.putUUID("SelectedWaypoint", selectedWaypoint);
        ListTag list = new ListTag();
        waypoints.forEach(waypoint -> list.add(waypoint.save()));
        tag.put("Waypoints", list);
        return tag;
    }

    public static FmsModuleData load(CompoundTag tag) {
        UUID id = tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID();
        List<FmsWaypoint> waypoints = new ArrayList<>();
        ListTag list = tag.getList("Waypoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            FmsWaypoint waypoint = FmsWaypoint.load(list.getCompound(i));
            if (waypoint != null) waypoints.add(waypoint);
        }
        UUID selected = tag.hasUUID("SelectedWaypoint") ? tag.getUUID("SelectedWaypoint") : null;
        return new FmsModuleData(id, waypoints, selected);
    }
}
