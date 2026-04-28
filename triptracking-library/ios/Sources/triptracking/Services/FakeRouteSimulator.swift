//
//  FakeRouteSimulator.swift
//  triptracking
//
//  Simulates a ~5km motorbike route for testing background/terminated tracking.
//  Toggle on/off via UserDefaults key "tt_fakeRouteEnabled" or TripTrackerSDK.
//
//  Route: Ho Chi Minh City area — realistic motorbike movement.
//  Speed: ~35-45 km/h (10-12.5 m/s) — typical motorbike speed.
//  Duration: ~7 minutes for 5km.
//  Auto-starts 10 seconds after app launch when enabled.
//

import Foundation
import CoreLocation

public final class FakeRouteSimulator {

    public static let shared = FakeRouteSimulator()
    private init() {}

    private var timer: Timer?
    private var routeIndex = 0
    private var isRunning = false

    // ═══════════════════════════════════════════════════════════════
    // Toggle — stored in UserDefaults
    // ═══════════════════════════════════════════════════════════════

    public static var isEnabled: Bool {
        get { UserDefaults.standard.bool(forKey: "tt_fakeRouteEnabled") }
        set {
            UserDefaults.standard.set(newValue, forKey: "tt_fakeRouteEnabled")
            print("🧪 Fake route simulator: \(newValue ? "ENABLED" : "DISABLED")")
            if !newValue {
                shared.stop()
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // Start / Stop
    // ═══════════════════════════════════════════════════════════════

    /// Start simulation after a delay (default 10 seconds).
    /// Call this from SDK init or plugin load.
    public func startAfterDelay(seconds: TimeInterval = 10.0) {
        guard FakeRouteSimulator.isEnabled else { return }
        guard !isRunning else { return }

        print("🧪 Fake route will start in \(Int(seconds)) seconds…")
        DispatchQueue.main.asyncAfter(deadline: .now() + seconds) { [weak self] in
            self?.start()
        }
    }

    /// Start the simulation immediately.
    public func start() {
        guard FakeRouteSimulator.isEnabled else {
            print("🧪 Fake route not enabled — skipping")
            return
        }
        guard !isRunning else { return }

        isRunning = true
        routeIndex = 0

        let service = LocationTrackingService.shared
        service.isFakeRouteActive = true
        service.lastKnownLocation = nil
        service.lastGPSLocation = nil
        service.lastGPSSpeed = 0


        print("🧪 ═══════════════════════════════════════════════")
        print("🧪 FAKE ROUTE STARTED — \(route.count) points, ~5km motorbike")
        print("🧪 ═══════════════════════════════════════════════")

        // Inject a point every 3 seconds (simulates GPS fix interval)
        timer = Timer.scheduledTimer(withTimeInterval: 3.0, repeats: true) { [weak self] _ in
            self?.injectNextPoint()
        }
        RunLoop.main.add(timer!, forMode: .common)

        // Inject first point immediately
        injectNextPoint()
    }

    /// Stop the simulation.
    public func stop() {
        timer?.invalidate()
        timer = nil
        isRunning = false
        routeIndex = 0

        let service = LocationTrackingService.shared
        service.isFakeRouteActive = false

        print("🧪 ═══════════════════════════════════════════════")
        print("🧪 FAKE ROUTE STOPPED")
        print("🧪 ═══════════════════════════════════════════════")
    }

    // ═══════════════════════════════════════════════════════════════
    // Route injection
    // ═══════════════════════════════════════════════════════════════

    private func injectNextPoint() {
        guard routeIndex < route.count else {
            // Route complete — loop back to start
            print("🧪 Route complete — looping")
            routeIndex = 0
            return
        }

        let point = route[routeIndex]
        let speed = point.2  // m/s

        let service = LocationTrackingService.shared
        service.injectFakeGPS(
            coordinate: CLLocationCoordinate2D(latitude: point.0, longitude: point.1),
            speed: speed
        )

        let speedKmh = speed * 3.6
        print("🧪 [\(routeIndex+1)/\(route.count)] (\(String(format:"%.5f", point.0)), \(String(format:"%.5f", point.1))) \(String(format:"%.0f", speedKmh)) km/h")

        routeIndex += 1
    }

    // ═══════════════════════════════════════════════════════════════
    // Route data — ~5km motorbike route in Ho Chi Minh City
    // (latitude, longitude, speed_m/s)
    //
    // Route: Nguyen Hue → Le Loi → Hai Ba Trung → Dien Bien Phu → Vo Thi Sau
    // Speed ramps up from 0 → 40 km/h, cruises at 35-45 km/h, then stops.
    // ═══════════════════════════════════════════════════════════════

    private let route: [(Double, Double, Double)] = [
        // Phase 1: Starting up (0 → 20 km/h) — Nguyen Hue Walking Street
        (10.77380, 106.70298, 0.0),     // Still at start
        (10.77380, 106.70298, 0.0),     // Still
        (10.77380, 106.70298, 0.5),     // Starting to move
        (10.77395, 106.70300, 2.0),     // Walking speed
        (10.77420, 106.70305, 4.0),     // Slow
        (10.77460, 106.70310, 5.5),     // Accelerating

        // Phase 2: Accelerating to vehicle speed (22+ km/h) — Le Loi Street
        (10.77510, 106.70320, 7.0),     // 25 km/h — triggers auto-start
        (10.77570, 106.70335, 8.5),     // 31 km/h
        (10.77640, 106.70350, 10.0),    // 36 km/h
        (10.77720, 106.70370, 11.0),    // 40 km/h

        // Phase 3: Cruising at motorbike speed — Hai Ba Trung
        (10.77810, 106.70395, 11.5),    // 41 km/h
        (10.77910, 106.70420, 12.0),    // 43 km/h
        (10.78020, 106.70450, 11.8),    // 42 km/h
        (10.78140, 106.70480, 12.2),    // 44 km/h
        (10.78270, 106.70510, 11.5),    // 41 km/h
        (10.78410, 106.70545, 12.0),    // 43 km/h
        (10.78560, 106.70580, 11.0),    // 40 km/h
        (10.78700, 106.70610, 12.5),    // 45 km/h
        (10.78850, 106.70640, 11.8),    // 42 km/h
        (10.79000, 106.70670, 12.0),    // 43 km/h

        // Phase 4: Turn onto Dien Bien Phu — slight slowdown
        (10.79120, 106.70700, 10.0),    // 36 km/h — turning
        (10.79200, 106.70750, 8.5),     // 31 km/h
        (10.79300, 106.70830, 10.5),    // 38 km/h
        (10.79420, 106.70920, 11.0),    // 40 km/h
        (10.79550, 106.71020, 12.0),    // 43 km/h
        (10.79680, 106.71120, 11.5),    // 41 km/h
        (10.79820, 106.71230, 12.0),    // 43 km/h
        (10.79960, 106.71340, 11.8),    // 42 km/h
        (10.80100, 106.71450, 12.2),    // 44 km/h
        (10.80250, 106.71560, 11.0),    // 40 km/h

        // Phase 5: Vo Thi Sau — continuing
        (10.80400, 106.71670, 12.0),    // 43 km/h
        (10.80550, 106.71780, 11.5),    // 41 km/h
        (10.80700, 106.71890, 12.5),    // 45 km/h
        (10.80850, 106.72000, 11.0),    // 40 km/h
        (10.81000, 106.72110, 12.0),    // 43 km/h
        (10.81150, 106.72220, 11.8),    // 42 km/h
        (10.81300, 106.72330, 10.5),    // 38 km/h
        (10.81420, 106.72430, 11.0),    // 40 km/h
        (10.81550, 106.72540, 12.0),    // 43 km/h
        (10.81680, 106.72650, 11.5),    // 41 km/h

        // Phase 6: Approaching destination — slowing down
        (10.81780, 106.72740, 9.0),     // 32 km/h
        (10.81850, 106.72800, 7.0),     // 25 km/h
        (10.81900, 106.72840, 5.0),     // 18 km/h
        (10.81930, 106.72860, 3.0),     // 11 km/h
        (10.81945, 106.72870, 1.5),     // 5 km/h
        (10.81950, 106.72875, 0.5),     // Creeping
        (10.81952, 106.72876, 0.0),     // Stopped

        // Phase 7: Still — should trigger auto-end after 5 minutes
        (10.81952, 106.72876, 0.0),
        (10.81952, 106.72876, 0.0),
        (10.81952, 106.72876, 0.0),
        (10.81952, 106.72876, 0.0),
        (10.81952, 106.72876, 0.0),
        (10.81952, 106.72876, 0.0),
        (10.81952, 106.72876, 0.0),
        (10.81952, 106.72876, 0.0),
        (10.81952, 106.72876, 0.0),
        (10.81952, 106.72876, 0.0),
        // 10 still points × 3s = 30s of stillness
        // Auto-end timer needs autoStopTimeoutMinutes (default 5 min)
        // For testing, the route loops — trip should auto-end during still phase
    ]
}
