package com.example.fabibookingvendorsystem;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.NonNull;

/**
 * Handles touch gestures and interaction signals following OOP principles.
 */
public class InputHandler implements View.OnTouchListener {
    private final GestureDetector gestureDetector;
    private final EventManager eventManager;

    public InputHandler(Context context, EventManager eventManager) {
        this.eventManager = eventManager;
        this.gestureDetector = new GestureDetector(context, new GestureListener());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Detect gestures
        boolean handled = gestureDetector.onTouchEvent(event);
        
        // Follow Android best practices for accessibility
        if (event.getAction() == MotionEvent.ACTION_UP) {
            v.performClick();
        }
        
        return handled;
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            EventLogger.log("Single Tap Detected at [" + e.getX() + ", " + e.getY() + "]");
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            eventManager.logAndNotify("Double Tap Detected!");
            return true;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            eventManager.logAndNotify("Long Press Detected!");
            super.onLongPress(e);
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            EventLogger.log("Fling/Swipe Detected");
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }
}
