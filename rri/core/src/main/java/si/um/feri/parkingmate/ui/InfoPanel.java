package si.um.feri.parkingmate.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;

import java.util.List;

import si.um.feri.parkingmate.model.Marker;
import si.um.feri.parkingmate.model.Parking;
import si.um.feri.parkingmate.model.Tariff;

/**
 * UI component for displaying parking information with animation.
 * UPDATED VERSION - White background, black text, smaller fonts
 */
public class InfoPanel {

    private float x, y;
    private float width, height;
    private boolean visible;
    private boolean isAnimating;
    private Marker selectedMarker;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private GlyphLayout glyphLayout;

    // Animation properties
    private float animationTime = 0f;
    private float animationDuration = 0.3f;
    private AnimationState animationState = AnimationState.HIDDEN;

    // Colors
    private Color backgroundColor = new Color(0.98f, 0.98f, 0.98f, 0.98f);
    private Color borderColor = new Color(0.8f, 0.8f, 0.8f, 1f);
    private Color titleColor = new Color(0.1f, 0.1f, 0.1f, 1f);
    private Color labelColor = new Color(0.4f, 0.4f, 0.4f, 1f);
    private Color valueColor = new Color(0.1f, 0.1f, 0.1f, 1f);
    private Color dividerColor = new Color(0.85f, 0.85f, 0.85f, 1f);

    // Status colors
    private Color statusFreeColor = new Color(0.2f, 0.7f, 0.2f, 1f);
    private Color statusPartialColor = new Color(0.9f, 0.6f, 0.1f, 1f);
    private Color statusFullColor = new Color(0.8f, 0.2f, 0.2f, 1f);
    private Color statusUnknownColor = new Color(0.5f, 0.5f, 0.5f, 1f);
    private Color priceColor = new Color(0.9f, 0.5f, 0.1f, 1f);

    // Spacing
    private float padding = 18f;
    private float titleTopPadding = 22f;
    private float lineHeight = 20f;
    private float sectionSpacing = 22f;
    private float dividerHeight = 1.5f;

    // Font scales
    private float titleFontScale = 0.52f;
    private float sectionFontScale = 0.45f;
    private float detailFontScale = 0.36f;

    // Close button
    private Rectangle closeButton;
    private float closeButtonSize = 18f;

    // Header icon
    private Texture headerIcon;

    // Marker textures
    private Texture markerFreeTexture;
    private Texture markerPartialTexture;
    private Texture markerFullTexture;
    private Texture markerUnknownTexture;

    // Tariff button
    private Rectangle tariffButton;
    private boolean showTariffButton = false;

    // Tariff popup
    private TariffPopup tariffPopup;

    private float headerIconSize = 28f;
    private float headerIconMargin = 12f;

    private boolean tariffPopupVisible = false;


    // Animation states
    private enum AnimationState {
        SHOWING,
        HIDING,
        SHOWN,
        HIDDEN
    }

    public InfoPanel() {
        this.shapeRenderer = new ShapeRenderer();
        this.spriteBatch = new SpriteBatch();
        this.glyphLayout = new GlyphLayout();
        this.visible = false;
        this.isAnimating = false;
        this.closeButton = new Rectangle();
        this.tariffButton = new Rectangle();
        this.tariffPopup = new TariffPopup();
        loadIcons();
        loadMarkerTextures();
    }

    /**
     * Load icons
     */
    private void loadIcons() {
        try {
            headerIcon = new Texture(Gdx.files.internal("markers/marker_icon.png"));
        } catch (Exception e) {
            try {
                headerIcon = new Texture(Gdx.files.internal("icons/parking.png"));
            } catch (Exception e2) {
                Gdx.app.debug("InfoPanel", "Parking icon not found");
                headerIcon = null;
            }
        }
    }

    /**
     * Load marker textures for the header
     */
    private void loadMarkerTextures() {
        try {
            markerFreeTexture = new Texture(Gdx.files.internal("markers/marker_free.png"));
        } catch (Exception e) {
            Gdx.app.debug("InfoPanel", "Marker texture not found: markers/marker_free.png");
            markerFreeTexture = null;
        }

        try {
            markerPartialTexture = new Texture(Gdx.files.internal("markers/marker_partial.png"));
        } catch (Exception e) {
            Gdx.app.debug("InfoPanel", "Marker texture not found: markers/marker_partial.png");
            markerPartialTexture = null;
        }

        try {
            markerFullTexture = new Texture(Gdx.files.internal("markers/marker_full.png"));
        } catch (Exception e) {
            Gdx.app.debug("InfoPanel", "Marker texture not found: markers/marker_full.png");
            markerFullTexture = null;
        }

        try {
            markerUnknownTexture = new Texture(Gdx.files.internal("markers/marker_unknown.png"));
        } catch (Exception e) {
            Gdx.app.debug("InfoPanel", "Marker texture not found: markers/marker_unknown.png");
            markerUnknownTexture = null;
        }
    }

