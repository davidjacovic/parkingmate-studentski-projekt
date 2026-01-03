package si.um.feri.parkingmate.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import si.um.feri.parkingmate.model.Tariff;

import java.util.List;

/**
 * Popup window for displaying tariffs in a clean, formatted way.
 */
public class TariffPopup {

    private float x, y;
    private float width, height;
    private boolean visible;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private GlyphLayout glyphLayout;

    // Colors
    private Color backgroundColor = new Color(1f, 1f, 1f, 0.85f);
    private Color borderColor = new Color(0.6f, 0.6f, 0.6f, 1f);
    private Color titleColor = new Color(0.1f, 0.1f, 0.1f, 1f);
    private Color headerColor = new Color(0.2f, 0.2f, 0.2f, 1f);
    private Color textColor = new Color(0.1f, 0.1f, 0.1f, 1f);
    private Color priceColor = new Color(0.1f, 0.1f, 0.1f, 1f);
    private Color dividerColor = new Color(0.8f, 0.8f, 0.8f, 1f);

    // Spacing
    private float padding = 25f;
    private float titleTopPadding = 30f;
    private float lineHeight = 24f;
    private float tablePadding = 15f;

    // Font scales
    private float titleFontScale = 0.7f;
    private float headerFontScale = 0.55f;
    private float contentFontScale = 0.3f;

    // Close button
    private Rectangle closeButton;
    private float closeButtonSize = 24f;

    // Tariffs data
    private List<Tariff> tariffs;
    private String parkingName;

    public TariffPopup() {
        this.shapeRenderer = new ShapeRenderer();
        this.spriteBatch = new SpriteBatch();
        this.glyphLayout = new GlyphLayout();
        this.visible = false;
        this.closeButton = new Rectangle();
    }

    /**
     * Show the tariff popup
     */
    public void show(String parkingName, List<Tariff> tariffs) {
        this.parkingName = parkingName;
        this.tariffs = tariffs;
        this.visible = true;

        // Calculate popup size (80% of screen)
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();

        this.width = screenWidth * 0.8f;
        this.height = screenHeight * 0.7f;
        this.x = (screenWidth - width) / 2;
        this.y = (screenHeight - height) / 2;

        // Set close button position
        closeButton.set(x + width - closeButtonSize - padding/2,
            y + height - closeButtonSize - padding/2,
            closeButtonSize, closeButtonSize);
    }

    /**
     * Hide the popup
     */
    public void hide() {
        this.visible = false;
        this.tariffs = null;
        this.parkingName = null;
    }

    /**
     * Check if close button clicked
     */
    public boolean isCloseButtonClicked(float screenX, float screenY) {
        return closeButton.contains(screenX, screenY);
    }

    /**
     * Check if point is inside popup
     */
    public boolean contains(float screenX, float screenY) {
        return screenX >= x && screenX <= x + width &&
            screenY >= y && screenY <= y + height;
    }

    /**
     * Render the tariff popup - BEZ OVERLAY, DA SE VIDI MAPA
     */
    public void render() {
        if (!visible || tariffs == null || font == null) {
            return;
        }

        // shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // shapeRenderer.setColor(0f, 0f, 0f, 0.5f);
        // shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        // shapeRenderer.end();

        // Draw popup background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a);
        shapeRenderer.rect(x, y, width, height);

        // Draw border
        shapeRenderer.setColor(borderColor.r, borderColor.g, borderColor.b, borderColor.a);
        shapeRenderer.rect(x, y, width, 1);
        shapeRenderer.rect(x, y + height - 1, width, 1);
        shapeRenderer.rect(x, y, 1, height);
        shapeRenderer.rect(x + width - 1, y, 1, height);
        shapeRenderer.end();

        // Draw close button
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 1f);
        shapeRenderer.rect(closeButton.x, closeButton.y, closeButtonSize, closeButtonSize);

        // Draw X
        shapeRenderer.setColor(1f, 1f, 1f, 1f);
        shapeRenderer.rectLine(
            closeButton.x + closeButtonSize * 0.2f, closeButton.y + closeButtonSize * 0.2f,
            closeButton.x + closeButtonSize * 0.8f, closeButton.y + closeButtonSize * 0.8f,
            2f
        );
        shapeRenderer.rectLine(
            closeButton.x + closeButtonSize * 0.8f, closeButton.y + closeButtonSize * 0.2f,
            closeButton.x + closeButtonSize * 0.2f, closeButton.y + closeButtonSize * 0.8f,
            2f
        );
        shapeRenderer.end();

