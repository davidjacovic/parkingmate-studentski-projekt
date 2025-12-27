package si.um.feri.parkingmate.model;

/**
 * Represents a single parking spot within a parking location.
 * Used for detailed tracking and simulation of individual spots.
 */
public class ParkingSpot {
    
    /**
     * Type of parking spot.
     */
    public enum SpotType {
        REGULAR,    // Standard parking spot
        INVALID,    // Spot for disabled persons
        BUS         // Bus parking spot
    }
    
    /**
     * Status of the parking spot.
     */
    public enum SpotStatus {
        AVAILABLE,      // Spot is available
        OCCUPIED,       // Spot is occupied by a vehicle
        RESERVED,       // Spot is reserved
        OUT_OF_ORDER    // Spot is temporarily unavailable
    }
    
    private String id;                      // Unique identifier for the spot
    private String parkingId;               // ID of the parent parking location
    private SpotType type;                  // Type of spot
    private SpotStatus status;              // Current status
    private int spotNumber;                 // Spot number within the parking location
    private String vehicleId;               // ID of vehicle currently occupying (if occupied)
    
    /**
     * Default constructor.
     */
    public ParkingSpot() {
        this.status = SpotStatus.AVAILABLE;
    }
    
    /**
     * Constructor with required fields.
     */
    public ParkingSpot(String id, String parkingId, SpotType type, int spotNumber) {
        this.id = id;
        this.parkingId = parkingId;
        this.type = type;
        this.spotNumber = spotNumber;
        this.status = SpotStatus.AVAILABLE;
    }
    
    /**
     * Full constructor with all fields.
     */
    public ParkingSpot(String id, String parkingId, SpotType type, SpotStatus status,
                       int spotNumber, String vehicleId) {
        this.id = id;
        this.parkingId = parkingId;
        this.type = type;
        this.status = status;
        this.spotNumber = spotNumber;
        this.vehicleId = vehicleId;
    }
    
    /**
     * Checks if the spot is available for parking.
     */
    public boolean isAvailable() {
        return status == SpotStatus.AVAILABLE;
    }
    
    /**
     * Checks if the spot is occupied.
     */
    public boolean isOccupied() {
        return status == SpotStatus.OCCUPIED;
    }
    
    /**
     * Checks if the spot is reserved.
     */
    public boolean isReserved() {
        return status == SpotStatus.RESERVED;
    }
    
    /**
     * Checks if the spot is out of order.
     */
    public boolean isOutOfOrder() {
        return status == SpotStatus.OUT_OF_ORDER;
    }
    
    /**
     * Marks the spot as occupied by a vehicle.
     */
    public void occupy(String vehicleId) {
        if (isAvailable()) {
            this.status = SpotStatus.OCCUPIED;
            this.vehicleId = vehicleId;
        }
    }
    
    /**
     * Marks the spot as available (vehicle left).
     */
    public void free() {
        this.status = SpotStatus.AVAILABLE;
        this.vehicleId = null;
    }
    
    /**
     * Marks the spot as reserved.
     */
    public void reserve() {
        if (isAvailable()) {
            this.status = SpotStatus.RESERVED;
        }
    }
    
    /**
     * Marks the spot as out of order.
     */
    public void setOutOfOrder() {
        this.status = SpotStatus.OUT_OF_ORDER;
        this.vehicleId = null;
    }
    
    // Getters and Setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getParkingId() {
        return parkingId;
    }
    
    public void setParkingId(String parkingId) {
        this.parkingId = parkingId;
    }
    
    public SpotType getType() {
        return type;
    }
    
    public void setType(SpotType type) {
        this.type = type;
    }
    
    public SpotStatus getStatus() {
        return status;
    }
    
    public void setStatus(SpotStatus status) {
        this.status = status;
    }
    
    public int getSpotNumber() {
        return spotNumber;
    }
    
    public void setSpotNumber(int spotNumber) {
        this.spotNumber = spotNumber;
    }
    
    public String getVehicleId() {
        return vehicleId;
    }
    
    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }
    
    @Override
    public String toString() {
        return "ParkingSpot{" +
                "id='" + id + '\'' +
                ", parkingId='" + parkingId + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", spotNumber=" + spotNumber +
                ", vehicleId='" + vehicleId + '\'' +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ParkingSpot that = (ParkingSpot) o;
        
        return id != null ? id.equals(that.id) : that.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

