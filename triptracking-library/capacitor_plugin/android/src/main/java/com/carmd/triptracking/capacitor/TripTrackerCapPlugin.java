package com.carmd.triptracking.capacitor;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.IBinder;

import androidx.core.content.FileProvider;
import com.carmd.triptracking.TripTrackerSDK;
import com.carmd.triptracking.database.LocationDatabase;
import com.carmd.triptracking.geofence.GeofenceManager;
import com.carmd.triptracking.services.LocationTrackingService;
import com.carmd.triptracking.ui.AppSettings;
import com.carmd.triptracking.ui.DailyLocationsActivity;
import com.carmd.triptracking.ui.GeofenceSettingsActivity;
import com.carmd.triptracking.ui.MainActivity;
import com.carmd.triptracking.ui.NotificationSettingsActivity;
import com.carmd.triptracking.ui.SettingsActivity;
import com.carmd.triptracking.ui.TripHistoryActivity;
import com.carmd.triptracking.util.VoiceFeedback;

import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import com.carmd.triptracking.util.LogcatWriter;
import com.carmd.triptracking.api.TripTrackerAPIService;


@CapacitorPlugin(name = "TripTracker")
public class TripTrackerCapPlugin extends Plugin {

    private LocationTrackingService trackingService;
    private boolean serviceBound = false;

    // Static self-reference so LocationTrackingService can call emitMotionChange via reflection
    private static TripTrackerCapPlugin instance;

    // ── Event name constants ──────────────────────────────────────────────────
    private static final String EVENT_ACTIVITY_CHANGE = "activityChange";
    private static final String EVENT_LOCATION_UPDATE = "locationUpdate";
    private static final String EVENT_TRACKING_STATE  = "trackingStateChange";
    private static final String EVENT_STATS_UPDATE    = "statsUpdate";