    /**
     * Get marker texture based on state
     */
    private Texture getMarkerTextureForState(Marker.MarkerState state) {
        switch (state) {
            case FREE:
                return markerFreeTexture != null ? markerFreeTexture : headerIcon;
            case PARTIAL:
                return markerPartialTexture != null ? markerPartialTexture : headerIcon;
            case FULL:
                return markerFullTexture != null ? markerFullTexture : headerIcon;
            case UNKNOWN:
            default:
                return markerUnknownTexture != null ? markerUnknownTexture : headerIcon;
        }
    }

    /**
     * Update animation
     */
    public void update(float deltaTime) {
        if (!isAnimating) return;

        animationTime += deltaTime;
        float progress = Math.min(1f, animationTime / animationDuration);

        switch (animationState) {
            case SHOWING:
                if (progress >= 1f) {
                    animationState = AnimationState.SHOWN;
                    isAnimating = false;
                }
                break;
            case HIDING:
                if (progress >= 1f) {
                    animationState = AnimationState.HIDDEN;
                    isAnimating = false;
                    visible = false;
                    selectedMarker = null;
                }
                break;
        }
    }

    /**
     * Get current panel X position with animation
     */
    private float getCurrentX() {
        switch (animationState) {
            case SHOWING:
                float progress = Math.min(1f, animationTime / animationDuration);
                float startX = x + width;
                float endX = x;
                return startX + (endX - startX) * Interpolation.smooth.apply(progress);

            case HIDING:
                progress = Math.min(1f, animationTime / animationDuration);
                startX = x;
                endX = x + width;
                return startX + (endX - startX) * Interpolation.smooth.apply(progress);

            case SHOWN:
                return x;
            case HIDDEN:
                return x + width;
            default:
                return x;
        }
    }

    /**
     * Get current alpha for fade effect
     */
    private float getCurrentAlpha() {
        switch (animationState) {
            case SHOWING:
                float progress = Math.min(1f, animationTime / animationDuration);
                return Interpolation.fade.apply(progress);
            case HIDING:
                progress = Math.min(1f, animationTime / animationDuration);
                return 1f - Interpolation.fade.apply(progress);
            case SHOWN:
                return 1f;
            case HIDDEN:
                return 0f;
            default:
                return 1f;
        }
    }

    /**
     * Set font
     */
    public void setFont(BitmapFont font) {
        this.font = font;
        this.tariffPopup.setFont(font);
    }


    /**
     * Set bounds - NOW FULL WINDOW HEIGHT
     */
    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        closeButton.set(x + width - closeButtonSize - padding/2,
            y + height - closeButtonSize - padding/2,
            closeButtonSize, closeButtonSize);

