package com.radaeli.aeronauticsflybyinstruments.fms;

import com.radaeli.aeronauticsflybyinstruments.avionics.SensorReading;
import net.minecraft.world.phys.Vec3;

/** Pure conversion of selected FMS target and aircraft pose to the two public data channels. */
public final class FmsSignalLogic {
    private FmsSignalLogic() {}

    public static FmsSignalSnapshot sample(FmsModuleData data, String currentDimension,
                                           Vec3 globalPosition, long gameTime) {
        var waypoint = data.selectedWaypoint();
        boolean selected = waypoint.isPresent();
        SensorReading<Boolean> selectedReading = SensorReading.valid(selected, "boolean", gameTime);
        if (waypoint.isEmpty() || !waypoint.get().dimension().equals(currentDimension)) {
            return new FmsSignalSnapshot(selectedReading, SensorReading.unavailable("blocks", gameTime));
        }
        var target = waypoint.get();
        double dx = target.x() - globalPosition.x;
        double dz = target.z() - globalPosition.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        return Double.isFinite(distance)
                ? new FmsSignalSnapshot(selectedReading, SensorReading.valid(distance, "blocks", gameTime))
                : new FmsSignalSnapshot(selectedReading, SensorReading.failed("blocks", gameTime));
    }

    public static boolean sameValues(FmsSignalSnapshot left, FmsSignalSnapshot right) {
        if (left == null || right == null) return left == right;
        return left.waypointSelected().status() == right.waypointSelected().status()
                && left.waypointSelected().value().equals(right.waypointSelected().value())
                && left.distanceToWaypoint().status() == right.distanceToWaypoint().status()
                && left.distanceToWaypoint().value().equals(right.distanceToWaypoint().value());
    }
}
