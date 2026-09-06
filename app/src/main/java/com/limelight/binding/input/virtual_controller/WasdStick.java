package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Virtual analog joystick mapped to keyboard keys (default: W, A, S, D).
 * Divides deflection into 8 directional zones (N, NE, E, SE, S, SW, W, NW).
 * Sends discrete ACTION_DOWN on entering a zone and ACTION_UP on leaving it.
 */
public class WasdStick extends VirtualControllerElement {

    private KeyboardInputListener keyboardListener;

    private int keyUp = KeyEvent.KEYCODE_W;
    private int keyLeft = KeyEvent.KEYCODE_A;
    private int keyDown = KeyEvent.KEYCODE_S;
    private int keyRight = KeyEvent.KEYCODE_D;

    private boolean isUpActive = false;
    private boolean isDownActive = false;
    private boolean isLeftActive = false;
    private boolean isRightActive = false;

    private float stickX = 0;
    private float stickY = 0;
    private boolean isTouching = false;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WasdStick(VirtualControllerContainer controller, Context context) {
        super(controller, context, EID_WASD_STICK);
    }

    public void setKeyboardInputListener(KeyboardInputListener listener) {
        this.keyboardListener = listener;
    }

    public void setMappings(int keyUp, int keyLeft, int keyDown, int keyRight) {
        // Release any currently held keys before changing mapping
        releaseAllKeys();
        this.keyUp = keyUp;
        this.keyLeft = keyLeft;
        this.keyDown = keyDown;
        this.keyRight = keyRight;
        invalidate();
    }

    public int getKeyUp() { return keyUp; }
    public int getKeyLeft() { return keyLeft; }
    public int getKeyDown() { return keyDown; }
    public int getKeyRight() { return keyRight; }

    public void releaseAllKeys() {
        updateDirectionalKeys(false, false, false, false);
    }

    private void updateDirectionalKeys(boolean newUp, boolean newDown, boolean newLeft, boolean newRight) {
        if (keyboardListener == null) {
            isUpActive = newUp;
            isDownActive = newDown;
            isLeftActive = newLeft;
            isRightActive = newRight;
            return;
        }

        if (newUp != isUpActive) {
            keyboardListener.sendKeyboardEvent(newUp, keyUp);
            isUpActive = newUp;
        }
        if (newDown != isDownActive) {
            keyboardListener.sendKeyboardEvent(newDown, keyDown);
            isDownActive = newDown;
        }
        if (newLeft != isLeftActive) {
            keyboardListener.sendKeyboardEvent(newLeft, keyLeft);
            isLeftActive = newLeft;
        }
        if (newRight != isRightActive) {
            keyboardListener.sendKeyboardEvent(newRight, keyRight);
            isRightActive = newRight;
        }
    }

