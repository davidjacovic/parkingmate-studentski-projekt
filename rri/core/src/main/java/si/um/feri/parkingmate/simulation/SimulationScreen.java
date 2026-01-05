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
import si.um.feri.parkingmate.util.FontManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private float clockX, clockY;
    private float clockWidth = 200f;
    private float clockHeight = 50f;

    private Texture markerFreeTexture;
    private Texture markerPartialTexture;
    private Texture markerFullTexture;
    private Texture markerUnknownTexture;

    private Texture closeButtonTexture;
    private float closeButtonSize = 48f;
    private float closeButtonMargin = 15f;
    private float closeButtonX, closeButtonY;
    private float markerSize = 128f;

    private BitmapFont font;
    private ParkingService parkingService;

    private float simulationTime = 8.0f * 60f;
    private float timeSpeedMultiplier = 1f;
    private boolean isClockPaused = false;
    private Texture clockIconTexture;
    private float clockUpdateTimer = 0f;
    private static final float CLOCK_UPDATE_INTERVAL = 0.1f;

    private static final String API_BASE_URL = "http://localhost:3002";

    private Texture slowButtonTexture;
    private Texture normalButtonTexture;
    private Texture fastButtonTexture;
    private float speedButtonSize = 48f;
    private float speedButtonMargin = 15f;
    private float speedButtonX, speedButtonY;
    private float speedButtonSpacing = 55f;

    public SimulationScreen(ParkingMate game) {
        this.game = game;
        this.markers = new ArrayList<>();
        this.parkingService = new ParkingService(API_BASE_URL);
    }

    @Override
    public void show() {
        initializeSimulationMap();
        initializeSimulationClock();
        initializeSpeedButtons();
    }

    private void initializeSimulationMap() {
        if (Keys.GEOAPIFY == null || Keys.GEOAPIFY.isEmpty()) {
            Gdx.app.error("SimulationScreen", "Geoapify API key is not set!");
            setupCamera();
            return;
        }

        try {
            ZoomXY centerTile = MapRasterTiles.getTileNumber(
                MapConstants.CENTER_GEOLOCATION.lat,
                MapConstants.CENTER_GEOLOCATION.lng,
                MapConstants.ZOOM
            );

            mapTiles = MapRasterTiles.getRasterTileZone(centerTile, MapConstants.NUM_TILES);

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

        tiledMap = new TiledMap();
        MapLayers layers = tiledMap.getLayers();

        TiledMapTileLayer layer = new TiledMapTileLayer(
            MapConstants.NUM_TILES,
            MapConstants.NUM_TILES,
            MapRasterTiles.TILE_SIZE,
            MapRasterTiles.TILE_SIZE
        );

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

        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);

        setupCamera();

        setupInputHandlers();

        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();

        loadFont();

        loadMarkerTextures();

        initializeCloseButton();

        loadParkingLocationsFromAPI();
    }

    private void initializeSimulationClock() {
        Gdx.app.log("SimulationScreen", "=== INITIALIZING SIMULATION CLOCK ===");

        try {
            clockIconTexture = new Texture(Gdx.files.internal("ui/clock_icon.png"));
        } catch (Exception e) {
            createDefaultClockIcon();
        }
        updateClockPosition();
    }

    private void initializeSpeedButtons() {
        Gdx.app.log("SimulationScreen", "=== INITIALIZING SPEED BUTTONS ===");

        try {
            slowButtonTexture = new Texture(Gdx.files.internal("ui/slow_speed_icon.png"));
        } catch (Exception e) {
            createDefaultSlowButton();
        }

        try {
            normalButtonTexture = new Texture(Gdx.files.internal("ui/normal_speed_icon.png"));
        } catch (Exception e) {
            createDefaultNormalButton();
        }

        try {
            fastButtonTexture = new Texture(Gdx.files.internal("ui/fast_speed_icon.png"));
        } catch (Exception e) {
            createDefaultFastButton();
        }

        updateSpeedButtonsPosition();
    }

    private void createDefaultSlowButton() {
        Pixmap pixmap = new Pixmap((int)speedButtonSize, (int)speedButtonSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.2f, 0.6f, 0.2f, 1f);
        pixmap.fillCircle((int)speedButtonSize/2, (int)speedButtonSize/2, (int)speedButtonSize/2 - 5);
        pixmap.setColor(Color.WHITE);
        pixmap.drawLine((int)(speedButtonSize*0.3), (int)(speedButtonSize*0.5), (int)(speedButtonSize*0.7), (int)(speedButtonSize*0.5));
        pixmap.drawLine((int)(speedButtonSize*0.3), (int)(speedButtonSize*0.6), (int)(speedButtonSize*0.7), (int)(speedButtonSize*0.6));
        pixmap.drawLine((int)(speedButtonSize*0.5), (int)(speedButtonSize*0.35), (int)(speedButtonSize*0.5), (int)(speedButtonSize*0.65));
        slowButtonTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createDefaultNormalButton() {
        Pixmap pixmap = new Pixmap((int)speedButtonSize, (int)speedButtonSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.2f, 0.4f, 0.8f, 1f);
        pixmap.fillCircle((int)speedButtonSize/2, (int)speedButtonSize/2, (int)speedButtonSize/2 - 5);
        pixmap.setColor(Color.WHITE);
        int triangleSize = (int)(speedButtonSize * 0.4);
        int[] xPoints = {(int)(speedButtonSize*0.4), (int)(speedButtonSize*0.4), (int)(speedButtonSize*0.7)};
        int[] yPoints = {(int)(speedButtonSize*0.35), (int)(speedButtonSize*0.65), (int)(speedButtonSize*0.5)};
        pixmap.fillTriangle(xPoints[0], yPoints[0], xPoints[1], yPoints[1], xPoints[2], yPoints[2]);
        normalButtonTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createDefaultFastButton() {
        Pixmap pixmap = new Pixmap((int)speedButtonSize, (int)speedButtonSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.8f, 0.2f, 0.2f, 1f);
        pixmap.fillCircle((int)speedButtonSize/2, (int)speedButtonSize/2, (int)speedButtonSize/2 - 5);
        pixmap.setColor(Color.WHITE);
        int arrowSize = (int)(speedButtonSize * 0.3);
        int[] xPoints1 = {(int)(speedButtonSize*0.35), (int)(speedButtonSize*0.35), (int)(speedButtonSize*0.55)};
        int[] yPoints1 = {(int)(speedButtonSize*0.35), (int)(speedButtonSize*0.65), (int)(speedButtonSize*0.5)};
        pixmap.fillTriangle(xPoints1[0], yPoints1[0], xPoints1[1], yPoints1[1], xPoints1[2], yPoints1[2]);
        int[] xPoints2 = {(int)(speedButtonSize*0.55), (int)(speedButtonSize*0.55), (int)(speedButtonSize*0.75)};
        int[] yPoints2 = {(int)(speedButtonSize*0.35), (int)(speedButtonSize*0.65), (int)(speedButtonSize*0.5)};
        pixmap.fillTriangle(xPoints2[0], yPoints2[0], xPoints2[1], yPoints2[1], xPoints2[2], yPoints2[2]);
        fastButtonTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void updateClockPosition() {
        clockX = closeButtonMargin;
        clockY = Gdx.graphics.getHeight() - clockHeight - closeButtonMargin - 70f;
    }

    private void updateCloseButtonPosition() {
        closeButtonX = Gdx.graphics.getWidth() - closeButtonSize - closeButtonMargin;
        closeButtonY = Gdx.graphics.getHeight() - closeButtonSize - closeButtonMargin;
    }

    private void updateSpeedButtonsPosition() {
        speedButtonX = closeButtonMargin;
        speedButtonY = Gdx.graphics.getHeight() - speedButtonSize - closeButtonMargin - 120f;
    }

    private void createDefaultClockIcon() {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 0.8f, 0f, 1f);
        pixmap.fillCircle(32, 32, 30);
        pixmap.setColor(0f, 0f, 0f, 1f);
        pixmap.drawCircle(32, 32, 30);
        pixmap.setColor(0f, 0f, 0f, 1f);
        pixmap.fillCircle(32, 32, 4);
        pixmap.drawLine(32, 32, 32, 18);
        pixmap.drawLine(32, 32, 44, 32);
        clockIconTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private String formatSimulationTime(float totalMinutes) {
        int hours = ((int) totalMinutes / 60) % 24;
        int minutes = (int) totalMinutes % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    private void updateSimulationTime(float delta) {
        if (isClockPaused) return;
        clockUpdateTimer += delta;
        if (clockUpdateTimer >= CLOCK_UPDATE_INTERVAL) {
            simulationTime += (timeSpeedMultiplier * CLOCK_UPDATE_INTERVAL * 60f);
            if (simulationTime >= 24 * 60f) {
                simulationTime -= 24 * 60f;
            }
            clockUpdateTimer = 0f;
        }
    }

    private void drawSimulationClock() {
        if (spriteBatch == null || font == null) return;
        updateClockPosition();
        spriteBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        spriteBatch.begin();
        if (clockIconTexture != null) {
            spriteBatch.draw(clockIconTexture, clockX + 5f, clockY + 1f, 40f, 40f);
        }
        String timeText = formatSimulationTime(simulationTime);
        String displayText = "🕒 " + timeText;
        font.setColor(Color.WHITE);
        font.draw(spriteBatch, displayText, clockX + 50f, clockY + 30f);
        spriteBatch.end();
    }

    private void drawSpeedButtons() {
        if (spriteBatch == null) return;
        updateSpeedButtonsPosition();
        spriteBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        spriteBatch.begin();

        float currentX = speedButtonX;

        if (slowButtonTexture != null) {
            spriteBatch.draw(slowButtonTexture, currentX, speedButtonY, speedButtonSize, speedButtonSize);
            currentX += speedButtonSpacing;
        }

        if (normalButtonTexture != null) {
            spriteBatch.draw(normalButtonTexture, currentX, speedButtonY, speedButtonSize, speedButtonSize);
            currentX += speedButtonSpacing;
        }

        if (fastButtonTexture != null) {
            spriteBatch.draw(fastButtonTexture, currentX, speedButtonY, speedButtonSize, speedButtonSize);
        }

        spriteBatch.end();
    }

    private void initializeCloseButton() {
        Gdx.app.log("SimulationScreen", "=== INITIALIZING CLOSE BUTTON ===");
        try {
            closeButtonTexture = new Texture(Gdx.files.internal("ui/close_button.png"));
        } catch (Exception e) {
            createDefaultCloseButton();
        }
        updateCloseButtonPosition();
        Gdx.app.log("SimulationScreen", "Close button at: " + closeButtonX + ", " + closeButtonY);
    }

    private void createDefaultCloseButton() {
        Pixmap pixmap = new Pixmap((int)closeButtonSize, (int)closeButtonSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(1f, 0.2f, 0.2f, 1f);
        pixmap.fillRectangle(0, 0, (int)closeButtonSize, (int)closeButtonSize);
        pixmap.setColor(Color.WHITE);
        int padding = (int)(closeButtonSize * 0.25f);
        int thickness = (int)(closeButtonSize * 0.1f);
        for (int i = 0; i < thickness; i++) {
            pixmap.drawLine(padding + i, padding + i, (int)closeButtonSize - padding + i, (int)closeButtonSize - padding + i);
        }
        for (int i = 0; i < thickness; i++) {
            pixmap.drawLine((int)closeButtonSize - padding + i, padding + i, padding + i, (int)closeButtonSize - padding + i);
        }
        closeButtonTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void loadFont() {
        font = FontManager.getFont(32);
    }

    private void loadMarkerTextures() {
        try {
            markerFreeTexture = new Texture(Gdx.files.internal("markers/marker_free.png"));
        } catch (Exception e) {
            markerFreeTexture = null;
        }
        try {
            markerPartialTexture = new Texture(Gdx.files.internal("markers/marker_partial.png"));
        } catch (Exception e) {
            markerPartialTexture = null;
        }
        try {
            markerFullTexture = new Texture(Gdx.files.internal("markers/marker_full.png"));
        } catch (Exception e) {
            markerFullTexture = null;
        }
        try {
            markerUnknownTexture = new Texture(Gdx.files.internal("markers/marker_unknown.png"));
        } catch (Exception e) {
            markerUnknownTexture = null;
        }
    }

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
                Gdx.app.postRunnable(() -> {
                    initializeTestMarkers();
                });
            }
        }).start();
    }

    private Marker convertParkingToMarker(Parking parking) {
        if (parking == null || parking.getLocation() == null) {
            return null;
        }
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
        float pricePerHour = parking.getPricePerHour();
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

    private void setupCamera() {
        camera.setToOrtho(false, MapConstants.MAP_WIDTH, MapConstants.MAP_HEIGHT);
        camera.position.set(MapConstants.MAP_WIDTH / 2f, MapConstants.MAP_HEIGHT / 2f, 0);
        camera.viewportWidth = MapConstants.MAP_WIDTH / 2f;
        camera.viewportHeight = MapConstants.MAP_HEIGHT / 2f;
        camera.zoom = 2f;
        camera.update();
    }

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

                if (isCloseButtonClicked(screenX, gdxY)) {
                    Gdx.app.log("SimulationScreen", "Close button clicked - returning to map");
                    returnToMapScreen();
                    return true;
                }

                if (isClockAreaClicked(screenX, gdxY)) {
                    isClockPaused = !isClockPaused;
                    Gdx.app.log("SimulationScreen", "Clock " + (isClockPaused ? "PAUSED" : "RESUMED") + " via click");
                    return true;
                }

                if (isSlowButtonClicked(screenX, gdxY)) {
                    timeSpeedMultiplier = 0.25f;
                    Gdx.app.log("SimulationScreen", "Speed set to SLOW (0.25x)");
                    return true;
                }

                if (isNormalButtonClicked(screenX, gdxY)) {
                    timeSpeedMultiplier = 1f;
                    Gdx.app.log("SimulationScreen", "Speed set to NORMAL (1x)");
                    return true;
                }

                if (isFastButtonClicked(screenX, gdxY)) {
                    timeSpeedMultiplier = 4f;
                    Gdx.app.log("SimulationScreen", "Speed set to FAST (4x)");
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

    private boolean isClockAreaClicked(float screenX, float screenY) {
        updateClockPosition();
        return screenX >= clockX && screenX <= clockX + clockWidth && screenY >= clockY && screenY <= clockY + clockHeight;
    }

    private boolean isCloseButtonClicked(float screenX, float screenY) {
        updateCloseButtonPosition();
        return screenX >= closeButtonX && screenX <= closeButtonX + closeButtonSize && screenY >= closeButtonY && screenY <= closeButtonY + closeButtonSize;
    }

    private boolean isSlowButtonClicked(float screenX, float screenY) {
        updateSpeedButtonsPosition();
        float buttonX = speedButtonX;
        return screenX >= buttonX && screenX <= buttonX + speedButtonSize && screenY >= speedButtonY && screenY <= speedButtonY + speedButtonSize;
    }

    private boolean isNormalButtonClicked(float screenX, float screenY) {
        updateSpeedButtonsPosition();
        float buttonX = speedButtonX + speedButtonSpacing;
        return screenX >= buttonX && screenX <= buttonX + speedButtonSize && screenY >= speedButtonY && screenY <= speedButtonY + speedButtonSize;
    }

    private boolean isFastButtonClicked(float screenX, float screenY) {
        updateSpeedButtonsPosition();
        float buttonX = speedButtonX + speedButtonSpacing * 2;
        return screenX >= buttonX && screenX <= buttonX + speedButtonSize && screenY >= speedButtonY && screenY <= speedButtonY + speedButtonSize;
    }

    private void returnToMapScreen() {
        Gdx.app.log("SimulationScreen", "Returning to MapScreen");
        game.setScreen(new MapScreen(game));
    }

    @Override
    public void render(float delta) {
        handleKeyboardInput();
        updateSimulationTime(delta);
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (tiledMapRenderer != null && tiledMap != null) {
            camera.zoom = MathUtils.clamp(camera.zoom, MapConstants.MIN_ZOOM, MapConstants.MAX_ZOOM);
            clampCameraPosition();
            camera.update();
            tiledMapRenderer.setView(camera);
            tiledMapRenderer.render();
            drawMarkers();
            drawUI();
            drawSimulationClock();
            drawSpeedButtons();
        }
    }

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
                spriteBatch.draw(markerTexture, pixelPos.x - markerSize / 2f, pixelPos.y - markerSize / 2f, markerSize, markerSize);
            }
        }
        spriteBatch.end();
    }

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

    private void drawUI() {
        drawCloseButton();
    }

    private void drawCloseButton() {
        if (spriteBatch == null || closeButtonTexture == null) return;
        updateCloseButtonPosition();
        spriteBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        spriteBatch.begin();
        spriteBatch.draw(closeButtonTexture, closeButtonX, closeButtonY, closeButtonSize, closeButtonSize);
        spriteBatch.end();
    }

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

    private void applyTimeBasedBehavior() {
        DayPhase phase = SimulationTimeMapper.getDayPhase(simulationTime);

        for (Marker marker : markers) {
            int available = marker.getAvailableSpots();
            int total = marker.getTotalSpots();

            if (total == 0) continue;

            switch (phase) {
                case MORNING:
                    available -= 1; // dolasci
                    break;
                case DAY:
                    // skoro stabilno
                    available += MathUtils.random(-1, 1);
                    break;
                case NIGHT:
                    available += 1; // odlasci
                    break;
            }

            available = MathUtils.clamp(available, 0, total);
            marker.setAvailableSpots(available);
        }
    }


    private void handleKeyboardInput() {
        if (Gdx.input.isKeyPressed(Input.Keys.Q) || Gdx.input.isKeyPressed(Input.Keys.PLUS)) {
            camera.zoom -= 0.02f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.MINUS)) {
            camera.zoom += 0.02f;
        }
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            returnToMapScreen();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            isClockPaused = !isClockPaused;
            Gdx.app.log("SimulationScreen", "Clock " + (isClockPaused ? "PAUSED" : "RESUMED"));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_ADD) || Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)) {
            timeSpeedMultiplier = Math.min(timeSpeedMultiplier * 2f, 100f);
            Gdx.app.log("SimulationScreen", "Time speed: " + timeSpeedMultiplier + "x");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_SUBTRACT) || Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
            timeSpeedMultiplier = Math.max(timeSpeedMultiplier / 2f, 0.25f);
            Gdx.app.log("SimulationScreen", "Time speed: " + timeSpeedMultiplier + "x");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            simulationTime = 8.0f * 60f;
            Gdx.app.log("SimulationScreen", "Time reset to 08:00");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            timeSpeedMultiplier = 0.25f;
            Gdx.app.log("SimulationScreen", "Speed set to SLOW (0.25x)");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            timeSpeedMultiplier = 1f;
            Gdx.app.log("SimulationScreen", "Speed set to NORMAL (1x)");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            timeSpeedMultiplier = 4f;
            Gdx.app.log("SimulationScreen", "Speed set to FAST (4x)");
        }
    }

    private void clampCameraPosition() {
        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;
        if (effectiveViewportWidth < MapConstants.MAP_WIDTH) {
            camera.position.x = MathUtils.clamp(camera.position.x, effectiveViewportWidth / 2f, MapConstants.MAP_WIDTH - effectiveViewportWidth / 2f);
        } else {
            camera.position.x = MapConstants.MAP_WIDTH / 2f;
        }
        if (effectiveViewportHeight < MapConstants.MAP_HEIGHT) {
            camera.position.y = MathUtils.clamp(camera.position.y, effectiveViewportHeight / 2f, MapConstants.MAP_HEIGHT - effectiveViewportHeight / 2f);
        } else {
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
        if (closeButtonTexture != null) {
            closeButtonTexture.dispose();
        }
        if (clockIconTexture != null) {
            clockIconTexture.dispose();
        }
        if (slowButtonTexture != null) {
            slowButtonTexture.dispose();
        }
        if (normalButtonTexture != null) {
            normalButtonTexture.dispose();
        }
        if (fastButtonTexture != null) {
            fastButtonTexture.dispose();
        }
    }

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

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        updateClockPosition();
        updateCloseButtonPosition();
        updateSpeedButtonsPosition();
        Gdx.app.log("SimulationScreen", "Screen resized to: " + width + "x" + height);
    }
}
