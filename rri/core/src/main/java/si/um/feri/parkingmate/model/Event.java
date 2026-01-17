package si.um.feri.parkingmate.model;

import java.time.Instant;
public class Event {
    private String id;
    private String topic;
    private String message;
    private String eventType;
    private double lat;
    private double lng;
    private Instant timestamp;

    public Event(String id, String topic, String message,
                 String eventType, double lat, double lng, Instant timestamp) {
        this.id = id;
        this.topic = topic;
        this.message = message;
        this.eventType = eventType;
        this.lat = lat;
        this.lng = lng;
        this.timestamp = timestamp;
    }

    public double getLat() { return lat; }
    public double getLng() { return lng; }
    public String getEventType() { return eventType; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
}
