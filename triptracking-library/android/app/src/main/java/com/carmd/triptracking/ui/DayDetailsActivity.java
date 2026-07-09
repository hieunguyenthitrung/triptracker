package com.carmd.triptracking.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.carmd.triptracking.R;
import com.carmd.triptracking.database.LocationDatabase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DayDetailsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private LocationDatabase database;
    private String date;

    private TextView tvDayTitle, tvTimeRange, tvDistance, tvLocationCount, tvDuration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Day Details");
            // Set action bar color
            getSupportActionBar().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            ContextCompat.getColor(this, R.color.header_blue)
                    )
            );
        }

        database = LocationDatabase.getInstance(this);

        // Get date from intent
        date = getIntent().getStringExtra("date");

        // Initialize views
        tvDayTitle = findViewById(R.id.tvDayTitle);
        tvTimeRange = findViewById(R.id.tvTimeRange);
        tvDistance = findViewById(R.id.tvDistance);
        tvLocationCount = findViewById(R.id.tvLocationCount);
        tvDuration = findViewById(R.id.tvDuration);

        // Setup map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Load and display data
        loadDayData();
    }

    private void loadDayData() {
        LocationDatabase.DailySummary summary = database.getDailySummary(date);

        tvDayTitle.setText(summary.getFormattedDate());
        tvTimeRange.setText(summary.getFormattedTimeRange());
        tvDistance.setText(summary.getFormattedDistance());
        tvLocationCount.setText(String.valueOf(summary.locationCount));
        tvDuration.setText(summary.getFormattedDuration());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);

        // Load locations and draw route
        drawDailyRoute();
    }

    private void drawDailyRoute() {
        // Load locations for this day (cache + trips)
        List<LocationDatabase.LocationPoint> allLocations = database.getAllLocationsByDay(date);

        if (allLocations.isEmpty()) {
            return;
        }

        // Filter to GPS and Sensor points only (remove WiFi/Cell jumps for cleaner route)
        List<LocationDatabase.LocationPoint> routeLocations = new ArrayList<>();

        // Use all locations then deduplicate and remove points closer than 5 m
        routeLocations = filterRoutePoints(allLocations);
        android.util.Log.d("DayDetails", "Filtered route points: " + routeLocations.size() + " from " + allLocations.size());

        // Need at least 2 points to draw a route
        if (routeLocations.size() < 2) {
            // Just show single point marker
            if (!routeLocations.isEmpty()) {
                LocationDatabase.LocationPoint point = routeLocations.get(0);
                LatLng position = new LatLng(point.latitude, point.longitude);
                map.addMarker(new MarkerOptions()
                        .position(position)
                        .title("Location")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f));
            }
            return;
        }

        // Draw route as separate segments — if two consecutive points are more
        // than 50 m apart the segment is skipped (e.g. gap between trips or
        // a long stationary period where location jumped).
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        List<LatLng> segment = new ArrayList<>();
        LocationDatabase.LocationPoint prev = null;
        final float MAX_SEGMENT_GAP_M = 200f;

        for (LocationDatabase.LocationPoint point : routeLocations) {
            LatLng latLng = new LatLng(point.latitude, point.longitude);
            boundsBuilder.include(latLng);

            if (prev == null) {
                segment.add(latLng);
            } else {
                float[] dist = new float[1];
                android.location.Location.distanceBetween(
                        prev.latitude, prev.longitude,
                        point.latitude, point.longitude, dist);

                if (dist[0] > MAX_SEGMENT_GAP_M) {
                    // Gap too large — commit the current segment and start a new one
                    if (segment.size() >= 2) {
                        map.addPolyline(new PolylineOptions()
                                .addAll(segment)
                                .color(Color.parseColor("#4CAF50"))
                                .width(10f)
                                .geodesic(true));
                    }
                    segment = new ArrayList<>();
                }
                segment.add(latLng);
            }
            prev = point;
        }

        // Commit the final segment
        if (segment.size() >= 2) {
            map.addPolyline(new PolylineOptions()
                    .addAll(segment)
                    .color(Color.parseColor("#4CAF50"))
                    .width(10f)
                    .geodesic(true));
        }

        // Add start marker (green)
        LocationDatabase.LocationPoint start = routeLocations.get(0);
        map.addMarker(new MarkerOptions()
                .position(new LatLng(start.latitude, start.longitude))
                .title("Start")
                .snippet(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                        .format(new java.util.Date(start.timestamp)))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        // Add end marker (red)
        LocationDatabase.LocationPoint end = routeLocations.get(routeLocations.size() - 1);
        map.addMarker(new MarkerOptions()
                .position(new LatLng(end.latitude, end.longitude))
                .title("End")
                .snippet(new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
                        .format(new java.util.Date(end.timestamp)))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        // Calculate bounds to show entire route
        LatLngBounds bounds = boundsBuilder.build();

        // Move camera to show entire route with padding
        int padding = 100; // pixels
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

        // Log for debugging
        android.util.Log.d("DayDetails",
                "Route drawn: " + routeLocations.size() + " points (filtered from " +
                        allLocations.size() + " total)");
    }

    /**
     * Remove duplicate coordinates and points closer than 5 m from the previous kept point.
     * Keeps the first occurrence of any duplicate and preserves chronological order.
     */
    private List<LocationDatabase.LocationPoint> filterRoutePoints(
            List<LocationDatabase.LocationPoint> input) {

        // 1. Sort ascending by timestamp — route must go start → end
        List<LocationDatabase.LocationPoint> sorted = new ArrayList<>(input);
        Collections.sort(sorted, (a, b) -> Long.compare(a.timestamp, b.timestamp));

        // 2. Deduplicate same-timestamp entries (cache table and trip table both
        //    store every fix; keep only the first occurrence per timestamp).
        List<LocationDatabase.LocationPoint> deduped = new ArrayList<>();
        long lastTimestamp = Long.MIN_VALUE;
        for (LocationDatabase.LocationPoint point : sorted) {
            if (point.timestamp == lastTimestamp) continue;
            deduped.add(point);
            lastTimestamp = point.timestamp;
        }

        // 3. Skip points within 10 m of the previous kept point (noise reduction)
        List<LocationDatabase.LocationPoint> result = new ArrayList<>();
        LocationDatabase.LocationPoint last = null;
        for (LocationDatabase.LocationPoint point : deduped) {
            if (last == null) {
                result.add(point);
                last = point;
                continue;
            }
            float[] distResult = new float[1];
            android.location.Location.distanceBetween(
                    last.latitude, last.longitude,
                    point.latitude, point.longitude,
                    distResult);
            if (distResult[0] < 10f) continue; // skip < 10 m
            result.add(point);
            last = point;
        }
        return result;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}