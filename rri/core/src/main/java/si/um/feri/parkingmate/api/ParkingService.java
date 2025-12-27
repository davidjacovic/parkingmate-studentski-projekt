package si.um.feri.parkingmate.api;

import com.badlogic.gdx.Gdx;
import org.json.JSONArray;
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
            
            // Make API call - adjust endpoint based on your API structure
            // Common endpoints: /parking-lots, /parking, /locations, etc.
            JSONObject response = apiClient.get("/parking-lots", queryParams);
            
            // Parse response - adjust based on your API response structure
            List<JSONObject> locations = new ArrayList<>();
            
            // Check if response is an array
            if (response.has("data") && response.get("data") instanceof JSONArray) {
                JSONArray dataArray = response.getJSONArray("data");
                for (int i = 0; i < dataArray.length(); i++) {
                    locations.add(dataArray.getJSONObject(i));
                }
            } 
            // Check if response is directly an array
            else if (response.has("parkingLots") && response.get("parkingLots") instanceof JSONArray) {
                JSONArray parkingLots = response.getJSONArray("parkingLots");
                for (int i = 0; i < parkingLots.length(); i++) {
                    locations.add(parkingLots.getJSONObject(i));
                }
            }
            // Check if response is a list/array at root level
            else if (response.has("results") && response.get("results") instanceof JSONArray) {
                JSONArray results = response.getJSONArray("results");
                for (int i = 0; i < results.length(); i++) {
                    locations.add(results.getJSONObject(i));
                }
            }
            // If response is directly an array (some APIs return arrays directly)
            else {
                // Try to parse as array - if this fails, we'll handle it
                Gdx.app.debug("ParkingService", "Response structure: " + response.toString());
                // For now, return empty list and log the structure
                // This will be adjusted based on actual API response
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
            JSONObject response = apiClient.get("/parking-lots/" + parkingId);
            
            // Adjust based on your API response structure
            if (response.has("data")) {
                return response.getJSONObject("data");
            }
            
            return response;
            
        } catch (ApiClient.ApiException e) {
            Gdx.app.error("ParkingService", "Failed to fetch parking location: " + parkingId, e);
            throw e;
        }
    }
    
    /**
     * Fetches parking locations within a geographic area.
     * 
     * @param minLat Minimum latitude
     * @param minLng Minimum longitude
     * @param maxLat Maximum latitude
     * @param maxLng Maximum longitude
     * @return List of parking locations as JSONObjects
     * @throws ApiException if request fails
     */
    public List<JSONObject> fetchParkingLocationsInArea(
            double minLat, double minLng, double maxLat, double maxLng) throws ApiClient.ApiException {
        
        Map<String, String> filters = new HashMap<>();
        filters.put("minLat", String.valueOf(minLat));
        filters.put("minLng", String.valueOf(minLng));
        filters.put("maxLat", String.valueOf(maxLat));
        filters.put("maxLng", String.valueOf(maxLng));
        
        return fetchParkingLocations(filters);
    }
    
    /**
     * Fetches parking locations by city.
     * 
     * @param city City name
     * @return List of parking locations as JSONObjects
     * @throws ApiException if request fails
     */
    public List<JSONObject> fetchParkingLocationsByCity(String city) throws ApiClient.ApiException {
        Map<String, String> filters = new HashMap<>();
        filters.put("city", city);
        
        return fetchParkingLocations(filters);
    }
}

