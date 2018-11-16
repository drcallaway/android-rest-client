package com.sourcestream.android.restclient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

/**
 * Splash activity.
 */
public class SplashActivity extends Activity
{
    public static final String LAUNCH_COUNT = "launchCount";
    public static final String RATED_OR_DECLINED = "ratedOrDeclined";
    public static final String ACTION_PROMPT_RATING = "promptRating";

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(this, MainActivity.class);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        int launchCount = settings.getInt(LAUNCH_COUNT, 0) + 1;

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(LAUNCH_COUNT, launchCount);

        editor.commit();

        if (launchCount > 3)
        {
            boolean ratedOrDeclined = settings.getBoolean(RATED_OR_DECLINED, false);

            if (!ratedOrDeclined)
            {
                intent.setAction(ACTION_PROMPT_RATING);
            }
        }

        startActivity(intent);
    }
}