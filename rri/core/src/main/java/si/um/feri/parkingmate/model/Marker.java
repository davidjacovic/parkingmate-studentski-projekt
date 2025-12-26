package si.um.feri.parkingmate.model;

import si.um.feri.parkingmate.map.Geolocation;

/**
 * Represents a marker on the map.
 * Contains position, type, and state information.
 */
public class Marker {
    
    /**
     * Marker types for different parking lot categories.
     */
    public enum MarkerType {
        PARKING_LOT,    // Standard parking lot
        STREET_PARKING, // Street parking
        GARAGE,         // Parking garage
        PRIVATE         // Private parking
    }
    
    /**
     * Marker state representing occupancy level.
     */
    public enum MarkerState {
        FREE,           // All spots available (green)
        PARTIAL,        // Some spots available (yellow)
        FULL,           // No spots available (red)
        UNKNOWN         // Occupancy data not available (gray)
    }
    
    private Geolocation position;
    private MarkerType type;
    private MarkerState state;
    private String id;              // Unique identifier (e.g., from API)
    private String name;             // Display name
    private int totalSpots;          // Total number of parking spots
    private int availableSpots;      // Number of available spots
    private float pricePerHour;      // Price per hour (optional)
    
    /**
     * Constructor with required fields.
     */
    public Marker(Geolocation position, MarkerType type, MarkerState state) {
        this.position = position;
        this.type = type;
        this.state = state;
        this.totalSpots = 0;
        this.availableSpots = 0;
        this.pricePerHour = 0f;
    }
    
    /**
     * Full constructor with all fields.
     */
    public Marker(Geolocation position, MarkerType type, MarkerState state, 
                  String id, String name, int totalSpots, int availableSpots, float pricePerHour) {
        this.position = position;
        this.type = type;
        this.state = state;
        this.id = id;
        this.name = name;
        this.totalSpots = totalSpots;
        this.availableSpots = availableSpots;
        this.pricePerHour = pricePerHour;
    }
    
    /**
     * Calculates and updates the marker state based on available spots.
     */
    public void updateStateFromSpots() {
        if (totalSpots == 0) {
            this.state = MarkerState.UNKNOWN;
        } else {
            float occupancyRatio = (float) availableSpots / totalSpots;
            if (occupancyRatio >= 0.5f) {
                this.state = MarkerState.FREE;
            } else if (occupancyRatio > 0f) {
                this.state = MarkerState.PARTIAL;
            } else {
                this.state = MarkerState.FULL;
            }
        }
    }
    
    // Getters and Setters
    
    public Geolocation getPosition() {
        return position;
    }
    
    public void setPosition(Geolocation position) {
        this.position = position;
    }
    
    public MarkerType getType() {
        return type;
    }
    
    public void setType(MarkerType type) {
        this.type = type;
    }
    
    public MarkerState getState() {
        return state;
    }
    
    public void setState(MarkerState state) {
        this.state = state;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public int getTotalSpots() {
        return totalSpots;
    }
    
    public void setTotalSpots(int totalSpots) {
        this.totalSpots = totalSpots;
        updateStateFromSpots();
    }
    
    public int getAvailableSpots() {
        return availableSpots;
    }
    
    public void setAvailableSpots(int availableSpots) {
        this.availableSpots = availableSpots;
        updateStateFromSpots();
    }
    
    public float getPricePerHour() {
        return pricePerHour;
    }
    
    public void setPricePerHour(float pricePerHour) {
        this.pricePerHour = pricePerHour;
    }
    
    /**
     * Returns occupancy percentage (0-100).
     */
    public float getOccupancyPercentage() {
        if (totalSpots == 0) {
            return 0f;
        }
        return ((float) (totalSpots - availableSpots) / totalSpots) * 100f;
    }
    
    @Override
    public String toString() {
        return "Marker{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", position=(" + position.lat + ", " + position.lng + ")" +
                ", type=" + type +
                ", state=" + state +
                ", spots=" + availableSpots + "/" + totalSpots +
                '}';
    }
}

