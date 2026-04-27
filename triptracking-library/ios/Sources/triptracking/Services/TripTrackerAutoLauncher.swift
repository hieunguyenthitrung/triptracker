//
//  TripTrackerAutoLauncher.swift
//  triptracking
//
//  Auto-initializes TripTracker when iOS relaunches the app for a location event.
//  Works without modifying AppDelegate — hooks into UIApplication lifecycle notifications.
//
//  Why this is needed:
//  - When iOS kills the app (memory pressure), significant location changes relaunch it.
//  - Capacitor/Ionic JS hasn't loaded yet at relaunch time.
//  - Plugin load() may fire too late — iOS gives only ~10 seconds.
//  - This class starts a CLLocationManager IMMEDIATELY on app launch,
//    ensuring location updates continue even before Capacitor loads.
//

import UIKit
import CoreLocation

/// Singleton that auto-starts location tracking as early as possible in the app lifecycle.
/// Registered via @objc and instantiated by the TripTrackerSDK on first use.
public final class TripTrackerAutoLauncher: NSObject, CLLocationManagerDelegate {

    public static let shared = TripTrackerAutoLauncher()

    private let earlyLocationManager = CLLocationManager()
    private var hasInitializedSDK = false

    private override init() {
        super.init()

        // Listen for app launch — fires even on background relaunch
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(appDidFinishLaunching(_:)),
            name: UIApplication.didFinishLaunchingNotification,
            object: nil
        )

        // Start location manager immediately — don't wait for notification
        earlyLocationManager.delegate = self
        earlyLocationManager.allowsBackgroundLocationUpdates = true
        earlyLocationManager.pausesLocationUpdatesAutomatically = false
        earlyLocationManager.desiredAccuracy = kCLLocationAccuracyThreeKilometers
        earlyLocationManager.distanceFilter = 500.0

        let status = earlyLocationManager.authorizationStatus
        if status == .authorizedAlways || status == .authorizedWhenInUse {
            earlyLocationManager.startUpdatingLocation()
            earlyLocationManager.startMonitoringSignificantLocationChanges()
            earlyLocationManager.startMonitoringVisits()
            print("🚀 TripTrackerAutoLauncher: early location manager started")
        }
    }

    @objc private func appDidFinishLaunching(_ notification: Notification) {
        let launchOptions = notification.userInfo as? [UIApplication.LaunchOptionsKey: Any]
        let isLocationRelaunch = launchOptions?[.location] != nil

        print("🚀 TripTrackerAutoLauncher: appDidFinishLaunching (locationRelaunch=\(isLocationRelaunch))")

        if isLocationRelaunch {
            initializeSDKForRelaunch(launchOptions: launchOptions)
        } else {
            // Normal launch — SDK will be initialized by Capacitor plugin later.
            // But still ensure location is running.
            let status = earlyLocationManager.authorizationStatus
            if status == .authorizedAlways {
                earlyLocationManager.startUpdatingLocation()
                earlyLocationManager.startMonitoringSignificantLocationChanges()
            }
        }
    }

    private func initializeSDKForRelaunch(launchOptions: [UIApplication.LaunchOptionsKey: Any]?) {
        guard !hasInitializedSDK else { return }
        hasInitializedSDK = true

        print("🚀 Auto-initializing SDK for location relaunch")

        // Begin background task — we have ~10 seconds
        var bgTask: UIBackgroundTaskIdentifier = .invalid
        bgTask = UIApplication.shared.beginBackgroundTask(withName: "TripTrackerRelaunch") {
            UIApplication.shared.endBackgroundTask(bgTask)
            bgTask = .invalid
        }

        // Initialize SDK — restores API config from UserDefaults
        TripTrackerSDK.initialize(config: TripTrackerConfig(), launchOptions: launchOptions)

        // End background task after a short delay to allow API calls to complete
        DispatchQueue.main.asyncAfter(deadline: .now() + 8.0) {
            if bgTask != .invalid {
                UIApplication.shared.endBackgroundTask(bgTask)
                bgTask = .invalid
            }
        }
    }

    // MARK: - CLLocationManagerDelegate

    public func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        print("🚀 AutoLauncher GPS fix: (\(location.coordinate.latitude), \(location.coordinate.longitude))")

        // If SDK is initialized, forward to the main service
        if TripTrackerSDK.isInitialized {
            // Main service is handling locations — stop the early manager
            stopEarlyManager()
            return
        }

        // SDK not initialized yet (very early in relaunch) — save to UserDefaults
        UserDefaults.standard.set(Date().timeIntervalSince1970, forKey: "tt_lastGPSTimestamp")

        // Try to initialize SDK if not done yet
        if !hasInitializedSDK {
            initializeSDKForRelaunch(launchOptions: [.location: true])
        }
    }

    public func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("🚀 AutoLauncher error: \(error.localizedDescription)")
    }

    public func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        if status == .authorizedAlways || status == .authorizedWhenInUse {
            manager.startUpdatingLocation()
            manager.startMonitoringSignificantLocationChanges()
            manager.startMonitoringVisits()
        }
    }

    /// Stop the early manager once the main LocationTrackingService takes over.
    public func stopEarlyManager() {
        earlyLocationManager.delegate = nil
        // Don't stop updates — let the main service handle it.
        // Just remove ourselves as delegate so we don't interfere.
        print("🚀 AutoLauncher: handing off to main LocationTrackingService")
    }
}
