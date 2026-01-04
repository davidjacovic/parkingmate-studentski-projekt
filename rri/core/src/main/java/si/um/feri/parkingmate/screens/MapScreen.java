package si.um.feri.parkingmate.screens;

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
import com.badlogic.gdx.math.Interpolation;
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
import si.um.feri.parkingmate.simulation.SimulationScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Main map screen displaying parking locations, navigation, and interactive elements.
 * This screen handles map rendering, marker display, routing, and user interactions.
 */
public class MapScreen extends BaseScreen {

    private final ParkingMate game;
    private TiledMap tiledMap;
    private TiledMapRenderer tiledMapRenderer;
    private Texture[] mapTiles;
    private ZoomXY beginTile;
    private GestureDetector gestureDetector;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private List<Marker> markers;

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
    private Vector2 carPosition;
    private Vector2 cursorWorldPos;
    private boolean isCalculatingRoute = false;

    // NAVIGATION TARGET VARIABLES
    private Vector2 navigationTarget = null;
    private Marker selectedParkingMarker = null;
    private boolean isMovingToTarget = false;

    // BUTTON DIMENSIONS AND POSITION
    private float navButtonSize = 48f;
    private float navButtonMargin = 15f;
    private float navButtonX, navButtonY;

    // ROUTE VISUALIZATION
    private List<Vector2> finalRoutePoints = null;
    private List<Vector2> traveledRoutePoints = new ArrayList<>();
    private Texture flagIconTexture;
    private Vector2 flagPosition = null;
    private float flagSize = 64f;

    // ROUTE COLORS
    private static final Color FUTURE_ROUTE_COLOR = new Color(0f, 0.5f, 1f, 0.6f);
    private static final Color TRAVELED_ROUTE_COLOR = new Color(0f, 0.8f, 0.2f, 0.8f);

    // DASHED LINE PROPERTIES
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
    private boolean isArriving = false;
    private float arrivalTimer = 0f;
    private static final float ARRIVAL_DURATION = 0.4f;

    // ARRIVAL EFFECTS
    private boolean arrivalPause = false;
    private static final float ARRIVAL_PAUSE_TIME = 0.25f;
    private float arrivalZoomTimer = 0f;
    private float arrivalZoomHoldTime = 1.2f;
    private float arrivalZoomTarget = 1.4f;
    private float arrivalZoomOriginal = -1f;

    // SIMULATION BUTTON
    private Texture simulationButtonTexture;
    private Texture simulationButtonActiveTexture;
    private boolean simulationMode = false;
    private float simulationButtonX, simulationButtonY;

    // Backend API base URL (backend runs on port 3002)
    private static final String API_BASE_URL = "http://localhost:3002";

    /**
     * Constructor for MapScreen.
     * @param game The main game instance.
     */
    public MapScreen(ParkingMate game) {
        this.game = game;
        this.markers = new ArrayList<>();
        this.parkingService = new ParkingService(API_BASE_URL);
        this.infoPanel = new InfoPanel();
    }

    /**
     * Called when this screen becomes the current screen.
     * Initializes the map and all related components.
     */
    @Override
    public void show() {
        initializeMap();
    }

    /**
     * Initializes the map, tiles, markers, and navigation system.
     * Loads map tiles from the Geoapify API and sets up the display.
     */
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

        // Initialize simulation button
        initializeSimulationButton();
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

        try {
            flagIconTexture = new Texture(Gdx.files.internal("markers/flag.png"));
        } catch (Exception e) {
            createDefaultFlagIcon();
        }

        navButtonX = navButtonMargin;
        navButtonY = Gdx.graphics.getHeight() - navButtonSize - navButtonMargin;

