package com.carmd.triptracking;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.carmd.triptracking.database.LocationDatabase;
import com.carmd.triptracking.geofence.GeofenceManager;
import com.carmd.triptracking.services.LocationTrackingService;
import com.carmd.triptracking.ui.*;
import com.carmd.triptracking.util.LogcatWriter;
import com.carmd.triptracking.util.VoiceFeedback;

public final class TripTrackerSDK {
    private static final String TAG = "TripTrackerSDK";
    private static Context appContext;
    private static boolean initialized = false;
    private TripTrackerSDK() {}

    public static void initialize(Context context) {
        if (initialized) return;
        appContext = context.getApplicationContext();
        LogcatWriter.start(appContext);
        LocationDatabase.getInstance(appContext);
        Intent si = new Intent(appContext, LocationTrackingService.class);
        appContext.startForegroundService(si);
        VoiceFeedback.getInstance(appContext);
        if (GeofenceManager.isEnabled(appContext)) GeofenceManager.registerAll(appContext);
        initialized = true;
        Log.i(TAG, "✅ TripTrackerSDK initialized");
    }

    public static boolean isInitialized() { return initialized; }

    // Native pages (each has built-in map)
    public static void openMainView(Activity a)       { a.startActivity(new Intent(a, MainActivity.class)); }
    public static void openSettings(Activity a)        { a.startActivity(new Intent(a, SettingsActivity.class)); }
    public static void openNotifications(Activity a)   { a.startActivity(new Intent(a, NotificationSettingsActivity.class)); }
    public static void openGeofence(Activity a)        { a.startActivity(new Intent(a, GeofenceSettingsActivity.class)); }
    public static void openHistory(Activity a)         { a.startActivity(new Intent(a, TripHistoryActivity.class)); }
    public static void openDailyLocations(Activity a)  { a.startActivity(new Intent(a, DailyLocationsActivity.class)); }

    // Context-based (from Service, Fragment, etc.)
    public static void openMainView(Context c)      { launch(c, MainActivity.class); }
    public static void openSettings(Context c)       { launch(c, SettingsActivity.class); }
    public static void openNotifications(Context c)  { launch(c, NotificationSettingsActivity.class); }
    public static void openGeofence(Context c)       { launch(c, GeofenceSettingsActivity.class); }
    public static void openHistory(Context c)        { launch(c, TripHistoryActivity.class); }
    public static void openDailyLocations(Context c) { launch(c, DailyLocationsActivity.class); }

    private static void launch(Context c, Class<?> cls) { Intent i = new Intent(c, cls); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(i); }

    // Data access
    public static boolean isTracking()    { LocationTrackingService s = LocationTrackingService.getInstance(); return s != null && s.isCurrentlyTracking(); }
    public static long getCurrentTripId() { LocationTrackingService s = LocationTrackingService.getInstance(); return s != null ? s.getCurrentTripId() : 0; }
    public static double getDistance()    { LocationTrackingService s = LocationTrackingService.getInstance(); return s != null ? s.getTotalDistance() : 0; }
    public static float getSpeed()        { LocationTrackingService s = LocationTrackingService.getInstance(); return s != null ? s.getEffectiveSpeed() : 0; }
    public static float getSpeedKmh()     { return getSpeed() * 3.6f; }
    public static long getDuration()      { LocationTrackingService s = LocationTrackingService.getInstance(); return s != null ? s.getCurrentTripDuration() : 0; }
    public static int getSteps()          { LocationTrackingService s = LocationTrackingService.getInstance(); return s != null ? s.getCurrentTripSteps() : 0; }
    public static android.location.Location getLastLocation() { LocationTrackingService s = LocationTrackingService.getInstance(); return s != null ? s.getLastKnownLocation() : null; }

    // Settings
    public static float getVehicleThreshold() { return appContext != null ? AppSettings.getVehicleSpeed(appContext) : 6f; }
    public static boolean isVoiceEnabled()    { return appContext != null && AppSettings.isVoiceEnabled(appContext); }
    public static boolean isWebMonitorEnabled() { return appContext != null && AppSettings.isWebServerEnabled(appContext); }
    public static boolean isGeofencingEnabled() { return appContext != null && GeofenceManager.isEnabled(appContext); }
}