    /**
     * Called via reflection from LocationTrackingService.emitMotionChange().
     */
    public static void emitMotionChange(String activity, String transition) {
        if (instance == null) return;
        JSObject data = new JSObject();
        data.put("activity", activity);
        data.put("transition", transition);
        instance.notifyListeners(EVENT_ACTIVITY_CHANGE, data);
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            LocationTrackingService.LocalBinder localBinder =
                    (LocationTrackingService.LocalBinder) binder;
            trackingService = localBinder.getService();
            serviceBound = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            trackingService = null;
            serviceBound = false;
        }
    };

    @Override
    public void load() {
        instance = this;
        bindToServiceIfRunning();
    }

    @Override
    protected void handleOnResume() {
        super.handleOnResume();
        if (TripTrackerSDK.isInitialized() && TripTrackerSDK.hasLocationPermission(getContext())) {
            TripTrackerSDK.onPermissionGranted(getContext());
            if (!serviceBound) bindToServiceIfRunning();
        }
        if (TripTrackerSDK.isInitialized() && TripTrackerSDK.hasLocationPermission(getContext())
                && trackingService != null) {
            trackingService.requestCurrentLocation(15_000, new LocationTrackingService.LocationCallback() {
                @Override public void onLocation(android.location.Location loc) {
                    android.util.Log.d("TripTrackerCap", "handleOnResume — location pinged ("
                            + loc.getLatitude() + ", " + loc.getLongitude() + ")");
                }
                @Override public void onError(String error) {
                    android.util.Log.d("TripTrackerCap", "handleOnResume — location unavailable: " + error);
                }
            });
        }
        // Notify JS so it can re-run BLE/dongle logic
        notifyListeners("appForeground", new JSObject());
    }

    private void bindToServiceIfRunning() {
        if (serviceBound) return;
        try {
            Intent intent = new Intent(getContext(), LocationTrackingService.class);
            getContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } catch (Exception e) {
            android.util.Log.e("TripTrackerCap", "bindService failed: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Permission & Tracking
    // ═══════════════════════════════════════════════════════════════════

    @PluginMethod
    public void hasLocationPermission(PluginCall call) {
        boolean granted = TripTrackerSDK.hasLocationPermission(getContext());
        if (granted) TripTrackerSDK.onPermissionGranted(getContext());
        JSObject ret = new JSObject();
        ret.put("granted", granted);
        call.resolve(ret);
    }

    @PluginMethod
    public void updateVehicleId(PluginCall call) {
        String vehicleId = call.getString("vehicleId");
        if (vehicleId == null) { call.reject("Missing 'vehicleId'"); return; }
        TripTrackerSDK.updateVehicleId(vehicleId);
        JSObject ret = new JSObject();
        ret.put("updated", true);
        ret.put("vehicleId", vehicleId);
        call.resolve(ret);
    }

    @PluginMethod
    public void updateToolId(PluginCall call) {
        String toolId = call.getString("toolId");
        if (toolId == null) { call.reject("Missing 'toolId'"); return; }
        TripTrackerAPIService.getInstance().updateToolId(toolId);
        JSObject ret = new JSObject();
        ret.put("updated", true);
        ret.put("toolId", toolId);
        call.resolve(ret);
    }

    @PluginMethod
    public void startTracking(PluginCall call) {
        if (!TripTrackerSDK.hasLocationPermission(getContext())) {
            call.reject("Location permission not granted.");
            return;
        }
        TripTrackerSDK.startTracking(getContext());
        bindToServiceIfRunning();
        call.resolve(new JSObject().put("started", true));
    }

    @PluginMethod
    public void stopTracking(PluginCall call) {
        TripTrackerSDK.stopTracking(getContext());
        call.resolve(new JSObject().put("stopped", true));
    }

    @PluginMethod
    public void writeLog(PluginCall call) {
        String message = call.getString("message", "");
        android.util.Log.i("⚡️ [Ionic]", message);
        call.resolve();
    }

    @PluginMethod
    public void endTrip(PluginCall call) {
        if (trackingService == null || !serviceBound) {
            call.resolve(new JSObject().put("ended", false).put("reason", "Service not bound"));
            return;
        }
        if (!trackingService.isCurrentlyTracking()) {
            call.resolve(new JSObject().put("ended", false).put("reason", "No active trip"));
            return;
        }
        long tripId = trackingService.getCurrentTripId();
        trackingService.forceEndTrip();
        TripTrackerAPIService.getInstance().flushQueue();
        call.resolve(new JSObject().put("ended", true).put("tripId", tripId));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Initialize
    // ═══════════════════════════════════════════════════════════════════

    @PluginMethod
    public void initializeWithConfig(PluginCall call) {
        TripTrackerSDK.Config config = new TripTrackerSDK.Config();

        Double saveInterval = call.getDouble("saveIntervalMinutes");
        if (saveInterval != null) config.saveIntervalMinutes = saveInterval;
        Double saveDist = call.getDouble("saveDistanceMeters");
        if (saveDist != null) config.saveDistanceMeters = saveDist;
        Double vehicleThresh = call.getDouble("vehicleThreshold");
        if (vehicleThresh != null) config.vehicleThreshold = vehicleThresh.floatValue();
        Integer transport = call.getInt("transportType");
        if (transport != null) config.transportType = transport;
        Double autoStop = call.getDouble("autoStopTimeoutMinutes");
        if (autoStop != null) config.autoStopTimeoutMinutes = autoStop;
        Double routeGap = call.getDouble("routeGapMeters");
        if (routeGap != null) config.routeGapMeters = routeGap;
        Boolean geofence = call.getBoolean("geofenceEnabled");
        if (geofence != null) config.geofenceEnabled = geofence;
        Boolean webMon = call.getBoolean("webMonitorEnabled");
        if (webMon != null) config.webMonitorEnabled = webMon;
        Boolean voice = call.getBoolean("voiceFeedbackEnabled");
        if (voice != null) config.voiceFeedbackEnabled = voice;

        // Individual notification flags
        Boolean nStart = call.getBoolean("notifyTripStart");
        if (nStart != null) config.notifyTripStart = nStart;
        Boolean nEnd = call.getBoolean("notifyTripEnd");
        if (nEnd != null) config.notifyTripEnd = nEnd;
        // notifyTrip sets both start and end together
        Boolean nTrip = call.getBoolean("notifyTrip");
        if (nTrip != null) { config.notifyTripStart = nTrip; config.notifyTripEnd = nTrip; }

        Boolean nDist = call.getBoolean("notifyDistanceKm");
        if (nDist != null) config.notifyDistanceKm = nDist;
        Boolean nEnter = call.getBoolean("notifyGeofenceEnter");
        if (nEnter != null) config.notifyGeofenceEnter = nEnter;
        Boolean nExit = call.getBoolean("notifyGeofenceExit");
        if (nExit != null) config.notifyGeofenceExit = nExit;

        String pingURL = call.getString("pingURL"); if (pingURL != null) config.pingURL = pingURL;
        String endURL  = call.getString("endURL");  if (endURL  != null) config.endURL  = endURL;
        String userId  = call.getString("userId");  if (userId  != null) config.userId  = userId;
        String vehicleId = call.getString("vehicleId"); if (vehicleId != null) config.vehicleId = vehicleId;
        String osInfo  = call.getString("osInfo");  if (osInfo  != null) config.osInfo  = osInfo;
        String routeId = call.getString("routeId"); if (routeId != null) config.routeId = routeId;
        String authKey = call.getString("authorizationKey"); if (authKey != null) config.authorizationKey = authKey;
        String apiAuth = call.getString("apiAuthKey"); if (apiAuth != null) config.apiAuthKey = apiAuth;
        String apiAuthTok = call.getString("apiAuthToken"); if (apiAuthTok != null) config.apiAuthToken = apiAuthTok;

        TripTrackerSDK.initialize(getContext(), config);

        boolean permGranted = TripTrackerSDK.hasLocationPermission(getContext());
        bindToServiceIfRunning();

        if (permGranted) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                LocationTrackingService svc = LocationTrackingService.getInstance();
                if (svc == null) return;
                svc.requestCurrentLocation(15_000, new LocationTrackingService.LocationCallback() {
                    @Override public void onLocation(android.location.Location loc) {
                        android.util.Log.d("TripTrackerCap", "init ping OK ("
                                + loc.getLatitude() + ", " + loc.getLongitude() + ")");
                    }
                    @Override public void onError(String error) {
                        android.util.Log.d("TripTrackerCap", "init ping failed: " + error);
                    }
                });
            }, 1_000);
        }

        JSObject ret = new JSObject();
        ret.put("initialized", true);
        ret.put("permissionGranted", permGranted);
        ret.put("trackingStarted", true);
        call.resolve(ret);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Native Pages
    // ═══════════════════════════════════════════════════════════════════

    @PluginMethod
    public void openSettings(PluginCall call) {
        launchActivity(SettingsActivity.class);
        call.resolve(new JSObject().put("opened", true));
    }

    @PluginMethod
    public void openNotificationSettings(PluginCall call) {
        launchActivity(NotificationSettingsActivity.class);
        call.resolve(new JSObject().put("opened", true));
    }

    @PluginMethod
    public void openGeofenceManager(PluginCall call) {
        launchActivity(GeofenceSettingsActivity.class);
        call.resolve(new JSObject().put("opened", true));
    }

    @PluginMethod
    public void openMainView(PluginCall call) {
        launchActivity(MainActivity.class);
        call.resolve(new JSObject().put("opened", true));
    }

    @PluginMethod
    public void openHistory(PluginCall call) {
        launchActivity(TripHistoryActivity.class);
        call.resolve(new JSObject().put("opened", true));
    }

    @PluginMethod
    public void openDailyLocations(PluginCall call) {
        launchActivity(DailyLocationsActivity.class);
        call.resolve(new JSObject().put("opened", true));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Tracking Status
    // ═══════════════════════════════════════════════════════════════════

    @PluginMethod
    public void getTrackingStatus(PluginCall call) {
        JSObject ret = new JSObject();
        if (trackingService != null && serviceBound) {
            float speed = trackingService.getEffectiveSpeed();
            ret.put("isTracking", trackingService.isCurrentlyTracking());
            ret.put("speed", (double) speed);
            ret.put("speedKmh", (double) speed * 3.6);
            ret.put("distance", trackingService.getTotalDistance());
            ret.put("duration", trackingService.getCurrentTripDuration());
            ret.put("steps", trackingService.getCurrentTripSteps());
            ret.put("tripId", trackingService.getCurrentTripId());
            android.location.Location loc = trackingService.getLastKnownLocation();
            if (loc != null) {
                ret.put("latitude", loc.getLatitude());
                ret.put("longitude", loc.getLongitude());
            }
        } else {
            ret.put("isTracking", false);
            ret.put("speed", 0.0);
            ret.put("speedKmh", 0.0);
            ret.put("distance", 0.0);
            ret.put("duration", 0L);
            ret.put("steps", 0);
            ret.put("tripId", 0L);
        }
        call.resolve(ret);
    }

    @PluginMethod
    public void getCurrentLocation(PluginCall call) {
        call.setKeepAlive(true);
        int timeoutMs = 15_000;
        LocationTrackingService svc = (trackingService != null)
                ? trackingService : LocationTrackingService.getInstance();
        if (svc == null) { call.reject("LocationTrackingService not available"); return; }
        svc.requestCurrentLocation(timeoutMs, new LocationTrackingService.LocationCallback() {
            @Override public void onLocation(android.location.Location loc) {
                JSObject ret = new JSObject();
                ret.put("latitude",  loc.getLatitude());
                ret.put("longitude", loc.getLongitude());
                ret.put("speed",     (double) loc.getSpeed());
                ret.put("speedKmh",  (double) loc.getSpeed() * 3.6);
                ret.put("accuracy",  (double) loc.getAccuracy());
                ret.put("bearing",   (double) loc.getBearing());
                ret.put("altitude",  loc.getAltitude());
                ret.put("timestamp", loc.getTime());
                call.resolve(ret);
            }
            @Override public void onError(String error) { call.reject(error); }
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    // Trip History
    // ═══════════════════════════════════════════════════════════════════

    @PluginMethod
    public void getTripHistory(PluginCall call) {
        int limit = call.getInt("limit", 50);
        LocationDatabase db = LocationDatabase.getInstance(getContext());
        List<LocationDatabase.Trip> trips = db.getAllTrips();
        JSArray tripArr = new JSArray();
        int count = Math.min(trips.size(), limit);
        for (int i = 0; i < count; i++) {
            LocationDatabase.Trip t = trips.get(i);
            JSObject obj = new JSObject();
            obj.put("id", t.id);
            obj.put("startTime", t.startTime);
            obj.put("endTime", t.endTime);
            obj.put("distance", t.distance);
            obj.put("duration", t.duration);
            obj.put("steps", t.steps);
            obj.put("isActive", "active".equals(t.status));
            tripArr.put(obj);
        }
        call.resolve(new JSObject().put("trips", tripArr).put("count", count));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Settings
    // ═══════════════════════════════════════════════════════════════════

    @PluginMethod
    public void getSettings(PluginCall call) {
        Context ctx = getContext();
        JSObject ret = new JSObject();
        ret.put("vehicleThreshold",      (double) AppSettings.getVehicleSpeed(ctx));
        ret.put("vehicleThresholdKmh",   (double) AppSettings.getVehicleSpeed(ctx) * 3.6);
        ret.put("saveIntervalMinutes",   (double) AppSettings.getStillInterval(ctx));
        ret.put("saveDistanceMeters",    (double) AppSettings.getVehicleDistance(ctx));
        ret.put("autoEndTimeoutMinutes", (double) AppSettings.getAutoStopTimeout(ctx));
        ret.put("routeGapThresholdMeters", (double) AppSettings.getRouteGap(ctx));
        ret.put("webMonitorEnabled",     AppSettings.isWebServerEnabled(ctx));
        ret.put("voiceFeedbackEnabled",  AppSettings.isVoiceEnabled(ctx));
        ret.put("geofencingEnabled",     GeofenceManager.isEnabled(ctx));
        ret.put("notifyTripStart",       AppSettings.isNotifTripStart(ctx));
        ret.put("notifyTripEnd",         AppSettings.isNotifTripEnd(ctx));
        ret.put("notifyDistanceKm",      AppSettings.isNotifDistanceKm(ctx));
        ret.put("notifyGeofenceEnter",   AppSettings.isNotifGeofenceEnter(ctx));
        ret.put("notifyGeofenceExit",    AppSettings.isNotifGeofenceExit(ctx));
        call.resolve(ret);
    }

    @PluginMethod
    public void updateSetting(PluginCall call) {
        String key = call.getString("key");
        if (key == null) { call.reject("Missing 'key'"); return; }
        Context ctx = getContext();
        SharedPreferences prefs = ctx.getSharedPreferences("triptracker_settings", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        switch (key) {
            case "vehicleThreshold":
                editor.putFloat(AppSettings.KEY_VEHICLE_SPEED, call.getFloat("value", 6f)); break;
            case "saveIntervalMinutes":
                editor.putFloat(AppSettings.KEY_STILL_INTERVAL, call.getFloat("value", 5f)); break;
            case "saveDistanceMeters":
                editor.putFloat(AppSettings.KEY_VEHICLE_DISTANCE, call.getFloat("value", 30f)); break;
            case "autoEndTimeoutMinutes":
                editor.putFloat(AppSettings.KEY_AUTO_STOP_TIMEOUT, call.getFloat("value", 2f)); break;
            case "routeGapThresholdMeters":
                editor.putFloat(AppSettings.KEY_ROUTE_GAP, call.getFloat("value", 500f)); break;
            case "webMonitorEnabled":
                AppSettings.setWebServerEnabled(ctx, call.getBoolean("value", false)); break;
            case "voiceFeedbackEnabled":
                AppSettings.setVoiceEnabled(ctx, call.getBoolean("value", true)); break;
            case "geofencingEnabled":
                boolean enabled = call.getBoolean("value", false);
                GeofenceManager.setEnabled(ctx, enabled);
                if (enabled) GeofenceManager.registerAll(ctx);
                else GeofenceManager.unregisterAll(ctx);
                break;
            case "notifyTrip":
                boolean notifyTrip = call.getBoolean("value", true);
                AppSettings.setNotifTripStart(ctx, notifyTrip);
                AppSettings.setNotifTripEnd(ctx, notifyTrip);
                break;
            case "notifyTripStart":
                AppSettings.setNotifTripStart(ctx, call.getBoolean("value", true)); break;
            case "notifyTripEnd":
                AppSettings.setNotifTripEnd(ctx, call.getBoolean("value", true)); break;
            default:
                call.reject("Unknown setting: " + key); return;
        }
        editor.apply();
        call.resolve(new JSObject().put("key", key).put("updated", true));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trip notification toggles (dedicated method from Ionic)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Enable or disable trip start / end push notifications.
     * options:
     *   { notify: bool }           — sets both start AND end together
     *   { start: bool, end: bool } — set each individually
     */
    @PluginMethod
    public void setTripNotifications(PluginCall call) {
        Context ctx = getContext();
        Boolean notify = call.getBoolean("notify");
        if (notify != null) {
            AppSettings.setNotifTripStart(ctx, notify);
            AppSettings.setNotifTripEnd(ctx, notify);
        } else {
            Boolean start = call.getBoolean("start");
            Boolean end   = call.getBoolean("end");
            if (start != null) AppSettings.setNotifTripStart(ctx, start);
            if (end   != null) AppSettings.setNotifTripEnd(ctx, end);
        }
        JSObject ret = new JSObject();
        ret.put("notifyTripStart", AppSettings.isNotifTripStart(ctx));
        ret.put("notifyTripEnd",   AppSettings.isNotifTripEnd(ctx));
        call.resolve(ret);
    }

    // ═══════════════════════════════════════════════════════════════════
    // Geofence
    // ═══════════════════════════════════════════════════════════════════

    @PluginMethod
    public void getGeofenceZones(PluginCall call) {
        List<GeofenceManager.GeofenceZone> zones = GeofenceManager.getAll(getContext());
        JSArray arr = new JSArray();
        for (GeofenceManager.GeofenceZone z : zones) {
            JSObject obj = new JSObject();
            obj.put("id", z.id);
            obj.put("name", z.name);
            obj.put("latitude", z.latitude);
            obj.put("longitude", z.longitude);
            obj.put("radius", z.radiusMeters);
            obj.put("notifyOnEnter", z.notifyOnEnter);
            obj.put("notifyOnExit", z.notifyOnExit);
            obj.put("autoStopOnEnter", z.autoStopTrip);
            arr.put(obj);
        }
        call.resolve(new JSObject().put("zones", arr).put("count", zones.size()));
    }

    @PluginMethod
    public void addGeofenceZone(PluginCall call) {
        String name = call.getString("name");
        Double lat  = call.getDouble("latitude");
        Double lon  = call.getDouble("longitude");
        if (name == null || lat == null || lon == null) {
            call.reject("Missing name/latitude/longitude"); return;
        }
        Double radiusDouble = call.getDouble("radius");
        float radiusMeters = radiusDouble != null ? radiusDouble.floatValue() : 200.0f;
        GeofenceManager.GeofenceZone zone = new GeofenceManager.GeofenceZone(
                name, lat, lon, radiusMeters,
                call.getBoolean("notifyOnEnter", true),
                call.getBoolean("notifyOnExit", true),
                call.getBoolean("autoStopOnEnter", false));
        GeofenceManager.addZone(getContext(), zone);
        GeofenceManager.registerAll(getContext());
        call.resolve(new JSObject().put("id", zone.id).put("added", true));
    }

    @PluginMethod
    public void removeGeofenceZone(PluginCall call) {
        String id = call.getString("id");
        if (id == null) { call.reject("Missing 'id'"); return; }
        GeofenceManager.removeZone(getContext(), id);
        call.resolve(new JSObject().put("id", id).put("removed", true));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Web Monitor
    // ═══════════════════════════════════════════════════════════════════

    @PluginMethod
    public void startWebMonitor(PluginCall call) {
        AppSettings.setWebServerEnabled(getContext(), true);
        call.resolve(new JSObject().put("started", true));
    }

    @PluginMethod
    public void stopWebMonitor(PluginCall call) {
        AppSettings.setWebServerEnabled(getContext(), false);
        call.resolve(new JSObject().put("stopped", true));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Logs
    // ═══════════════════════════════════════════════════════════════════

    @PluginMethod
    public void sendTodayLog(PluginCall call) {
        shareLogFiles(0);
        call.resolve(new JSObject().put("shared", true));
    }

    @PluginMethod
    public void sendAllLogs(PluginCall call) {
        shareLogFiles(3);
        call.resolve(new JSObject().put("shared", true));
    }

    @PluginMethod
    public void sendRecentLogs(PluginCall call) {
        int days = call.getInt("days", 3);
        Integer zipDays = (days == -1) ? null : (days == 0 ? 1 : days);
        File zip = LogcatWriter.getZippedLogs(getContext(), zipDays);
        if (zip == null) { call.reject("No log files found or zip failed"); return; }
        File outDir = getContext().getExternalCacheDir();
        if (outDir == null) outDir = getContext().getCacheDir();
        File shareFile = new File(outDir, zip.getName());
        try {
            java.nio.file.Files.copy(zip.toPath(), shareFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            call.reject("Failed to prepare zip: " + e.getMessage()); return;
        }
        shareLogFiles(days);
        call.resolve(new JSObject().put("path", shareFile.getAbsolutePath()));
    }

    @PluginMethod
    public void resetConfig(PluginCall call) {
        TripTrackerSDK.resetConfig(getContext());
        call.resolve(new JSObject().put("reset", true));
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private void launchActivity(Class<?> cls) {
        Intent intent = new Intent(getContext(), cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
    }

    private void shareLogFiles(int days) {
        if (getActivity() == null) return;
        Integer zipDays = (days == -1) ? null : (days == 0 ? 1 : days);
        File zip = LogcatWriter.getZippedLogs(getContext(), zipDays);
        if (zip == null) return;
        String subject = (days == 0) ? "TripTracker Today's Log"
                : (days == -1) ? "TripTracker All Logs"
                : "TripTracker Logs — Last " + days + " days";
        try {
            File externalDir = getContext().getExternalCacheDir();
            if (externalDir == null) externalDir = getContext().getCacheDir();
            File shareFile = new File(externalDir, zip.getName());
            java.nio.file.Files.copy(zip.toPath(), shareFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Uri uri = FileProvider.getUriForFile(getContext(),
                    getContext().getPackageName() + ".fileprovider", shareFile);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/zip");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getActivity().startActivity(Intent.createChooser(intent, subject));
        } catch (Exception e) {
            android.util.Log.e("TripTrackerPlugin", "Share failed: " + e.getMessage());
        }
    }

    private Uri getUriForFile(File f) {
        return FileProvider.getUriForFile(getContext(),
                getContext().getPackageName() + ".fileprovider", f);
    }
}
