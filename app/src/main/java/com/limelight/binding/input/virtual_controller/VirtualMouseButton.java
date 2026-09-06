package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.limelight.nvstream.input.MouseButtonPacket;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * On-screen button representing a mouse click (Left, Right, Middle) or remapped keyboard key.
 * Supports:
 * - Instant tap (quick click with minimum hold duration to prevent dropped host packets)
 * - Sustained press-and-hold immune to touch jitter
 * - Toggle lock mode (tap once locks down, tap again releases)
 * - Full remapping to mouse buttons or keyboard keys
 */
public class VirtualMouseButton extends VirtualControllerElement {

    public static final int TYPE_MOUSE = 0;
    public static final int TYPE_KEY = 1;

    private static final long MIN_CLICK_HOLD_MS = 60;

    private MouseInputListener mouseListener;
    private KeyboardInputListener keyboardListener;

    private int bindingType = TYPE_MOUSE;
    private byte mouseButton = MouseButtonPacket.BUTTON_LEFT;
    private int keyCode = KeyEvent.KEYCODE_SPACE;
    private String customLabel = null;

    private boolean lockModeEnabled = false;
    private boolean isLockedDown = false;
    private boolean isPhysicallyPressed = false;
    private long downTimeMs = 0;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private final Runnable delayedReleaseRunnable = new Runnable() {
        @Override
        public void run() {
            sendUp();
            invalidate();
        }
    };

    public VirtualMouseButton(VirtualControllerContainer controller, Context context, int elementId, byte defaultButton, String label) {
        super(controller, context, elementId);
        this.bindingType = TYPE_MOUSE;
        this.mouseButton = defaultButton;
        this.customLabel = label;
    }

    public void setMouseInputListener(MouseInputListener listener) {
        this.mouseListener = listener;
    }

    public void setKeyboardInputListener(KeyboardInputListener listener) {
        this.keyboardListener = listener;
    }

    public void setMouseButton(byte button, String label) {
        releaseIfDown();
        this.bindingType = TYPE_MOUSE;
        this.mouseButton = button;
        this.customLabel = label;
        invalidate();
    }

    public void setKeyMapping(int keyCode, String label) {
        releaseIfDown();
        this.bindingType = TYPE_KEY;
        this.keyCode = keyCode;
        this.customLabel = label;
        invalidate();
    }

    public int getBindingType() {
        return bindingType;
    }

