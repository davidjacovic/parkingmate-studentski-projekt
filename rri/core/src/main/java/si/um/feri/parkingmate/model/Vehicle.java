package si.um.feri.parkingmate.model;

import com.badlogic.gdx.math.Vector2;

/**
 * Vehicle model for visual simulation and animation in the application.
 * Used exclusively on the client side
 */
public class Vehicle {

    // Basic information
    private String registrationNumber;
    private String vehicleType;

    // Position and movement
    private Vector2 position;        // current position
    private Vector2 targetPosition;  // parking spot (destination)
    private float speed;             // movement speed (px/s)

    // Vehicle state
    private VehicleState state;

    /**
     * Animation states of the vehicle
     */
    public enum VehicleState {
        ENTERING,   // vehicle is approaching the parking spot
        PARKED      // vehicle is parked
    }

    /**
     * Creates a vehicle entering the parking lot
     *
     * @param startPosition starting position (entrance to parking)
     * @param targetPosition target position (parking spot)
     */
    public Vehicle(Vector2 startPosition, Vector2 targetPosition) {
        this.position = new Vector2(startPosition);
        this.targetPosition = new Vector2(targetPosition);
        this.speed = 80f; // default speed
        this.state = VehicleState.ENTERING;
    }

    /**
     * Updates the vehicle's position (called every frame)
     *
     * @param delta time elapsed between frames
     */
    public void update(float delta) {
        if (state != VehicleState.ENTERING) {
            return;
        }

        Vector2 direction = targetPosition.cpy().sub(position);

        // Check if the vehicle has reached its destination
        if (direction.len() < 2f) {
            position.set(targetPosition);
            state = VehicleState.PARKED;
            return;
        }

        // Move the vehicle towards the target
        direction.nor();
        position.mulAdd(direction, speed * delta);
    }

    // GETTERS & SETTERS

    public Vector2 getPosition() {
        return position;
    }

    public Vector2 getTargetPosition() {
        return targetPosition;
    }

    public float getSpeed() {
        return speed;
    }

    public VehicleState getState() {
        return state;
    }

    public boolean isParked() {
        return state == VehicleState.PARKED;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
}
