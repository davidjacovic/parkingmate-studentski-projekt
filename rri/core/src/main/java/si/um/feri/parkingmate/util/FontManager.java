package si.um.feri.parkingmate.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

public class FontManager {

    private static BitmapFont font;
    public static BitmapFont getFont() {
        if (font == null) {
            createFont();
        }
        return font;
    }

    private static void createFont() {
        try {
            font = new BitmapFont(Gdx.files.internal("fonts/arial.fnt"));
            font.getData().setScale(1f);
        } catch (Exception e) {
            Gdx.app.error("FontManager", "Failed to load bitmap font", e);
            font = new BitmapFont();
        }
    }
    public static void dispose() {
        if (font != null) {
            font.dispose();
            font = null;
        }
    }
}
