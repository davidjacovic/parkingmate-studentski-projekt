package si.um.feri.parkingmate.model;

import si.um.feri.parkingmate.map.Geolocation;

import java.util.Date;

/**
 * Represents a parking location entity.
 * Maps to the backend parking_location model from MongoDB.
 */
public class Parking {
    
    private String id;                      // MongoDB _id
    private String name;                    // Parking location name
    private String address;                 // Street address
    private Geolocation location;           // Geographic coordinates (lat, lng)
    
    // Total spots by type
    private int totalRegularSpots;          // Total regular parking spots
    private int totalInvalidSpots;          // Total spots for disabled persons
    private int totalBusSpots;              // Total bus parking spots
    
    // Available spots by type
    private int availableRegularSpots;      // Available regular spots
    private int availableInvalidSpots;      // Available invalid spots
    private int availableBusSpots;          // Available bus spots
    
    // Metadata
    private String description;              // Optional description
    private boolean hidden;                 // Whether location is hidden from public view
    private Date created;                   // Creation timestamp
    private Date modified;                  // Last modification timestamp
    
    /**
     * Default constructor.
     */
    public Parking() {
        this.hidden = false;
    }
    
    /**
     * Constructor with required fields.
     */
    public Parking(String id, String name, Geolocation location) {
        this.id = id;
        this.name = name;
        this.location = location;
        this.hidden = false;
    }
    
    /**
     * Full constructor with all fields.
     */
    public Parking(String id, String name, String address, Geolocation location,
                   int totalRegularSpots, int totalInvalidSpots, int totalBusSpots,
                   int availableRegularSpots, int availableInvalidSpots, int availableBusSpots,
                   String description, boolean hidden, Date created, Date modified) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.location = location;
        this.totalRegularSpots = totalRegularSpots;
        this.totalInvalidSpots = totalInvalidSpots;
        this.totalBusSpots = totalBusSpots;
        this.availableRegularSpots = availableRegularSpots;
        this.availableInvalidSpots = availableInvalidSpots;
        this.availableBusSpots = availableBusSpots;
        this.description = description;
        this.hidden = hidden;
        this.created = created;
        this.modified = modified;
    }
    
    /**
     * Calculates total number of parking spots (all types combined).
     */
    public int getTotalSpots() {
        return totalRegularSpots + totalInvalidSpots + totalBusSpots;
    }
    
    /**
     * Calculates total number of available spots (all types combined).
     */
    public int getTotalAvailableSpots() {
        return availableRegularSpots + availableInvalidSpots + availableBusSpots;
    }
    
    /**
     * Calculates total number of occupied spots (all types combined).
     */
    public int getTotalOccupiedSpots() {
        return getTotalSpots() - getTotalAvailableSpots();
    }
    
    /**
     * Calculates overall occupancy percentage (0-100).
     */
    public float getOccupancyPercentage() {
        int total = getTotalSpots();
        if (total == 0) {
            return 0f;
        }
        return ((float) getTotalOccupiedSpots() / total) * 100f;
    }
    
    /**
     * Calculates occupancy percentage for regular spots (0-100).
     */
    public float getRegularOccupancyPercentage() {
        if (totalRegularSpots == 0) {
            return 0f;
        }
        int occupied = totalRegularSpots - availableRegularSpots;
        return ((float) occupied / totalRegularSpots) * 100f;
    }
    
    /**
     * Calculates occupancy percentage for invalid spots (0-100).
     */
    public float getInvalidOccupancyPercentage() {
        if (totalInvalidSpots == 0) {
            return 0f;
        }
        int occupied = totalInvalidSpots - availableInvalidSpots;
        return ((float) occupied / totalInvalidSpots) * 100f;
    }
    
    /**
     * Calculates occupancy percentage for bus spots (0-100).
     */
    public float getBusOccupancyPercentage() {
        if (totalBusSpots == 0) {
            return 0f;
        }
        int occupied = totalBusSpots - availableBusSpots;
        return ((float) occupied / totalBusSpots) * 100f;
    }
    
    /**
     * Checks if parking has any available spots.
     */
    public boolean hasAvailableSpots() {
        return getTotalAvailableSpots() > 0;
    }
    
    /**
     * Checks if parking is full (no available spots).
     */
    public boolean isFull() {
        return getTotalAvailableSpots() == 0 && getTotalSpots() > 0;
    }
    
    /**
     * Checks if parking is empty (all spots available).
     */
    public boolean isEmpty() {
        return getTotalAvailableSpots() == getTotalSpots() && getTotalSpots() > 0;
    }
    
    // Getters and Setters
    
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
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public Geolocation getLocation() {
        return location;
    }
    
    public void setLocation(Geolocation location) {
        this.location = location;
    }
    
    public int getTotalRegularSpots() {
        return totalRegularSpots;
    }
    
    public void setTotalRegularSpots(int totalRegularSpots) {
        this.totalRegularSpots = totalRegularSpots;
    }
    
    public int getTotalInvalidSpots() {
        return totalInvalidSpots;
    }
    
    public void setTotalInvalidSpots(int totalInvalidSpots) {
        this.totalInvalidSpots = totalInvalidSpots;
    }
    
    public int getTotalBusSpots() {
        return totalBusSpots;
    }
    
    public void setTotalBusSpots(int totalBusSpots) {
        this.totalBusSpots = totalBusSpots;
    }
    
    public int getAvailableRegularSpots() {
        return availableRegularSpots;
    }
    
    public void setAvailableRegularSpots(int availableRegularSpots) {
        this.availableRegularSpots = availableRegularSpots;
    }
    
    public int getAvailableInvalidSpots() {
        return availableInvalidSpots;
    }
    
    public void setAvailableInvalidSpots(int availableInvalidSpots) {
        this.availableInvalidSpots = availableInvalidSpots;
    }
    
    public int getAvailableBusSpots() {
        return availableBusSpots;
    }
    
    public void setAvailableBusSpots(int availableBusSpots) {
        this.availableBusSpots = availableBusSpots;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isHidden() {
        return hidden;
    }
    
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }
    
    public Date getCreated() {
        return created;
    }
    
    public void setCreated(Date created) {
        this.created = created;
    }
    
    public Date getModified() {
        return modified;
    }
    
    public void setModified(Date modified) {
        this.modified = modified;
    }
    
    @Override
    public String toString() {
        return "Parking{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", location=(" + (location != null ? location.lat + ", " + location.lng : "null") + ")" +
                ", spots=" + getTotalAvailableSpots() + "/" + getTotalSpots() +
                ", occupancy=" + String.format("%.1f", getOccupancyPercentage()) + "%" +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Parking parking = (Parking) o;
        
        return id != null ? id.equals(parking.id) : parking.id == null;
    }
    
    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

