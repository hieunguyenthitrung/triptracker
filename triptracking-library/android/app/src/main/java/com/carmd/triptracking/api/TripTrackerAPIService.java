package com.carmd.triptracking.api;

import android.location.Location;
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

public final class TripTrackerAPIService {
    private static final String TAG = "TripTrackerAPI";
    private static TripTrackerAPIService instance;

    // Config
    private String pingURL = "";
    private String endURL = "";
    private String userId = "";
    private String vehicleId = "";
    private String osInfo = "Android " + Build.VERSION.RELEASE;
    private String routeId = "";
    private String authorizationKey = "";
    private String apiAuthKey = "";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private TripTrackerAPIService() {}

    public static synchronized TripTrackerAPIService getInstance() {
        if (instance == null) instance = new TripTrackerAPIService();
        return instance;
    }

    // ── Config setters ──
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
        Log.i(TAG, "API configured: ping=" + this.pingURL + " end=" + this.endURL + " user=" + this.userId);
    }

    public void setRouteId(String id) { this.routeId = id != null ? id : ""; }
    public boolean isEnabled() { return !pingURL.isEmpty() && !endURL.isEmpty() && !userId.isEmpty(); }

    // ── POST /ping/v2 ──
    public void sendPing(Location location, boolean isMoving, float speed, String activityType) {
        sendPing(location, isMoving, speed, activityType, this.routeId);
    }

    public void sendPing(Location location, boolean isMoving, float speed, String activityType, String routeId) {
        if (!isEnabled()) return;
        executor.execute(() -> {
            try {
                JSONObject locObj = new JSONObject();
                locObj.put("is_Moving", isMoving);
                locObj.put("timestamp", isoNow());
                locObj.put("latitude", location.getLatitude());
                locObj.put("longitude", location.getLongitude());
                locObj.put("speed", speed);
                locObj.put("activityType", activityType);
                locObj.put("route_Id", routeId != null ? routeId : this.routeId);

                JSONArray locArr = new JSONArray();
                locArr.put(locObj);

                JSONObject body = new JSONObject();
                body.put("user_Id", userId);
                body.put("vehicle_Id", vehicleId);
                body.put("os_Info", osInfo);
                body.put("location", locArr);

                boolean ok = post(pingURL, body);
                Log.d(TAG, "Ping " + (ok ? "OK" : "FAIL") + ": " + location.getLatitude() + "," + location.getLongitude());
            } catch (Exception e) {
                Log.e(TAG, "Ping error: " + e.getMessage());
            }
        });
    }

    // ── POST /end ──
    public void sendTripEnd(Location location) {
        if (!isEnabled()) return;
        executor.execute(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("user_Id", userId);
                body.put("timestamp", isoNow());
                body.put("latitude", location.getLatitude());
                body.put("longitude", location.getLongitude());

                boolean ok = post(endURL, body);
                Log.d(TAG, "Trip-end " + (ok ? "OK" : "FAIL"));

                // Retry once on failure
                if (!ok) {
                    Thread.sleep(5000);
                    post(endURL, body);
                }
            } catch (Exception e) {
                Log.e(TAG, "Trip-end error: " + e.getMessage());
            }
        });
    }

    // ── HTTP POST ──
    private boolean post(String urlStr, JSONObject body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            if (!authorizationKey.isEmpty())
                conn.setRequestProperty("AuthorizationKey", authorizationKey);
            if (!apiAuthKey.isEmpty())
                conn.setRequestProperty("api-auth-key", apiAuthKey);

            OutputStream os = conn.getOutputStream();
            os.write(body.toString().getBytes("UTF-8"));
            os.flush(); os.close();

            int code = conn.getResponseCode();
            return code >= 200 && code < 300;
        } catch (Exception e) {
            Log.e(TAG, "POST error: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String isoNow() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }
}