        // Tariff button position
        tariffButton.set(x + width - closeButtonSize - padding/2 - closeButtonSize - 10,
            y + height - closeButtonSize - padding/2,
            closeButtonSize, closeButtonSize);
    }
    /**
     * Show panel
     */
    public void show(Marker marker) {
        this.selectedMarker = marker;
        this.visible = true;
        this.animationState = AnimationState.SHOWING;
        this.isAnimating = true;
        this.animationTime = 0f;
        this.showTariffButton = false;

        if (marker.getParkingData() != null &&
            !marker.getParkingData().getTariffs().isEmpty()) {
            this.showTariffButton = true;
        }
    }

    /**
     * Check if tariff button clicked
     */
    public boolean isTariffButtonClicked(float screenX, float screenY) {
        if (!showTariffButton || !isVisibleForInteraction()) {
            return false;
        }

        float currentX = getCurrentX();
        Rectangle currentTariffButton = new Rectangle(
            currentX + width - closeButtonSize - padding/2 - closeButtonSize - 10,
            tariffButton.y,
            tariffButton.width,
            tariffButton.height
        );
        return currentTariffButton.contains(screenX, screenY);
    }

    /**
     * Open tariff popup
     */
    public void openTariffPopup() {
        if (selectedMarker != null && selectedMarker.getParkingData() != null) {
            String parkingName = selectedMarker.getName();
            List<Tariff> tariffs = selectedMarker.getParkingData().getTariffs();
            tariffPopup.show(parkingName, tariffs);
            tariffPopupVisible = true; // ✅ KLJUČNO
        }
    }

    /**
     * Close tariff popup
     */
    public void closeTariffPopup() {
        tariffPopup.hide();
        tariffPopupVisible = false;
    }

    /**
     * Check if tariff popup close button clicked
     */
    public boolean isTariffPopupCloseButtonClicked(float screenX, float screenY) {
        return tariffPopup != null && tariffPopup.isCloseButtonClicked(screenX, screenY);
    }


    /**
     * Check if click is on tariff popup
     */
    public boolean isTariffPopupClicked(float screenX, float screenY) {
        return tariffPopup != null && tariffPopup.contains(screenX, screenY);
    }

    /**
     * Hide panel
     */
    public void hide() {
        if (animationState == AnimationState.HIDDEN || animationState == AnimationState.HIDING) {
            return;
        }
        closeTariffPopup();
        this.animationState = AnimationState.HIDING;
        this.isAnimating = true;
        this.animationTime = 0f;
    }

    /**
     * Immediately hide
     */
    public void hideImmediate() {
        this.visible = false;
        this.selectedMarker = null;
        this.animationState = AnimationState.HIDDEN;
        this.isAnimating = false;
    }

    /**
     * Toggle visibility
     */
    public void toggle(Marker marker) {
        if (selectedMarker == marker && (animationState == AnimationState.SHOWN || animationState == AnimationState.SHOWING)) {
            hide();
        } else {
            show(marker);
        }
    }

    /**
     * Check if close button clicked
     */
    public boolean isCloseButtonClicked(float screenX, float screenY) {
        float currentX = getCurrentX();
        Rectangle currentCloseButton = new Rectangle(
            currentX + width - closeButtonSize - padding/2,
            closeButton.y,
            closeButton.width,
            closeButton.height
        );
        return currentCloseButton.contains(screenX, screenY);
    }

    /**
     * Check if point is inside panel
     */
    public boolean contains(float screenX, float screenY) {
        if (!isVisibleForInteraction()) {
            return false;
        }

        float currentX = getCurrentX();
        return screenX >= currentX && screenX <= currentX + width &&
            screenY >= y && screenY <= y + height;
    }

    /**
     * Check if panel is ready for interaction
     */
    private boolean isVisibleForInteraction() {
        return animationState == AnimationState.SHOWING ||
            animationState == AnimationState.SHOWN;
    }

    /**
     * Draw X for close button - CRNA NA BELOJ POZADINI
     */
    private void drawX(float x, float y, float size, float alpha) {
        shapeRenderer.setColor(0.2f, 0.2f, 0.2f, alpha);
        shapeRenderer.rectLine(
            x + size * 0.2f, y + size * 0.2f,
            x + size * 0.8f, y + size * 0.8f,
            2f
        );
        shapeRenderer.rectLine(
            x + size * 0.8f, y + size * 0.2f,
            x + size * 0.2f, y + size * 0.8f,
            2f
        );
    }

    /**
     * Draw icon
     */
    private void drawIcon(Texture icon, float x, float y, float alpha, float size) {
        if (icon != null) {
            spriteBatch.setColor(1f, 1f, 1f, alpha);
            float iconY = y - size/2;
            spriteBatch.draw(icon, x, iconY, size, size);
        }
    }

    /**
     * Set marker textures from MapScreen
     */
    public void setMarkerTextures(Texture free, Texture partial, Texture full, Texture unknown) {
        if (markerFreeTexture != null && markerFreeTexture != free) markerFreeTexture.dispose();
        if (markerPartialTexture != null && markerPartialTexture != partial) markerPartialTexture.dispose();
        if (markerFullTexture != null && markerFullTexture != full) markerFullTexture.dispose();
        if (markerUnknownTexture != null && markerUnknownTexture != unknown) markerUnknownTexture.dispose();

        markerFreeTexture = free;
        markerPartialTexture = partial;
        markerFullTexture = full;
        markerUnknownTexture = unknown;
    }

    /**
     * MAIN RENDER METHOD - White background, smaller fonts
     */
    public void render() {
        if (animationState == AnimationState.HIDDEN || selectedMarker == null || font == null) {
            return;
        }

        float currentX = getCurrentX();
        float alpha = getCurrentAlpha();

        // Render background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(backgroundColor.r, backgroundColor.g, backgroundColor.b, backgroundColor.a * alpha);
        shapeRenderer.rect(currentX, y, width, height);
        shapeRenderer.end();

        // Calculate positions
        float contentStartX = currentX + padding;
        float contentWidth = width - padding * 2;
        float currentY = y + height - padding - titleTopPadding;

        // Draw HEADER with MARKER ICON and title
        spriteBatch.begin();

        // Draw MARKER ICON based on marker state
        float iconX = contentStartX;
        float textX = contentStartX;

        Texture markerTexture = getMarkerTextureForState(selectedMarker.getState());
        if (markerTexture != null) {
            drawIcon(markerTexture, iconX, currentY - 5, alpha, headerIconSize);
            textX = iconX + headerIconSize + headerIconMargin;
        }

        // Draw title with SMALLER font
        font.setColor(titleColor.r, titleColor.g, titleColor.b, alpha);
        font.getData().setScale(titleFontScale);
        font.draw(spriteBatch, selectedMarker.getName(), textX, currentY);

        spriteBatch.end();

        // Draw divider line under header
        currentY -= 20;
        drawDivider(contentStartX, currentY, contentWidth, alpha);
        currentY -= sectionSpacing;

        // SECTION 1: BASIC INFO
        drawSectionLabel("STATUS", contentStartX, currentY, contentWidth, alpha);
        currentY -= lineHeight;

        Color statusColor = getStatusColor(selectedMarker.getState());
        String statusText = getStatusText(selectedMarker.getState());
        drawLine("STATUS", statusText, contentStartX, currentY, contentWidth, alpha,
            labelColor, statusColor);
        currentY -= lineHeight;

        drawLine("TIP", formatMarkerType(selectedMarker.getType()),
            contentStartX, currentY, contentWidth, alpha, labelColor, valueColor);
        currentY -= lineHeight;

        drawLine("UKUPNO MESTA", String.valueOf(selectedMarker.getTotalSpots()),
            contentStartX, currentY, contentWidth, alpha, labelColor, valueColor);
        currentY -= lineHeight;

        drawLine("SLOBODNO", String.valueOf(selectedMarker.getAvailableSpots()),
            contentStartX, currentY, contentWidth, alpha, labelColor, statusColor);
        currentY -= lineHeight;

        if (selectedMarker.getTotalSpots() > 0) {
            float occupancy = selectedMarker.getOccupancyPercentage();
            Color occupancyColor = getOccupancyColor(occupancy);
            drawLine("ZAUZEĆE", String.format("%.0f %%", occupancy),
                contentStartX, currentY, contentWidth, alpha, labelColor, occupancyColor);
            currentY -= lineHeight;
        }

        // Divider
        currentY -= 10;
        drawDivider(contentStartX, currentY, contentWidth, alpha);
        currentY -= sectionSpacing;

        // SECTION 3: LOCATION
        drawSectionLabel("LOKACIJA", contentStartX, currentY, contentWidth, alpha);
        currentY -= lineHeight;

        drawLine("LAT", String.format("%.6f", selectedMarker.getPosition().lat),
            contentStartX, currentY, contentWidth, alpha, labelColor, valueColor);
        currentY -= lineHeight;

        drawLine("LNG", String.format("%.6f", selectedMarker.getPosition().lng),
            contentStartX, currentY, contentWidth, alpha, labelColor, valueColor);

        if (showTariffButton && isVisibleForInteraction()) {
            // Draw tariff button
            float tariffBtnX = currentX + width - closeButtonSize - padding/2 - closeButtonSize - 10;
            float tariffBtnY = y + height - closeButtonSize - padding/2;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.3f, 0.5f, 0.8f, alpha);
            shapeRenderer.rect(tariffBtnX, tariffBtnY, closeButtonSize, closeButtonSize);
            shapeRenderer.end();

            // Draw "T" inside tariff button
            spriteBatch.begin();
            font.setColor(1f, 1f, 1f, alpha);
            font.getData().setScale(0.5f);

            String tariffText = "T";
            glyphLayout.setText(font, tariffText);
            float textXpos = tariffBtnX + (closeButtonSize - glyphLayout.width) / 2;
            float textYpos = tariffBtnY + (closeButtonSize + glyphLayout.height) / 2;
            font.draw(spriteBatch, tariffText, textXpos, textYpos);

            font.getData().setScale(1.0f);
            spriteBatch.end();
        }

        // Render tariff popup if visible
        if (tariffPopup.isVisible()) {
            tariffPopup.render();
        }
        // Draw close button
        if (isVisibleForInteraction()) {
            float closeBtnX = currentX + width - closeButtonSize - padding/2;
            float closeBtnY = y + height - closeButtonSize - padding/2;

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.8f, 0.2f, 0.2f, alpha);
            shapeRenderer.rect(closeBtnX, closeBtnY, closeButtonSize, closeButtonSize);
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            drawX(closeBtnX, closeBtnY, closeButtonSize, alpha);
            shapeRenderer.end();
        }
    }

    /**
     * Draw a section label
     */
    private void drawSectionLabel(String text, float x, float y, float width, float alpha) {
        spriteBatch.begin();
        font.setColor(titleColor.r, titleColor.g, titleColor.b, alpha * 0.9f);
        font.getData().setScale(sectionFontScale);
        font.draw(spriteBatch, text, x, y);
        font.getData().setScale(1.0f);
        spriteBatch.end();
    }

    /**
     * Draw a line with label and value
     */
    private void drawLine(String label, String value, float x, float y,
                          float width, float alpha, Color labelColor, Color valueColor) {
        spriteBatch.begin();
        font.getData().setScale(detailFontScale);

        // Draw label (left side)
        font.setColor(labelColor.r, labelColor.g, labelColor.b, alpha);
        font.draw(spriteBatch, label, x, y);

        // Draw value (right aligned)
        font.setColor(valueColor.r, valueColor.g, valueColor.b, alpha);

        // Calculate text width for right alignment
        glyphLayout.setText(font, value);
        float valueWidth = glyphLayout.width;
        font.draw(spriteBatch, value, x + width - valueWidth, y);

        font.getData().setScale(1.0f);
        spriteBatch.end();
    }

    /**
     * Draw a divider line
     */
    private void drawDivider(float x, float y, float width, float alpha) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(dividerColor.r, dividerColor.g, dividerColor.b, dividerColor.a * alpha);
        shapeRenderer.rect(x, y, width, dividerHeight);
        shapeRenderer.end();
    }

    /**
     * Get status color
     */
    private Color getStatusColor(Marker.MarkerState state) {
        switch (state) {
            case FREE: return statusFreeColor;
            case PARTIAL: return statusPartialColor;
            case FULL: return statusFullColor;
            case UNKNOWN: return statusUnknownColor;
            default: return valueColor;
        }
    }

    /**
     * Get status text
     */
    private String getStatusText(Marker.MarkerState state) {
        switch (state) {
            case FREE: return "SLOBODNO";
            case PARTIAL: return "DELIMIČNO";
            case FULL: return "PUNO";
            case UNKNOWN: return "NEPOZNATO";
            default: return state.name();
        }
    }

    /**
     * Format marker type
     */
    private String formatMarkerType(Marker.MarkerType type) {
        switch (type) {
            case PARKING_LOT: return "Parking lot";
            case GARAGE: return "GARAŽA";
            case STREET_PARKING: return "Ulično parkiranje";
            default: return type.name().replace("_", " ");
        }
    }

    /**
     * Get occupancy color
     */
    private Color getOccupancyColor(float occupancy) {
        if (occupancy < 30) return statusFreeColor;
        if (occupancy < 70) return statusPartialColor;
        return statusFullColor;
    }

    /**
     * Dispose resources
     */
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (spriteBatch != null) spriteBatch.dispose();
        if (font != null) font.dispose();
        if (headerIcon != null) headerIcon.dispose();

        // Dispose marker textures
        if (markerFreeTexture != null) markerFreeTexture.dispose();
        if (markerPartialTexture != null) markerPartialTexture.dispose();
        if (markerFullTexture != null) markerFullTexture.dispose();
        if (markerUnknownTexture != null) markerUnknownTexture.dispose();

        // Dispose tariff popup
        if (tariffPopup != null) tariffPopup.dispose();
    }

    // Getters
    public boolean isVisible() {
        return visible || animationState != AnimationState.HIDDEN;
    }

    public boolean isFullyVisible() {
        return animationState == AnimationState.SHOWN;
    }

    public boolean isAnimating() {
        return isAnimating;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public Marker getSelectedMarker() {
        return selectedMarker;
    }

    public void setAnimationDuration(float duration) {
        this.animationDuration = duration;
    }

    public boolean isTariffPopupVisible() {
        return tariffPopup != null && tariffPopup.isVisible();
    }

}

