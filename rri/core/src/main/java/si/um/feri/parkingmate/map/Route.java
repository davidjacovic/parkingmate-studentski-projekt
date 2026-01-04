package si.um.feri.parkingmate.map;

import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a navigation route with waypoints
 */
public class Route {
    private List<Vector2> waypoints;
    private int currentWaypointIndex;
    private float totalDistance;

    public Route() {
        this.waypoints = new ArrayList<>();
        this.currentWaypointIndex = 0;
        this.totalDistance = 0;
    }

    public void addWaypoint(Vector2 waypoint) {
        if (!waypoints.isEmpty()) {
            Vector2 last = waypoints.get(waypoints.size() - 1);
            totalDistance += last.dst(waypoint);
        }
        waypoints.add(waypoint);
    }

    public Vector2 getCurrentWaypoint() {
        if (waypoints.isEmpty() || currentWaypointIndex >= waypoints.size()) {
            return null;
        }
        return waypoints.get(currentWaypointIndex);
    }

    public boolean moveToNextWaypoint() {
        if (currentWaypointIndex < waypoints.size() - 1) {
            currentWaypointIndex++;
            return true;
        }
        return false;
    }

    public boolean isComplete() {
        return currentWaypointIndex >= waypoints.size() - 1;
    }

    public void reset() {
        currentWaypointIndex = 0;
    }

    public List<Vector2> getWaypoints() {
        return waypoints;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public int getCurrentWaypointIndex() {
        return currentWaypointIndex;
    }


    public int getTotalWaypoints() {
        return waypoints.size();
    }
}
