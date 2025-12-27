package si.um.feri.parkingmate.api;

import com.badlogic.gdx.Gdx;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HTTP client for REST API communication.
 * Handles GET and POST requests with JSON support.
 */
public class ApiClient {
    
    private static final int CONNECT_TIMEOUT = 5000; // 5 seconds
    private static final int READ_TIMEOUT = 10000; // 10 seconds
    private String baseUrl;
    
    /**
     * Constructor with base URL.
     * @param baseUrl Base URL for the API (e.g., "https://api.example.com")
     */
    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
    
    /**
     * Default constructor.
     * Base URL should be set via setBaseUrl() or in specific methods.
     */
    public ApiClient() {
        this.baseUrl = "";
    }
    
    /**
     * Sets the base URL for API requests.
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
    
    /**
     * Performs a GET request.
     * 
     * @param endpoint API endpoint (e.g., "/parking-lots")
     * @return JSONObject response
     * @throws ApiException if request fails
     */
    public JSONObject get(String endpoint) throws ApiException {
        return get(endpoint, null);
    }
    
    /**
     * Performs a GET request that returns an array.
     * 
     * @param endpoint API endpoint
     * @return JSONArray response
     * @throws ApiException if request fails
     */
    public org.json.JSONArray getArray(String endpoint) throws ApiException {
        return getArray(endpoint, null);
    }
    
    /**
     * Performs a GET request that returns an array with query parameters.
     * 
     * @param endpoint API endpoint
     * @param queryParams Query parameters (key-value pairs)
     * @return JSONArray response
     * @throws ApiException if request fails
     */
    public org.json.JSONArray getArray(String endpoint, Map<String, String> queryParams) throws ApiException {
        try {
            String urlString = buildUrl(endpoint, queryParams);
            URL url = new URL(urlString);
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept", "application/json");
            
            return executeRequestAsArray(connection);
            
        } catch (IOException e) {
            throw new ApiException("Failed to execute GET request: " + e.getMessage(), e);
        }
    }
    
    /**
     * Performs a GET request with query parameters.
     * 
     * @param endpoint API endpoint
     * @param queryParams Query parameters (key-value pairs)
     * @return JSONObject response
     * @throws ApiException if request fails
     */
    public JSONObject get(String endpoint, Map<String, String> queryParams) throws ApiException {
        try {
            String urlString = buildUrl(endpoint, queryParams);
            URL url = new URL(urlString);
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Accept", "application/json");
            
            return executeRequest(connection);
            
        } catch (IOException e) {
            throw new ApiException("Failed to execute GET request: " + e.getMessage(), e);
        }
    }
    
    /**
     * Performs a POST request with JSON body.
     * 
     * @param endpoint API endpoint
     * @param jsonBody JSON body as string
     * @return JSONObject response
     * @throws ApiException if request fails
     */
    public JSONObject post(String endpoint, String jsonBody) throws ApiException {
        return post(endpoint, jsonBody, null);
    }
    
