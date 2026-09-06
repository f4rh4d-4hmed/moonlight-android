package com.limelight.binding.input.virtual_controller;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.limelight.R;
import com.limelight.nvstream.input.MouseButtonPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * Manager and lifecycle controller for the custom WASD & Virtual Mouse input overlay.
 */
public class WasdMouseController implements VirtualControllerContainer {

    private final MouseInputListener mouseListener;
    private final KeyboardInputListener keyboardListener;
    private final Context context;
    private final Handler handler;
    private FrameLayout frame_layout = null;

    private VirtualController.ControllerMode currentMode = VirtualController.ControllerMode.Active;
    private Button buttonConfigure = null;
    private List<VirtualControllerElement> elements = new ArrayList<>();

    public WasdMouseController(MouseInputListener mouseListener,
                               KeyboardInputListener keyboardListener,
                               FrameLayout layout,
                               final Context context) {
        this.mouseListener = mouseListener;
        this.keyboardListener = keyboardListener;
        this.frame_layout = layout;
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());

        buttonConfigure = new Button(context);
        buttonConfigure.setAlpha(0.35f);
        buttonConfigure.setFocusable(false);
        buttonConfigure.setBackgroundResource(R.drawable.ic_settings);
        buttonConfigure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message;
                if (currentMode == VirtualController.ControllerMode.Active) {
                    currentMode = VirtualController.ControllerMode.MoveButtons;
                    message = "WASD/Mouse Config Mode: Move Elements (Tap element for options)";
                } else if (currentMode == VirtualController.ControllerMode.MoveButtons) {
                    currentMode = VirtualController.ControllerMode.ResizeButtons;
                    message = "WASD/Mouse Config Mode: Resize Elements (Drag corner or pinch)";
                } else {
                    currentMode = VirtualController.ControllerMode.Active;
                    WasdMouseConfigurationLoader.saveProfile(WasdMouseController.this, context);
                    message = "Exiting WASD/Mouse Config Mode (Layout Saved)";
                }

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                buttonConfigure.invalidate();
                for (VirtualControllerElement element : elements) {
                    element.invalidate();
                }
            }
        });
    }

    public MouseInputListener getMouseInputListener() {
        return mouseListener;
    }

    public KeyboardInputListener getKeyboardInputListener() {
        return keyboardListener;
    }

    @Override
    public VirtualController.ControllerMode getControllerMode() {
        return currentMode;
    }

    @Override
    public Handler getHandler() {
        return handler;
    }

    @Override
    public List<VirtualControllerElement> getElements() {
        return elements;
    }

    public void addElement(final VirtualControllerElement element, int x, int y, int width, int height) {
        elements.add(element);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
        layoutParams.setMargins(x, y, 0, 0);
        frame_layout.addView(element, layoutParams);

        // Allow opening configuration menu on click during edit modes
        element.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (currentMode != VirtualController.ControllerMode.Active) {
                    showElementConfigDialog(element);
                    return true;
                }
                return false;
            }
        });
    }

    public void showElementConfigDialog(final VirtualControllerElement element) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        if (element instanceof WasdStick) {
            final WasdStick stick = (WasdStick) element;
            builder.setTitle("Configure Joystick");
            CharSequence[] options = new CharSequence[] {
                    "Map to WASD Keys",
                    "Map to Arrow Keys",
                    "Map to IJKL Keys",
                    "Scale: 75%",
                    "Scale: 100% (Default)",
                    "Scale: 125%",
                    "Scale: 150%",
                    "Hide Joystick"
            };
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DisplayMetrics dm = context.getResources().getDisplayMetrics();
                    int baseSize = (int) (dm.heightPixels * 0.40f);
                    switch (which) {
                        case 0:
                            stick.setMappings(KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_D);
                            Toast.makeText(context, "Mapped to WASD", Toast.LENGTH_SHORT).show();
                            break;
                        case 1:
                            stick.setMappings(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT);
                            Toast.makeText(context, "Mapped to Arrow Keys", Toast.LENGTH_SHORT).show();
                            break;
                        case 2:
                            stick.setMappings(KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L);
                            Toast.makeText(context, "Mapped to IJKL", Toast.LENGTH_SHORT).show();
                            break;
                        case 3:
                            stick.setElementSize((int)(baseSize * 0.75f), (int)(baseSize * 0.75f));
                            break;
                        case 4:
                            stick.setElementSize(baseSize, baseSize);
                            break;
                        case 5:
                            stick.setElementSize((int)(baseSize * 1.25f), (int)(baseSize * 1.25f));
                            break;
                        case 6:
                            stick.setElementSize((int)(baseSize * 1.50f), (int)(baseSize * 1.50f));
                            break;
                        case 7:
                            stick.setVisibility(View.GONE);
                            break;
                    }
                    stick.invalidate();
                }
            });
        } else if (element instanceof VirtualMouseButton) {
            final VirtualMouseButton btn = (VirtualMouseButton) element;
            builder.setTitle("Configure " + btn.getDisplayLabel());
            CharSequence[] options = new CharSequence[] {
                    "Bind: Left Click",
                    "Bind: Right Click",
                    "Bind: Middle Click",
                    "Bind: Space (Jump)",
                    "Bind: Shift (Sprint)",
                    "Bind: Ctrl (Crouch)",
                    "Bind: E (Interact)",
                    "Bind: R (Reload)",
                    "Bind: F (Use)",
                    "Bind: Esc",
                    "Bind: Tab",
                    btn.isLockModeEnabled() ? "Disable Lock Mode" : "Enable Lock Mode (Tap to toggle hold)",
                    "Scale: 75%",
                    "Scale: 100% (Default)",
                    "Scale: 125%",
                    "Hide Button"
            };
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DisplayMetrics dm = context.getResources().getDisplayMetrics();
                    int baseW = (int) (dm.widthPixels * 0.12f);
                    int baseH = (int) (dm.heightPixels * 0.18f);
                    switch (which) {
                        case 0: btn.setMouseButton(MouseButtonPacket.BUTTON_LEFT, "L-Click"); break;
                        case 1: btn.setMouseButton(MouseButtonPacket.BUTTON_RIGHT, "R-Click"); break;
                        case 2: btn.setMouseButton(MouseButtonPacket.BUTTON_MIDDLE, "M-Click"); break;
                        case 3: btn.setKeyMapping(KeyEvent.KEYCODE_SPACE, "SPACE"); break;
                        case 4: btn.setKeyMapping(KeyEvent.KEYCODE_SHIFT_LEFT, "SHIFT"); break;
                        case 5: btn.setKeyMapping(KeyEvent.KEYCODE_CTRL_LEFT, "CTRL"); break;
                        case 6: btn.setKeyMapping(KeyEvent.KEYCODE_E, "E"); break;
                        case 7: btn.setKeyMapping(KeyEvent.KEYCODE_R, "R"); break;
                        case 8: btn.setKeyMapping(KeyEvent.KEYCODE_F, "F"); break;
                        case 9: btn.setKeyMapping(KeyEvent.KEYCODE_ESCAPE, "ESC"); break;
                        case 10: btn.setKeyMapping(KeyEvent.KEYCODE_TAB, "TAB"); break;
                        case 11:
                            btn.setLockModeEnabled(!btn.isLockModeEnabled());
                            Toast.makeText(context, btn.isLockModeEnabled() ? "Lock Mode Enabled" : "Lock Mode Disabled", Toast.LENGTH_SHORT).show();
                            break;
                        case 12: btn.setElementSize((int)(baseW * 0.75f), (int)(baseH * 0.75f)); break;
                        case 13: btn.setElementSize(baseW, baseH); break;
                        case 14: btn.setElementSize((int)(baseW * 1.25f), (int)(baseH * 1.25f)); break;
                        case 15: btn.setVisibility(View.GONE); break;
                    }
                    btn.invalidate();
                }
            });
        } else if (element instanceof VirtualMousePad) {
            final VirtualMousePad pad = (VirtualMousePad) element;
            builder.setTitle("Configure Mouse Pad");
            CharSequence[] options = new CharSequence[] {
                    "Sensitivity: 0.5x (Slow)",
                    "Sensitivity: 0.75x",
                    "Sensitivity: 1.0x (Normal)",
                    "Sensitivity: 1.5x (Fast)",
                    "Sensitivity: 2.0x (Very Fast)",
                    "Scale: 75%",
                    "Scale: 100% (Default)",
                    "Scale: 125%",
                    "Hide Mouse Pad"
            };
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DisplayMetrics dm = context.getResources().getDisplayMetrics();
                    int baseW = (int) (dm.widthPixels * 0.42f);
                    int baseH = (int) (dm.heightPixels * 0.50f);
                    switch (which) {
                        case 0: pad.setSensitivity(0.5f); break;
                        case 1: pad.setSensitivity(0.75f); break;
                        case 2: pad.setSensitivity(1.0f); break;
                        case 3: pad.setSensitivity(1.5f); break;
                        case 4: pad.setSensitivity(2.0f); break;
                        case 5: pad.setElementSize((int)(baseW * 0.75f), (int)(baseH * 0.75f)); break;
                        case 6: pad.setElementSize(baseW, baseH); break;
                        case 7: pad.setElementSize((int)(baseW * 1.25f), (int)(baseH * 1.25f)); break;
                        case 8: pad.setVisibility(View.GONE); break;
                    }
                    pad.invalidate();
                }
            });
        }

        builder.setNegativeButton("Close", null);
        builder.show();
    }

    public void removeElements() {
        for (VirtualControllerElement element : elements) {
            frame_layout.removeView(element);
        }
        elements.clear();
        frame_layout.removeView(buttonConfigure);
    }

    public void setOpacity(int opacity) {
        for (VirtualControllerElement element : elements) {
            element.setOpacity(opacity);
        }
    }

    public void hide() {
        for (VirtualControllerElement element : elements) {
            element.setVisibility(View.INVISIBLE);
        }
        buttonConfigure.setVisibility(View.INVISIBLE);
    }

    public void show() {
        for (VirtualControllerElement element : elements) {
            element.setVisibility(View.VISIBLE);
        }
        buttonConfigure.setVisibility(View.VISIBLE);
    }

    public void refreshLayout() {
        removeElements();

        DisplayMetrics screen = context.getResources().getDisplayMetrics();
        int buttonSize = (int)(screen.heightPixels * 0.06f);

        // Position the WASD/Mouse configure button in the top-right corner
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.rightMargin = 15;
        params.topMargin = 15;
        frame_layout.addView(buttonConfigure, params);

        // Create default layout
        WasdMouseConfigurationLoader.createDefaultLayout(this, context);

        // Load custom preferences onto layout
        WasdMouseConfigurationLoader.loadFromPreferences(this, context);
    }
}
