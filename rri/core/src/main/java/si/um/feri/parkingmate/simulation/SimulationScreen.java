package si.um.feri.parkingmate.simulation;

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
import si.um.feri.parkingmate.screens.BaseScreen;
import si.um.feri.parkingmate.screens.MapScreen;
import si.um.feri.parkingmate.ui.InfoPanel;
import si.um.feri.parkingmate.util.FontManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulation screen displaying parking locations for simulation purposes.
 */
public class SimulationScreen extends BaseScreen {

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

    // CLOSE BUTTON
    private Texture closeButtonTexture;
    private float closeButtonSize = 48f;
    private float closeButtonMargin = 15f;
    private float closeButtonX, closeButtonY;

    // Marker size configuration (in pixels)
    private float markerSize = 128f;

    // Font for text
    private BitmapFont font;

    // API service for fetching parking data
    private ParkingService parkingService;

    // Backend API base URL
    private static final String API_BASE_URL = "http://localhost:3002";

    // Title
    private String screenTitle = "PARKING SIMULATION";

    /**
     * Constructor for SimulationScreen.
     * @param game The main game instance.
     */
    public SimulationScreen(ParkingMate game) {
        this.game = game;
        this.markers = new ArrayList<>();
        this.parkingService = new ParkingService(API_BASE_URL);
    }

    /**
     * Called when this screen becomes the current screen.
     */
    @Override
    public void show() {
        initializeSimulationMap();
    }