    /**
     * Performs a POST request with JSON body and headers.
     * 
     * @param endpoint API endpoint
     * @param jsonBody JSON body as string
     * @param headers Additional headers (key-value pairs)
     * @return JSONObject response
     * @throws ApiException if request fails
     */
    public JSONObject post(String endpoint, String jsonBody, Map<String, String> headers) throws ApiException {
        try {
            String urlString = buildUrl(endpoint, null);
            URL url = new URL(urlString);
            
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            
            // Add custom headers if provided
            if (headers != null) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
            }
            
            // Write JSON body
            if (jsonBody != null && !jsonBody.isEmpty()) {
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }
            
            return executeRequest(connection);
            
        } catch (IOException e) {
            throw new ApiException("Failed to execute POST request: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes the HTTP request and returns the response as JSONArray.
     */
    private org.json.JSONArray executeRequestAsArray(HttpURLConnection connection) throws IOException, ApiException {
        int responseCode = connection.getResponseCode();
        
        InputStream inputStream;
        if (responseCode >= 200 && responseCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        
        if (inputStream == null) {
            throw new ApiException("No response from server. HTTP code: " + responseCode);
        }
        
        // Read response
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();
        
        // Check for errors
        if (responseCode < 200 || responseCode >= 300) {
            String errorMessage = "HTTP Error " + responseCode;
            try {
                JSONObject errorJson = new JSONObject(response.toString());
                if (errorJson.has("message")) {
                    errorMessage = errorJson.getString("message");
                } else if (errorJson.has("error")) {
                    errorMessage = errorJson.getString("error");
                }
            } catch (Exception e) {
                if (response.length() > 0) {
                    errorMessage += ": " + response.toString();
                }
            }
            throw new ApiException(errorMessage, responseCode);
        }
        
        // Parse JSON array response
        try {
            return new org.json.JSONArray(response.toString());
        } catch (Exception e) {
            Gdx.app.error("ApiClient", "Failed to parse JSON array response: " + response.toString(), e);
            throw new ApiException("Invalid JSON array response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Executes the HTTP request and returns the response as JSONObject.
     */
    private JSONObject executeRequest(HttpURLConnection connection) throws IOException, ApiException {
        int responseCode = connection.getResponseCode();
        
        InputStream inputStream;
        if (responseCode >= 200 && responseCode < 300) {
            inputStream = connection.getInputStream();
        } else {
            inputStream = connection.getErrorStream();
        }
        
        if (inputStream == null) {
            throw new ApiException("No response from server. HTTP code: " + responseCode);
        }
        
        // Read response
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );
        
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        connection.disconnect();
        
        // Check for errors
        if (responseCode < 200 || responseCode >= 300) {
            String errorMessage = "HTTP Error " + responseCode;
            try {
                JSONObject errorJson = new JSONObject(response.toString());
                if (errorJson.has("message")) {
                    errorMessage = errorJson.getString("message");
                } else if (errorJson.has("error")) {
                    errorMessage = errorJson.getString("error");
                }
            } catch (Exception e) {
                // If response is not JSON, use raw response
                if (response.length() > 0) {
                    errorMessage += ": " + response.toString();
                }
            }
            throw new ApiException(errorMessage, responseCode);
        }
        
        // Parse JSON response
        try {
            return new JSONObject(response.toString());
        } catch (Exception e) {
            Gdx.app.error("ApiClient", "Failed to parse JSON response: " + response.toString(), e);
            throw new ApiException("Invalid JSON response: " + e.getMessage(), e);
        }
    }
    
    /**
     * Builds the full URL from endpoint and query parameters.
     */
    private String buildUrl(String endpoint, Map<String, String> queryParams) {
        StringBuilder urlBuilder = new StringBuilder();
        
        // Add base URL
        if (baseUrl != null && !baseUrl.isEmpty()) {
            urlBuilder.append(baseUrl);
        }
        
        // Add endpoint (ensure it starts with /)
        if (endpoint != null && !endpoint.isEmpty()) {
            if (!endpoint.startsWith("/")) {
                urlBuilder.append("/");
            }
            urlBuilder.append(endpoint);
        }
        
        // Add query parameters
        if (queryParams != null && !queryParams.isEmpty()) {
            urlBuilder.append("?");
            boolean first = true;
            for (Map.Entry<String, String> param : queryParams.entrySet()) {
                if (!first) {
                    urlBuilder.append("&");
                }
                try {
                    urlBuilder.append(param.getKey())
                              .append("=")
                              .append(java.net.URLEncoder.encode(param.getValue(), "UTF-8"));
                } catch (java.io.UnsupportedEncodingException e) {
                    // UTF-8 is always supported, but handle just in case
                    urlBuilder.append(param.getKey())
                              .append("=")
                              .append(param.getValue()); // Fallback without encoding
                }
                first = false;
            }
        }
        
        return urlBuilder.toString();
    }
    
    /**
     * Custom exception for API errors.
     */
    public static class ApiException extends Exception {
        private int httpCode = -1;
        
        public ApiException(String message) {
            super(message);
        }
        
        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public ApiException(String message, int httpCode) {
            super(message);
            this.httpCode = httpCode;
        }
        
        public int getHttpCode() {
            return httpCode;
        }
        
        public boolean hasHttpCode() {
            return httpCode > 0;
        }
    }
}
