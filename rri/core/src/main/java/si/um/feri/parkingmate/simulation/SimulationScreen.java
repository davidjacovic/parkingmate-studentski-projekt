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
    private Texture concertButtonTexture;
    private Texture restartButtonTexture;
    private float crowdButtonSize = 48f;
    private float crowdButtonX, crowdButtonY;
    private float crowdButtonSpacing = 55f;
    private Texture restartSimulationTexture;
    private float restartSimulationSize = 48f;
    private float restartSimulationX, restartSimulationY;

    private float originalSimulationTime = 8.0f * 60f;
    private List<Marker> originalMarkersState = new ArrayList<>();

    private boolean isCrowdSimulationActive = false;
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

    private Texture playButtonTexture;
    private Texture pauseButtonTexture;
    private float playButtonSize = 48f;
    private float playButtonX, playButtonY;

    private boolean isSimulationRunning = false;
    private float simulationUpdateTimer = 0f;
    private static final float SIMULATION_UPDATE_INTERVAL = 0.5f;
    private static final float SLOW_CLOCK_UPDATE_INTERVAL = 0.2f;


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
        initializeSimulationButton();
        initializeRestartButton();
    }
    private void drawSimulationButton() {
        if (spriteBatch == null) return;

        updatePlayButtonPosition();
        spriteBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        spriteBatch.begin();

        if (isSimulationRunning) {
            if (pauseButtonTexture != null) {
                spriteBatch.draw(pauseButtonTexture, playButtonX, playButtonY, playButtonSize, playButtonSize);
            }
        } else {
            if (playButtonTexture != null) {
                spriteBatch.draw(playButtonTexture, playButtonX, playButtonY, playButtonSize, playButtonSize);
            }
        }

        spriteBatch.end();
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
    private void initializeRestartButton() {
        Gdx.app.log("SimulationScreen", "=== INITIALIZING RESTART BUTTON ===");

        try {
            restartSimulationTexture = new Texture(Gdx.files.internal("ui/restart_icon.png"));
        } catch (Exception e) {
            createDefaultRestartButton();
        }

        updateRestartButtonPosition();
    }
    private void saveInitialState() {
        originalSimulationTime = simulationTime;

        originalMarkersState.clear();
        for (Marker marker : markers) {
            Marker copy = new Marker(
                new Geolocation(marker.getPosition().lat, marker.getPosition().lng),
                marker.getType(),
                marker.getState(),
                marker.getId(),
                marker.getName(),
                marker.getTotalSpots(),
                marker.getAvailableSpots(),
                marker.getPricePerHour()
            );
            originalMarkersState.add(copy);
        }

        Gdx.app.log("SimulationScreen", "Initial state saved. Markers: " + originalMarkersState.size());
    }
    private void resetSimulation() {
        Gdx.app.log("SimulationScreen", "=== RESETTING SIMULATION ===");

        isSimulationRunning = false;
        isClockPaused = false;

        simulationTime = 8.0f * 60f;
        timeSpeedMultiplier = 1f;

        if (!originalMarkersState.isEmpty()) {
            markers.clear();
            for (Marker original : originalMarkersState) {
                Marker restored = new Marker(
                    new Geolocation(original.getPosition().lat, original.getPosition().lng),
                    original.getType(),
                    original.getState(),
                    original.getId(),
                    original.getName(),
                    original.getTotalSpots(),
                    original.getAvailableSpots(),
                    original.getPricePerHour()
                );
                markers.add(restored);
            }
            Gdx.app.log("SimulationScreen", "Markers restored: " + markers.size());
        }

        Gdx.app.log("SimulationScreen", "Simulation reset to initial state (08:00)");
    }
    private void drawRestartButton() {
        if (spriteBatch == null || restartSimulationTexture == null) return;

        updateRestartButtonPosition();
        spriteBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        spriteBatch.begin();

        spriteBatch.draw(restartSimulationTexture, restartSimulationX, restartSimulationY,
            restartSimulationSize, restartSimulationSize);

        spriteBatch.end();
    }
    private void updateRestartButtonPosition() {
        restartSimulationX = closeButtonMargin;
        restartSimulationY = Gdx.graphics.getHeight() - restartSimulationSize - closeButtonMargin - 220f;
    }
    private void initializeCrowdSimulation() {
        Gdx.app.log("SimulationScreen", "=== INITIALIZING CROWD SIMULATION ===");

        // Dugme za pokretanje koncert simulacije
        try {
            concertButtonTexture = new Texture(Gdx.files.internal("ui/concert_icon.png"));
        } catch (Exception e) {
            createDefaultConcertButton();
        }

        // Dugme za restart
        try {
            restartButtonTexture = new Texture(Gdx.files.internal("ui/restart_icon.png"));
        } catch (Exception e) {
            createDefaultRestartButton();
        }

        updateCrowdButtonsPosition();
    }
    private void updateCrowdButtonsPosition() {
        // Pomerite malo više (smanjite broj 220f na 270f ili slično)
        crowdButtonX = closeButtonMargin;
        crowdButtonY = Gdx.graphics.getHeight() - crowdButtonSize - closeButtonMargin - 270f;
    }

    private void drawCrowdSimulationButtons() {
        if (spriteBatch == null) return;

        updateCrowdButtonsPosition();
        spriteBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        spriteBatch.begin();

        float currentX = crowdButtonX;

        // Dugme za koncert simulaciju
        if (concertButtonTexture != null) {
            spriteBatch.draw(concertButtonTexture, currentX, crowdButtonY, crowdButtonSize, crowdButtonSize);
            currentX += crowdButtonSpacing;
        }

        // Dugme za restart (vidljivo samo kada je aktivna koncert simulacija)
        if (isCrowdSimulationActive && restartButtonTexture != null) {
            spriteBatch.draw(restartButtonTexture, currentX, crowdButtonY, crowdButtonSize, crowdButtonSize);
        }

        spriteBatch.end();
    }
    private void activateCrowdSimulation() {
        Gdx.app.log("SimulationScreen", "=== ACTIVATING CROWD SIMULATION ===");

        // Sačuvaj originalno stanje
        originalSimulationTime = simulationTime;
        saveOriginalMarkersState();

        // Postavi vreme na početak koncerta (npr. 18:00)
        simulationTime = 18.0f * 60f;

        // Označi da je aktivna koncert simulacija
        isCrowdSimulationActive = true;

        // Apliciraj efekte gužve na markere
        applyCrowdEffects();

        Gdx.app.log("SimulationScreen", "Crowd simulation activated! Time set to 18:00");
    }
    private void applyCrowdEffects() {
        Gdx.app.log("SimulationScreen", "Applying crowd effects to markers");

        for (Marker marker : markers) {
            int total = marker.getTotalSpots();
            if (total == 0) continue;

            // Za koncert, većina parkinga će biti puna
            // 70% šanse da bude FULL, 25% PARTIAL, 5% FREE
            float random = MathUtils.random();

            if (random < 0.70f) {
                // FULL - samo 0-10% mesta slobodno
                int available = MathUtils.random(0, Math.max(1, (int)(total * 0.1f)));
                marker.setAvailableSpots(available);
                marker.setState(Marker.MarkerState.FULL);
            } else if (random < 0.95f) {
                // PARTIAL - 10-40% mesta slobodno
                int available = MathUtils.random(
                    Math.max(1, (int)(total * 0.1f)),
                    Math.max(2, (int)(total * 0.4f))
                );
                marker.setAvailableSpots(available);
                marker.setState(Marker.MarkerState.PARTIAL);
            } else {
                // FREE - 40-80% mesta slobodno (retko)
                int available = MathUtils.random(
                    Math.max(1, (int)(total * 0.4f)),
                    Math.max(2, (int)(total * 0.8f))
                );
                marker.setAvailableSpots(available);
                marker.setState(Marker.MarkerState.FREE);
            }
        }
    }
    private void updateCrowdSimulation(float delta) {
        if (!isCrowdSimulationActive || !isSimulationRunning) return;

        // Dodatni efekti tokom koncerta
        // Tokom koncerta (18:00-22:00) parkingi polako postaju sve puniji
        int currentHour = ((int)simulationTime / 60) % 24;

        if (currentHour >= 18 && currentHour < 22) {
            // Na svakih 5 sim sekundi, smanji broj slobodnih mesta
            simulationUpdateTimer += delta;
            if (simulationUpdateTimer >= 5f) {
                for (Marker marker : markers) {
                    int available = marker.getAvailableSpots();
                    if (available > 0 && MathUtils.random() < 0.3f) {
                        // Nasumično oslobodi ili zauzmi mesto
                        if (MathUtils.random() < 0.7f) {
                            // Veća šansa za zauzimanje tokom koncerta
                            available = Math.max(0, available - MathUtils.random(1, 3));
                        } else {
                            // Manja šansa za oslobađanje
                            available = Math.min(marker.getTotalSpots(),
                                available + MathUtils.random(0, 1));
                        }
                        marker.setAvailableSpots(available);
                        marker.updateStateFromSpots();
                    }
                }
                simulationUpdateTimer = 0f;
            }
        }
    }
    private boolean isConcertButtonClicked(float screenX, float screenY) {
        updateCrowdButtonsPosition();
        return screenX >= crowdButtonX && screenX <= crowdButtonX + crowdButtonSize &&
            screenY >= crowdButtonY && screenY <= crowdButtonY + crowdButtonSize;
    }

    private boolean isRestartButtonClicked(float screenX, float screenY) {
        updateRestartButtonPosition();
        return screenX >= restartSimulationX &&
            screenX <= restartSimulationX + restartSimulationSize &&
            screenY >= restartSimulationY &&
            screenY <= restartSimulationY + restartSimulationSize;
    }

    private void saveOriginalMarkersState() {
        originalMarkersState.clear();
        for (Marker marker : markers) {
            // Napravi deep copy markera
            Marker copy = new Marker(
                new Geolocation(marker.getPosition().lat, marker.getPosition().lng),
                marker.getType(),
                marker.getState(),
                marker.getId(),
                marker.getName(),
                marker.getTotalSpots(),
                marker.getAvailableSpots(),
                marker.getPricePerHour()
            );
            originalMarkersState.add(copy);
        }
    }
    private void resetToOriginalState() {
        Gdx.app.log("SimulationScreen", "=== RESETTING TO ORIGINAL STATE ===");

        // Vrati vreme na originalno
        simulationTime = originalSimulationTime;

        // Vrati markere na originalno stanje
        markers.clear();
        for (Marker original : originalMarkersState) {
            Marker restored = new Marker(
                new Geolocation(original.getPosition().lat, original.getPosition().lng),
                original.getType(),
                original.getState(),
                original.getId(),
                original.getName(),
                original.getTotalSpots(),
                original.getAvailableSpots(),
                original.getPricePerHour()
            );
            markers.add(restored);
        }

        // Deaktiviraj koncert simulaciju
        isCrowdSimulationActive = false;

        Gdx.app.log("SimulationScreen", "Simulation reset to original state");
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
    private void createDefaultConcertButton() {
        Pixmap pixmap = new Pixmap((int)crowdButtonSize, (int)crowdButtonSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.8f, 0.2f, 0.8f, 1f); // Ljubičasta boja za koncert
        pixmap.fillCircle((int)crowdButtonSize/2, (int)crowdButtonSize/2, (int)crowdButtonSize/2 - 5);
        pixmap.setColor(Color.WHITE);

        // Simbol note za muziku
        int centerX = (int)crowdButtonSize/2;
        int centerY = (int)crowdButtonSize/2;

        // Krug na dnu note
        pixmap.fillCircle(centerX, (int)(centerY*1.1f), (int)(crowdButtonSize*0.15f));

        // Stab note
        pixmap.fillRectangle(centerX - 2, (int)(centerY*0.3f), 4, (int)(centerY*0.7f));

        // Zastavica note
        int[] xPoints = {centerX + 4, centerX + 20, centerX + 4};
        int[] yPoints = {(int)(centerY*0.3f), (int)(centerY*0.4f), (int)(centerY*0.5f)};
        pixmap.fillTriangle(xPoints[0], yPoints[0], xPoints[1], yPoints[1], xPoints[2], yPoints[2]);

        concertButtonTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createDefaultRestartButton() {
        Pixmap pixmap = new Pixmap((int)restartSimulationSize, (int)restartSimulationSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.2f, 0.6f, 0.9f, 1f); // Plava boja za restart
        pixmap.fillCircle((int)restartSimulationSize/2, (int)restartSimulationSize/2, (int)restartSimulationSize/2 - 5);
        pixmap.setColor(Color.WHITE);

        // Strelica u krug za restart
        int centerX = (int)restartSimulationSize/2;
        int centerY = (int)restartSimulationSize/2;
        int radius = (int)(restartSimulationSize * 0.3f);

        // Krug
        pixmap.drawCircle(centerX, centerY, radius);

        // Strelica
        int[] arrowX = {centerX - radius/2, centerX - radius/2, centerX + radius/2};
        int[] arrowY = {centerY - radius/3, centerY + radius/3, centerY};
        pixmap.fillTriangle(arrowX[0], arrowY[0], arrowX[1], arrowY[1], arrowX[2], arrowY[2]);

        restartSimulationTexture = new Texture(pixmap);
        pixmap.dispose();
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
    private void initializeSimulationButton() {
        Gdx.app.log("SimulationScreen", "=== INITIALIZING SIMULATION BUTTON ===");

        try {
            playButtonTexture = new Texture(Gdx.files.internal("ui/play_button.png"));
        } catch (Exception e) {
            createDefaultPlayButton();
        }

        try {
            pauseButtonTexture = new Texture(Gdx.files.internal("ui/pause_button.png"));
        } catch (Exception e) {
            createDefaultPauseButton();
        }

        updatePlayButtonPosition();
    }

    private void createDefaultPlayButton() {
        Pixmap pixmap = new Pixmap((int)playButtonSize, (int)playButtonSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.2f, 0.7f, 0.2f, 1f);
        pixmap.fillCircle((int)playButtonSize/2, (int)playButtonSize/2, (int)playButtonSize/2 - 5);
        pixmap.setColor(Color.WHITE);

        int triangleSize = (int)(playButtonSize * 0.4);
        int[] xPoints = {(int)(playButtonSize*0.4), (int)(playButtonSize*0.4), (int)(playButtonSize*0.7)};
        int[] yPoints = {(int)(playButtonSize*0.35), (int)(playButtonSize*0.65), (int)(playButtonSize*0.5)};
        pixmap.fillTriangle(xPoints[0], yPoints[0], xPoints[1], yPoints[1], xPoints[2], yPoints[2]);

        playButtonTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void createDefaultPauseButton() {
        Pixmap pixmap = new Pixmap((int)playButtonSize, (int)playButtonSize, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.8f, 0.6f, 0.2f, 1f);
        pixmap.fillCircle((int)playButtonSize/2, (int)playButtonSize/2, (int)playButtonSize/2 - 5);
        pixmap.setColor(Color.WHITE);

        int barWidth = (int)(playButtonSize * 0.15f);
        int barHeight = (int)(playButtonSize * 0.4f);

        pixmap.fillRectangle(
            (int)(playButtonSize * 0.35f) - barWidth/2,
            (int)(playButtonSize * 0.5f) - barHeight/2,
            barWidth,
            barHeight
        );

        pixmap.fillRectangle(
            (int)(playButtonSize * 0.65f) - barWidth/2,
            (int)(playButtonSize * 0.5f) - barHeight/2,
            barWidth,
            barHeight
        );

        pauseButtonTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    private void updatePlayButtonPosition() {
        playButtonX = closeButtonMargin;
        playButtonY = Gdx.graphics.getHeight() - playButtonSize - closeButtonMargin - 170f;
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
        if (clockUpdateTimer >= SLOW_CLOCK_UPDATE_INTERVAL) {
            simulationTime += (timeSpeedMultiplier * SLOW_CLOCK_UPDATE_INTERVAL * 30f);
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

                    // DODAJTE OVO - sačuvaj početno stanje kada se učitaju markeri
                    saveInitialState();
                });
            } catch (Exception e) {
                Gdx.app.error("SimulationScreen", "Failed to load parking locations", e);
                Gdx.app.postRunnable(() -> {
                    initializeTestMarkers();
                    // DODAJTE OVO - sačuvaj početno stanje i za test markere
                    saveInitialState();
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

        // DODAJTE OVO - sačuvaj početno stanje
        saveInitialState();
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

                if (isPlayButtonClicked(screenX, gdxY)) {
                    isSimulationRunning = !isSimulationRunning;
                    Gdx.app.log("SimulationScreen", "Simulation " +
                        (isSimulationRunning ? "STARTED" : "PAUSED"));
                    return true;
                }

                if (isClockAreaClicked(screenX, gdxY)) {
                    isClockPaused = !isClockPaused;
                    Gdx.app.log("SimulationScreen", "Clock " + (isClockPaused ? "PAUSED" : "RESUMED") + " via click");
                    return true;
                }

                // DODAJTE OVO - provera za restart dugme
                if (isRestartButtonClicked(screenX, gdxY)) {
                    resetSimulation();
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
    private void updateSimulation(float delta) {
        if (!isSimulationRunning) return;

        simulationUpdateTimer += delta;
        if (simulationUpdateTimer >= SIMULATION_UPDATE_INTERVAL) {
            applyTimeBasedBehavior();
            updateMarkersBasedOnTime();
            simulationUpdateTimer = 0f;
        }
    }
    private boolean isPlayButtonClicked(float screenX, float screenY) {
        updatePlayButtonPosition();
        return screenX >= playButtonX && screenX <= playButtonX + playButtonSize &&
            screenY >= playButtonY && screenY <= playButtonY + playButtonSize;
    }
    private void updateMarkersBasedOnTime() {
        if (!isSimulationRunning) return;

        DayPhase phase = SimulationTimeMapper.getDayPhase(simulationTime);

        if (phase == DayPhase.DAYTIME) {
            applySmoothDaytimeChanges();
        } else {
            applyNormalTimeBasedChanges(phase);
        }
    }

    private void applySmoothDaytimeChanges() {
        for (Marker marker : markers) {
            int available = marker.getAvailableSpots();
            int total = marker.getTotalSpots();

            if (total == 0) continue;

            float currentRatio = (float) available / total;

            float targetRatio = getDaytimeTargetRatio();

            int change = calculateSmoothChange(currentRatio, targetRatio, total);

            available += change;
            available = MathUtils.clamp(available, 0, total);

            marker.setAvailableSpots(available);

            updateMarkerState(marker);
        }
    }

    private float getDaytimeTargetRatio() {
        return 0.4f + (MathUtils.random() * 0.3f);
    }

    private int calculateSmoothChange(float currentRatio, float targetRatio, int total) {
        float difference = targetRatio - currentRatio;

        if (Math.abs(difference) < 0.1f) {
            return MathUtils.random(-1, 1);
        }

        int maxChange = (int)(total * 0.05f);
        maxChange = Math.max(1, Math.min(3, maxChange));

        int change = (int)(difference * total * 0.1f);

        change = MathUtils.clamp(change, -maxChange, maxChange);

        if (MathUtils.random() < 0.4f) {
            change = MathUtils.random(-1, 1);
        }

        return change;
    }

    private void applyNormalTimeBasedChanges(DayPhase phase) {
        float arrivalMultiplier = SimulationTimeMapper.getArrivalMultiplier(phase);

        for (Marker marker : markers) {
            int available = marker.getAvailableSpots();
            int total = marker.getTotalSpots();

            if (total == 0) continue;

            float baseChange = calculateBaseChange(phase);

            float adjustedChange = baseChange * arrivalMultiplier;

            float randomFactor = MathUtils.random(-0.5f, 0.5f);
            float finalChange = adjustedChange + randomFactor;

            available -= Math.round(finalChange);

            available = MathUtils.clamp(available, 0, total);

            if (finalChange < 0 && (phase == DayPhase.MORNING_RUSH ||
                phase == DayPhase.AFTERNOON_RUSH)) {
                if (available > 0) {
                    available -= MathUtils.random(0, 2);
                    available = Math.max(available, 0);
                }
            }

            marker.setAvailableSpots(available);

            updateMarkerState(marker);
        }
    }
    private void drawStabilityInfo() {
        if (spriteBatch == null || font == null) return;

        DayPhase phase = SimulationTimeMapper.getDayPhase(simulationTime);

        if (phase == DayPhase.DAYTIME) {
            spriteBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0,
                Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
            spriteBatch.begin();

            font.setColor(new Color(0.2f, 0.8f, 0.2f, 1f));
            String stabilityText = "⚖️ STABILNO STANJE";
            font.draw(spriteBatch, stabilityText, playButtonX, playButtonY - 140f);

            String descText = "Mali promet, bez naglih skokova";
            font.setColor(Color.LIGHT_GRAY);
            font.draw(spriteBatch, descText, playButtonX, playButtonY - 170f);

            spriteBatch.end();
        }
    }
    private float calculateBaseChange(DayPhase phase) {
        switch (phase) {
            case MORNING_RUSH:
                return 3.0f;
            case DAYTIME:
                return 1.0f;
            case AFTERNOON_RUSH:
                return 2.5f;
            case EVENING:
                return -1.0f;
            case NIGHT:
                return -2.0f;
            default:
                return 0.5f;
        }
    }

    private void updateMarkerState(Marker marker) {
        int total = marker.getTotalSpots();
        int available = marker.getAvailableSpots();

        if (total == 0) {
            marker.setState(Marker.MarkerState.UNKNOWN);
            return;
        }

        float occupancyRatio = (float) available / total;

        if (occupancyRatio >= 0.6f) {
            marker.setState(Marker.MarkerState.FREE);
        } else if (occupancyRatio >= 0.2f) {
            marker.setState(Marker.MarkerState.PARTIAL);
        } else {
            marker.setState(Marker.MarkerState.FULL);
        }
    }
    @Override
    public void render(float delta) {
        handleKeyboardInput();

        if (isSimulationRunning) {
            updateSimulationTime(delta);
            updateSimulation(delta);
        }

        // OVO JE NEOPHODNO ZA CLEAR EKRANA
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);

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
            drawSimulationButton();
            drawStabilityInfo();
            drawRestartButton(); // DODAJTE OVO
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

            int baseChange = 0;
            boolean smallChangeOnly = false;

            switch (phase) {
                case MORNING_RUSH:
                    baseChange = -MathUtils.random(3, 6);
                    break;

                case DAYTIME:
                    baseChange = calculateDaytimeChange(marker, available, total);
                    smallChangeOnly = true;
                    break;

                case AFTERNOON_RUSH:
                    baseChange = -MathUtils.random(2, 5);
                    break;

                case EVENING:
                    baseChange = MathUtils.random(0, 2);
                    smallChangeOnly = true;
                    break;

                case NIGHT:
                    baseChange = MathUtils.random(1, 3);
                    break;
            }

            if (smallChangeOnly) {
                baseChange = applyStabilityConstraints(baseChange, available, total);
            }

            available += baseChange;
            available = MathUtils.clamp(available, 0, total);

            marker.setAvailableSpots(available);

            updateMarkerState(marker);
        }
    }

    private int calculateDaytimeChange(Marker marker, int available, int total) {
        float occupancyRatio = (float) available / total;

        if (occupancyRatio > 0.8f) {
            return MathUtils.random(-2, 0);
        } else if (occupancyRatio > 0.4f) {
            return MathUtils.random(-1, 1);
        } else {
            return MathUtils.random(0, 1);
        }
    }

    private int applyStabilityConstraints(int change, int available, int total) {
        if (change > 0 && available >= total * 0.9f) {
            change = Math.min(change, 1);
        }

        if (change < 0 && available <= total * 0.1f) {
            change = Math.max(change, -1);
        }

        if (Math.abs(change) > 2) {
            change = change > 0 ? 1 : -1;
        }

        if (MathUtils.random() < 0.3f) {
            change = 0;
        }

        return change;
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
        if (concertButtonTexture != null) {
            concertButtonTexture.dispose();
        }
        if (restartButtonTexture != null) {
            restartButtonTexture.dispose();
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
        if (playButtonTexture != null) {
            playButtonTexture.dispose();
        }
        if (pauseButtonTexture != null) {
            pauseButtonTexture.dispose();
        }
        if (restartSimulationTexture != null) {
            restartSimulationTexture.dispose();
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
        updatePlayButtonPosition();
        updateRestartButtonPosition();
        Gdx.app.log("SimulationScreen", "Screen resized to: " + width + "x" + height);
    }
}
