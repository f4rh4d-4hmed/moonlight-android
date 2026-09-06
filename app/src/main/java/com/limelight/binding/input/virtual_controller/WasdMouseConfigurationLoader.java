package com.limelight.binding.input.virtual_controller;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.DisplayMetrics;
import android.view.View;

import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.preferences.PreferenceConfiguration;

import org.json.JSONException;
import org.json.JSONObject;

public class WasdMouseConfigurationLoader {
    public static final String KBMOUSE_OSC_PREFERENCE = "KBMOUSE_OSC";

    public static void createDefaultLayout(final WasdMouseController controller, final Context context) {
        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        PreferenceConfiguration config = PreferenceConfiguration.readPreferences(context);

        int height = screen.heightPixels;
        int width = screen.widthPixels;

        // 1. Virtual Mouse Pad (added first so buttons sit on top if overlapping)
        if (config.wasdShowMousePad) {
            VirtualMousePad mousePad = new VirtualMousePad(controller, context);
            mousePad.setMouseInputListener(controller.getMouseInputListener());

            int padWidth = (int) (width * 0.42f);
            int padHeight = (int) (height * 0.50f);
            int padX = width - padWidth - (int) (height * 0.05f);
            int padY = (int) (height * 0.18f);

            controller.addElement(mousePad, padX, padY, padWidth, padHeight);
        }

        // 2. WASD Virtual Joystick (left-thumb area)
        if (config.wasdShowJoystick) {
            WasdStick wasdStick = new WasdStick(controller, context);
            wasdStick.setKeyboardInputListener(controller.getKeyboardInputListener());

            int stickSize = (int) (height * 0.40f);
            int stickX = (int) (height * 0.08f);
            int stickY = (int) (height * 0.50f);

            controller.addElement(wasdStick, stickX, stickY, stickSize, stickSize);
        }

        // 3. Mouse Click Buttons (Left, Middle, Right arranged horizontally at bottom right)
        int btnWidth = (int) (width * 0.12f);
        int btnHeight = (int) (height * 0.18f);
        int btnSpacing = (int) (width * 0.015f);
        int btnY = height - btnHeight - (int) (height * 0.06f);

        int rightX = width - btnWidth - (int) (height * 0.05f);
        int middleX = rightX - btnWidth - btnSpacing;
        int leftX = middleX - btnWidth - btnSpacing;

        if (config.wasdShowLeftClick) {
            VirtualMouseButton leftBtn = new VirtualMouseButton(
                    controller, context, VirtualControllerElement.EID_MOUSE_BTN_LEFT,
                    MouseButtonPacket.BUTTON_LEFT, "L-Click");
            leftBtn.setMouseInputListener(controller.getMouseInputListener());
            leftBtn.setKeyboardInputListener(controller.getKeyboardInputListener());
            controller.addElement(leftBtn, leftX, btnY, btnWidth, btnHeight);
        }

        if (config.wasdShowMiddleClick) {
            VirtualMouseButton middleBtn = new VirtualMouseButton(
                    controller, context, VirtualControllerElement.EID_MOUSE_BTN_MIDDLE,
                    MouseButtonPacket.BUTTON_MIDDLE, "M-Click");
            middleBtn.setMouseInputListener(controller.getMouseInputListener());
            middleBtn.setKeyboardInputListener(controller.getKeyboardInputListener());
            controller.addElement(middleBtn, middleX, btnY, btnWidth, btnHeight);
        }

        if (config.wasdShowRightClick) {
            VirtualMouseButton rightBtn = new VirtualMouseButton(
                    controller, context, VirtualControllerElement.EID_MOUSE_BTN_RIGHT,
                    MouseButtonPacket.BUTTON_RIGHT, "R-Click");
            rightBtn.setMouseInputListener(controller.getMouseInputListener());
            rightBtn.setKeyboardInputListener(controller.getKeyboardInputListener());
            controller.addElement(rightBtn, rightX, btnY, btnWidth, btnHeight);
        }

        controller.setOpacity(config.wasdOpacity);
    }

    public static void saveProfile(final WasdMouseController controller, final Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(KBMOUSE_OSC_PREFERENCE, Activity.MODE_PRIVATE).edit();

        for (VirtualControllerElement element : controller.getElements()) {
            String key = "" + element.getElementId();
            try {
                JSONObject config = element.getConfiguration();
                config.put("VISIBLE", element.getVisibility() == View.VISIBLE);
                editor.putString(key, config.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        editor.apply();
    }

    public static void loadFromPreferences(final WasdMouseController controller, final Context context) {
        SharedPreferences pref = context.getSharedPreferences(KBMOUSE_OSC_PREFERENCE, Activity.MODE_PRIVATE);

        for (VirtualControllerElement element : controller.getElements()) {
            String key = "" + element.getElementId();
            String jsonConfig = pref.getString(key, null);
            if (jsonConfig != null) {
                try {
                    JSONObject config = new JSONObject(jsonConfig);
                    element.loadConfiguration(config);
                    if (config.has("VISIBLE")) {
                        element.setVisibility(config.getBoolean("VISIBLE") ? View.VISIBLE : View.GONE);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    pref.edit().remove(key).apply();
                }
            }
        }
    }

    public static void resetToDefaults(final Context context) {
        context.getSharedPreferences(KBMOUSE_OSC_PREFERENCE, Activity.MODE_PRIVATE).edit().clear().apply();
    }
}
