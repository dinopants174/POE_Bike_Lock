package com.tonydicola.bletest.app;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.text.TextUtils;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends Activity {

    private static Button back_button;
    private static TextView passcode_text;
    private static String passcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //set layout
        setContentView(R.layout.settings_activity);

        //Init back button
        back_button = (Button) findViewById(R.id.settings_button);
        back_button.setOnClickListener(back_handler);
        passcode_text = (TextView) findViewById(R.id.passcode);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        passcode = sharedPref.getString("Passcode", "");
        passcode_text.setText(passcode);

        sharedPref.registerOnSharedPreferenceChangeListener(
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    public void onSharedPreferenceChanged(
                            SharedPreferences prefs, String key) {
                        passcode = prefs.getString(key, "");
                        passcode_text.setText(passcode);
                    }
                }
        );
    }

    //On click listener for back button
    View.OnClickListener back_handler = new View.OnClickListener(){
        public void onClick(View v){
            Intent i = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(i);
        }
    };

    //Create preference Fragment class
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_general);
        }
    }
}
