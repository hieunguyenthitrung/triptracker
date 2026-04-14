# TripTracking SDK — Android Integration Guide (Java)

## Requirements

| Tool | Minimum Version |
|---|---|
| Android Studio | Hedgehog 2023.1+ |
| Android | API 24 (Android 7.0)+ |
| Java | 17+ |
| Gradle | 8.0+ |

---

## Step 1: Create new Android project

```
1. Open Android Studio
2. File → New → New Project
3. Select: Empty Views Activity → Next
4. Fill in:
   - Name:                  HelloWorld
   - Package name:          com.yourname.helloworld
   - Save location:         (choose folder)
   - Language:              Java
   - Minimum SDK:           API 24
5. Click Finish
```

---

## Step 2: Create GitHub Personal Access Token

```
1. Go to github.com → Settings
2. Developer settings → Personal access tokens → Tokens (classic)
3. Generate new token (classic)
4. Select scopes:
   ✅ read:packages
   ✅ repo
5. Copy token — shown only once!
```

---

## Step 3: Add GitHub Packages repository

Open `settings.gradle` (project level):

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()

        // ← Add TripTracking library repo
        maven {
            url = uri("https://maven.pkg.github.com/hieunguyentt/TripTracker")
            credentials {
                username = "hieunguyentt"
                password = "your_github_token"   // ← paste your token here
            }
        }
    }
}
```

---

## Step 4: Add dependency

Open `app/build.gradle`:

```gradle
android {
    ...
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'com.github.hieunguyentt:triptracking-android:1.0.0'

    // Required dependencies
    implementation 'androidx.core:core:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.gms:play-services-maps:18.2.0'
    implementation 'com.google.android.gms:play-services-location:21.1.0'
}
```

Click **Sync Now** when prompted.

---

## Step 5: Add permissions to AndroidManifest.xml

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Location -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

    <!-- Motion & Activity -->
    <uses-permission android:name="android.permission.ACTIVITY_RECOGNITION" />

    <!-- Foreground Service -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

    <!-- Network -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

    <!-- Boot -->
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

    <application ...>
        ...
    </application>
</manifest>
```

---

## Step 6: Add Google Maps API Key

In `AndroidManifest.xml` inside `<application>`:

```xml
<application ...>

    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="your_google_maps_api_key" />

</application>
```

> Get API key: https://console.cloud.google.com → APIs & Services → Credentials → Create API Key → Enable Maps SDK for Android

---

## Step 7: Initialize SDK in Application class

Create `MyApplication.java`:

```java
package com.yourname.helloworld;

import android.app.Application;
import com.carmd.triptracking.TripTrackerSDK;

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize TripTracking SDK
        TripTrackerSDK.initialize(this);
    }
}
```

Register in `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"   // ← add this line
    ...>
```

---

## Step 8: Implement in MainActivity.java

```java
package com.yourname.helloworld;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.carmd.triptracking.TripTrackerSDK;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private TextView statusText;
    private TextView locationText;
    private TextView statsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText   = findViewById(R.id.statusText);
        locationText = findViewById(R.id.locationText);
        statsText    = findViewById(R.id.statsText);

        setupButtons();
        requestPermissions();
        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    // ── Setup Buttons ─────────────────────────────────────────
    private void setupButtons() {

        // Open Map (MainActivity from library)
        Button mapBtn = findViewById(R.id.mapBtn);
        mapBtn.setOnClickListener(v ->
            TripTrackerSDK.openMainView(this)
        );

        // Open Settings
        Button settingsBtn = findViewById(R.id.settingsBtn);
        settingsBtn.setOnClickListener(v ->
            TripTrackerSDK.openSettings(this)
        );

        // Open Notification Settings
        Button notifBtn = findViewById(R.id.notifBtn);
        notifBtn.setOnClickListener(v ->
            TripTrackerSDK.openNotifications(this)
        );

        // Open Geofence Manager
        Button geofenceBtn = findViewById(R.id.geofenceBtn);
        geofenceBtn.setOnClickListener(v ->
            TripTrackerSDK.openGeofence(this)
        );

        // Open Trip History
        Button historyBtn = findViewById(R.id.historyBtn);
        historyBtn.setOnClickListener(v ->
            TripTrackerSDK.openHistory(this)
        );

        // Open Daily Locations
        Button dailyBtn = findViewById(R.id.dailyBtn);
        dailyBtn.setOnClickListener(v ->
            TripTrackerSDK.openDailyLocations(this)
        );

        // Refresh status
        Button refreshBtn = findViewById(R.id.refreshBtn);
        refreshBtn.setOnClickListener(v -> updateStatus());
    }

    // ── Update Status ─────────────────────────────────────────
    private void updateStatus() {
        // Tracking status
        boolean isTracking = TripTrackerSDK.isTracking();
        statusText.setText(isTracking ? "● Tracking active" : "● Not tracking");
        statusText.setTextColor(isTracking ?
            getColor(android.R.color.holo_green_dark) :
            getColor(android.R.color.holo_red_dark)
        );

        // Last known location
        Location loc = TripTrackerSDK.getLastLocation();
        if (loc != null) {
            locationText.setText(String.format(
                "📍 %.5f, %.5f",
                loc.getLatitude(),
                loc.getLongitude()
            ));
        } else {
            locationText.setText("📍 No location yet");
        }

        // Stats
        if (isTracking) {
            statsText.setText(String.format(
                "Speed: %.1f km/h  |  Distance: %.2f km  |  Steps: %d",
                TripTrackerSDK.getSpeedKmh(),
                TripTrackerSDK.getDistance() / 1000,
                TripTrackerSDK.getSteps()
            ));
        } else {
            statsText.setText("—");
        }
    }

    // ── Permissions ───────────────────────────────────────────
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
            new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION,
            },
            PERMISSION_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode,
        String[] permissions,
        int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE &&
            grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            updateStatus();
        }
    }
}
```

