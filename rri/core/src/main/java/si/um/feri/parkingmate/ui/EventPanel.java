package si.um.feri.parkingmate.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;

import si.um.feri.parkingmate.model.Event;

/**
 * UI panel for displaying EVENT information on the map.
 * Similar behavior and animation as InfoPanel, but simplified.
 */
public class EventPanel {

    private float x, y;
    private float width, height;

    private boolean visible;
    private boolean isAnimating;

    private Event selectedEvent;

    private ShapeRenderer shapeRenderer;
    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private GlyphLayout glyphLayout;

    // Animation
    private float animationTime = 0f;
    private float animationDuration = 0.3f;
    private AnimationState animationState = AnimationState.HIDDEN;

    // Layout
    private float padding = 18f;
    private float lineHeight = 22f;

    // Close button
    private Rectangle closeButton;
    private float closeButtonSize = 20f;

    // Colors
    private Color backgroundColor = new Color(0f, 0f, 0f, 0.88f);
    private Color titleColor = Color.WHITE;
    private Color labelColor = new Color(0.75f, 0.75f, 0.75f, 1f);
    private Color valueColor = Color.WHITE;

    private enum AnimationState {
        SHOWING,
        HIDING,
        SHOWN,
        HIDDEN
    }

    public EventPanel() {
        shapeRenderer = new ShapeRenderer();
        spriteBatch = new SpriteBatch();
        glyphLayout = new GlyphLayout();
        closeButton = new Rectangle();
    }

    /* ================= SETUP ================= */

    public void setFont(BitmapFont font) {
        this.font = font;
    }

    public void setBounds(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        closeButton.set(
            x + width - closeButtonSize - 10,
            y + height - closeButtonSize - 10,
            closeButtonSize,
            closeButtonSize
        );
    }

    public void setAnimationDuration(float duration) {
        this.animationDuration = duration;
    }

    /* ================= VISIBILITY ================= */

    public void show(Event event) {
        this.selectedEvent = event;
        this.visible = true;
        this.isAnimating = true;
        this.animationState = AnimationState.SHOWING;
        this.animationTime = 0f;
    }

    public void hide() {
        if (!visible) return;
        this.isAnimating = true;
        this.animationState = AnimationState.HIDING;
        this.animationTime = 0f;
    }

    public boolean isVisible() {
        return visible || animationState != AnimationState.HIDDEN;
    }

    public boolean isCloseButtonClicked(float screenX, float screenY) {
        float cx = getCurrentX();
        Rectangle btn = new Rectangle(
            cx + width - closeButtonSize - 10,
            closeButton.y,
            closeButton.width,
            closeButton.height
        );
        return btn.contains(screenX, screenY);
    }

    public boolean contains(float screenX, float screenY) {
        float cx = getCurrentX();
        return screenX >= cx && screenX <= cx + width &&
            screenY >= y && screenY <= y + height;
    }

    /* ================= UPDATE ================= */

    public void update(float delta) {
        if (!isAnimating) return;

        animationTime += delta;
        float p = Math.min(1f, animationTime / animationDuration);

        if (animationState == AnimationState.SHOWING && p >= 1f) {
            animationState = AnimationState.SHOWN;
            isAnimating = false;
        }

        if (animationState == AnimationState.HIDING && p >= 1f) {
            animationState = AnimationState.HIDDEN;
            isAnimating = false;
            visible = false;
            selectedEvent = null;
        }
    }

    private float getCurrentX() {
        float p = Math.min(1f, animationTime / animationDuration);

        if (animationState == AnimationState.SHOWING) {
            return (x + width) + (x - (x + width)) * Interpolation.smooth.apply(p);
        }

        if (animationState == AnimationState.HIDING) {
            return x + (width * Interpolation.smooth.apply(p));
        }

        return x;
    }

    /* ================= RENDER ================= */

    public void render() {
        if (animationState == AnimationState.HIDDEN || selectedEvent == null || font == null) return;

        float cx = getCurrentX();

        // Background
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(backgroundColor);
        shapeRenderer.rect(cx, y, width, height);
        shapeRenderer.end();

        float curY = y + height - padding;

        spriteBatch.begin();

        // TITLE
        font.getData().setScale(0.55f);
        font.setColor(titleColor);
        font.draw(spriteBatch, "EVENT", cx + padding, curY);
        curY -= 35;

        font.getData().setScale(0.38f);

        // TYPE
        drawLine("TYPE", selectedEvent.getEventType(), cx, curY);
        curY -= lineHeight;

        // MESSAGE
        drawMultiline(
            "MESSAGE",
            selectedEvent.getMessage(),
            cx,
            curY
        );
        curY -= 80;

        // TIME
        drawLine(
            "TIME",
            selectedEvent.getTimestamp().toString(),
            cx,
            curY
        );

        // CLOSE BUTTON
        font.getData().setScale(0.45f);
        font.draw(spriteBatch, "X",
            cx + width - closeButtonSize,
            y + height - 8
        );

        spriteBatch.end();
    }

    private void drawLine(String label, String value, float cx, float y) {
        font.setColor(labelColor);
        font.draw(spriteBatch, label, cx + padding, y);

        glyphLayout.setText(font, value);
        font.setColor(valueColor);
        font.draw(
            spriteBatch,
            value,
            cx + width - padding - glyphLayout.width,
            y
        );
    }

    private void drawMultiline(String label, String value, float cx, float y) {
        font.setColor(labelColor);
        font.draw(spriteBatch, label, cx + padding, y);
        y -= 18;

        font.setColor(valueColor);
        font.draw(
            spriteBatch,
            value,
            cx + padding,
            y,
            width - padding * 2,
            com.badlogic.gdx.utils.Align.left,
            true
        );
    }

    /* ================= DISPOSE ================= */

    public void dispose() {
        shapeRenderer.dispose();
        spriteBatch.dispose();
    }
}
