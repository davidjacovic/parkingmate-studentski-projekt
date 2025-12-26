package si.um.feri.parkingmate.map;

/**
 * Constants for map rendering and configuration.
 */
public class MapConstants {
    public static final int NUM_TILES = 3;
    public static final int ZOOM = 15;
    public static final int MAP_WIDTH = MapRasterTiles.TILE_SIZE * NUM_TILES;
    public static final int MAP_HEIGHT = MapRasterTiles.TILE_SIZE * NUM_TILES;
    
    // Center location for Ljubljana (can be adjusted)
    public static final Geolocation CENTER_GEOLOCATION = new Geolocation(46.0569, 14.5058);
    
    // Default zoom limits
    public static final float MIN_ZOOM = 0.5f;
    public static final float MAX_ZOOM = 3.0f;
}