---

## Step 9: Create layout `res/layout/activity_main.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp"
        android:gravity="center_horizontal">

        <!-- Title -->
        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Hello World! 👋"
            android:textSize="28sp"
            android:textStyle="bold"
            android:layout_marginBottom="8dp"/>

        <!-- Tracking Status -->
        <TextView
            android:id="@+id/statusText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="● Not tracking"
            android:textSize="16sp"
            android:layout_marginBottom="4dp"/>

        <!-- Location -->
        <TextView
            android:id="@+id/locationText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="📍 No location yet"
            android:textSize="14sp"
            android:textColor="#666666"
            android:layout_marginBottom="4dp"/>

        <!-- Stats -->
        <TextView
            android:id="@+id/statsText"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="—"
            android:textSize="13sp"
            android:textColor="#888888"
            android:layout_marginBottom="24dp"/>

        <!-- Buttons -->
        <Button
            android:id="@+id/mapBtn"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:text="🗺 Open Map"
            android:layout_marginBottom="12dp"/>

        <Button
            android:id="@+id/settingsBtn"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:text="⚙️ Settings"
            android:layout_marginBottom="12dp"/>

        <Button
            android:id="@+id/notifBtn"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:text="🔔 Notification Settings"
            android:layout_marginBottom="12dp"/>

        <Button
            android:id="@+id/geofenceBtn"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:text="📍 Geofence Manager"
            android:layout_marginBottom="12dp"/>

        <Button
            android:id="@+id/historyBtn"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:text="📋 Trip History"
            android:layout_marginBottom="12dp"/>

        <Button
            android:id="@+id/dailyBtn"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:text="📅 Daily Locations"
            android:layout_marginBottom="12dp"/>

        <Button
            android:id="@+id/refreshBtn"
            android:layout_width="match_parent"
            android:layout_height="52dp"
            android:text="🔄 Refresh Status"
            android:layout_marginBottom="12dp"/>

    </LinearLayout>
</ScrollView>
```

---

## Step 10: Run the app

```
Android Studio → Run → Run 'app' (⇧F10)
Select emulator or physical device
```

---

## SDK API Reference

### Initialization

| Method | Description |
|---|---|
| `TripTrackerSDK.initialize(context)` | Initialize SDK — call in `Application.onCreate()` |
| `TripTrackerSDK.isInitialized()` | Check if SDK is initialized |

### Open Native Screens

| Method | Description |
|---|---|
| `TripTrackerSDK.openMainView(activity)` | Open map with trip tracking |
| `TripTrackerSDK.openSettings(activity)` | Open settings screen |
| `TripTrackerSDK.openNotifications(activity)` | Open notification settings |
| `TripTrackerSDK.openGeofence(activity)` | Open geofence manager |
| `TripTrackerSDK.openHistory(activity)` | Open trip history |
| `TripTrackerSDK.openDailyLocations(activity)` | Open daily locations |

> All methods also accept `Context` instead of `Activity` (e.g. from Service or Fragment).

### Data Access

| Method | Return | Description |
|---|---|---|
| `TripTrackerSDK.isTracking()` | `boolean` | Current tracking state |
| `TripTrackerSDK.getCurrentTripId()` | `long` | Active trip ID |
| `TripTrackerSDK.getDistance()` | `double` | Distance in meters |
| `TripTrackerSDK.getSpeed()` | `float` | Speed in m/s |
| `TripTrackerSDK.getSpeedKmh()` | `float` | Speed in km/h |
| `TripTrackerSDK.getDuration()` | `long` | Trip duration in ms |
| `TripTrackerSDK.getSteps()` | `int` | Step count |
| `TripTrackerSDK.getLastLocation()` | `Location` | Last known GPS location |

### Settings

| Method | Return | Description |
|---|---|---|
| `TripTrackerSDK.isVoiceEnabled()` | `boolean` | Voice feedback state |
| `TripTrackerSDK.isWebMonitorEnabled()` | `boolean` | Web monitor state |
| `TripTrackerSDK.isGeofencingEnabled()` | `boolean` | Geofencing state |
| `TripTrackerSDK.getVehicleThreshold()` | `float` | Vehicle speed threshold |

---

## Troubleshooting

### Could not resolve library
```
→ Check GitHub token in settings.gradle
→ Ensure token has read:packages scope
→ File → Sync Project with Gradle Files
```

### App crashes on start — SDK not initialized
```java
// Make sure MyApplication is registered in AndroidManifest.xml
android:name=".MyApplication"
```

### Google Maps blank / grey screen
```
→ Check API key in AndroidManifest.xml
→ Enable "Maps SDK for Android" in Google Cloud Console
→ Check internet permission in AndroidManifest.xml
```

### Permission denied at runtime (Android 10+)
```java
// Request background location separately after ACCESS_FINE_LOCATION is granted
ActivityCompat.requestPermissions(this,
    new String[]{ Manifest.permission.ACCESS_BACKGROUND_LOCATION },
    PERMISSION_REQUEST_CODE
);
```

### Build error: Duplicate class
```gradle
android {
    packagingOptions {
        exclude 'META-INF/DEPENDENCIES'
    }
}
```

---

## Version History

| Version | Notes |
|---|---|
| 1.0.0 | Initial release |
