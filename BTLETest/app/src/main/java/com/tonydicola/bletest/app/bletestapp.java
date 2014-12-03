package com.tonydicola.bletest.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RemoteViews;



/**
 * Implementation of App Widget functionality.
 */
public class bletestapp extends AppWidgetProvider {
    String ble_state;
    Boolean mode;
    Boolean lock_state;
    ImageButton lock_btn;
    ImageButton mode_btn;

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        Log.i("widget", "0");
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {

            //updateAppWidget(context, appWidgetManager, appWidgetIds[i]);

            Log.i("widget", "1");
            SharedPreferences pref =  context.getSharedPreferences("ble", Activity.MODE_PRIVATE);
            ble_state = pref.getString("ble_state", "wrong pref name");
            mode = pref.getBoolean("mode",true);
            lock_state = pref.getBoolean("lock_state",true);
            Log.i("widget", "2");
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.bletestapp);

            if (lock_state) {
                remoteViews.setImageViewResource(R.id.widget_lock, R.drawable.lock_icon);
            }
            else {
                remoteViews.setImageViewResource(R.id.widget_lock, R.drawable.unlock_icon);
            }
            Log.i("widget", "3");
            if (mode) {
                remoteViews.setImageViewResource(R.id.widget_mode, R.drawable.on_icon);
            }
            else {
                remoteViews.setImageViewResource(R.id.widget_mode, R.drawable.off_icon);
            }
            Log.i("widget", "4");
            if(ble_state.equals("connected")) {
                remoteViews.setImageViewResource(R.id.background, R.drawable.background_connected);
            }
            else {
                remoteViews.setImageViewResource(R.id.background, R.drawable.background_disconnected);
            }
            Log.i("widget", "onUpdate");
            appWidgetManager.updateAppWidget(appWidgetIds[i],remoteViews);

        }

    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        //CharSequence widgetText = context.getString(R.string.appwidget_text);
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.bletestapp);
       // views.setTextViewText(R.id.appwidget_text, widgetText);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}


