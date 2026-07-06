package com.example.fabibookingvendorsystem;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

/**
 * EventManager follows OOP principles to centralize logging and user notifications.
 */
public class EventManager {
    private static final String TAG = "FABI_EVENTS";
    private Context context;

    public EventManager(Context context) {
        this.context = context;
    }

    public void logAndNotify(String message) {
        logEvent(message);
        showToast(message);
    }

    public void logEvent(String event) {
        Log.d(TAG, "Event: " + event);
        // In a real production app, this could also save to a local database or remote analytics
    }

    private void showToast(String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
