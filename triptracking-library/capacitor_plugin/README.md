# capacitor-triptracker (iOS + Android)

Capacitor plugin for TripTracker — GPS trip tracking, geofencing, voice, CarPlay/Android Auto.

## Install

```bash
npm install ./plugins/capacitor-triptracker
npx cap sync
```

## Setup

### iOS
1. Add TripTracker Swift sources via CocoaPod or copy into Xcode
2. Initialize in `AppDelegate.swift`:
```swift
TripTrackerSDK.initialize(launchOptions: launchOptions)
```
3. Add location permissions to `Info.plist`

### Android
1. Copy `com.carmd.triptracking` Java sources into your Android module
2. Register activities in `AndroidManifest.xml`
3. `TripTrackerApp.java` handles initialization

### Register Plugin (Android)

In your `MainActivity.java`:
```java
import com.carmd.triptracking.capacitor.TripTrackerCapPlugin;

public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        registerPlugin(TripTrackerCapPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
```

## Usage

```typescript
import { TripTracker } from 'capacitor-triptracker';

// Open native pages
await TripTracker.openSettings();
await TripTracker.openNotificationSettings();
await TripTracker.openGeofenceManager();
await TripTracker.openMainView();
await TripTracker.openHistory();
await TripTracker.openDailyLocations();

// Tracking status
const status = await TripTracker.getTrackingStatus();
console.log(status.speedKmh, status.distance, status.isTracking);

// Settings
const settings = await TripTracker.getSettings();
await TripTracker.updateSetting({ key: 'voiceFeedbackEnabled', value: false });

// Geofence
const { zones } = await TripTracker.getGeofenceZones();
await TripTracker.addGeofenceZone({
  name: 'Office', latitude: 10.80, longitude: 106.64, radius: 200,
});

// Trip history
const { trips } = await TripTracker.getTripHistory({ limit: 20 });

// Web monitor & logs
await TripTracker.startWebMonitor();
await TripTracker.sendTodayLog();
```

## API

| Method | iOS | Android |
|--------|-----|---------|
| `openSettings()` | ✅ | ✅ |
| `openNotificationSettings()` | ✅ | ✅ |
| `openGeofenceManager()` | ✅ | ✅ |
| `openMainView()` | ✅ | ✅ |
| `openHistory()` | ✅ | ✅ |
| `openDailyLocations()` | ✅ | ✅ |
| `getTrackingStatus()` | ✅ | ✅ |
| `getCurrentLocation()` | ✅ | ✅ |
| `getTripHistory()` | ✅ | ✅ |
| `getSettings()` | ✅ | ✅ |
| `updateSetting()` | ✅ | ✅ |
| `getGeofenceZones()` | ✅ | ✅ |
| `addGeofenceZone()` | ✅ | ✅ |
| `removeGeofenceZone()` | ✅ | ✅ |
| `startWebMonitor()` | ✅ | ✅ |
| `stopWebMonitor()` | ✅ | ✅ |
| `sendTodayLog()` | ✅ | ✅ |
| `sendAllLogs()` | ✅ | ✅ |

## Structure

```
capacitor-triptracker/
├── src/
│   ├── definitions.ts          ← TypeScript types
│   ├── index.ts                ← Plugin registration
│   └── web.ts                  ← Web stub
├── ios/Plugin/
│   ├── TripTrackerPlugin.swift ← iOS Swift bridge
│   └── TripTrackerPlugin.m     ← ObjC bridge
├── android/
│   ├── build.gradle
│   └── src/main/java/.../TripTrackerCapPlugin.java  ← Android Java bridge
├── example/
│   └── triptracker.page.ts     ← Example Ionic Angular page
├── package.json
├── CapacitorTripTracker.podspec
├── tsconfig.json
└── rollup.config.mjs
```