    private static String getKeyLabel(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_W: return "W";
            case KeyEvent.KEYCODE_A: return "A";
            case KeyEvent.KEYCODE_S: return "S";
            case KeyEvent.KEYCODE_D: return "D";
            case KeyEvent.KEYCODE_DPAD_UP: return "▲";
            case KeyEvent.KEYCODE_DPAD_DOWN: return "▼";
            case KeyEvent.KEYCODE_DPAD_LEFT: return "◀";
            case KeyEvent.KEYCODE_DPAD_RIGHT: return "▶";
            default:
                String name = KeyEvent.keyCodeToString(keyCode);
                if (name != null && name.startsWith("KEYCODE_")) {
                    return name.substring(8);
                }
                return "" + keyCode;
        }
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);

        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float outerRadius = Math.min(centerX, centerY) * 0.90f;
        float knobRadius = outerRadius * 0.40f;

        paint.setStrokeWidth(getDefaultStrokeWidth());

        // Draw outer ring
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(getDefaultColor());
        canvas.drawCircle(centerX, centerY, outerRadius, paint);

        // Draw center deadzone circle (faint)
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor((getDefaultColor() & 0x00FFFFFF) | 0x30000000);
        canvas.drawCircle(centerX, centerY, outerRadius * 0.22f, paint);

        // Draw directional labels & active status
        paint.setTextSize(outerRadius * 0.28f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStyle(Paint.Style.FILL);

        // Up
        paint.setColor(isUpActive ? pressedColor : getDefaultColor());
        canvas.drawText(getKeyLabel(keyUp), centerX, centerY - outerRadius * 0.60f + paint.getTextSize() * 0.35f, paint);

        // Down
        paint.setColor(isDownActive ? pressedColor : getDefaultColor());
        canvas.drawText(getKeyLabel(keyDown), centerX, centerY + outerRadius * 0.65f + paint.getTextSize() * 0.35f, paint);

        // Left
        paint.setColor(isLeftActive ? pressedColor : getDefaultColor());
        canvas.drawText(getKeyLabel(keyLeft), centerX - outerRadius * 0.65f, centerY + paint.getTextSize() * 0.35f, paint);

        // Right
        paint.setColor(isRightActive ? pressedColor : getDefaultColor());
        canvas.drawText(getKeyLabel(keyRight), centerX + outerRadius * 0.65f, centerY + paint.getTextSize() * 0.35f, paint);

        // Draw knob
        float currentKnobX = isTouching ? stickX : centerX;
        float currentKnobY = isTouching ? stickY : centerY;

        // Knob body fill
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(isTouching ? (pressedColor & 0x00FFFFFF) | 0x80000000 : (getDefaultColor() & 0x00FFFFFF) | 0x50000000);
        canvas.drawCircle(currentKnobX, currentKnobY, knobRadius, paint);

        // Knob outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(isTouching ? pressedColor : getDefaultColor());
        canvas.drawCircle(currentKnobX, currentKnobY, knobRadius, paint);
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        float centerX = getWidth() / 2f;
        float centerY = getHeight() / 2f;
        float outerRadius = Math.min(centerX, centerY) * 0.90f;
        float maxKnobDistance = outerRadius - (outerRadius * 0.30f);
        float deadzone = outerRadius * 0.22f;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                isTouching = true;
                float touchX = event.getX();
                float touchY = event.getY();

                float dx = touchX - centerX;
                float dy = touchY - centerY;
                float distance = (float) Math.hypot(dx, dy);

                // Position the knob
                if (distance > maxKnobDistance) {
                    stickX = centerX + (dx / distance) * maxKnobDistance;
                    stickY = centerY + (dy / distance) * maxKnobDistance;
                } else {
                    stickX = touchX;
                    stickY = touchY;
                }

                // Check 8-directional zones if outside deadzone
                if (distance >= deadzone) {
                    double angleDeg = Math.toDegrees(Math.atan2(dy, dx));
                    if (angleDeg < 0) {
                        angleDeg += 360.0;
                    }

                    // Up: 202.5° to 337.5° (NW, N, NE)
                    boolean newUp = (angleDeg >= 202.5 && angleDeg <= 337.5);
                    // Down: 22.5° to 157.5° (SE, S, SW)
                    boolean newDown = (angleDeg >= 22.5 && angleDeg <= 157.5);
                    // Right: 292.5° to 360° or 0° to 67.5° (NE, E, SE)
                    boolean newRight = (angleDeg >= 292.5 || angleDeg <= 67.5);
                    // Left: 112.5° to 247.5° (SW, W, NW)
                    boolean newLeft = (angleDeg >= 112.5 && angleDeg <= 247.5);

                    updateDirectionalKeys(newUp, newDown, newLeft, newRight);
                } else {
                    // Inside deadzone
                    updateDirectionalKeys(false, false, false, false);
                }

                invalidate();
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                isTouching = false;
                stickX = centerX;
                stickY = centerY;
                releaseAllKeys();
                invalidate();
                return true;
            }

            default:
                return false;
        }
    }

    @Override
    public JSONObject getConfiguration() throws JSONException {
        JSONObject config = super.getConfiguration();
        config.put("KEY_UP", keyUp);
        config.put("KEY_DOWN", keyDown);
        config.put("KEY_LEFT", keyLeft);
        config.put("KEY_RIGHT", keyRight);
        return config;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        if (configuration.has("KEY_UP")) keyUp = configuration.getInt("KEY_UP");
        if (configuration.has("KEY_DOWN")) keyDown = configuration.getInt("KEY_DOWN");
        if (configuration.has("KEY_LEFT")) keyLeft = configuration.getInt("KEY_LEFT");
        if (configuration.has("KEY_RIGHT")) keyRight = configuration.getInt("KEY_RIGHT");
    }
}
