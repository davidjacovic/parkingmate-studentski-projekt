package si.um.feri.parkingmate.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import si.um.feri.parkingmate.model.Tariff;
import si.um.feri.parkingmate.model.Parking;

import si.um.feri.parkingmate.model.Marker;

public class ParkingHoverTooltip {

    private boolean visible = false;
    private Marker marker;

    private float x, y;
    private float width = 220f;
    private float height = 110f;

    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch spriteBatch;
    private final BitmapFont font;
    private final GlyphLayout layout;

    // Styling – isto kao InfoPanel (white card)
    private final Color bgColor = new Color(0.98f, 0.98f, 0.98f, 0.97f);
    private final Color borderColor = new Color(0.8f, 0.8f, 0.8f, 1f);
    private final Color titleColor = new Color(0.1f, 0.1f, 0.1f, 1f);
    private final Color labelColor = new Color(0.45f, 0.45f, 0.45f, 1f);

    private final float padding = 12f;

    public ParkingHoverTooltip(BitmapFont font) {
        this.font = font;
        this.shapeRenderer = new ShapeRenderer();
        this.spriteBatch = new SpriteBatch();
        this.layout = new GlyphLayout();
    }

    /* ================= API ================= */

    public void show(Marker marker, float screenX, float screenY) {
        this.marker = marker;
        this.visible = true;

        // Offset od kursora
        this.x = screenX + 14;
        this.y = screenY - 14;

        clampToScreen();
    }

    public void hide() {
        visible = false;
        marker = null;
    }

    public void render() {
        if (!visible || marker == null) return;

        // SCREEN SPACE
        Matrix4 uiMatrix = new Matrix4().setToOrtho2D(
            0, 0,
            Gdx.graphics.getWidth(),
            Gdx.graphics.getHeight()
        );

        shapeRenderer.setProjectionMatrix(uiMatrix);
        spriteBatch.setProjectionMatrix(uiMatrix);

        // BACKGROUND
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(bgColor);
        shapeRenderer.rect(x, y - height, width, height);
        shapeRenderer.end();

        // BORDER
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(borderColor);
        shapeRenderer.rect(x, y - height, width, height);
        shapeRenderer.end();

        // TEXT
        spriteBatch.begin();

        float cursorY = y - padding;

        // TITLE
        font.getData().setScale(0.45f);
        font.setColor(titleColor);
        font.draw(spriteBatch, marker.getName(), x + padding, cursorY);

        cursorY -= 22;

        // STATUS
        font.getData().setScale(0.36f);
        font.setColor(labelColor);
        font.draw(spriteBatch,
            "Slobodno: " + marker.getAvailableSpots() +
                " / " + marker.getTotalSpots(),
            x + padding, cursorY
        );

        cursorY -= 18;

        // PRICE
        Parking p = marker.getParkingData();

        if (p != null && p.getTariffs() != null && !p.getTariffs().isEmpty()) {
            Tariff t = p.getTariffs().get(0);

            font.draw(spriteBatch,
                "Cena: " + t.getFormattedPrice(),
                x + padding, cursorY
            );

            cursorY -= 16f;

            if (t.getDuration() != null) {
                font.draw(spriteBatch,
                    "Vreme: " + t.getDuration(),
                    x + padding, cursorY
                );
            }

        } else {
            font.draw(spriteBatch,
                "Cena: N/A",
                x + padding, cursorY
            );
        }



        font.getData().setScale(1f);
        spriteBatch.end();
    }

    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
    }

    /* ================= HELPERS ================= */

    private void clampToScreen() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        if (x + width > screenW) {
            x = screenW - width - 8;
        }

        if (y - height < 0) {
            y = height + 8;
        }
    }
}
