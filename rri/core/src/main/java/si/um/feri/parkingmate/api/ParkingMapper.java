package si.um.feri.parkingmate.api;

import com.badlogic.gdx.Gdx;
import org.json.JSONObject;
import si.um.feri.parkingmate.map.Geolocation;
import si.um.feri.parkingmate.model.Parking;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Maps JSON objects from the API to internal Parking models.
 * Handles conversion from backend JSON structure to Java objects.
 */
public class ParkingMapper {
    
    // Date format used by backend (ISO 8601)
    private static final String[] DATE_FORMATS = {
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss"
    };
    
    /**
     * Maps a JSONObject from the API to a Parking model.
     * 
     * @param json JSONObject from the API
     * @return Parking model instance, or null if mapping fails
     */
    public static Parking mapToParking(JSONObject json) {
        if (json == null) {
            return null;
        }
        
        try {
            Parking parking = new Parking();
            
            // Map ID (can be _id or id)
            String id = null;
            if (json.has("_id")) {
                Object idObj = json.get("_id");
                if (idObj instanceof JSONObject) {
                    // MongoDB ObjectId is sometimes returned as object with $oid
                    JSONObject idJson = (JSONObject) idObj;
                    if (idJson.has("$oid")) {
                        id = idJson.getString("$oid");
                    } else {
                        id = idJson.toString();
                    }
                } else {
                    id = idObj.toString();
                }
            } else if (json.has("id")) {
                id = json.getString("id");
            }
            parking.setId(id);
            
            // Map basic fields
            parking.setName(json.optString("name", null));
            parking.setAddress(json.optString("address", null));
            parking.setDescription(json.optString("description", null));
            parking.setHidden(json.optBoolean("hidden", false));
            
            // Map location (GeoJSON Point format: { type: "Point", coordinates: [lng, lat] })
            if (json.has("location")) {
                Object locationObj = json.get("location");
                if (locationObj instanceof JSONObject) {
                    JSONObject locationJson = (JSONObject) locationObj;
                    if (locationJson.has("coordinates")) {
                        try {
                            org.json.JSONArray coords = locationJson.getJSONArray("coordinates");
                            if (coords.length() >= 2) {
                                // Backend uses [longitude, latitude] format
                                double lng = coords.getDouble(0);
                                double lat = coords.getDouble(1);
                                parking.setLocation(new Geolocation(lat, lng));
                            }
                        } catch (Exception e) {
                            Gdx.app.error("ParkingMapper", "Failed to parse location coordinates", e);
                        }
                    }
                }
            }
            
            // Map spot counts
            parking.setTotalRegularSpots(json.optInt("total_regular_spots", 0));
            parking.setTotalInvalidSpots(json.optInt("total_invalid_spots", 0));
            parking.setTotalBusSpots(json.optInt("total_bus_spots", 0));
            
            parking.setAvailableRegularSpots(json.optInt("available_regular_spots", 0));
            parking.setAvailableInvalidSpots(json.optInt("available_invalid_spots", 0));
            parking.setAvailableBusSpots(json.optInt("available_bus_spots", 0));
            
            // Map dates
            if (json.has("created")) {
                parking.setCreated(parseDate(json.optString("created", null)));
            }
            if (json.has("modified")) {
                parking.setModified(parseDate(json.optString("modified", null)));
            }
            
            return parking;
            
        } catch (Exception e) {
            Gdx.app.error("ParkingMapper", "Failed to map JSON to Parking: " + json.toString(), e);
            return null;
        }
    }
    
    /**
     * Maps a list of JSONObjects to a list of Parking models.
     * 
     * @param jsonList List of JSONObjects from the API
     * @return List of Parking models (null entries are filtered out)
     */
    public static List<Parking> mapToParkingList(List<JSONObject> jsonList) {
        List<Parking> parkingList = new ArrayList<>();
        
        if (jsonList == null) {
            return parkingList;
        }
        
        for (JSONObject json : jsonList) {
            Parking parking = mapToParking(json);
            if (parking != null) {
                parkingList.add(parking);
            }
        }
        
        return parkingList;
    }
    
    /**
     * Maps a JSONArray to a list of Parking models.
     * 
     * @param jsonArray JSONArray from the API
     * @return List of Parking models
     */
    public static List<Parking> mapToParkingList(org.json.JSONArray jsonArray) {
        List<Parking> parkingList = new ArrayList<>();
        
        if (jsonArray == null) {
            return parkingList;
        }
        
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                JSONObject json = jsonArray.getJSONObject(i);
                Parking parking = mapToParking(json);
                if (parking != null) {
                    parkingList.add(parking);
                }
            } catch (Exception e) {
                Gdx.app.error("ParkingMapper", "Failed to parse JSON object at index " + i, e);
            }
        }
        
        return parkingList;
    }
    
    /**
     * Parses a date string from various formats.
     * 
     * @param dateString Date string to parse
     * @return Date object, or null if parsing fails
     */
    private static Date parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        
        // Try different date formats
        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                return sdf.parse(dateString);
            } catch (ParseException e) {
                // Try next format
            }
        }
        
        // Try parsing as timestamp (milliseconds)
        try {
            long timestamp = Long.parseLong(dateString);
            return new Date(timestamp);
        } catch (NumberFormatException e) {
            // Not a timestamp
        }
        
        Gdx.app.debug("ParkingMapper", "Failed to parse date: " + dateString);
        return null;
    }
}

