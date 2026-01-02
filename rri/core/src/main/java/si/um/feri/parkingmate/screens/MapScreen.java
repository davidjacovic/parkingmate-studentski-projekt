package si.um.feri.parkingmate.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import si.um.feri.parkingmate.ParkingMate;
import si.um.feri.parkingmate.api.ParkingMapper;
import si.um.feri.parkingmate.api.ParkingService;
import si.um.feri.parkingmate.map.*;
import si.um.feri.parkingmate.model.Marker;
import si.um.feri.parkingmate.model.Parking;
import si.um.feri.parkingmate.util.FontManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapScreen extends BaseScreen {

    private final ParkingMate game;
    private TiledMap tiledMap;
    private TiledMapRenderer tiledMapRenderer;
    private Texture[] mapTiles;
    private ZoomXY beginTile; // top left tile
    private GestureDetector gestureDetector;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private List<Marker> markers; // List of markers to display

    // Marker textures (can be null if using default shapes)
    private Texture markerFreeTexture;
    private Texture markerPartialTexture;
    private Texture markerFullTexture;
    private Texture markerUnknownTexture;

    // Marker size configuration (in pixels)
    private float markerSize = 128f; // Default size, can be adjusted

    // Selected marker for info panel
    private Marker selectedMarker = null;

    // Font for info panel text
    private BitmapFont font;

    // API service for fetching parking data
    private ParkingService parkingService;

    // Backend API base URL (backend runs on port 3002)
    // Change this if your backend runs on a different URL
    private static final String API_BASE_URL = "http://localhost:3002";

    public MapScreen(ParkingMate game) {
        this.game = game;
        this.markers = new ArrayList<>();
        this.parkingService = new ParkingService(API_BASE_URL);
    }

    @Override
    public void show() {
        initializeMap();
    }

    private void initializeMap() {
        // Check if API key is set
        if (Keys.GEOAPIFY == null || Keys.GEOAPIFY.isEmpty()) {
            Gdx.app.error("MapScreen", "Geoapify API key is not set! Please add your API key in Keys.java");
            Gdx.app.error("MapScreen", "Get your free API key at: https://www.geoapify.com/get-started-with-maps-api");
            // Still setup camera so the screen doesn't crash
            setupCamera();
            return;
        }

        try {
            // Get center tile based on center geolocation (Ljubljana)
            ZoomXY centerTile = MapRasterTiles.getTileNumber(
                    MapConstants.CENTER_GEOLOCATION.lat,
                    MapConstants.CENTER_GEOLOCATION.lng,
                    MapConstants.ZOOM
            );

            // Fetch tiles for the area (NUM_TILES x NUM_TILES grid)
            mapTiles = MapRasterTiles.getRasterTileZone(centerTile, MapConstants.NUM_TILES);

            // Calculate beginning tile (top left corner)
            beginTile = new ZoomXY(
                    MapConstants.ZOOM,
                    centerTile.x - ((MapConstants.NUM_TILES - 1) / 2),
                    centerTile.y - ((MapConstants.NUM_TILES - 1) / 2)
            );
        } catch (IOException e) {
            e.printStackTrace();
            Gdx.app.error("MapScreen", "Failed to load map tiles", e);
            Gdx.app.error("MapScreen", "Error: " + e.getMessage());
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                Gdx.app.error("MapScreen", "Invalid or missing API key! Please check Keys.GEOAPIFY");
            }
            // Still setup camera so the screen doesn't crash
            setupCamera();
            return;
        }

        // Create TiledMap
        tiledMap = new TiledMap();
        MapLayers layers = tiledMap.getLayers();

        // Create a tile layer
        TiledMapTileLayer layer = new TiledMapTileLayer(
                MapConstants.NUM_TILES,
                MapConstants.NUM_TILES,
                MapRasterTiles.TILE_SIZE,
                MapRasterTiles.TILE_SIZE
        );

        // Fill layer with tiles (note: tiles are arranged from top to bottom, left to right)
        int index = 0;
        for (int j = MapConstants.NUM_TILES - 1; j >= 0; j--) {
            for (int i = 0; i < MapConstants.NUM_TILES; i++) {
                TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
                cell.setTile(new StaticTiledMapTile(
                        new TextureRegion(
                                mapTiles[index],
                                MapRasterTiles.TILE_SIZE,
                                MapRasterTiles.TILE_SIZE
                        )
                ));
                layer.setCell(i, j, cell);
                index++;
            }
        }
        layers.add(layer);

        // Create renderer
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);

        // Setup camera
        setupCamera();

        // Setup input handlers for zoom and pan
        setupInputHandlers();

        // Initialize shape renderer for markers (fallback if textures not available)
        shapeRenderer = new ShapeRenderer();

        // Initialize sprite batch for marker textures
        spriteBatch = new SpriteBatch();

        // Load font for info panel
        loadFont();

        // Try to load marker textures (optional - will fallback to shapes if not found)
        loadMarkerTextures();

        // Load parking locations from API
        loadParkingLocationsFromAPI();
    }

    /**
     * Loads font for info panel text rendering.
     */
    private void loadFont() {
        font = FontManager.getFont();
    }

    /**
     * Loads marker textures from assets folder.
     * If textures are not found, will use default shape rendering.
     *
     * Place your PNG marker images in: assets/markers/
     * - marker_free.png (green marker)
     * - marker_partial.png (yellow marker)
     * - marker_full.png (red marker)
     * - marker_unknown.png (gray marker)
     */
    private void loadMarkerTextures() {
        try {
            markerFreeTexture = new Texture(Gdx.files.internal("markers/marker_free.png"));
        } catch (Exception e) {
            Gdx.app.debug("MapScreen", "Marker texture not found: markers/marker_free.png - using default shapes");
            markerFreeTexture = null;
        }

        try {
            markerPartialTexture = new Texture(Gdx.files.internal("markers/marker_partial.png"));
        } catch (Exception e) {
            Gdx.app.debug("MapScreen", "Marker texture not found: markers/marker_partial.png - using default shapes");
            markerPartialTexture = null;
        }

        try {
            markerFullTexture = new Texture(Gdx.files.internal("markers/marker_full.png"));
        } catch (Exception e) {
            Gdx.app.debug("MapScreen", "Marker texture not found: markers/marker_full.png - using default shapes");
            markerFullTexture = null;
        }

        try {
            markerUnknownTexture = new Texture(Gdx.files.internal("markers/marker_unknown.png"));
        } catch (Exception e) {
            Gdx.app.debug("MapScreen", "Marker texture not found: markers/marker_unknown.png - using default shapes");
            markerUnknownTexture = null;
        }
    }

    /**
     * Loads parking locations from the API and converts them to markers.
     * This runs in a separate thread to avoid blocking the UI.
     */
    private void loadParkingLocationsFromAPI() {
        // Run API call in a separate thread to avoid blocking the UI
        new Thread(() -> {
            try {
                Gdx.app.log("MapScreen", "Fetching parking locations from API...");

                // Fetch parking locations from API
                List<org.json.JSONObject> jsonLocations = parkingService.fetchParkingLocations();

                // Map JSON to Parking models
                List<Parking> parkingList = ParkingMapper.mapToParkingList(jsonLocations);

                Gdx.app.log("MapScreen", "Loaded " + parkingList.size() + " parking locations from API");

                // Convert Parking models to Marker models and add to markers list
                // We need to do this on the main thread (libGDX thread)
                final List<Parking> finalParkingList = parkingList;
                Gdx.app.postRunnable(() -> {
                    markers.clear();
                    for (Parking parking : finalParkingList) {
                        Marker marker = convertParkingToMarker(parking);
                        if (marker != null) {
                            markers.add(marker);
                        }
                    }
                    Gdx.app.log("MapScreen", "Added " + markers.size() + " markers to map");
                });

            } catch (Exception e) {
                Gdx.app.error("MapScreen", "Failed to load parking locations from API", e);
                Gdx.app.error("MapScreen", "Error: " + e.getMessage());

                // Fallback to test markers if API fails
                Gdx.app.postRunnable(() -> {
                    Gdx.app.log("MapScreen", "Using test markers as fallback");
                    initializeTestMarkers();
                });
            }
        }).start();
    }

    /**
     * Converts a Parking model to a Marker model for display on the map.
     */
    private Marker convertParkingToMarker(Parking parking) {
        if (parking == null || parking.getLocation() == null) {
            return null;
        }

        // Determine marker type (default to PARKING_LOT)
        Marker.MarkerType markerType = Marker.MarkerType.PARKING_LOT;

        // Determine marker state based on occupancy
        Marker.MarkerState markerState;
        int totalSpots = parking.getTotalSpots();
        int availableSpots = parking.getTotalAvailableSpots();

        if (totalSpots == 0) {
            markerState = Marker.MarkerState.UNKNOWN;
        } else {
            float occupancyRatio = (float) availableSpots / totalSpots;
            if (occupancyRatio >= 0.5f) {
                markerState = Marker.MarkerState.FREE;
            } else if (occupancyRatio > 0f) {
                markerState = Marker.MarkerState.PARTIAL;
            } else {
                markerState = Marker.MarkerState.FULL;
            }
        }

        // Create marker
        Marker marker = new Marker(
                parking.getLocation(),
                markerType,
                markerState,
                parking.getId(),
                parking.getName() != null ? parking.getName() : "Unknown",
                totalSpots,
                availableSpots,
                0f // Price not available in backend model
        );

        return marker;
    }

    /**
     * Initialize test markers for demonstration (fallback when API fails).
     */
    private void initializeTestMarkers() {
        // Test markers around Ljubljana center
        markers.add(new Marker(
                new Geolocation(46.0569, 14.5058), // Center of Ljubljana
                Marker.MarkerType.PARKING_LOT,
                Marker.MarkerState.FREE,
                "test-1", "Parking Center", 50, 35, 2.5f
        ));

        markers.add(new Marker(
                new Geolocation(46.0580, 14.5070),
                Marker.MarkerType.GARAGE,
                Marker.MarkerState.PARTIAL,
                "test-2", "Garage North", 100, 45, 3.0f
        ));

        markers.add(new Marker(
                new Geolocation(46.0550, 14.5040),
                Marker.MarkerType.STREET_PARKING,
                Marker.MarkerState.FULL,
                "test-3", "Street Parking South", 20, 0, 1.5f
        ));

        markers.add(new Marker(
                new Geolocation(46.0590, 14.5030),
                Marker.MarkerType.PARKING_LOT,
                Marker.MarkerState.UNKNOWN,
                "test-4", "Parking East", 30, 0, 0f
        ));
    }

    /**
     * Sets the size of markers in pixels.
     * @param size Size in pixels (default is 24f, recommended range: 16-48)
     */
    public void setMarkerSize(float size) {
        this.markerSize = Math.max(8f, Math.min(128f, size)); // Clamp between 8 and 64 pixels
    }

    /**
     * Gets the current marker size.
     * @return Current marker size in pixels
     */
    public float getMarkerSize() {
        return markerSize;
    }

    private void setupCamera() {
        camera.setToOrtho(false, MapConstants.MAP_WIDTH, MapConstants.MAP_HEIGHT);
        camera.position.set(MapConstants.MAP_WIDTH / 2f, MapConstants.MAP_HEIGHT / 2f, 0);
        camera.viewportWidth = MapConstants.MAP_WIDTH / 2f;
        camera.viewportHeight = MapConstants.MAP_HEIGHT / 2f;
        camera.zoom = 2f;
        camera.update();
    }

    private void setupInputHandlers() {
        // Create gesture detector for zoom and pan
        gestureDetector = new GestureDetector(new MapGestureListener());

        // Create input adapter for scroll wheel
        InputAdapter scrollInputAdapter = new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                // amountY > 0 means scroll up (zoom in), < 0 means scroll down (zoom out)
                float zoomSpeed = 0.1f;
                camera.zoom += amountY * zoomSpeed;
                camera.zoom = MathUtils.clamp(camera.zoom, MapConstants.MIN_ZOOM, MapConstants.MAX_ZOOM);
                return true;
            }
        };

        // Use InputMultiplexer to handle gestures, scroll, and keyboard input
        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(scrollInputAdapter);
        inputMultiplexer.addProcessor(gestureDetector);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    @Override
    public void render(float delta) {
        handleKeyboardInput();

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (tiledMapRenderer != null && tiledMap != null) {
            // Clamp zoom to valid range
            camera.zoom = MathUtils.clamp(camera.zoom, MapConstants.MIN_ZOOM, MapConstants.MAX_ZOOM);

            // Clamp camera position to map bounds
            clampCameraPosition();

            camera.update();
            tiledMapRenderer.setView(camera);
            tiledMapRenderer.render();

            // Draw markers on top of the map
            drawMarkers();

            // Draw info panel if marker is selected
            drawInfoPanel();
        }
    }

    /**
     * Handles click on marker.
     * Converts screen coordinates to world coordinates and checks if click is on a marker.
     */
    private void handleMarkerClick(float screenX, float screenY) {
        if (beginTile == null || markers == null) {
            return;
        }

        // Convert screen coordinates to world coordinates
        Vector3 worldPos = new Vector3(screenX, screenY, 0);
        camera.unproject(worldPos);

        // Check each marker to see if click is within marker bounds
        float clickRadius = markerSize / 2f + 5f; // Add some tolerance

        for (Marker marker : markers) {
            Vector2 markerPixelPos = MapRasterTiles.getPixelPosition(
                    marker.getPosition().lat,
                    marker.getPosition().lng,
                    beginTile.x,
                    beginTile.y
            );

            // Calculate distance from click to marker
            float distance = Vector2.dst(
                    worldPos.x, worldPos.y,
                    markerPixelPos.x, markerPixelPos.y
            );

            if (distance <= clickRadius) {
                // Marker clicked!
                selectedMarker = marker;
                Gdx.app.debug("MapScreen", "Marker clicked: " + marker.getName());
                return;
            }
        }

        // Click was not on any marker - deselect
        selectedMarker = null;
    }

    /**
     * Draws info panel with marker information.
     */
    private void drawInfoPanel() {
        if (selectedMarker == null) {
            return;
        }

        // Draw info panel using ShapeRenderer
        if (shapeRenderer == null) {
            return;
        }

        float panelWidth = 300f;
        float panelHeight = 150f;
        float padding = 10f;
        float screenWidth = Gdx.graphics.getWidth();

        // Position panel at bottom center
        float panelX = (screenWidth - panelWidth) / 2f;
        float panelY = padding;

        // Use orthographic camera for UI rendering (screen coordinates)
        // Create a temporary camera for UI
        com.badlogic.gdx.graphics.OrthographicCamera uiCamera = new com.badlogic.gdx.graphics.OrthographicCamera();
        uiCamera.setToOrtho(false, screenWidth, Gdx.graphics.getHeight());
        uiCamera.update();

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw panel background (semi-transparent)
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.9f);
        shapeRenderer.rect(panelX, panelY, panelWidth, panelHeight);

        // Draw border
        shapeRenderer.setColor(0.5f, 0.5f, 0.5f, 1f);
        shapeRenderer.rect(panelX, panelY, panelWidth, 2f); // Top border
        shapeRenderer.rect(panelX, panelY + panelHeight - 2f, panelWidth, 2f); // Bottom border
        shapeRenderer.rect(panelX, panelY, 2f, panelHeight); // Left border
        shapeRenderer.rect(panelX + panelWidth - 2f, panelY, 2f, panelHeight); // Right border

        shapeRenderer.end();

        // Draw text with font
        if (font != null && spriteBatch != null) {
            spriteBatch.setProjectionMatrix(uiCamera.combined);
            spriteBatch.begin();

            float textX = panelX + padding;
            float textY = panelY + panelHeight - padding - 20f; // Start from top

            // Draw marker name
            font.setColor(Color.WHITE);
            font.draw(spriteBatch, selectedMarker.getName() != null ? selectedMarker.getName() : "Unknown",
                     textX, textY);

            textY -= 20f; // Smaller spacing for smaller font

            // Draw marker type
            font.setColor(Color.LIGHT_GRAY);
            font.draw(spriteBatch, "Type: " + selectedMarker.getType().name().replace("_", " "),
                     textX, textY);

            textY -= 20f; // Smaller spacing for smaller font

            // Draw occupancy info
            if (selectedMarker.getTotalSpots() > 0) {
                font.setColor(getColorForState(selectedMarker.getState()));
                String spotsInfo = String.format("Spots: %d/%d (%.0f%%)",
                    selectedMarker.getAvailableSpots(),
                    selectedMarker.getTotalSpots(),
                    selectedMarker.getOccupancyPercentage());
                font.draw(spriteBatch, spotsInfo, textX, textY);
            } else {
                font.setColor(Color.GRAY);
                font.draw(spriteBatch, "Spots: Unknown", textX, textY);
            }

            textY -= 20f; // Smaller spacing for smaller font

            // Draw price if available
            if (selectedMarker.getPricePerHour() > 0) {
                font.setColor(Color.LIGHT_GRAY);
                font.draw(spriteBatch, String.format("Price: %.2f €/h", selectedMarker.getPricePerHour()),
                         textX, textY);
            }

            spriteBatch.end();
        }
    }

    /**
     * Draws all markers on the map.
     * Uses PNG textures if available, otherwise falls back to colored circles.
     * Markers are colored/textured based on their state:
     * - FREE: Green
     * - PARTIAL: Yellow
     * - FULL: Red
     * - UNKNOWN: Gray
     */
    private void drawMarkers() {
        if (beginTile == null || markers == null) {
            return;
        }

        // Check if we have any textures loaded
        boolean useTextures = markerFreeTexture != null || markerPartialTexture != null
                           || markerFullTexture != null || markerUnknownTexture != null;

        if (useTextures) {
            drawMarkersWithTextures();
        } else {
            drawMarkersWithShapes();
        }
    }

    /**
     * Draws markers using PNG textures.
     */
    private void drawMarkersWithTextures() {
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();

        for (Marker marker : markers) {
            // Convert geolocation to pixel position
            Vector2 pixelPos = MapRasterTiles.getPixelPosition(
                    marker.getPosition().lat,
                    marker.getPosition().lng,
                    beginTile.x,
                    beginTile.y
            );

            // Get texture for marker state
            Texture markerTexture = getTextureForState(marker.getState());

            if (markerTexture != null) {
                // Draw texture centered at marker position
                spriteBatch.draw(
                        markerTexture,
                        pixelPos.x - markerSize / 2f,
                        pixelPos.y - markerSize / 2f,
                        markerSize,
                        markerSize
                );
            }
        }

        spriteBatch.end();
    }

    /**
     * Draws markers using colored circles (fallback when textures not available).
     */
    private void drawMarkersWithShapes() {
        if (shapeRenderer == null) {
            return;
        }

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Marker marker : markers) {
            // Convert geolocation to pixel position
            Vector2 pixelPos = MapRasterTiles.getPixelPosition(
                    marker.getPosition().lat,
                    marker.getPosition().lng,
                    beginTile.x,
                    beginTile.y
            );

            // Set color based on marker state
            Color markerColor = getColorForState(marker.getState());
            shapeRenderer.setColor(markerColor);

            // Draw marker as a circle (size scales with markerSize)
            float markerRadius = markerSize / 2f; // Scale circle radius with marker size
            shapeRenderer.circle(pixelPos.x, pixelPos.y, markerRadius);

            // Draw a small border in darker color
            shapeRenderer.setColor(markerColor.cpy().mul(0.7f));
            shapeRenderer.circle(pixelPos.x, pixelPos.y, markerRadius + 2f);
        }

        shapeRenderer.end();
    }

    /**
     * Returns texture for marker based on its state.
     */
    private Texture getTextureForState(Marker.MarkerState state) {
        switch (state) {
            case FREE:
                return markerFreeTexture;
            case PARTIAL:
                return markerPartialTexture;
            case FULL:
                return markerFullTexture;
            case UNKNOWN:
            default:
                return markerUnknownTexture;
        }
    }

    /**
     * Returns color for marker based on its state.
     */
    private Color getColorForState(Marker.MarkerState state) {
        switch (state) {
            case FREE:
                return Color.GREEN;
            case PARTIAL:
                return Color.YELLOW;
            case FULL:
                return Color.RED;
            case UNKNOWN:
            default:
                return Color.GRAY;
        }
    }

    private void handleKeyboardInput() {
        // Keyboard zoom controls: Q for zoom in, A for zoom out
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            camera.zoom -= 0.02f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.zoom += 0.02f;
        }

        // Alternative: + and - keys
        if (Gdx.input.isKeyPressed(Input.Keys.PLUS) || Gdx.input.isKeyPressed(Input.Keys.EQUALS)) {
            camera.zoom -= 0.02f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            camera.zoom += 0.02f;
        }

        // Keyboard pan controls: Arrow keys to move the map
        float panSpeed = 3f * camera.zoom; // Pan speed scales with zoom level
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            camera.translate(-panSpeed, 0, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            camera.translate(panSpeed, 0, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            camera.translate(0, -panSpeed, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            camera.translate(0, panSpeed, 0);
        }
    }

    private void clampCameraPosition() {
        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;

        // Only clamp if viewport is smaller than map (don't clamp when zoomed out too much)
        if (effectiveViewportWidth < MapConstants.MAP_WIDTH) {
            camera.position.x = MathUtils.clamp(
                    camera.position.x,
                    effectiveViewportWidth / 2f,
                    MapConstants.MAP_WIDTH - effectiveViewportWidth / 2f
            );
        } else {
            // When zoomed out, center the camera
            camera.position.x = MapConstants.MAP_WIDTH / 2f;
        }

        if (effectiveViewportHeight < MapConstants.MAP_HEIGHT) {
            camera.position.y = MathUtils.clamp(
                    camera.position.y,
                    effectiveViewportHeight / 2f,
                    MapConstants.MAP_HEIGHT - effectiveViewportHeight / 2f
            );
        } else {
            // When zoomed out, center the camera
            camera.position.y = MapConstants.MAP_HEIGHT / 2f;
        }
    }

    @Override
    public void dispose() {
        if (tiledMap != null) {
            tiledMap.dispose();
        }
        if (mapTiles != null) {
            for (Texture texture : mapTiles) {
                if (texture != null) {
                    texture.dispose();
                }
            }
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (spriteBatch != null) {
            spriteBatch.dispose();
        }
        // Dispose marker textures
        if (markerFreeTexture != null) {
            markerFreeTexture.dispose();
        }
        if (markerPartialTexture != null) {
            markerPartialTexture.dispose();
        }
        if (markerFullTexture != null) {
            markerFullTexture.dispose();
        }
        if (markerUnknownTexture != null) {
            markerUnknownTexture.dispose();
        }
        if (font != null) {
            font.dispose();
        }
        FontManager.dispose();
    }

    /**
     * Gesture listener for map interactions (zoom and pan).
     */
    private class MapGestureListener implements GestureDetector.GestureListener {

        @Override
        public boolean touchDown(float x, float y, int pointer, int button) {
            return false;
        }

        @Override
        public boolean tap(float x, float y, int count, int button) {
            // Handle marker click
            if (count == 1) { // Single tap
                handleMarkerClick(x, y);
            }
            return false;
        }

        @Override
        public boolean longPress(float x, float y) {
            return false;
        }

        @Override
        public boolean fling(float velocityX, float velocityY, int button) {
            return false;
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY) {
            // Pan the camera by translating it opposite to the drag direction
            // deltaX and deltaY are in screen coordinates, need to convert to world coordinates
            camera.translate(-deltaX * camera.zoom, deltaY * camera.zoom);
            return true;
        }

        @Override
        public boolean panStop(float x, float y, int pointer, int button) {
            return false;
        }

        @Override
        public boolean zoom(float initialDistance, float distance) {
            // Zoom in/out based on gesture distance
            float ratio = initialDistance / distance;
            camera.zoom *= ratio;
            camera.zoom = MathUtils.clamp(camera.zoom, MapConstants.MIN_ZOOM, MapConstants.MAX_ZOOM);
            return true;
        }

        @Override
        public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
            return false;
        }

        @Override
        public void pinchStop() {
            // Not used
        }
    }
}

