package si.um.feri.parkingmate.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import org.json.JSONObject;

import si.um.feri.parkingmate.ParkingMate;
import si.um.feri.parkingmate.api.ParkingMapper;
import si.um.feri.parkingmate.api.ParkingService;
import si.um.feri.parkingmate.map.*;
import si.um.feri.parkingmate.model.Marker;
import si.um.feri.parkingmate.model.Parking;
import si.um.feri.parkingmate.ui.InfoPanel;
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

    // Marker textures
    private Texture markerFreeTexture;
    private Texture markerPartialTexture;
    private Texture markerFullTexture;
    private Texture markerUnknownTexture;

    // NAVIGATION BUTTON
    private Texture navButtonTexture;
    private Texture navButtonActiveTexture;
    private Texture carIconTexture;
    private boolean navigationMode = false;
    private Vector2 carPosition; // Car position on the map
    private Vector2 cursorWorldPos; // Cursor position in world coordinates

    // Route navigation variables
    private Route currentRoute;
    private float routeUpdateTimer = 0f;
    private static final float ROUTE_UPDATE_INTERVAL = 5f; // Update route every 5 seconds
    private boolean isCalculatingRoute = false;

    // NAVIGATION TARGET VARIABLES
    private Vector2 navigationTarget = null; // Target parking marker position
    private Marker selectedParkingMarker = null; // Currently selected parking marker
    private boolean isMovingToTarget = false; // Whether car is moving to target
    private float moveProgress = 0f; // Progress of movement (0 to 1)
    private float moveSpeed = 1.0f; // Speed of car movement (units per second)

    // BUTTON DIMENSIONS AND POSITION
    private float navButtonSize = 48f;
    private float navButtonMargin = 15f;
    private float navButtonX, navButtonY;

    // DASHED LINE PROPERTIES
    private float[] dashedLinePattern = {10f, 5f}; // 10px line, 5px gap
    private float dashedLinePhase = 0f;

    // Marker size configuration (in pixels)
    private float markerSize = 128f;

    // Info Panel
    private InfoPanel infoPanel;
    private float infoPanelWidth;
    private float infoPanelHeight;
    private float infoPanelX;
    private float infoPanelY;
    private Vector2 originalCarPosition = null;

    // Font for info panel text
    private BitmapFont font;

    // API service for fetching parking data
    private ParkingService parkingService;


    // Backend API base URL (backend runs on port 3002)
    private static final String API_BASE_URL = "http://localhost:3002";

    public MapScreen(ParkingMate game) {
        this.game = game;
        this.markers = new ArrayList<>();
        this.parkingService = new ParkingService(API_BASE_URL);
        this.infoPanel = new InfoPanel();
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

        // Fill layer with tiles
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

        // Setup info panel
        setupInfoPanel();

        infoPanel.setAnimationDuration(0.4f);

        // Setup input handlers for zoom and pan
        setupInputHandlers();

        // Initialize shape renderer for markers (fallback if textures not available)
        shapeRenderer = new ShapeRenderer();

        // Initialize sprite batch for marker textures
        spriteBatch = new SpriteBatch();

        // Load font for info panel
        loadFont();

        // Initialize info panel with font
        infoPanel.setFont(font);

        // Try to load marker textures
        loadMarkerTextures();

        infoPanel.setMarkerTextures(markerFreeTexture, markerPartialTexture,
            markerFullTexture, markerUnknownTexture);

        // Initialize navigation button
        initializeNavigationButton();

        // Set initial car position (center of Ljubljana)
        carPosition = new Vector2(MapConstants.MAP_WIDTH / 2f, MapConstants.MAP_HEIGHT / 2f);
        cursorWorldPos = new Vector2();

        // Load parking locations from API
        loadParkingLocationsFromAPI();

        // Set initial car position (camera center)
        setupInitialCarPosition();

        // Load parking locations from API
        loadParkingLocationsFromAPI();
    }

    /**
     * Initialize textures for navigation button.
     */
    private void initializeNavigationButton() {
        Gdx.app.log("MapScreen", "=== INITIALIZING NAVIGATION ===");

        try {
            navButtonTexture = new Texture(Gdx.files.internal("ui/nav_button.png"));
        } catch (Exception e) {
            createDefaultNavButton();
        }

        try {
            navButtonActiveTexture = new Texture(Gdx.files.internal("ui/nav_button_active.png"));
        } catch (Exception e) {
            navButtonActiveTexture = navButtonTexture;
        }

        try {
            carIconTexture = new Texture(Gdx.files.internal("markers/car_icon.png"));
        } catch (Exception e) {
            createDefaultCarIcon();
        }

        navButtonX = navButtonMargin;
        navButtonY = Gdx.graphics.getHeight() - navButtonSize - navButtonMargin;

        Gdx.app.log("MapScreen", "Nav button (TOP-LEFT) at: " + navButtonX + ", " + navButtonY);
    }

    /**
     * Create default navigation button (red circle).
     */
    private void createDefaultNavButton() {
        Pixmap pixmap = new Pixmap((int)navButtonSize, (int)navButtonSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.RED);
        pixmap.fillCircle((int)navButtonSize/2, (int)navButtonSize/2, (int)navButtonSize/2 - 2);

        // White arrow symbol
        pixmap.setColor(Color.WHITE);
        pixmap.fillTriangle(
            (int)(navButtonSize * 0.3f), (int)(navButtonSize * 0.3f),
            (int)(navButtonSize * 0.3f), (int)(navButtonSize * 0.7f),
            (int)(navButtonSize * 0.7f), (int)(navButtonSize * 0.5f)
        );

        navButtonTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    /**
     * Create default car icon (blue rectangle).
     */
    /**
     * Create default car icon for 48px.
     */
    private void createDefaultCarIcon() {
        int size = 48; // Changed from 32 to 48
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLUE);

        // Car body
        pixmap.fillRectangle(8, 15, 32, 18);

        // Windows
        pixmap.setColor(Color.CYAN);
        pixmap.fillRectangle(10, 24, 12, 6);
        pixmap.fillRectangle(26, 24, 12, 6);

        // Wheels
        pixmap.setColor(Color.BLACK);
        pixmap.fillCircle(12, 12, 6);
        pixmap.fillCircle(36, 12, 6);

        carIconTexture = new Texture(pixmap);
        pixmap.dispose();
        Gdx.app.log("MapScreen", "Created 48px default car icon");
    }

    /**
     * Setup info panel dimensions and position.
     */
    private void setupInfoPanel() {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        // Set panel width to 40% of screen width
        infoPanelWidth = screenWidth * 0.40f;
        infoPanelHeight = screenHeight;

        // Position at right side of screen
        infoPanelX = screenWidth - infoPanelWidth;
        infoPanelY = 0; // Start from bottom of screen

        // Set bounds for info panel
        infoPanel.setBounds(infoPanelX, infoPanelY, infoPanelWidth, infoPanelHeight);
    }

    /**
     * Loads font for info panel text rendering.
     */
    private void loadFont() {
        font = FontManager.getFont();
        infoPanel.setFont(font);
    }

    /**
     * Loads marker textures from assets folder.
     * If textures are not found, will use default shape rendering.
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
        new Thread(() -> {
            try {
                Gdx.app.log("MapScreen", "Fetching parking locations with tariffs from API...");

                // Fetch parking locations WITH TARIFFS
                List<JSONObject> jsonLocations = parkingService.fetchParkingLocationsWithTariffs();

                // Map JSON to Parking models WITH TARIFFS
                List<Parking> parkingList = ParkingMapper.mapToParkingListWithTariffs(jsonLocations);

                Gdx.app.log("MapScreen", "Loaded " + parkingList.size() + " parking locations with tariffs");

                // Convert Parking models to Marker models
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
                Gdx.app.error("MapScreen", "Failed to load parking locations with tariffs", e);

                try {
                    List<JSONObject> jsonLocations = parkingService.fetchParkingLocations();
                    List<Parking> parkingList = ParkingMapper.mapToParkingList(jsonLocations);

                    final List<Parking> finalParkingList = parkingList;
                    Gdx.app.postRunnable(() -> {
                        markers.clear();
                        for (Parking parking : finalParkingList) {
                            Marker marker = convertParkingToMarker(parking);
                            if (marker != null) {
                                markers.add(marker);
                            }
                        }
                        Gdx.app.log("MapScreen", "Added " + markers.size() + " markers (without tariffs)");
                    });
                } catch (Exception e2) {
                    Gdx.app.error("MapScreen", "Fallback also failed, using test markers", e2);
                    Gdx.app.postRunnable(() -> {
                        initializeTestMarkers();
                    });
                }
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

        // Determine marker type
        Marker.MarkerType markerType = Marker.MarkerType.PARKING_LOT;

        // Determine marker state
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

        // Get price from tariffs
        float pricePerHour = parking.getPricePerHour();

        // Create marker
        Marker marker = new Marker(
            parking.getLocation(),
            markerType,
            markerState,
            parking.getId(),
            parking.getName() != null ? parking.getName() : "Unknown",
            totalSpots,
            availableSpots,
            pricePerHour
        );

        // Store the full Parking object in the marker for tariffs
        marker.setParkingData(parking);

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
     */
    public void setMarkerSize(float size) {
        this.markerSize = Math.max(8f, Math.min(128f, size)); // Clamp between 8 and 64 pixels
    }

    /**
     * Gets the current marker size.
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
        gestureDetector = new GestureDetector(new MapGestureListener());

        InputAdapter scrollInputAdapter = new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                float zoomSpeed = 0.1f;
                camera.zoom += amountY * zoomSpeed;
                camera.zoom = MathUtils.clamp(camera.zoom, MapConstants.MIN_ZOOM, MapConstants.MAX_ZOOM);
                return true;
            }

            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                float gdxY = Gdx.graphics.getHeight() - screenY;

                // CHECK CLICK ON NAVIGATION BUTTON
                if (isNavButtonClicked(screenX, gdxY)) {
                    navigationMode = !navigationMode;
                    Gdx.app.log("MapScreen", "Navigation mode: " + navigationMode);

                    // Reset navigation state when turning off navigation
                    if (!navigationMode) {
                        resetNavigation();
                    }

                    // WHEN NAVIGATION IS TURNED ON, CENTER CAMERA ON CAR
                    if (navigationMode) {
                        Vector2 ljubljanaPixel = MapRasterTiles.getPixelPosition(
                            46.0569, 14.5058,
                            beginTile.x,
                            beginTile.y
                        );

                        carPosition = new Vector2(ljubljanaPixel);
                        cursorWorldPos = new Vector2(carPosition);

                        camera.position.set(carPosition.x, carPosition.y, 0);
                        camera.update();

                        Gdx.app.log("MapScreen", "Navigation ON – car at Ljubljana: " + carPosition);
                    }
                    return true;
                }

                if (infoPanel.isTariffPopupCloseButtonClicked(screenX, gdxY)) {
                    infoPanel.closeTariffPopup();
                    return true;
                }

                if (infoPanel.isTariffPopupClicked(screenX, gdxY)) {
                    return true;
                }

                if (infoPanel.isTariffButtonClicked(screenX, gdxY)) {
                    infoPanel.openTariffPopup();
                    return true;
                }

                if (infoPanel.isCloseButtonClicked(screenX, gdxY)) {
                    infoPanel.hide();
                    return true;
                }

                if (infoPanel.contains(screenX, gdxY)) {
                    return true;
                }

                // Handle marker click for navigation
                if (navigationMode) {
                    boolean markerClicked = handleMarkerClickForNavigation(screenX, screenY);
                    if (markerClicked) {
                        return true;
                    }
                }

                handleMarkerClick(screenX, screenY);
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (navigationMode && !isMovingToTarget) {
                    Vector3 worldPos = new Vector3(screenX, screenY, 0);
                    camera.unproject(worldPos);
                    cursorWorldPos.set(worldPos.x, worldPos.y);
                }
                return false;
            }
        };

        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(scrollInputAdapter);
        inputMultiplexer.addProcessor(gestureDetector);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    /**
     * Checks if the navigation button was clicked.
     */
    private boolean isNavButtonClicked(float screenX, float screenY) {
        float x = navButtonMargin;
        float y = Gdx.graphics.getHeight() - navButtonSize - navButtonMargin;

        return screenX >= x &&
            screenX <= x + navButtonSize &&
            screenY >= y &&
            screenY <= y + navButtonSize;
    }

    /**
     * Draw dashed line between two points.
     */
    private void drawDashedLine(ShapeRenderer shapeRenderer, Vector2 start, Vector2 end) {
        if (shapeRenderer == null) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f);

        float distance = start.dst(end);
        Vector2 direction = new Vector2(end).sub(start).nor();

        float dashLength = 25f;
        float gapLength = 12f;

        float drawn = 0;
        boolean draw = true;

        while (drawn < distance) {
            float segmentLength = draw ? dashLength : gapLength;
            float segmentEnd = Math.min(drawn + segmentLength, distance);

            if (draw) {
                Vector2 s = new Vector2(start).add(new Vector2(direction).scl(drawn));
                Vector2 e = new Vector2(start).add(new Vector2(direction).scl(segmentEnd));
                shapeRenderer.rectLine(s, e, 6f);
            }

            drawn = segmentEnd;
            draw = !draw;
        }

        shapeRenderer.end();
    }

    /**
     * Draw solid line between two points.
     */
    private void drawSolidLine(ShapeRenderer shapeRenderer, Vector2 start, Vector2 end, float thickness) {
        if (shapeRenderer == null) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0.5f, 1f, 0.8f); // Blue color for navigation line
        shapeRenderer.rectLine(start, end, thickness);
        shapeRenderer.end();
    }

    @Override
    public void render(float delta) {
        handleKeyboardInput();
        // Update car movement
        if (isMovingToTarget) {
            updateCarMovement(delta);
        }

        // Update dashed line phase for animation
        if (navigationMode && !isMovingToTarget) {
            dashedLinePhase += delta * 100f;
        }

        // Update info panel animation
        infoPanel.update(delta);

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

            // DRAW NAVIGATION IF ACTIVE
            if (navigationMode) {
                drawNavigation();
            }

            // Draw info panel if visible
            infoPanel.render();
            drawNavigationButton();
        }
    }
    private void updateRouteFromCurrentPosition() {
        if (selectedParkingMarker == null || isCalculatingRoute) return;

        final Geolocation carGeolocation = getGeolocationFromPixel(carPosition);
        if (carGeolocation == null) return;

        isCalculatingRoute = true;

        new Thread(() -> {
            try {
                Geolocation[] newRoutePoints = MapRasterTiles.fetchRoute(
                    carGeolocation,
                    selectedParkingMarker.getPosition()
                );

                Gdx.app.postRunnable(() -> {
                    if (newRoutePoints != null && newRoutePoints.length > 1) {
                        Route newRoute = createRouteFromGeolocations(newRoutePoints);
                        if (newRoute != null) {
                            currentRoute = newRoute;
                            Gdx.app.log("MapScreen", "Route updated from current position");
                        }
                    }
                    isCalculatingRoute = false;
                });

            } catch (Exception e) {
                Gdx.app.error("MapScreen", "Failed to update route", e);
                Gdx.app.postRunnable(() -> {
                    isCalculatingRoute = false;
                });
            }
        }).start();
    }

    /**
     * Update car movement - follows route
     */
    private void updateCarMovement(float delta) {
        if (currentRoute != null && navigationTarget != null && isMovingToTarget) {
            followRoute(delta);
        }
    }

    /**
     * Follow route waypoints
     */
    private void followRoute(float delta) {
        if (currentRoute == null || navigationTarget == null) return;

        Vector2 currentWaypoint = currentRoute.getCurrentWaypoint();
        if (currentWaypoint == null) return;

        float speed = 150f; // pixels per second
        float distanceToWaypoint = carPosition.dst(currentWaypoint);

        if (distanceToWaypoint < 10f) {
            // Reached waypoint, move to next
            if (!currentRoute.moveToNextWaypoint()) {
                // Route complete - auto JE STIGAO NA PARKING
                carPosition.set(navigationTarget); // Postavi tačno na parking
                isMovingToTarget = false;

                // Resetuj samo neke stvari, ali ostavi auto na parkingu
                selectedParkingMarker = null;
                navigationTarget = null;
                originalCarPosition = null;

                // NE resetuj currentRoute - ostavi ga da bi mogao da crtaš gde je auto bio
                Gdx.app.log("MapScreen", "Car arrived at parking!");
                return;
            }
            currentWaypoint = currentRoute.getCurrentWaypoint();
        }

        // Move towards current waypoint
        Vector2 direction = new Vector2(currentWaypoint).sub(carPosition).nor();
        carPosition.add(direction.scl(speed * delta));

        // Update camera to follow car
        camera.position.set(carPosition.x, carPosition.y, 0);
        camera.update();
    }
    /**
     * Draw navigation
     */
    private void drawNavigation() {
        if (shapeRenderer == null || spriteBatch == null || carPosition == null) return;

        // Draw route from current car position
        if (currentRoute != null && currentRoute.getWaypoints().size() > 1 && isMovingToTarget) {
            drawRoute(shapeRenderer);
        }
        // Draw dashed line to cursor when no target selected
        else if (navigationTarget == null && cursorWorldPos != null &&
            carPosition.dst(cursorWorldPos) > 5f) {
            drawDashedLine(shapeRenderer, carPosition, cursorWorldPos);
        }

        // Draw car
        drawCar();
    }
    /**
     * Draw route from CURRENT CAR POSITION to target
     */
    private void drawRoute(ShapeRenderer shapeRenderer) {
        if (shapeRenderer == null || currentRoute == null) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0.5f, 1f, 0.8f); // Blue route

        List<Vector2> waypoints = currentRoute.getWaypoints();

        // Pronađi indeks trenutne tačke (ili najbliže tačke)
        int startIndex = findClosestWaypointIndex(carPosition, waypoints);

        // Crtaj od trenutne pozicije auta do kraja rute
        for (int i = startIndex; i < waypoints.size() - 1; i++) {
            Vector2 start = waypoints.get(i);
            Vector2 end = waypoints.get(i + 1);

            // Ako je prvi segment, koristi trenutnu poziciju auta umesto početne tačke
            if (i == startIndex) {
                start = carPosition;
            }

            shapeRenderer.rectLine(start, end, 6f);
        }

        shapeRenderer.end();
    }

    /**
     * Pronalazi indeks najbliže tačke u ruti
     */
    private int findClosestWaypointIndex(Vector2 position, List<Vector2> waypoints) {
        if (waypoints.isEmpty()) return 0;

        int closestIndex = 0;
        float minDistance = position.dst(waypoints.get(0));

        for (int i = 1; i < waypoints.size(); i++) {
            float distance = position.dst(waypoints.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                closestIndex = i;
            }
        }

        return closestIndex;
    }
    /**
     * Draw car separately for reuse
     */
    private void drawCar() {
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();

        float carIconSize = 150f;
        spriteBatch.draw(
            carIconTexture,
            carPosition.x - carIconSize / 2f,
            carPosition.y - carIconSize / 2f,
            carIconSize,
            carIconSize
        );

        spriteBatch.end();
    }

    /**
     * Draw navigation button (always on top of everything).
     */
    private void drawNavigationButton() {
        if (spriteBatch == null || navButtonTexture == null) return;

        float x = navButtonMargin;
        float y = Gdx.graphics.getHeight() - navButtonSize - navButtonMargin;

        spriteBatch.setProjectionMatrix(
            new Matrix4().setToOrtho2D(
                0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
            )
        );

        spriteBatch.begin();
        spriteBatch.draw(
            navigationMode ? navButtonActiveTexture : navButtonTexture,
            x, y,
            navButtonSize, navButtonSize
        );
        spriteBatch.end();
    }

    // Set initial car position
    private void setupInitialCarPosition() {
        // Place car at screen center (camera) instead of map center
        carPosition = new Vector2(camera.position.x, camera.position.y);
        cursorWorldPos = new Vector2(carPosition);

        Gdx.app.log("MapScreen", "Initial car position (camera center): " + carPosition);
    }

    /**
     * Handles click on marker for navigation purposes.
     */
    /**
     * Handle marker click for navigation with route calculation
     */
    private boolean handleMarkerClickForNavigation(float screenX, float screenY) {
        if (beginTile == null || markers == null || isCalculatingRoute) {
            return false;
        }

        // Convert screen coordinates to world coordinates
        Vector3 worldPos = new Vector3(screenX, screenY, 0);
        camera.unproject(worldPos);

        // Check each marker
        float clickRadius = markerSize / 2f + 10f;

        for (Marker marker : markers) {
            Vector2 markerPixelPos = MapRasterTiles.getPixelPosition(
                marker.getPosition().lat,
                marker.getPosition().lng,
                beginTile.x,
                beginTile.y
            );

            float distance = Vector2.dst(
                worldPos.x, worldPos.y,
                markerPixelPos.x, markerPixelPos.y
            );

            if (distance <= clickRadius) {
                // Start route calculation
                calculateRouteToMarker(marker);
                return true;
            }
        }
        return false;
    }
    /**
     * Calculate route from car position to marker using Geoapify API
     */
    private void calculateRouteToMarker(final Marker marker) {
        if (isCalculatingRoute) return;

        isCalculatingRoute = true;

        // Get current car geolocation
        final Geolocation carGeolocation = getGeolocationFromPixel(carPosition);
        if (carGeolocation == null) {
            isCalculatingRoute = false;
            return;
        }

        new Thread(() -> {
            try {
                Gdx.app.log("MapScreen", "Calculating route from car to marker...");

                // Use existing Geoapify routing API
                Geolocation[] routePoints = MapRasterTiles.fetchRoute(
                    carGeolocation,
                    marker.getPosition()
                );

                Gdx.app.postRunnable(() -> {
                    if (routePoints != null && routePoints.length > 1) {
                        // Create route from geolocations
                        currentRoute = createRouteFromGeolocations(routePoints);

                        // Set navigation target
                        selectedParkingMarker = marker;
                        navigationTarget = MapRasterTiles.getPixelPosition(
                            marker.getPosition().lat,
                            marker.getPosition().lng,
                            beginTile.x,
                            beginTile.y
                        );
                        originalCarPosition = new Vector2(carPosition);

                        // Reset route index na početak
                        currentRoute.reset();

                        // Start moving immediately
                        isMovingToTarget = true;

                        Gdx.app.log("MapScreen", "Route calculated with " +
                            routePoints.length + " points. Starting navigation.");

                    } else {
                        // Fallback: create simple route
                        Gdx.app.log("MapScreen", "Route calculation failed, creating simple route");
                        selectedParkingMarker = marker;
                        navigationTarget = MapRasterTiles.getPixelPosition(
                            marker.getPosition().lat,
                            marker.getPosition().lng,
                            beginTile.x,
                            beginTile.y
                        );
                        originalCarPosition = new Vector2(carPosition);

                        // Kreiraj jednostavnu rutu
                        currentRoute = new Route();
                        currentRoute.addWaypoint(carPosition);
                        currentRoute.addWaypoint(navigationTarget);

                        // Start moving immediately
                        isMovingToTarget = true;
                    }
                    isCalculatingRoute = false;
                });

            } catch (Exception e) {
                Gdx.app.error("MapScreen", "Route calculation error", e);
                Gdx.app.postRunnable(() -> {
                    // Fallback: create simple route
                    selectedParkingMarker = marker;
                    navigationTarget = MapRasterTiles.getPixelPosition(
                        marker.getPosition().lat,
                        marker.getPosition().lng,
                        beginTile.x,
                        beginTile.y
                    );
                    originalCarPosition = new Vector2(carPosition);

                    // Kreiraj jednostavnu rutu
                    currentRoute = new Route();
                    currentRoute.addWaypoint(carPosition);
                    currentRoute.addWaypoint(navigationTarget);

                    // Start moving immediately
                    isMovingToTarget = true;
                    isCalculatingRoute = false;
                });
            }
        }).start();
    }
    /**
     * Create Route object from array of geolocations
     */
    private Route createRouteFromGeolocations(Geolocation[] geolocations) {
        if (geolocations == null || geolocations.length < 2) {
            return null;
        }

        Route route = new Route();
        for (Geolocation geo : geolocations) {
            Vector2 pixelPos = MapRasterTiles.getPixelPosition(
                geo.lat,
                geo.lng,
                beginTile.x,
                beginTile.y
            );
            route.addWaypoint(pixelPos);
        }

        // Simplify route if too many points (for performance)
        return simplifyRoute(route, 20); // Keep max 20 points
    }

    /**
     * Simplify route by removing unnecessary points
     */
    private Route simplifyRoute(Route original, int maxPoints) {
        if (original.getTotalWaypoints() <= maxPoints) {
            return original;
        }

        Route simplified = new Route();
        List<Vector2> points = original.getWaypoints();

        // Always keep first and last points
        simplified.addWaypoint(points.get(0));

        // Sample points evenly
        int step = points.size() / (maxPoints - 1);
        for (int i = step; i < points.size() - step; i += step) {
            simplified.addWaypoint(points.get(i));
        }

        simplified.addWaypoint(points.get(points.size() - 1));
        return simplified;
    }

    /**
     * Convert pixel position to geolocation using tile calculations
     */
    private Geolocation getGeolocationFromPixel(Vector2 pixelPos) {
        if (beginTile == null) return null;

        double n = Math.pow(2.0, MapConstants.ZOOM);

        // MapRasterTiles.TILE_SIZE je u px
        double lon = (beginTile.x + pixelPos.x / MapRasterTiles.TILE_SIZE) / n * 360.0 - 180.0;
        double latRad = Math.atan(Math.sinh(Math.PI * (1 - 2 * (beginTile.y + pixelPos.y / MapRasterTiles.TILE_SIZE) / n)));
        double lat = Math.toDegrees(latRad);

        return new Geolocation(lat, lon);
    }

    /**
     * Handles click on marker for info panel display.
     */
    private void handleMarkerClick(float screenX, float screenY) {
        if (beginTile == null || markers == null || navigationMode) {
            return;
        }

        // Convert screen coordinates to world coordinates
        Vector3 worldPos = new Vector3(screenX, screenY, 0);
        camera.unproject(worldPos);

        // Check each marker to see if click is within marker bounds
        float clickRadius = markerSize / 2f + 10f;

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
                // Marker clicked
                Gdx.app.debug("MapScreen", "Marker clicked: " + marker.getName());

                // Toggle info panel with animation
                if (infoPanel.isVisible() && infoPanel.getSelectedMarker() == marker) {
                    // If same marker is clicked again, hide panel
                    infoPanel.hide();
                } else {
                    // Show panel for clicked marker
                    infoPanel.show(marker);
                }
                return;
            }
        }

        // Click was not on any marker - hide info panel if click was outside
        float gdxY = Gdx.graphics.getHeight() - screenY;
        if (!infoPanel.contains(screenX, gdxY)) {
            infoPanel.hide();
        }
    }

    /**
     * Resets navigation state.
     */
    /**
     * Resets navigation state
     */
    /**
     * Resets navigation state
     */
    /**
     * Draw simple line between two points (za fallback)
     */
    private void drawSimpleLine(Vector2 start, Vector2 end) {
        if (shapeRenderer == null) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0.5f, 1f, 0.8f);
        shapeRenderer.rectLine(start, end, 6f);
        shapeRenderer.end();
    }
    private void resetNavigation() {
        // Samo resetuj navigaciju, ali ostavi auto gde jeste
        selectedParkingMarker = null;
        navigationTarget = null;
        isMovingToTarget = false;
        originalCarPosition = null;
        currentRoute = null;
        cursorWorldPos = new Vector2(carPosition);
        isCalculatingRoute = false;
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
            if (infoPanel.isVisible() && infoPanel.getSelectedMarker() == marker) {
                continue;
            }

            Vector2 pixelPos = MapRasterTiles.getPixelPosition(
                marker.getPosition().lat,
                marker.getPosition().lng,
                beginTile.x,
                beginTile.y
            );

            Texture markerTexture = getTextureForState(marker.getState());

            if (markerTexture != null) {
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
            if (infoPanel.isVisible() && infoPanel.getSelectedMarker() == marker) {
                continue;
            }

            Vector2 pixelPos = MapRasterTiles.getPixelPosition(
                marker.getPosition().lat,
                marker.getPosition().lng,
                beginTile.x,
                beginTile.y
            );

            Color markerColor = getColorForState(marker.getState());
            shapeRenderer.setColor(markerColor);

            float markerRadius = markerSize / 2f;
            shapeRenderer.circle(pixelPos.x, pixelPos.y, markerRadius);
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

        // Close info panel with ESC key
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            infoPanel.hide();
        }

        // Reset navigation with R key
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetNavigation();
        }
    }

    private void clampCameraPosition() {
        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;

        // Only clamp if viewport is smaller than map
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
        if (infoPanel != null) {
            infoPanel.dispose();
        }
        if (navButtonTexture != null) {
            navButtonTexture.dispose();
        }
        if (navButtonActiveTexture != null && navButtonActiveTexture != navButtonTexture) {
            navButtonActiveTexture.dispose();
        }
        if (carIconTexture != null) {
            carIconTexture.dispose();
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
