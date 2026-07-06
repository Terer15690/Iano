package com.example.fabibookingvendorsystem;

import android.util.Log;

/**
 * EventLogger handles static logging across the application.
 * Fixed version of the user-created file.
 */
public class EventLogger {
    private static final String TAG = "FABI_EVENT";

    public static void log(String message) {
        if (message != null) {
            Log.d(TAG, message);
        }
    }
}
