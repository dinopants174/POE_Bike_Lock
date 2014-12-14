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
    String ble_state;
    Boolean mode;
    Boolean lock_state;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.i("widget","onUpdate!");
        for (int appWidgetId : appWidgetIds) {
            //updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
            SharedPreferences pref = context.getSharedPreferences("ble", Activity.MODE_PRIVATE);
            ble_state = pref.getString("ble_state", "wrong pref name");
            mode = pref.getBoolean("mode", true);
            lock_state = pref.getBoolean("lock_state", true);
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.bletestapp);
            Log.i("widget", ble_state);

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
            remoteViews.setOnClickPendingIntent(R.id.widget_lock, getPendingSelfIntent(context, "lock"));
            remoteViews.setOnClickPendingIntent(R.id.widget_mode, getPendingSelfIntent(context, "mode"));
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        }
    }

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

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences pref =  context.getSharedPreferences("ble", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.bletestapp);
        boolean lock_state;
        boolean mode;
        if ("lock".equals(intent.getAction())) {
            lock_state = MainActivity.toggleLock(true);
            editor.putBoolean("lock_state",lock_state);
            editor.apply();
            if (lock_state) {
                Log.i("widget","locked");
                remoteViews.setImageViewResource(R.id.widget_lock, R.drawable.lock_icon);
            }
            else {
                Log.i("widget","unlocked");
                remoteViews.setImageViewResource(R.id.widget_lock, R.drawable.unlock_icon);
            }
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
        ComponentName component_name = new ComponentName(context,bletestapp.class);
        AppWidgetManager.getInstance(context).updateAppWidget(component_name, remoteViews);
        super.onReceive(context, intent);
    }
}


