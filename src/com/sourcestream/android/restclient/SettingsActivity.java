package com.sourcestream.android.restclient;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import java.util.prefs.Preferences;

/**
 * Preferences activity.
 */
public class SettingsActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.preferences);

        PreferenceManager.setDefaultValues(SettingsActivity.this, R.xml.preferences, false);

        String timeoutInSeconds = (String)getIntent().getExtras().get("timeoutInSeconds");

        for(int i=0; i < getPreferenceScreen().getPreferenceCount(); i++)
        {
            Preference p = getPreferenceScreen().getPreference(i);

            if (p.getKey().equals("timeoutInSeconds"))
            {
                p.setSummary(String.format(getString(R.string.pref_set_timeout_desc), timeoutInSeconds));
                break;
            }
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        if (key.equals("timeoutInSeconds"))
        {
            EditTextPreference p = (EditTextPreference)findPreference(key);
            p.setSummary(String.format(getString(R.string.pref_set_timeout_desc), p.getText()));
        }
    }
}