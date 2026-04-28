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

    // Route: Etown Cộng Hòa (Tân Bình) → Gò Vấp
// Via: Cộng Hòa → Hoàng Văn Thụ → Phan Đình Phùng → Nguyễn Kiệm → Gò Vấp
// Distance: ~5.5 km | Duration: ~15 min
// Format: (latitude, longitude, speed m/s)

private let route: [(Double, Double, Double)] = [

    // ── Phase 1: Still at Etown Cộng Hòa ─────────────────────────────────
    // 364 Cộng Hòa, Tân Bình
    (10.80170, 106.65540, 0.0),     // Still at Etown parking
    (10.80170, 106.65540, 0.0),     // Still
    (10.80170, 106.65540, 0.0),     // Still
    (10.80175, 106.65542, 0.5),     // Starting engine
    (10.80180, 106.65545, 1.5),     // Exiting parking
    (10.80190, 106.65550, 3.0),     // Slow onto Cộng Hòa

    // ── Phase 2: Accelerating on Cộng Hòa street ─────────────────────────
    (10.80210, 106.65560, 5.0),     // 18 km/h
    (10.80240, 106.65575, 7.0),     // 25 km/h — triggers auto-start
    (10.80280, 106.65595, 8.5),     // 31 km/h
    (10.80330, 106.65620, 10.0),    // 36 km/h
    (10.80390, 106.65650, 11.5),    // 41 km/h
    (10.80460, 106.65685, 12.0),    // 43 km/h

    // ── Phase 3: Cruising on Cộng Hòa heading north ───────────────────────
    (10.80540, 106.65720, 12.5),    // 45 km/h
    (10.80630, 106.65755, 11.8),    // 42 km/h
    (10.80720, 106.65785, 12.0),    // 43 km/h
    (10.80820, 106.65810, 11.5),    // 41 km/h
    (10.80920, 106.65830, 12.2),    // 44 km/h
    (10.81020, 106.65850, 11.0),    // 40 km/h
    (10.81120, 106.65865, 12.0),    // 43 km/h
    (10.81220, 106.65878, 11.8),    // 42 km/h

    // ── Phase 4: Turn right onto Hoàng Văn Thụ ───────────────────────────
    // Intersection Cộng Hòa - Hoàng Văn Thụ
    (10.81310, 106.65890, 9.0),     // 32 km/h — slowing for turn
    (10.81380, 106.65905, 7.0),     // 25 km/h — turning right
    (10.81430, 106.65970, 8.0),     // 29 km/h — onto Hoàng Văn Thụ
    (10.81480, 106.66050, 10.0),    // 36 km/h
    (10.81520, 106.66140, 11.5),    // 41 km/h
    (10.81550, 106.66240, 12.0),    // 43 km/h
    (10.81575, 106.66350, 11.8),    // 42 km/h
    (10.81595, 106.66460, 12.2),    // 44 km/h
    (10.81610, 106.66580, 11.5),    // 41 km/h
    (10.81620, 106.66700, 12.0),    // 43 km/h

    // ── Phase 5: Continue on Hoàng Văn Thụ → Phan Đình Phùng ─────────────
    (10.81625, 106.66820, 11.0),    // 40 km/h
    (10.81628, 106.66940, 12.5),    // 45 km/h
    (10.81625, 106.67060, 11.8),    // 42 km/h
    (10.81618, 106.67180, 12.0),    // 43 km/h
    (10.81608, 106.67290, 11.5),    // 41 km/h

    // ── Phase 6: Turn left onto Phan Đình Phùng heading north ────────────
    // Intersection Hoàng Văn Thụ - Phan Đình Phùng
    (10.81600, 106.67380, 8.5),     // 31 km/h — slowing
    (10.81620, 106.67440, 7.0),     // 25 km/h — turning left
    (10.81680, 106.67470, 9.0),     // 32 km/h — onto Phan Đình Phùng
    (10.81760, 106.67490, 11.0),    // 40 km/h
    (10.81850, 106.67505, 12.0),    // 43 km/h
    (10.81950, 106.67515, 11.5),    // 41 km/h
    (10.82055, 106.67522, 12.2),    // 44 km/h
    (10.82165, 106.67528, 11.8),    // 42 km/h
    (10.82280, 106.67532, 12.0),    // 43 km/h
    (10.82400, 106.67535, 11.0),    // 40 km/h

    // ── Phase 7: Onto Nguyễn Kiệm heading to Gò Vấp ──────────────────────
    (10.82520, 106.67540, 12.5),    // 45 km/h
    (10.82640, 106.67548, 11.8),    // 42 km/h
    (10.82760, 106.67558, 12.0),    // 43 km/h
    (10.82880, 106.67570, 11.5),    // 41 km/h
    (10.82990, 106.67585, 12.2),    // 44 km/h
    (10.83100, 106.67602, 11.0),    // 40 km/h
    (10.83210, 106.67620, 12.0),    // 43 km/h
    (10.83320, 106.67640, 11.8),    // 42 km/h
    (10.83430, 106.67662, 12.5),    // 45 km/h
    (10.83540, 106.67685, 11.0),    // 40 km/h

    // ── Phase 8: Entering Gò Vấp district ────────────────────────────────
    (10.83650, 106.67710, 12.0),    // 43 km/h
    (10.83760, 106.67738, 11.5),    // 41 km/h
    (10.83865, 106.67768, 10.0),    // 36 km/h — approaching destination
    (10.83955, 106.67800, 8.5),     // 31 km/h
    (10.84030, 106.67832, 7.0),     // 25 km/h
    (10.84090, 106.67860, 5.5),     // 20 km/h
    (10.84130, 106.67882, 4.0),     // 14 km/h
    (10.84160, 106.67898, 2.5),     // 9 km/h
    (10.84178, 106.67908, 1.5),     // 5 km/h
    (10.84188, 106.67914, 0.5),     // Creeping

    // ── Phase 9: Arrived at Gò Vấp ───────────────────────────────────────
    (10.84192, 106.67916, 0.0),     // Stopped
    (10.84192, 106.67916, 0.0),     // Still
    (10.84192, 106.67916, 0.0),     // Still
    (10.84192, 106.67916, 0.0),     // Still
    (10.84192, 106.67916, 0.0),     // Still
    (10.84192, 106.67916, 0.0),     // Still
    (10.84192, 106.67916, 0.0),     // Still
    (10.84192, 106.67916, 0.0),     // Still
    (10.84192, 106.67916, 0.0),     // Still
    (10.84192, 106.67916, 0.0),     // Still — auto-end after 5 min
]
}
