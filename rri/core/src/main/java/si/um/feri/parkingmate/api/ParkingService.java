package si.um.feri.parkingmate.api;

import com.badlogic.gdx.Gdx;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for fetching parking data from the API.
 * Handles communication with parking API endpoints.
 */
public class ParkingService {
    
    private ApiClient apiClient;
    private String baseUrl;
    
    /**
     * Constructor with base URL.
     * @param baseUrl Base URL for the parking API
     */
    public ParkingService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.apiClient = new ApiClient(baseUrl);
    }
    
    /**
     * Default constructor.
     * Base URL should be set via setBaseUrl().
     */
    public ParkingService() {
        this.apiClient = new ApiClient();
    }
    
    /**
     * Sets the base URL for the parking API.
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        this.apiClient.setBaseUrl(baseUrl);
    }
    
    /**
     * Fetches all parking locations from the API.
     * 
     * @return List of parking locations as JSONObjects
     * @throws ApiException if request fails
     */
    public List<JSONObject> fetchParkingLocations() throws ApiClient.ApiException {
        return fetchParkingLocations(null);
    }
    
    /**
     * Fetches parking locations with optional filters.
     * 
     * @param filters Optional filters (e.g., city, type, etc.)
     * @return List of parking locations as JSONObjects
     * @throws ApiException if request fails
     */
    public List<JSONObject> fetchParkingLocations(Map<String, String> filters) throws ApiClient.ApiException {
        try {
            // Build query parameters
            Map<String, String> queryParams = new HashMap<>();
            if (filters != null) {
                queryParams.putAll(filters);
            }
            
            // Make API call to parkingLocations endpoint
            // Backend returns array directly (res.json(array)), not wrapped in object
            org.json.JSONArray responseArray = apiClient.getArray("/parkingLocations", queryParams);
            
            // Parse response - backend returns array directly
            List<JSONObject> locations = new ArrayList<>();
            
            for (int i = 0; i < responseArray.length(); i++) {
                locations.add(responseArray.getJSONObject(i));
            }
            
            Gdx.app.debug("ParkingService", "Fetched " + locations.size() + " parking locations");
            return locations;
            
        } catch (ApiClient.ApiException e) {
            Gdx.app.error("ParkingService", "Failed to fetch parking locations", e);
            throw e;
        } catch (Exception e) {
            Gdx.app.error("ParkingService", "Unexpected error while fetching parking locations", e);
            throw new ApiClient.ApiException("Failed to parse parking locations: " + e.getMessage(), e);
        }
    }
    
    /**
     * Fetches a single parking location by ID.
     * 
     * @param parkingId Parking location ID
     * @return Parking location as JSONObject
     * @throws ApiException if request fails
     */
    public JSONObject fetchParkingLocationById(String parkingId) throws ApiClient.ApiException {
        try {
            // Backend endpoint: GET /parkingLocations/:id
            JSONObject response = apiClient.get("/parkingLocations/" + parkingId);
            
            // Backend returns object directly
            return response;
            
        } catch (ApiClient.ApiException e) {
            Gdx.app.error("ParkingService", "Failed to fetch parking location: " + parkingId, e);
            throw e;
        }
    }
    
    /**
     * Fetches nearby parking locations using geospatial search.
     * Backend endpoint: GET /parkingLocations/nearby/search?lat=X&lng=Y&radius=Z
     * 
     * @param lat Latitude
     * @param lng Longitude
     * @param radius Radius in meters
     * @return List of parking locations as JSONObjects
     * @throws ApiException if request fails
     */
    public List<JSONObject> fetchNearbyParkingLocations(double lat, double lng, int radius) throws ApiClient.ApiException {
        try {
            Map<String, String> queryParams = new HashMap<>();
            queryParams.put("lat", String.valueOf(lat));
            queryParams.put("lng", String.valueOf(lng));
            queryParams.put("radius", String.valueOf(radius));
            
            org.json.JSONArray responseArray = apiClient.getArray("/parkingLocations/nearby/search", queryParams);
            
            List<JSONObject> locations = new ArrayList<>();
            for (int i = 0; i < responseArray.length(); i++) {
                locations.add(responseArray.getJSONObject(i));
            }
            
            return locations;
            
        } catch (ApiClient.ApiException e) {
            Gdx.app.error("ParkingService", "Failed to fetch nearby parking locations", e);
            throw e;
        }
    }
    
    /**
     * Fetches occupancy status for all parking locations.
     * Backend endpoint: GET /parkingLocations/occupancy/status
     * Returns occupancy percentage for each location.
     * 
     * @return List of parking locations with occupancy data as JSONObjects
     *         Each object contains: _id, name, address, location, occupancy (percentage)
     * @throws ApiException if request fails
     */
    public List<JSONObject> fetchOccupancyStatus() throws ApiClient.ApiException {
        try {
            org.json.JSONArray responseArray = apiClient.getArray("/parkingLocations/occupancy/status");
            
            List<JSONObject> locations = new ArrayList<>();
            for (int i = 0; i < responseArray.length(); i++) {
                locations.add(responseArray.getJSONObject(i));
            }
            
            Gdx.app.debug("ParkingService", "Fetched occupancy status for " + locations.size() + " locations");
            return locations;
            
        } catch (ApiClient.ApiException e) {
            Gdx.app.error("ParkingService", "Failed to fetch occupancy status", e);
            throw e;
        }
    }
    
    /**
     * Fetches detailed occupancy data for a specific parking location.
     * This includes all spot types (regular, invalid, bus).
     * Backend endpoint: GET /parkingLocations/:id
     * 
     * @param parkingId Parking location ID
     * @return JSONObject with detailed occupancy data including:
     *         - total_regular_spots, available_regular_spots
     *         - total_invalid_spots, available_invalid_spots
     *         - total_bus_spots, available_bus_spots
     * @throws ApiException if request fails
     */
    public JSONObject fetchOccupancyData(String parkingId) throws ApiClient.ApiException {
        try {
            // Use existing method to get full location data which includes occupancy
            JSONObject location = fetchParkingLocationById(parkingId);
            
            // Location already contains all occupancy data
            return location;
            
        } catch (ApiClient.ApiException e) {
            Gdx.app.error("ParkingService", "Failed to fetch occupancy data for: " + parkingId, e);
            throw e;
        }
    }
    
    /**
     * Fetches parking logs (historical occupancy data) for a specific location.
     * Backend endpoint: GET /parkingLocations/:id/logs?from=DATE&to=DATE
     * 
     * @param parkingId Parking location ID
     * @param fromDate Optional start date (ISO 8601 format or timestamp)
     * @param toDate Optional end date (ISO 8601 format or timestamp)
     * @return List of parking log entries as JSONObjects
     *         Each log contains: timestamp, available_regular_spots, available_invalid_spots, available_bus_spots
     * @throws ApiException if request fails
     */
    public List<JSONObject> fetchParkingLogs(String parkingId, String fromDate, String toDate) throws ApiClient.ApiException {
        try {
            Map<String, String> queryParams = new HashMap<>();
            if (fromDate != null && !fromDate.isEmpty()) {
                queryParams.put("from", fromDate);
            }
            if (toDate != null && !toDate.isEmpty()) {
                queryParams.put("to", toDate);
            }
            
            org.json.JSONArray responseArray = apiClient.getArray("/parkingLocations/" + parkingId + "/logs", queryParams);
            
            List<JSONObject> logs = new ArrayList<>();
            for (int i = 0; i < responseArray.length(); i++) {
                logs.add(responseArray.getJSONObject(i));
            }
            
            Gdx.app.debug("ParkingService", "Fetched " + logs.size() + " parking logs for location: " + parkingId);
            return logs;
            
        } catch (ApiClient.ApiException e) {
            Gdx.app.error("ParkingService", "Failed to fetch parking logs for: " + parkingId, e);
            throw e;
        }
    }
    
    /**
     * Fetches parking logs for a specific location (without date filters).
     * 
     * @param parkingId Parking location ID
     * @return List of parking log entries as JSONObjects
     * @throws ApiException if request fails
     */
    public List<JSONObject> fetchParkingLogs(String parkingId) throws ApiClient.ApiException {
        return fetchParkingLogs(parkingId, null, null);
    }
}

