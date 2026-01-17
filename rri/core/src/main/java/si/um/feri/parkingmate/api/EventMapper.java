package si.um.feri.parkingmate.api;

import org.json.JSONArray;
import org.json.JSONObject;
import si.um.feri.parkingmate.model.Event;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class EventMapper {

    public static Event mapToEvent(JSONObject json) {

        String id = json.optString("_id", "");
        String topic = json.optString("topic", "");
        String message = json.optString("message", "");

        // eventType ostaje STRING (PARKING_FULL, PARKING_FREE...)
        String eventType = json.optString("eventType", "UNKNOWN");

        // timestamp
        Instant timestamp = Instant.parse(
            json.optString("timestamp", Instant.now().toString())
        );

        // location: "46.5604952,15.6333924"
        double lat = 0;
        double lng = 0;

        String locationStr = json.optString("location", null);
        if (locationStr != null && locationStr.contains(",")) {
            try {
                String[] parts = locationStr.split(",");
                lat = Double.parseDouble(parts[0].trim());
                lng = Double.parseDouble(parts[1].trim());
            } catch (Exception ignored) {}
        }

        return new Event(
            id,
            topic,
            message,
            eventType,
            lat,
            lng,
            timestamp
        );
    }

    public static List<Event> mapToEventList(JSONArray array) {
        List<Event> events = new ArrayList<>();

        for (int i = 0; i < array.length(); i++) {
            events.add(mapToEvent(array.getJSONObject(i)));
        }

        return events;
    }
}