        Gdx.app.log("MapScreen", "Nav button (TOP-LEFT) at: " + navButtonX + ", " + navButtonY);
    }

    /**
     * Initialize textures for simulation button.
     */
    private void initializeSimulationButton() {
        Gdx.app.log("MapScreen", "=== INITIALIZING SIMULATION BUTTON ===");

        try {
            simulationButtonTexture = new Texture(Gdx.files.internal("markers/simulation.png"));
        } catch (Exception e) {
            createDefaultSimulationButton();
        }

        try {
            simulationButtonActiveTexture = new Texture(Gdx.files.internal("markers/simulation.png"));
        } catch (Exception e) {
            simulationButtonActiveTexture = simulationButtonTexture;
        }

        // Position below navigation button
        simulationButtonX = navButtonMargin;
        simulationButtonY = navButtonY - navButtonSize - navButtonMargin;

        Gdx.app.log("MapScreen", "Simulation button at: " + simulationButtonX + ", " + simulationButtonY);
    }

    /**
     * Create default simulation button (blue square with play icon).
     */
    private void createDefaultSimulationButton() {
        Pixmap pixmap = new Pixmap((int)navButtonSize, (int)navButtonSize, Pixmap.Format.RGBA8888);

        // Blue background
        pixmap.setColor(0.2f, 0.4f, 0.8f, 1f);
        pixmap.fillRectangle(0, 0, (int)navButtonSize, (int)navButtonSize);

        // White play symbol (triangle)
        pixmap.setColor(Color.WHITE);
        pixmap.fillTriangle(
            (int)(navButtonSize * 0.35f), (int)(navButtonSize * 0.25f),
            (int)(navButtonSize * 0.35f), (int)(navButtonSize * 0.75f),
            (int)(navButtonSize * 0.75f), (int)(navButtonSize * 0.5f)
        );

        simulationButtonTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    /**
     * Draw simulation button (below navigation button).
     */
    private void drawSimulationButton() {
        if (spriteBatch == null || simulationButtonTexture == null) return;

        float x = simulationButtonX;
        float y = simulationButtonY;

        spriteBatch.setProjectionMatrix(
            new Matrix4().setToOrtho2D(
                0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
            )
        );

        spriteBatch.begin();
        spriteBatch.draw(
            simulationMode ? simulationButtonActiveTexture : simulationButtonTexture,
            x, y,
            navButtonSize, navButtonSize
        );
        spriteBatch.end();
    }

    /**
     * Create default flag icon
     */
    private void createDefaultFlagIcon() {
        Pixmap pixmap = new Pixmap((int)flagSize, (int)flagSize, Pixmap.Format.RGBA8888);

        // White and black checkered pattern
        int cellSize = (int)flagSize / 8;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if ((x + y) % 2 == 0) {
                    pixmap.setColor(Color.WHITE);
                } else {
                    pixmap.setColor(Color.BLACK);
                }
                pixmap.fillRectangle(x * cellSize, y * cellSize, cellSize, cellSize);
            }
        }

        // Red pole
        pixmap.setColor(Color.RED);
        pixmap.fillRectangle(0, 0, 8, (int)flagSize);

        flagIconTexture = new Texture(pixmap);
        pixmap.dispose();
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
     * Create default car icon
     */
    private void createDefaultCarIcon() {
        int size = 48;
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
     * Sets up the camera with initial position and zoom settings.
     */
    private void setupCamera() {
        camera.setToOrtho(false, MapConstants.MAP_WIDTH, MapConstants.MAP_HEIGHT);
        camera.position.set(MapConstants.MAP_WIDTH / 2f, MapConstants.MAP_HEIGHT / 2f, 0);
        camera.viewportWidth = MapConstants.MAP_WIDTH / 2f;
        camera.viewportHeight = MapConstants.MAP_HEIGHT / 2f;
        camera.zoom = 2f;
        camera.update();
    }

    /**
     * Sets up input handlers for gestures, clicks, and keyboard input.
     */
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
                    if (infoPanel.isVisible()) {
                        Gdx.app.log("MapScreen", "Cannot turn on navigation while info panel is open");
                        return true;
                    }

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
                // CHECK CLICK ON SIMULATION BUTTON
                if (isSimulationButtonClicked(screenX, gdxY)) {
                    Gdx.app.log("MapScreen", "Simulation button clicked");

                    // Otvori novi simulation screen
                    openSimulationScreen();
                    return true;
                }

                // If info panel is clicked, turn off navigation
                if (infoPanel.isCloseButtonClicked(screenX, gdxY)) {
                    infoPanel.hide();
                    // Do not reset navigation here, just hide the panel
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

                if (infoPanel.contains(screenX, gdxY)) {
                    // If click is inside info panel, turn off navigation
                    if (navigationMode) {
                        navigationMode = false;
                        resetNavigation();
                    }
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
     * Checks if the simulation button was clicked.
     */
    private boolean isSimulationButtonClicked(float screenX, float screenY) {
        float x = simulationButtonX;
        float y = simulationButtonY;

        return screenX >= x &&
            screenX <= x + navButtonSize &&
            screenY >= y &&
            screenY <= y + navButtonSize;
    }
    private void openSimulationScreen() {
        Gdx.app.log("MapScreen", "Opening simulation screen...");

        // Zaustavi sve aktivne animacije ili procese
        if (isMovingToTarget) {
            isMovingToTarget = false;
        }

        if (navigationMode) {
            navigationMode = false;
            resetNavigation();
        }

        // Sakrij info panel ako je otvoren
        infoPanel.hide();

        // Otvori novi simulation screen
        SimulationScreen simulationScreen = new SimulationScreen(game);
        game.setScreen(simulationScreen);
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
        shapeRenderer.setColor(0f, 0f, 0f, 0.8f); // Black dashed line

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
                shapeRenderer.rectLine(s, e, 4f); // Thinner dashed line
            }

            drawn = segmentEnd;
            draw = !draw;
        }

        shapeRenderer.end();
    }

    /**
     * Main render method called every frame.
     * @param delta Time elapsed since last frame.
     */
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

            // Draw markers on top of the map (ALWAYS)
            drawMarkers();

            // DRAW NAVIGATION IF ACTIVE
            drawNavigation();

            // Draw simulation button
            drawSimulationButton();

            // Draw info panel if visible
            infoPanel.render();
            drawNavigationButton();
        }

        if (isArriving) {
            arrivalTimer += delta;
            if (arrivalTimer >= ARRIVAL_DURATION) {
                isArriving = false;
            }
        }

        if (isArriving) {
            camera.zoom = MathUtils.lerp(camera.zoom, 1.4f, 0.06f);
        }
    }

    /**
     * Update car movement - follows route
     */
    private void updateCarMovement(float delta) {
        if (arrivalPause) {
            arrivalTimer += delta;
            if (arrivalTimer >= ARRIVAL_PAUSE_TIME) {
                arrivalPause = false;
            }
            return;
        }

        // CHANGED: Checks finalRoutePoints instead of currentRoute
        if (finalRoutePoints != null && navigationTarget != null && isMovingToTarget) {
            followRoute(delta);
        }
    }

    /**
     * Follow route smoothly - BETTER VERSION
     */
    private void followRoute(float delta) {
        if (finalRoutePoints == null || finalRoutePoints.size() < 2 || navigationTarget == null) return;

        float speed = 150f;
        float distanceToTarget = carPosition.dst(navigationTarget);

        // Check arrival
        if (distanceToTarget < 15f) {
            handleArrival();
            return;
        }

        // Find current segment
        int segmentIndex = -1;
        float closestDistance = Float.MAX_VALUE;

        for (int i = 0; i < finalRoutePoints.size() - 1; i++) {
            Vector2 start = finalRoutePoints.get(i);
            Vector2 end = finalRoutePoints.get(i + 1);

            float distance = pointToSegmentDistance(carPosition, start, end);
            if (distance < closestDistance) {
                closestDistance = distance;
                segmentIndex = i;
            }
        }

        if (segmentIndex >= 0) {
            Vector2 segmentStart = finalRoutePoints.get(segmentIndex);
            Vector2 segmentEnd = finalRoutePoints.get(segmentIndex + 1);

            // Move along segment
            Vector2 segmentDir = new Vector2(segmentEnd).sub(segmentStart).nor();

            // Projection of current position on segment
            Vector2 toStart = new Vector2(carPosition).sub(segmentStart);
            float projection = toStart.dot(segmentDir);

            // New position is projection + movement along segment
            float moveDistance = speed * delta;
            float newProjection = projection + moveDistance;
            float segmentLength = segmentStart.dst(segmentEnd);

            if (newProjection <= segmentLength) {
                // Stay on this segment
                carPosition.set(segmentStart).add(segmentDir.scl(newProjection));
            } else {
                // Move to next segment
                float remaining = newProjection - segmentLength;

                if (segmentIndex + 2 < finalRoutePoints.size()) {
                    // There are more segments
                    Vector2 nextSegmentStart = segmentEnd;
                    Vector2 nextSegmentEnd = finalRoutePoints.get(segmentIndex + 2);
                    Vector2 nextSegmentDir = new Vector2(nextSegmentEnd).sub(nextSegmentStart).nor();

                    carPosition.set(nextSegmentStart).add(nextSegmentDir.scl(remaining));
                } else {
                    // This is the last segment, go towards target
                    Vector2 toTarget = new Vector2(navigationTarget).sub(segmentEnd).nor();
                    carPosition.set(segmentEnd).add(toTarget.scl(remaining));
                }
            }
        }

        // Update traveled route
        updateTraveledRoute();

        // Camera follows car
        camera.position.lerp(new Vector3(carPosition.x, carPosition.y, 0), 0.08f);
        camera.update();
    }

    /**
     * Handle arrival at destination
     */
    private void handleArrival() {
        carPosition.set(navigationTarget);
        isMovingToTarget = false;

        // TURN OFF NAVIGATION MODE WHEN CAR ARRIVES
        navigationMode = false;

        isArriving = true;
        arrivalPause = true;
        arrivalTimer = 0f;
        arrivalZoomTimer = 0f;
        arrivalZoomOriginal = camera.zoom;

        // Show info panel for the parking
        if (selectedParkingMarker != null) {
            infoPanel.show(selectedParkingMarker);
        }

        // Clear route visualization but keep flag
        finalRoutePoints = null;
        traveledRoutePoints.clear();

        Gdx.app.log("MapScreen", "Arrived at parking - Navigation mode turned OFF");
    }

    /**
     * Calculate distance from point to line segment
     */
    private float pointToSegmentDistance(Vector2 point, Vector2 segmentStart, Vector2 segmentEnd) {
        Vector2 line = new Vector2(segmentEnd).sub(segmentStart);
        float lineLength = line.len();
        line.nor();

        Vector2 v = new Vector2(point).sub(segmentStart);
        float dot = v.dot(line);

        if (dot <= 0) return point.dst(segmentStart);
        if (dot >= lineLength) return point.dst(segmentEnd);

        Vector2 projection = new Vector2(segmentStart).add(line.scl(dot));
        return point.dst(projection);
    }

    /**
     * Update traveled route points - SIMPLE VERSION
     */
    private void updateTraveledRoute() {
        if (finalRoutePoints == null) return;

        traveledRoutePoints.clear();

        // Go through all route points and add those that the car has already passed
        for (int i = 0; i < finalRoutePoints.size(); i++) {
            Vector2 routePoint = finalRoutePoints.get(i);

            // If car has passed this point (or is close to it)
            if (carPosition.dst(routePoint) < 50f) {
                traveledRoutePoints.add(new Vector2(routePoint));
            } else {
                // Add current car position as last point
                if (i > 0) {
                    traveledRoutePoints.add(new Vector2(carPosition));
                }
                break;
            }
        }
    }

    /**
     * Get progress along segment (0-1)
     */
    private float getProgressOnSegment(Vector2 point, Vector2 segmentStart, Vector2 segmentEnd) {
        Vector2 segmentVec = new Vector2(segmentEnd).sub(segmentStart);
        Vector2 pointVec = new Vector2(point).sub(segmentStart);

        float segmentLength = segmentVec.len();
        if (segmentLength == 0) return 0f;

        segmentVec.nor();
        float dot = pointVec.dot(segmentVec);

        return MathUtils.clamp(dot / segmentLength, 0f, 1f);
    }

    /**
     * Simplify route points
     */
    private List<Vector2> simplifyRoutePoints(List<Vector2> points, int maxPoints) {
        if (points.size() <= maxPoints) return points;

        List<Vector2> simplified = new ArrayList<>();

        // Always keep first and last points
        simplified.add(points.get(0));

        // Sample points evenly
        int step = points.size() / (maxPoints - 1);
        for (int i = step; i < points.size() - step; i += step) {
            simplified.add(points.get(i));
        }

        simplified.add(points.get(points.size() - 1));
        return simplified;
    }

    /**
     * Draw navigation elements
     */
    private void drawNavigation() {
        // DO NOT draw navigation if navigation mode is NOT enabled
        if (!navigationMode) {
            return;
        }

        if (shapeRenderer == null || spriteBatch == null || carPosition == null) return;

        // 1. FIRST: Draw dashed line (if no target and car is not moving)
        if (navigationTarget == null && !isMovingToTarget && cursorWorldPos != null &&
            carPosition.dst(cursorWorldPos) > 5f) {
            drawDashedLine(shapeRenderer, carPosition, cursorWorldPos);
        }

        // 2. SECOND: Draw route (if exists)
        if (!infoPanel.isVisible()) {
            // Draw complete route (future part)
            if (finalRoutePoints != null && finalRoutePoints.size() > 1) {
                drawRouteWithColors();
            }

            // Draw arrival effect only if info panel is not open
            drawParkingArrivalEffect();
        }

        // 3. THIRD: Draw flag (ABOVE route)
        if (flagPosition != null) {
            drawFlag();
        }

        // 4. FOURTH: Draw car (ABOVE everything)
        drawCar();
    }

    /**
     * Draw flag at target position
     */
    private void drawFlag() {
        // Add check if navigation mode is enabled
        if (!navigationMode) {
            return;
        }

        if (flagIconTexture == null || flagPosition == null) return;

        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();

        float size = flagSize * (1f / camera.zoom); // Scale with zoom

        // Reduce flag size slightly when info panel is open
        if (infoPanel.isVisible()) {
            size *= 0.8f;
        }

        // Draw flag with slight bounce animation if arriving
        float yOffset = 0;
        if (isArriving) {
            float t = arrivalTimer / ARRIVAL_DURATION;
            t = MathUtils.clamp(t, 0f, 1f);
            yOffset = 20f * (float)Math.sin(t * Math.PI * 2f);
        }

        spriteBatch.draw(
            flagIconTexture,
            flagPosition.x - size / 2f,
            flagPosition.y - size / 2f + yOffset,
            size,
            size
        );

        spriteBatch.end();
    }

    /**
     * Draw route with different colors for traveled and future parts
     */
    private void drawRouteWithColors() {
        // Add check if navigation mode is enabled
        if (!navigationMode) {
            return;
        }

        if (shapeRenderer == null || finalRoutePoints == null || finalRoutePoints.size() < 2) return;

        shapeRenderer.setProjectionMatrix(camera.combined);

        // First draw entire route in blue color (future part) - THINNER LINE
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(FUTURE_ROUTE_COLOR);

        for (int i = 0; i < finalRoutePoints.size() - 1; i++) {
            Vector2 start = finalRoutePoints.get(i);
            Vector2 end = finalRoutePoints.get(i + 1);
            shapeRenderer.rectLine(start, end, 4f); // Reduced from 6f to 4f
        }

        shapeRenderer.end();

        // Then draw traveled part in green color (over blue) - THINNER LINE
        if (!traveledRoutePoints.isEmpty() && traveledRoutePoints.size() > 1) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(TRAVELED_ROUTE_COLOR);

            for (int i = 0; i < traveledRoutePoints.size() - 1; i++) {
                Vector2 start = traveledRoutePoints.get(i);
                Vector2 end = traveledRoutePoints.get(i + 1);
                shapeRenderer.rectLine(start, end, 4f); // Reduced from 6f to 4f
            }

            // Add line from last point to car
            if (!traveledRoutePoints.isEmpty()) {
                Vector2 lastPoint = traveledRoutePoints.get(traveledRoutePoints.size() - 1);
                shapeRenderer.rectLine(lastPoint, carPosition, 4f); // Reduced from 6f to 4f
            }

            shapeRenderer.end();
        }
    }


    /**
     * Draw car separately for reuse - drawn LAST to be ABOVE everything
     */
    private void drawCar() {
        // Add check if navigation mode is enabled
        if (!navigationMode) {
            return;
        }

        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();

        float baseSize = 150f;
        float scale = 1f;
        float alpha = 1f;

        // Reduce car size slightly when info panel is open
        if (infoPanel.isVisible()) {
            baseSize = 120f; // Smaller car when info panel is open
            alpha = 0.9f;    // Slightly transparent
        }

        if (isArriving) {
            float t = arrivalTimer / ARRIVAL_DURATION;
            t = MathUtils.clamp(t, 0f, 1f);

            scale = 1.25f - 0.25f * Interpolation.bounceOut.apply(t);
            alpha = Interpolation.fade.apply(t);
        }

        float size = baseSize * scale;

        spriteBatch.setColor(1f, 1f, 1f, alpha);
        spriteBatch.draw(
            carIconTexture,
            carPosition.x - size / 2f,
            carPosition.y - size / 2f,
            size,
            size
        );
        spriteBatch.setColor(Color.WHITE);

        spriteBatch.end();
    }

    /**
     * Draw arrival effect for parking
     */
    private void drawParkingArrivalEffect() {
        // Add check if navigation mode is enabled
        if (!navigationMode) {
            return;
        }

        // Do not draw arrival effect when info panel is open
        if (infoPanel.isVisible()) {
            return;
        }

        if (!isArriving || originalCarPosition == null) return;

        float t = arrivalTimer / ARRIVAL_DURATION;
        t = MathUtils.clamp(t, 0f, 1f);

        float radius = 30f + 80f * t;
        float alpha = 1f - t;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0f, 1f, 0f, alpha);
        shapeRenderer.circle(carPosition.x, carPosition.y, radius);
        shapeRenderer.end();
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

    /**
     * Set initial car position at camera center
     */
    private void setupInitialCarPosition() {
        // Place car at screen center (camera) instead of map center
        carPosition = new Vector2(camera.position.x, camera.position.y);
        cursorWorldPos = new Vector2(carPosition); // Initialize cursorWorldPos

        Gdx.app.log("MapScreen", "Initial car position (camera center): " + carPosition);
    }

    /**
     * Handle marker click for navigation with route calculation
     */
    private boolean handleMarkerClickForNavigation(float screenX, float screenY) {
        // If info panel is open, DO NOT ALLOW NAVIGATION
        if (infoPanel.isVisible()) {
            Gdx.app.log("MapScreen", "Cannot start navigation while info panel is open");
            return false;
        }

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

        // Clear previous route visualization
        finalRoutePoints = null;
        traveledRoutePoints.clear();
        flagPosition = null;

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
                        // Create final route points
                        finalRoutePoints = new ArrayList<>();
                        for (Geolocation geo : routePoints) {
                            Vector2 pixelPos = MapRasterTiles.getPixelPosition(
                                geo.lat,
                                geo.lng,
                                beginTile.x,
                                beginTile.y
                            );
                            finalRoutePoints.add(pixelPos);
                        }

                        // Simplify route if too many points
                        if (finalRoutePoints.size() > 20) {
                            finalRoutePoints = simplifyRoutePoints(finalRoutePoints, 20);
                        }

                        // Add starting point (current car position)
                        finalRoutePoints.add(0, new Vector2(carPosition));

                        // Set navigation target
                        selectedParkingMarker = marker;
                        navigationTarget = MapRasterTiles.getPixelPosition(
                            marker.getPosition().lat,
                            marker.getPosition().lng,
                            beginTile.x,
                            beginTile.y
                        );

                        // Set flag at target
                        flagPosition = new Vector2(navigationTarget);

                        // Start moving immediately
                        isMovingToTarget = true;

                        Gdx.app.log("MapScreen", "Route calculated with " +
                            finalRoutePoints.size() + " points. Starting navigation.");

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

                        // Create simple route
                        finalRoutePoints = new ArrayList<>();
                        finalRoutePoints.add(new Vector2(carPosition));
                        finalRoutePoints.add(new Vector2(navigationTarget));

                        // Set flag
                        flagPosition = new Vector2(navigationTarget);

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

                    finalRoutePoints = new ArrayList<>();
                    finalRoutePoints.add(new Vector2(carPosition));
                    finalRoutePoints.add(new Vector2(navigationTarget));

                    flagPosition = new Vector2(navigationTarget);

                    // Start moving immediately
                    isMovingToTarget = true;
                    isCalculatingRoute = false;
                });
            }
        }).start();
    }

    /**
     * Convert pixel position to geolocation using tile calculations
     */
    private Geolocation getGeolocationFromPixel(Vector2 pixelPos) {
        if (beginTile == null) return null;

        double n = Math.pow(2.0, MapConstants.ZOOM);

        // MapRasterTiles.TILE_SIZE is in px
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
     * Resets navigation state to initial values.
     */
    private void resetNavigation() {
        // Reset navigation state
        selectedParkingMarker = null;
        navigationTarget = null;
        isMovingToTarget = false;
        flagPosition = null;
        finalRoutePoints = null;
        traveledRoutePoints.clear();
        // DO NOT reset cursorWorldPos - leave it where it is
        // cursorWorldPos = new Vector2(carPosition); // THIS LINE IS NOT NEEDED
        isCalculatingRoute = false;

        // TURN OFF NAVIGATION MODE
        navigationMode = false;

        // Hide info panel if it was showing the target
        infoPanel.hide();

        Gdx.app.log("MapScreen", "Navigation reset - Navigation mode turned OFF");
    }

    /**
     * Draws all parking markers on the map.
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
            // If info panel is open for this marker, still show it
            // but with slightly different style
            boolean isSelectedInInfoPanel = infoPanel.isVisible() &&
                infoPanel.getSelectedMarker() == marker;

            Vector2 pixelPos = MapRasterTiles.getPixelPosition(
                marker.getPosition().lat,
                marker.getPosition().lng,
                beginTile.x,
                beginTile.y
            );

            Texture markerTexture = getTextureForState(marker.getState());

            if (markerTexture != null) {
                float currentMarkerSize = markerSize;
                float alpha = 1f;

                // If this is marker displayed in info panel
                if (isSelectedInInfoPanel) {
                    currentMarkerSize = markerSize * 1.2f; // Larger marker
                    alpha = 0.9f; // Slightly transparent
                }

                spriteBatch.setColor(1f, 1f, 1f, alpha);
                spriteBatch.draw(
                    markerTexture,
                    pixelPos.x - currentMarkerSize / 2f,
                    pixelPos.y - currentMarkerSize / 2f,
                    currentMarkerSize,
                    currentMarkerSize
                );
                spriteBatch.setColor(Color.WHITE);
            }
        }

        spriteBatch.end();
    }

    /**
     * Draws markers using simple shapes as fallback.
     */
    private void drawMarkersWithShapes() {
        if (shapeRenderer == null) {
            return;
        }

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Marker marker : markers) {
            // If info panel is open for this marker, still show it
            boolean isSelectedInInfoPanel = infoPanel.isVisible() &&
                infoPanel.getSelectedMarker() == marker;

            Vector2 pixelPos = MapRasterTiles.getPixelPosition(
                marker.getPosition().lat,
                marker.getPosition().lng,
                beginTile.x,
                beginTile.y
            );

            Color markerColor = getColorForState(marker.getState());

            // If marker is selected in info panel, make it brighter color
            if (isSelectedInInfoPanel) {
                markerColor = new Color(
                    markerColor.r * 1.2f,
                    markerColor.g * 1.2f,
                    markerColor.b * 1.2f,
                    0.9f
                );
            }

            shapeRenderer.setColor(markerColor);

            float markerRadius = markerSize / 2f;
            if (isSelectedInInfoPanel) {
                markerRadius *= 1.2f; // Larger marker when selected
            }

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

    /**
     * Handles keyboard input for map navigation and controls.
     */
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

        // Reset navigation with R key (only if info panel is not open)
        if (Gdx.input.isKeyJustPressed(Input.Keys.R) && !infoPanel.isVisible()) {
            resetNavigation();
        }

        // T key for test - turn off navigation if info panel is open
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            if (infoPanel.isVisible() && navigationMode) {
                navigationMode = false;
                Gdx.app.log("MapScreen", "Navigation turned OFF because info panel is open");
            }
        }
    }

    /**
     * Clamps camera position to stay within map bounds.
     */
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

    /**
     * Cleans up resources when screen is disposed.
     */
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
        if (flagIconTexture != null) {
            flagIconTexture.dispose();
        }
        if (simulationButtonTexture != null) {
            simulationButtonTexture.dispose();
        }
        if (simulationButtonActiveTexture != null && simulationButtonActiveTexture != simulationButtonTexture) {
            simulationButtonActiveTexture.dispose();
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
