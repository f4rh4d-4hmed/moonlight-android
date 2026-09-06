package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Touch-driven relative mouse pad area. Translates finger drag movements into
 * relative mouse move packets sent via MouseInputListener.
 */
public class VirtualMousePad extends VirtualControllerElement {

    private MouseInputListener mouseListener;

    private float lastTouchX = 0f;
    private float lastTouchY = 0f;
    private boolean isTracking = false;
    private float sensitivity = 1.0f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    public VirtualMousePad(VirtualControllerContainer controller, Context context) {
        super(controller, context, EID_MOUSE_PAD);
    }

    public void setMouseInputListener(MouseInputListener listener) {
        this.mouseListener = listener;
    }

    public void setSensitivity(float sensitivity) {
        this.sensitivity = Math.max(0.2f, Math.min(3.0f, sensitivity));
    }

    public float getSensitivity() {
        return sensitivity;
    }

    @Override
    protected void onElementDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);

        float strokeWidth = getDefaultStrokeWidth();
        paint.setStrokeWidth(strokeWidth);

        rect.set(strokeWidth, strokeWidth, getWidth() - strokeWidth, getHeight() - strokeWidth);
        float cornerRadius = getPercent(getCorrectWidth(), 8);

        // Fill background (semi-transparent)
        paint.setStyle(Paint.Style.FILL);
        int baseColor = isTracking ? pressedColor : getDefaultColor();
        paint.setColor((baseColor & 0x00FFFFFF) | (isTracking ? 0x30000000 : 0x18000000));
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        // Draw outline
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(baseColor);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);

        // Center trackpad label
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(getPercent(getCorrectWidth(), 10));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor((baseColor & 0x00FFFFFF) | 0x80000000);
        canvas.drawText("MOUSE PAD", getWidth() / 2f, getHeight() / 2f + paint.getTextSize() * 0.35f, paint);
    }

    @Override
    public boolean onElementTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                isTracking = true;
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_MOVE: {
                float currentX = event.getX();
                float currentY = event.getY();

                float dx = (currentX - lastTouchX) * sensitivity;
                float dy = (currentY - lastTouchY) * sensitivity;

                int deltaX = Math.round(dx);
                int deltaY = Math.round(dy);

                if (deltaX != 0 || deltaY != 0) {
                    if (mouseListener != null) {
                        mouseListener.sendMouseMove(deltaX, deltaY);
                    }
                    lastTouchX += deltaX / sensitivity;
                    lastTouchY += deltaY / sensitivity;
                }
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                isTracking = false;
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
        config.put("SENSITIVITY", (double) sensitivity);
        return config;
    }

    @Override
    public void loadConfiguration(JSONObject configuration) throws JSONException {
        super.loadConfiguration(configuration);
        if (configuration.has("SENSITIVITY")) {
            sensitivity = (float) configuration.getDouble("SENSITIVITY");
        }
    }
}
