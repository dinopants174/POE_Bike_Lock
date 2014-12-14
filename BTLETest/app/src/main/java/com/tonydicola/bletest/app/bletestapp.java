package com.tonydicola.bletest.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.RemoteViews;



/**
 * Implementation of App Widget functionality.
 */
public class bletestapp extends AppWidgetProvider {
    private String ble_state;
    private Boolean mode;
    private Boolean lock_state;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.i("widget","onUpdate!");

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            // Load SharedPreference with the tag "ble".
            SharedPreferences pref = context.getSharedPreferences("ble", Activity.MODE_PRIVATE);
            // Params: tag and value if the tag not found.
            ble_state = pref.getString("ble_state", "wrong pref name");
            mode = pref.getBoolean("mode", true);
            lock_state = pref.getBoolean("lock_state", true);

            // Widgets access to layout through remoteViews.
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.bletestapp);

            // Change icons.
            if (lock_state) {
                remoteViews.setImageViewResource(R.id.widget_lock, R.drawable.lock_icon);
            } else {
                remoteViews.setImageViewResource(R.id.widget_lock, R.drawable.unlock_icon);
            }
            if (mode) {
                remoteViews.setImageViewResource(R.id.widget_mode, R.drawable.on_icon);
            } else {
                remoteViews.setImageViewResource(R.id.widget_mode, R.drawable.off_icon);
            }
            if (ble_state.equals("connected")) {
                remoteViews.setImageViewResource(R.id.background, R.drawable.background_connected);
            } else {
                remoteViews.setImageViewResource(R.id.background, R.drawable.background_disconnected);
            }

            // Set OnClickListener to buttons.
            // If the buttons are clicked, pendingIntent starts.
            // onReceive receives this intent.
            remoteViews.setOnClickPendingIntent(R.id.widget_lock, getPendingSelfIntent(context, "lock"));
            remoteViews.setOnClickPendingIntent(R.id.widget_mode, getPendingSelfIntent(context, "mode"));

            // Apply all changes.
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

    // Make new pending intent with specific action.
    protected PendingIntent getPendingSelfIntent(Context context, String action) {
        Intent intent = new Intent(context, getClass());
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        super.onEnabled(context);
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        super.onDisabled(context);
    }

    // Receives intents and take actions.
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref =  context.getSharedPreferences("ble", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.bletestapp);
        boolean lock_state;
        boolean mode;

        // If the lock button is clicked.
        if ("lock".equals(intent.getAction())) {
            // Toggle the button of MainActivity.
            lock_state = MainActivity.toggleLock(true);
            // Save lock_state to SharedPreference.
            editor.putBoolean("lock_state",lock_state);
            editor.apply();
            // Change button's icon.
            if (lock_state) {
                Log.i("widget","locked");
                remoteViews.setImageViewResource(R.id.widget_lock, R.drawable.lock_icon);
            }
            else {
                Log.i("widget","unlocked");
                remoteViews.setImageViewResource(R.id.widget_lock, R.drawable.unlock_icon);
            }

        // if the mode button is clicked.
        } else if ("mode".equals(intent.getAction())) {
            mode = MainActivity.toggleMode(true);
            editor.putBoolean("mode",mode);
            editor.apply();
            if (mode) {
                remoteViews.setImageViewResource(R.id.widget_mode, R.drawable.on_icon);
            }
            else {
                remoteViews.setImageViewResource(R.id.widget_mode, R.drawable.off_icon);
            }
        }
        // Call onUpdate.
        ComponentName component_name = new ComponentName(context,bletestapp.class);
        AppWidgetManager.getInstance(context).updateAppWidget(component_name, remoteViews);

        super.onReceive(context, intent);
    }
}


