package si.um.feri.parkingmate.util;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public class FontManager {

    private static BitmapFont font;

    /**
     * Vraća globalni font sa Unicode podrškom.
     * Ako font nije generisan, kreira ga iz TTF fajla.
     */
    public static BitmapFont getFont() {
        if (font == null) {
            createFont();
        }
        return font;
    }

    /**
     * Generiše font iz TTF fajla sa Unicode karakterima.
     */
    private static void createFont() {
        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial-32.fnt"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 32; // osnovna veličina
            parameter.color = Color.WHITE;
            // sve karaktere koje planiraš koristiti, uključujući ć, š, č, ž
            parameter.characters =
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789ćščžĆŠČŽ.,:!?-+/() ";

            font = generator.generateFont(parameter);
            font.getData().setScale(0.75f); // možeš promeniti skaliranje po potrebi
            generator.dispose();
        } catch (Exception e) {
            Gdx.app.error("FontManager", "Failed to load font", e);
            font = new BitmapFont(); // fallback
            font.getData().setScale(0.75f);
        }
    }

    /**
     * Oslobađa resurse fonta.
     */
    public static void dispose() {
        if (font != null) {
            font.dispose();
            font = null;
        }
    }
}
