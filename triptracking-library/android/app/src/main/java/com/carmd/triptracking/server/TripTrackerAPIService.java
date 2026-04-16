package com.carmd.triptracking.server;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sends location data to server via REST API.
 *   POST /ping/v2 — location updates (on every GPS save)
 *   POST /end     — final GPS location when trip ends
 *
 * Configured via TripTrackerSDK.Config API fields.
 * Headers: AuthorizationKey, api-auth-key
 */
public class TripTrackerAPIService {

    private static final String TAG = "TripTrackerAPI";
    private static TripTrackerAPIService instance;

    // Config
    private String pingURL = "";
    private String endURL = "";
    private String userId = "";
    private String vehicleId = "";
    private String osInfo = "";
    private String routeId = "";
    private String authorizationKey = "";
    private String apiAuthKey = "";
    private boolean enabled = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final SimpleDateFormat iso8601;

    private TripTrackerAPIService() {
        iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        iso8601.setTimeZone(TimeZone.getTimeZone("UTC"));
        osInfo = "Android " + Build.VERSION.RELEASE + " / " + Build.MODEL;
    }

    public static synchronized TripTrackerAPIService getInstance() {
        if (instance == null) instance = new TripTrackerAPIService();
        return instance;
    }

    // ── Configuration ──

    public void configure(String pingURL, String endURL, String userId, String vehicleId,
                          String osInfo, String routeId, String authorizationKey, String apiAuthKey) {
        this.pingURL = pingURL != null ? pingURL : "";
        this.endURL = endURL != null ? endURL : "";
        this.userId = userId != null ? userId : "";
        this.vehicleId = vehicleId != null ? vehicleId : "";
        if (osInfo != null && !osInfo.isEmpty()) this.osInfo = osInfo;
        this.routeId = routeId != null ? routeId : "";
        this.authorizationKey = authorizationKey != null ? authorizationKey : "";
        this.apiAuthKey = apiAuthKey != null ? apiAuthKey : "";
        this.enabled = !this.pingURL.isEmpty() && !this.userId.isEmpty();

        Log.i(TAG, "Configured: ping=" + this.pingURL + " end=" + this.endURL
                + " user=" + this.userId + " enabled=" + this.enabled);
    }

    public boolean isEnabled() { return enabled; }

    public void setRouteId(String routeId) { this.routeId = routeId != null ? routeId : ""; }

    // ═══════════════════════════════════════════════════════════════════
    // POST /ping/v2 — Send location update
    // ═══════════════════════════════════════════════════════════════════

    public void postLocation(double latitude, double longitude, double speed,
                             boolean isMoving, String activityType) {
        postLocation(latitude, longitude, speed, isMoving, activityType, new Date());
    }

    public void postLocation(double latitude, double longitude, double speed,
                             boolean isMoving, String activityType, Date timestamp) {
        if (!enabled || pingURL.isEmpty()) return;

        executor.execute(() -> {
            try {
                JSONObject locObj = new JSONObject();
                locObj.put("is_Moving", isMoving);
                locObj.put("timestamp", iso8601.format(timestamp));
                locObj.put("latitude", latitude);
                locObj.put("longitude", longitude);
                locObj.put("speed", speed);
                locObj.put("activityType", activityType != null ? activityType : "unknown");
                locObj.put("route_Id", routeId);

                JSONArray locArray = new JSONArray();
                locArray.put(locObj);

                JSONObject body = new JSONObject();
                body.put("user_Id", userId);
                body.put("vehicle_Id", vehicleId);
                body.put("os_Info", osInfo);
                body.put("location", locArray);

                boolean ok = post(pingURL, body.toString());
                Log.d(TAG, ok ? "📡 Location posted (" + latitude + ", " + longitude + ")"
                        : "❌ Failed to post location");
            } catch (Exception e) {
                Log.e(TAG, "Post location error: " + e.getMessage());
            }
        });
    }

    /** Batch post multiple locations. */
    public void postLocations(List<LocationData> locations) {
        if (!enabled || pingURL.isEmpty() || locations == null || locations.isEmpty()) return;

        executor.execute(() -> {
            try {
                JSONArray locArray = new JSONArray();
                for (LocationData loc : locations) {
                    JSONObject obj = new JSONObject();
                    obj.put("is_Moving", loc.isMoving);
                    obj.put("timestamp", iso8601.format(new Date(loc.timestampMs)));
                    obj.put("latitude", loc.latitude);
                    obj.put("longitude", loc.longitude);
                    obj.put("speed", loc.speed);
                    obj.put("activityType", loc.activityType);
                    obj.put("route_Id", routeId);
                    locArray.put(obj);
                }

                JSONObject body = new JSONObject();
                body.put("user_Id", userId);
                body.put("vehicle_Id", vehicleId);
                body.put("os_Info", osInfo);
                body.put("location", locArray);

                boolean ok = post(pingURL, body.toString());
                Log.d(TAG, ok ? "📡 Batch " + locations.size() + " locations posted"
                        : "❌ Batch post failed");
            } catch (Exception e) {
                Log.e(TAG, "Batch post error: " + e.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    // POST /end — Final location when trip ends
    // ═══════════════════════════════════════════════════════════════════

    public void postTripEnd(double latitude, double longitude) {
        postTripEnd(latitude, longitude, new Date());
    }

    public void postTripEnd(double latitude, double longitude, Date timestamp) {
        if (!enabled || endURL.isEmpty()) return;

        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("user_Id", userId);
                body.put("timestamp", iso8601.format(timestamp));
                body.put("latitude", latitude);
                body.put("longitude", longitude);

                boolean ok = post(endURL, body.toString());
                Log.d(TAG, ok ? "📡 Trip end posted (" + latitude + ", " + longitude + ")"
                        : "❌ Failed to post trip end");
            } catch (Exception e) {
                Log.e(TAG, "Post trip end error: " + e.getMessage());
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════
    // HTTP Helper
    // ═══════════════════════════════════════════════════════════════════

    private boolean post(String urlString, String jsonBody) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            // Auth headers
            if (authorizationKey != null && !authorizationKey.isEmpty()) {
                conn.setRequestProperty("AuthorizationKey", authorizationKey);
            }
            if (apiAuthKey != null && !apiAuthKey.isEmpty()) {
                conn.setRequestProperty("api-auth-key", apiAuthKey);
            }

            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.getBytes("UTF-8"));
            os.flush();
            os.close();

            int code = conn.getResponseCode();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            Log.e(TAG, "HTTP POST error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ── Location data holder for batch posting ──
    public static class LocationData {
        public double latitude, longitude, speed;
        public boolean isMoving;
        public String activityType;
        public long timestampMs;

        public LocationData(double lat, double lon, double speed, boolean moving, String activity, long ts) {
            this.latitude = lat; this.longitude = lon; this.speed = speed;
            this.isMoving = moving; this.activityType = activity; this.timestampMs = ts;
        }
    }
}
