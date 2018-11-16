package com.sourcestream.android.restclient;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

/**
 * Rate app activity.
 */
public class RateAppActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.rate_app);
    }

    public void onClick(View view)
    {
        if (view.getId() == R.id.rateAppButton || view.getId() == R.id.rateNoButton)
        {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(SplashActivity.RATED_OR_DECLINED, true);
            editor.commit();

            if (view.getId() == R.id.rateAppButton)
            {
                AndroidUtil.openAppInGoogleStore(this);
            }
        }

        finish();
    }
}