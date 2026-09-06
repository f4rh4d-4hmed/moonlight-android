package com.limelight.binding.input.virtual_controller;

public interface MouseInputListener {
    void sendMouseMove(int deltaX, int deltaY);
    void sendMouseButtonDown(byte button);
    void sendMouseButtonUp(byte button);
}
