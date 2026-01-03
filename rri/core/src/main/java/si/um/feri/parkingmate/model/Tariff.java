package si.um.feri.parkingmate.model;

import java.util.Date;

/**
 * Represents a parking tariff (pricing information).
 */
public class Tariff {

    private String id;                      // MongoDB _id
    private String tariffType;              // Type of tariff (e.g., "hourly", "daily", "monthly")
    private String duration;                // Duration (e.g., "1h", "24h", "1 month")
    private String vehicleType;             // Vehicle type (e.g., "car", "motorcycle", "bus")
    private float price;                    // Price amount
    private String priceUnit;               // Price unit (e.g., "€", "$")
    private boolean hidden;                 // Whether tariff is hidden
    private Date created;                   // Creation timestamp
    private Date modified;                  // Last modification timestamp

    // Default constructor
    public Tariff() {
        this.hidden = false;
    }

    // Full constructor
    public Tariff(String id, String tariffType, String duration, String vehicleType,
                  float price, String priceUnit, boolean hidden, Date created, Date modified) {
        this.id = id;
        this.tariffType = tariffType;
        this.duration = duration;
        this.vehicleType = vehicleType;
        this.price = price;
        this.priceUnit = priceUnit;
        this.hidden = hidden;
        this.created = created;
        this.modified = modified;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTariffType() {
        return tariffType;
    }

    public void setTariffType(String tariffType) {
        this.tariffType = tariffType;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }

    public String getPriceUnit() {
        return priceUnit;
    }

    public void setPriceUnit(String priceUnit) {
        this.priceUnit = priceUnit;
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

    /**
     * Gets the formatted price string (e.g., "1.20 €").
     */
    public String getFormattedPrice() {
        return String.format("%.2f %s", price, priceUnit != null ? priceUnit : "");
    }

    /**
     * Gets a display-friendly description of the tariff.
     */
    public String getDisplayDescription() {
        StringBuilder sb = new StringBuilder();

        if (tariffType != null) {
            sb.append(tariffType);
        }

        if (duration != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(duration);
        }

        if (vehicleType != null && !vehicleType.equalsIgnoreCase("car")) {
            if (sb.length() > 0) sb.append(" (");
            sb.append(vehicleType);
            if (sb.toString().contains("(")) sb.append(")");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "Tariff{" +
            "id='" + id + '\'' +
            ", type='" + tariffType + '\'' +
            ", duration='" + duration + '\'' +
            ", vehicle='" + vehicleType + '\'' +
            ", price=" + getFormattedPrice() +
            '}';
    }
}