    /**
     * Initializes the simulation map with tiles and markers.
     */
    private void initializeSimulationMap() {
        // Check if API key is set
        if (Keys.GEOAPIFY == null || Keys.GEOAPIFY.isEmpty()) {
            Gdx.app.error("SimulationScreen", "Geoapify API key is not set!");
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

            // Fetch tiles for the area
            mapTiles = MapRasterTiles.getRasterTileZone(centerTile, MapConstants.NUM_TILES);

            // Calculate beginning tile (top left corner)
            beginTile = new ZoomXY(
                MapConstants.ZOOM,
                centerTile.x - ((MapConstants.NUM_TILES - 1) / 2),
                centerTile.y - ((MapConstants.NUM_TILES - 1) / 2)
            );
        } catch (IOException e) {
            e.printStackTrace();
            Gdx.app.error("SimulationScreen", "Failed to load map tiles", e);
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

        // Setup input handlers
        setupInputHandlers();

        // Initialize renderers
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();

        // Load font
        loadFont();

        // Load marker textures
        loadMarkerTextures();

        // Initialize close button
        initializeCloseButton();

        // Load parking locations from API
        loadParkingLocationsFromAPI();
    }

    /**
     * Initialize close button.
     */
    private void initializeCloseButton() {
        Gdx.app.log("SimulationScreen", "=== INITIALIZING CLOSE BUTTON ===");

        try {
            closeButtonTexture = new Texture(Gdx.files.internal("ui/close_button.png"));
        } catch (Exception e) {
            createDefaultCloseButton();
        }

        // Position at top-right corner
        closeButtonX = Gdx.graphics.getWidth() - closeButtonSize - closeButtonMargin;
        closeButtonY = Gdx.graphics.getHeight() - closeButtonSize - closeButtonMargin;

        Gdx.app.log("SimulationScreen", "Close button at: " + closeButtonX + ", " + closeButtonY);
    }

    /**
     * Create default close button (red X).
     */
    private void createDefaultCloseButton() {
        Pixmap pixmap = new Pixmap((int)closeButtonSize, (int)closeButtonSize, Pixmap.Format.RGBA8888);

        // Red background
        pixmap.setColor(0.8f, 0.2f, 0.2f, 1f);
        pixmap.fillRectangle(0, 0, (int)closeButtonSize, (int)closeButtonSize);

        // White X symbol
        pixmap.setColor(Color.WHITE);
        int padding = (int)(closeButtonSize * 0.25f);
        int thickness = (int)(closeButtonSize * 0.1f);

        // First diagonal
        for (int i = 0; i < thickness; i++) {
            pixmap.drawLine(
                padding + i, padding + i,
                (int)closeButtonSize - padding + i, (int)closeButtonSize - padding + i
            );
        }

        // Second diagonal
        for (int i = 0; i < thickness; i++) {
            pixmap.drawLine(
                (int)closeButtonSize - padding + i, padding + i,
                padding + i, (int)closeButtonSize - padding + i
            );
        }

        closeButtonTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    /**
     * Loads font for text rendering.
     */
    private void loadFont() {
        font = FontManager.getFont(24);
    }

    /**
     * Loads marker textures from assets folder.
     */
    private void loadMarkerTextures() {
        try {
            markerFreeTexture = new Texture(Gdx.files.internal("markers/marker_free.png"));
        } catch (Exception e) {
            Gdx.app.debug("SimulationScreen", "Marker texture not found: markers/marker_free.png");
            markerFreeTexture = null;
        }

        try {
            markerPartialTexture = new Texture(Gdx.files.internal("markers/marker_partial.png"));
        } catch (Exception e) {
            Gdx.app.debug("SimulationScreen", "Marker texture not found: markers/marker_partial.png");
            markerPartialTexture = null;
        }

        try {
            markerFullTexture = new Texture(Gdx.files.internal("markers/marker_full.png"));
        } catch (Exception e) {
            Gdx.app.debug("SimulationScreen", "Marker texture not found: markers/marker_full.png");
            markerFullTexture = null;
        }

        try {
            markerUnknownTexture = new Texture(Gdx.files.internal("markers/marker_unknown.png"));
        } catch (Exception e) {
            Gdx.app.debug("SimulationScreen", "Marker texture not found: markers/marker_unknown.png");
            markerUnknownTexture = null;
        }
    }

    /**
     * Loads parking locations from the API.
     */
    private void loadParkingLocationsFromAPI() {
        new Thread(() -> {
            try {
                Gdx.app.log("SimulationScreen", "Fetching parking locations for simulation...");

                List<JSONObject> jsonLocations = parkingService.fetchParkingLocationsWithTariffs();
                List<Parking> parkingList = ParkingMapper.mapToParkingListWithTariffs(jsonLocations);

                Gdx.app.log("SimulationScreen", "Loaded " + parkingList.size() + " parking locations");

                final List<Parking> finalParkingList = parkingList;
                Gdx.app.postRunnable(() -> {
                    markers.clear();
                    for (Parking parking : finalParkingList) {
                        Marker marker = convertParkingToMarker(parking);
                        if (marker != null) {
                            markers.add(marker);
                        }
                    }
                    Gdx.app.log("SimulationScreen", "Added " + markers.size() + " markers to simulation map");
                });

            } catch (Exception e) {
                Gdx.app.error("SimulationScreen", "Failed to load parking locations", e);

                // Fallback to test markers
                Gdx.app.postRunnable(() -> {
                    initializeTestMarkers();
                });
            }
        }).start();
    }

    /**
     * Converts a Parking model to a Marker model.
     */
    private Marker convertParkingToMarker(Parking parking) {
        if (parking == null || parking.getLocation() == null) {
            return null;
        }

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
            Marker.MarkerType.PARKING_LOT,
            markerState,
            parking.getId(),
            parking.getName() != null ? parking.getName() : "Unknown",
            totalSpots,
            availableSpots,
            pricePerHour
        );

        return marker;
    }

    /**
     * Initialize test markers for demonstration.
     */
    private void initializeTestMarkers() {
        markers.add(new Marker(
            new Geolocation(46.0569, 14.5058),
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
    }

    /**
     * Sets up the camera.
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
     * Sets up input handlers for gestures and clicks.
     */
    private void setupInputHandlers() {
        gestureDetector = new GestureDetector(new SimulationGestureListener());

        InputAdapter inputAdapter = new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                float zoomSpeed = 0.1f;
                camera.zoom += amountY * zoomSpeed;
                camera.zoom = MathUtils.clamp(camera.zoom, MapConstants.MIN_ZOOM, MapConstants.MAX_ZOOM);
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                float gdxY = Gdx.graphics.getHeight() - screenY;

                // CHECK CLICK ON CLOSE BUTTON
                if (isCloseButtonClicked(screenX, gdxY)) {
                    Gdx.app.log("SimulationScreen", "Close button clicked - returning to map");
                    returnToMapScreen();
                    return true;
                }

                return false;
            }
        };

        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(inputAdapter);
        inputMultiplexer.addProcessor(gestureDetector);
        Gdx.input.setInputProcessor(inputMultiplexer);
    }

    /**
     * Checks if the close button was clicked.
     */
    private boolean isCloseButtonClicked(float screenX, float screenY) {
        return screenX >= closeButtonX &&
            screenX <= closeButtonX + closeButtonSize &&
            screenY >= closeButtonY &&
            screenY <= closeButtonY + closeButtonSize;
    }

    /**
     * Returns to the main map screen.
     */
    private void returnToMapScreen() {
        Gdx.app.log("SimulationScreen", "Returning to MapScreen");
        game.setScreen(new MapScreen(game));
    }

    /**
     * Main render method.
     */
    @Override
    public void render(float delta) {
        handleKeyboardInput();

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (tiledMapRenderer != null && tiledMap != null) {
            // Clamp zoom
            camera.zoom = MathUtils.clamp(camera.zoom, MapConstants.MIN_ZOOM, MapConstants.MAX_ZOOM);

            // Clamp camera position
            clampCameraPosition();

            camera.update();
            tiledMapRenderer.setView(camera);
            tiledMapRenderer.render();

            // Draw markers
            drawMarkers();

            // Draw UI elements
            drawUI();
        }
    }

    /**
     * Draws all parking markers on the map.
     */
    private void drawMarkers() {
        if (beginTile == null || markers == null) {
            return;
        }

        boolean useTextures = markerFreeTexture != null || markerPartialTexture != null
            || markerFullTexture != null || markerUnknownTexture != null;

        if (useTextures) {
            drawMarkersWithTextures();
        } else {
            drawMarkersWithShapes();
        }
    }

    /**
     * Draws markers using textures.
     */
    private void drawMarkersWithTextures() {
        spriteBatch.setProjectionMatrix(camera.combined);
        spriteBatch.begin();

        for (Marker marker : markers) {
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
     * Draws markers using shapes.
     */
    private void drawMarkersWithShapes() {
        if (shapeRenderer == null) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Marker marker : markers) {
            Vector2 pixelPos = MapRasterTiles.getPixelPosition(
                marker.getPosition().lat,
                marker.getPosition().lng,
                beginTile.x,
                beginTile.y
            );

            Color markerColor = getColorForState(marker.getState());
            shapeRenderer.setColor(markerColor);
            shapeRenderer.circle(pixelPos.x, pixelPos.y, markerSize / 2f);
        }

        shapeRenderer.end();
    }

    /**
     * Draws UI elements (close button and title).
     */
    private void drawUI() {
        // Draw close button
        drawCloseButton();

        // Draw title
        drawTitle();
    }

    /**
     * Draws the close button.
     */
    private void drawCloseButton() {
        if (spriteBatch == null || closeButtonTexture == null) return;

        spriteBatch.setProjectionMatrix(
            new Matrix4().setToOrtho2D(
                0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
            )
        );

        spriteBatch.begin();
        spriteBatch.draw(
            closeButtonTexture,
            closeButtonX, closeButtonY,
            closeButtonSize, closeButtonSize
        );
        spriteBatch.end();
    }

    /**
     * Draws the screen title.
     */
    private void drawTitle() {
        if (font == null || spriteBatch == null) return;

        spriteBatch.setProjectionMatrix(
            new Matrix4().setToOrtho2D(
                0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
            )
        );

        spriteBatch.begin();

        // Draw title at top-center
        float titleX = Gdx.graphics.getWidth() / 2f;
        float titleY = Gdx.graphics.getHeight() - closeButtonMargin - 20f;

        // Draw background for title
        shapeRenderer.setProjectionMatrix(spriteBatch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        float textWidth = 300f;
        float textHeight = 40f;
        shapeRenderer.rect(
            titleX - textWidth / 2f - 10f,
            titleY - textHeight / 2f - 5f,
            textWidth + 20f,
            textHeight + 10f
        );
        shapeRenderer.end();

        // Draw title text
        font.setColor(Color.YELLOW);
        font.draw(spriteBatch, screenTitle,
            titleX - textWidth / 2f,
            titleY + textHeight / 2f);
        font.setColor(Color.WHITE);

        // Draw instruction
        String instruction = "Click X to exit simulation";
        float instX = closeButtonMargin;
        float instY = closeButtonMargin + 20f;
        font.setColor(Color.LIGHT_GRAY);
        font.draw(spriteBatch, instruction, instX, instY);

        spriteBatch.end();
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
     * Handles keyboard input.
     */
    private void handleKeyboardInput() {
        // Zoom controls
        if (Gdx.input.isKeyPressed(Input.Keys.Q) || Gdx.input.isKeyPressed(Input.Keys.PLUS)) {
            camera.zoom -= 0.02f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            camera.zoom += 0.02f;
        }

        // Pan controls
        float panSpeed = 3f * camera.zoom;
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

        // ESC key to exit
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            returnToMapScreen();
        }
    }

    /**
     * Clamps camera position to stay within map bounds.
     */
    private void clampCameraPosition() {
        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;

        if (effectiveViewportWidth < MapConstants.MAP_WIDTH) {
            camera.position.x = MathUtils.clamp(
                camera.position.x,
                effectiveViewportWidth / 2f,
                MapConstants.MAP_WIDTH - effectiveViewportWidth / 2f
            );
        } else {
            camera.position.x = MapConstants.MAP_WIDTH / 2f;
        }

        if (effectiveViewportHeight < MapConstants.MAP_HEIGHT) {
            camera.position.y = MathUtils.clamp(
                camera.position.y,
                effectiveViewportHeight / 2f,
                MapConstants.MAP_HEIGHT - effectiveViewportHeight / 2f
            );
        } else {
            camera.position.y = MapConstants.MAP_HEIGHT / 2f;
        }
    }

    /**
     * Cleans up resources.
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
        if (closeButtonTexture != null) {
            closeButtonTexture.dispose();
        }
    }

    /**
     * Gesture listener for map interactions.
     */
    private class SimulationGestureListener implements GestureDetector.GestureListener {

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
            camera.translate(-deltaX * camera.zoom, deltaY * camera.zoom);
            return true;
        }

        @Override
        public boolean panStop(float x, float y, int pointer, int button) {
            return false;
        }

        @Override
        public boolean zoom(float initialDistance, float distance) {
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
        }
    }
}
