package com.sourcestream.android.restclient;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.util.List;

/**
 * Android utility methods.
 */
public class AndroidUtil
{
    private static final String GOOGLE_PLAY_URL = "market://details?id=";
    private static final String GOOGLE_VERSION_SUFFIX = "0";
    private static final String AMAZON_STORE_URL = "http://www.amazon.com/gp/mas/dl/android?p=";
    private static final String AMAZON_VERSION_SUFFIX = "1";

    public static void openAppInStore(Context ctx)
    {
        String storeUrl = GOOGLE_PLAY_URL; //default to Google Play Store

        try
        {
            String versionName = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName;
            String version = versionName.split(" ")[0]; //strip off extra information following version

            if (version.endsWith(AMAZON_VERSION_SUFFIX))
            {
                storeUrl = AMAZON_STORE_URL;
            }
        }
        catch (PackageManager.NameNotFoundException e)
        {
            //ignore error and stick with default
        }

        launchStore(ctx, storeUrl);
    }

    public static void openAppInGoogleStore(Context ctx)
    {
        launchStore(ctx, GOOGLE_PLAY_URL);
    }

    public static void openAppInAmazonStore(Context ctx)
    {
        launchStore(ctx, AMAZON_STORE_URL);
    }

    private static void launchStore(Context ctx, String url)
    {
        Uri marketUri = Uri.parse(url + ctx.getPackageName());
        Intent marketIntent = new Intent(Intent.ACTION_VIEW, marketUri);
        ctx.startActivity(marketIntent);
    }

    /**
     * From http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html
     * Indicates whether the specified action can be used as an intent. This
     * method queries the package manager for installed packages that can
     * respond to an intent with the specified action. If no suitable package is
     * found, this method returns false.
     *
     * @param context The application's environment.
     * @param action  The Intent action to check for availability.
     * @return True if an Intent with the specified action can be sent and
     *         responded to, false otherwise.
     */
    public static boolean isIntentAvailable(Context context, String action)
    {
        final PackageManager packageManager = context.getPackageManager();
        final Intent intent = new Intent(action);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }
}