package si.um.feri.parkingmate.api;

import com.badlogic.gdx.Gdx;
import org.json.JSONArray;

public class EventService {

    private ApiClient apiClient;
    private String baseUrl;
    private boolean fallbackEnabled = true;

    public EventService(String baseUrl) {
        this.baseUrl = baseUrl;
        this.apiClient = new ApiClient(baseUrl);
    }

    public EventService() {
        this.apiClient = new ApiClient();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        this.apiClient.setBaseUrl(baseUrl);
    }

    public void setFallbackEnabled(boolean enabled) {
        this.fallbackEnabled = enabled;
    }

    public boolean isFallbackEnabled() {
        return fallbackEnabled;
    }

    /**
     * Fetch RAW events JSON array from backend
     * Backend endpoint: GET /api/events
     */
    public JSONArray fetchEvents() throws ApiClient.ApiException {
        try {
            // Umesto getArray → get OBJECT
            org.json.JSONObject response =
                apiClient.get("/api/events");

            // Izvuci "data" niz
            JSONArray eventsArray = response.optJSONArray("data");

            if (eventsArray == null) {
                Gdx.app.error("EventService", "No 'data' array in response");
                return new JSONArray();
            }

            Gdx.app.debug(
                "EventService",
                "Fetched " + eventsArray.length() + " events"
            );

            return eventsArray;

        } catch (ApiClient.ApiException e) {
            Gdx.app.error(
                "EventService",
                "Failed to fetch events: " + e.getMessage(),
                e
            );

            if (fallbackEnabled) {
                Gdx.app.log(
                    "EventService",
                    "Using fallback: returning empty event list"
                );
                return new JSONArray();
            }
            throw e;
        }
    }

}
