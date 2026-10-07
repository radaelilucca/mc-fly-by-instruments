package com.radaeli.aeronauticsflybyinstruments.fms;

import com.radaeli.aeronauticsflybyinstruments.avionics.SensorReading;

public record FmsSignalSnapshot(SensorReading<Boolean> waypointSelected,
                                SensorReading<Double> distanceToWaypoint) {
    public static final String WAYPOINT_SELECTED = "waypoint_selected";
    public static final String DISTANCE_TO_WAYPOINT = "distance_to_waypoint";
}