        // Draw content
        drawContent();
    }

    /**
     * Draw popup content
     */
    private void drawContent() {
        if (tariffs == null || tariffs.isEmpty()) {
            drawNoTariffs();
            return;
        }

        float contentStartX = x + padding;
        float contentWidth = width - padding * 2;
        float currentY = y + height - padding - titleTopPadding;

        // Draw title
        spriteBatch.begin();
        font.setColor(titleColor.r, titleColor.g, titleColor.b, 1f);
        font.getData().setScale(titleFontScale);

        String title = "TARIFE - " + (parkingName != null ? parkingName : "");
        glyphLayout.setText(font, title);
        float titleX = x + (width - glyphLayout.width) / 2;
        font.draw(spriteBatch, title, titleX, currentY);

        font.getData().setScale(1.0f);
        spriteBatch.end();

        currentY -= 40;

        // Draw table header
        drawTableHeader(contentStartX, currentY);
        currentY -= lineHeight + 10;

        // Draw divider line
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(dividerColor.r, dividerColor.g, dividerColor.b, 1f);
        shapeRenderer.rect(contentStartX, currentY, contentWidth, 1);
        shapeRenderer.end();

        currentY -= 10;

        // Draw tariff rows
        int visibleRows = 0;
        int maxRows = 10;

        for (Tariff tariff : tariffs) {
            if (currentY < y + padding + 50 || visibleRows >= maxRows) {
                if (visibleRows < tariffs.size()) {
                    drawMoreIndicator(contentStartX, currentY, tariffs.size() - visibleRows);
                }
                break;
            }

            drawTariffRow(tariff, contentStartX, currentY);
            currentY -= lineHeight;
            visibleRows++;
        }
    }

    /**
     * Draw table header
     */
    private void drawTableHeader(float startX, float y) {
        spriteBatch.begin();
        font.setColor(headerColor.r, headerColor.g, headerColor.b, 1f);
        font.getData().setScale(headerFontScale);

        float colWidth = 120f;
        float col1 = startX;
        float col2 = col1 + colWidth;
        float col3 = col2 + colWidth;
        float col4 = col3 + colWidth;

        font.draw(spriteBatch, "TIP", col1, y);
        font.draw(spriteBatch, "TRAJANJE", col2, y);
        font.draw(spriteBatch, "VOZILO", col3, y);
        font.draw(spriteBatch, "CENA", col4, y);

        font.getData().setScale(1.0f);
        spriteBatch.end();
    }

    /**
     * Draw a single tariff row
     */
    private void drawTariffRow(Tariff tariff, float startX, float y) {
        spriteBatch.begin();
        font.setColor(textColor.r, textColor.g, textColor.b, 1f);
        font.getData().setScale(contentFontScale);

        float colWidth = 120f;
        float col1 = startX;
        float col2 = col1 + colWidth;
        float col3 = col2 + colWidth;
        float col4 = col3 + colWidth;

        // Column 1: Tariff Type
        String type = getTariffTypeLabel(tariff.getTariffType());
        font.draw(spriteBatch, type, col1, y);

        // Column 2: Duration
        String duration = tariff.getDuration() != null ? tariff.getDuration() : "-";
        font.draw(spriteBatch, duration, col2, y);

        // Column 3: Vehicle Type
        String vehicle = getVehicleTypeLabel(tariff.getVehicleType());
        font.draw(spriteBatch, vehicle, col3, y);

        // Column 4: Price
        String price = tariff.getFormattedPrice();
        font.draw(spriteBatch, price, col4, y);

        font.getData().setScale(1.0f);
        spriteBatch.end();
    }

    /**
     * Draw "more tariffs" indicator
     */
    private void drawMoreIndicator(float x, float y, int remaining) {
        spriteBatch.begin();
        font.setColor(0.5f, 0.5f, 0.5f, 1f);
        font.getData().setScale(contentFontScale * 0.9f);

        String text = "... još " + remaining + " tarifa";
        font.draw(spriteBatch, text, x, y);

        font.getData().setScale(1.0f);
        spriteBatch.end();
    }

    /**
     * Draw "no tariffs" message
     */
    private void drawNoTariffs() {
        float centerX = x + width / 2;
        float centerY = y + height / 2;

        spriteBatch.begin();
        font.setColor(0.5f, 0.5f, 0.5f, 1f);
        font.getData().setScale(titleFontScale);

        String text = "Nema dostupnih tarifa";
        glyphLayout.setText(font, text);
        float textX = centerX - glyphLayout.width / 2;
        float textY = centerY + glyphLayout.height / 2;

        font.draw(spriteBatch, text, textX, textY);
        font.getData().setScale(1.0f);
        spriteBatch.end();
    }

    /**
     * Get translated tariff type label
     */
    private String getTariffTypeLabel(String type) {
        if (type == null) return "Standardna";

        switch (type.toLowerCase()) {
            case "hourly": return "Satna";
            case "daily": return "Dnevna";
            case "weekly": return "Nedeljna";
            case "monthly": return "Mesečna";
            case "annual": return "Godišnja";
            default: return type;
        }
    }

    /**
     * Get translated vehicle type label
     */
    private String getVehicleTypeLabel(String vehicleType) {
        if (vehicleType == null) return "Automobil";

        switch (vehicleType.toLowerCase()) {
            case "car": return "Automobil";
            case "motorcycle": return "Motocikl";
            case "bus": return "Autobus";
            case "truck": return "Kamion";
            case "bicycle": return "Bicikl";
            default: return vehicleType;
        }
    }

    /**
     * Set font
     */
    public void setFont(BitmapFont font) {
        this.font = font;
    }

    /**
     * Dispose resources
     */
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
    }

    // Getters
    public boolean isVisible() {
        return visible;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
