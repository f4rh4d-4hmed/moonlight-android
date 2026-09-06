package com.limelight.binding.input.virtual_controller;

import android.os.Handler;
import java.util.List;

public interface VirtualControllerContainer {
    VirtualController.ControllerMode getControllerMode();
    Handler getHandler();
    List<VirtualControllerElement> getElements();
}