    public byte getMouseButton() {
        return mouseButton;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public boolean isLockModeEnabled() {
        return lockModeEnabled;
    }

    public void setLockModeEnabled(boolean enabled) {
        if (!enabled && isLockedDown) {
            isLockedDown = false;
            sendUp();
        }
        this.lockModeEnabled = enabled;
        invalidate();
    }

    public boolean isLockedDown() {
        return isLockedDown;
    }

    public void releaseIfDown() {
        virtualController.getHandler().removeCallbacks(delayedReleaseRunnable);
        if (isLockedDown || isPhysicallyPressed) {
            isLockedDown = false;
            isPhysicallyPressed = false;
            sendUp();
            invalidate();
        }
    }

    private void sendDown() {
        if (bindingType == TYPE_MOUSE) {
            if (mouseListener != null) {
                mouseListener.sendMouseButtonDown(mouseButton);
            }
        } else {
            if (keyboardListener != null) {
                keyboardListener.sendKeyboardEvent(true, keyCode);
            }
        }
    }

    private void sendUp() {
        if (bindingType == TYPE_MOUSE) {
            if (mouseListener != null) {
                mouseListener.sendMouseButtonUp(mouseButton);
            }
        } else {
            if (keyboardListener != null) {
                keyboardListener.sendKeyboardEvent(false, keyCode);
            }
        }
    }

    public String getDisplayLabel() {
        if (customLabel != null && !customLabel.isEmpty()) {
            return customLabel;
        }
        if (bindingType == TYPE_MOUSE) {
            switch (mouseButton) {
                case MouseButtonPacket.BUTTON_LEFT: return "L-Click";
                case MouseButtonPacket.BUTTON_RIGHT: return "R-Click";
                case MouseButtonPacket.BUTTON_MIDDLE: return "M-Click";
                case MouseButtonPacket.BUTTON_X1: return "Mouse 4";
                case MouseButtonPacket.BUTTON_X2: return "Mouse 5";
                default: return "Click";
            }
        } else {
            String name = KeyEvent.keyCodeToString(keyCode);
            if (name != null && name.startsWith("KEYCODE_")) {
                return name.substring(8);
            }
            return "Key " + keyCode;
        }
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);

        float strokeWidth = getDefaultStrokeWidth();
        paint.setStrokeWidth(strokeWidth);

        rect.set(strokeWidth, strokeWidth, getWidth() - strokeWidth, getHeight() - strokeWidth);
        float cornerRadius = getPercent(getCorrectWidth(), 20);

        boolean isActive = isLockedDown || isPhysicallyPressed;
        int activeColor = isActive ? pressedColor : getDefaultColor();

        // Background fill
        paint.setStyle(Paint.Style.FILL);
        paint.setColor((activeColor & 0x00FFFFFF) | (isActive ? 0x80000000 : 0x28000000));
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        // Outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(activeColor);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        // Lock mode badge indicator
        if (lockModeEnabled) {
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(getPercent(getCorrectWidth(), 16));
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(isLockedDown ? 0xFFFFDD00 : (getDefaultColor() & 0x00FFFFFF) | 0x80000000);
            canvas.drawText(isLockedDown ? "🔒" : "🔓", getWidth() - strokeWidth - 6, strokeWidth + paint.getTextSize() + 2, paint);
        }

        // Main text label
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getPercent(getCorrectWidth(), 22));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(isActive ? pressedColor : getDefaultColor());
        canvas.drawText(getDisplayLabel(), getWidth() / 2f, getHeight() / 2f + paint.getTextSize() * 0.35f, paint);
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                virtualController.getHandler().removeCallbacks(delayedReleaseRunnable);

                if (lockModeEnabled) {
                    if (isLockedDown) {
                        // Unlocking
                        isLockedDown = false;
                        isPhysicallyPressed = false;
                        sendUp();
                    } else {
                        // Locking down
                        isLockedDown = true;
                        isPhysicallyPressed = true;
                        sendDown();
                    }
                } else {
                    // Momentary mode
                    isPhysicallyPressed = true;
                    downTimeMs = SystemClock.uptimeMillis();
                    sendDown();
                }

                invalidate();
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                // Jitter protection: maintain held state during moves
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (lockModeEnabled) {
                    // In lock mode, the button stays locked down until tapped again
                    isPhysicallyPressed = false;
                    invalidate();
                    return true;
                }

                // In momentary mode, release button
                isPhysicallyPressed = false;
                long elapsed = SystemClock.uptimeMillis() - downTimeMs;
                if (elapsed < MIN_CLICK_HOLD_MS) {
                    // Ensure the hold time is at least MIN_CLICK_HOLD_MS
                    virtualController.getHandler().postDelayed(delayedReleaseRunnable, MIN_CLICK_HOLD_MS - elapsed);
                } else {
                    sendUp();
                    invalidate();
                }
                return true;
            }

            default:
                return false;
        }
    }

    @Override
    public JSONObject getConfiguration() throws JSONException {
        JSONObject config = super.getConfiguration();
        config.put("BINDING_TYPE", bindingType);
        config.put("MOUSE_BUTTON", (int) mouseButton);
        config.put("KEY_CODE", keyCode);
        config.put("LOCK_MODE", lockModeEnabled);
        if (customLabel != null) {
            config.put("LABEL", customLabel);
        }
        return config;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        if (configuration.has("BINDING_TYPE")) bindingType = configuration.getInt("BINDING_TYPE");
        if (configuration.has("MOUSE_BUTTON")) mouseButton = (byte) configuration.getInt("MOUSE_BUTTON");
        if (configuration.has("KEY_CODE")) keyCode = configuration.getInt("KEY_CODE");
        if (configuration.has("LOCK_MODE")) lockModeEnabled = configuration.getBoolean("LOCK_MODE");
        if (configuration.has("LABEL")) customLabel = configuration.getString("LABEL");
    }
}
