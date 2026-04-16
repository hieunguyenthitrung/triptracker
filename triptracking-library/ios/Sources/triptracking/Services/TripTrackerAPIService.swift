import Foundation
import CoreLocation
import UIKit

// MARK: - API Configuration

public struct TripTrackerAPIConfig {
    public var pingURL: String = ""
    public var endURL: String = ""
    public var userId: String = ""
    public var vehicleId: String = ""
    public var osInfo: String = ""
    public var routeId: String = ""
    public var authorizationKey: String = ""
    public var apiAuthKey: String = ""

    public var isConfigured: Bool { !pingURL.isEmpty && !endURL.isEmpty && !userId.isEmpty }

    public init() { self.osInfo = "iOS \(UIDevice.current.systemVersion)" }

    public init(from dict: [String: Any]) {
        self.init()
        if let v = dict["pingURL"] as? String         { pingURL = v }
        if let v = dict["endURL"] as? String           { endURL = v }
        if let v = dict["userId"] as? String           { userId = v }
        if let v = dict["vehicleId"] as? String        { vehicleId = v }
        if let v = dict["osInfo"] as? String           { osInfo = v }
        if let v = dict["routeId"] as? String          { routeId = v }
        if let v = dict["authorizationKey"] as? String { authorizationKey = v }
        if let v = dict["apiAuthKey"] as? String       { apiAuthKey = v }
    }
}

// MARK: - API Service

public final class TripTrackerAPIService {
    public static let shared = TripTrackerAPIService()
    private init() {}

    public var config = TripTrackerAPIConfig()
    public var isEnabled: Bool { config.isConfigured }

    private let session: URLSession = {
        let cfg = URLSessionConfiguration.default
        cfg.timeoutIntervalForRequest = 15
        return URLSession(configuration: cfg)
    }()

    // POST /ping/v2
    public func sendPing(location: CLLocation, isMoving: Bool, speed: Float, activityType: String, routeId: String? = nil) {
        guard isEnabled else { return }
        let body: [String: Any] = [
            "user_Id": config.userId,
            "vehicle_Id": config.vehicleId,
            "os_Info": config.osInfo,
            "location": [[
                "is_Moving": isMoving,
                "timestamp": ISO8601DateFormatter().string(from: Date()),
                "latitude": location.coordinate.latitude,
                "longitude": location.coordinate.longitude,
                "speed": speed,
                "activityType": activityType,
                "route_Id": routeId ?? config.routeId
            ]]
        ]
        post(url: config.pingURL, body: body) { ok in
            print("📡 API ping \(ok ? "OK" : "FAIL"): \(location.coordinate.latitude),\(location.coordinate.longitude)")
        }
    }

    // POST /ping/v2 batch
    public func sendPingBatch(locations: [(CLLocation, Bool, Float, String, Date)], routeId: String? = nil) {
        guard isEnabled, !locations.isEmpty else { return }
        let fmt = ISO8601DateFormatter()
        let arr: [[String: Any]] = locations.map { loc, moving, spd, activity, ts in
            ["is_Moving": moving, "timestamp": fmt.string(from: ts),
             "latitude": loc.coordinate.latitude, "longitude": loc.coordinate.longitude,
             "speed": spd, "activityType": activity, "route_Id": routeId ?? config.routeId]
        }
        let body: [String: Any] = ["user_Id": config.userId, "vehicle_Id": config.vehicleId, "os_Info": config.osInfo, "location": arr]
        post(url: config.pingURL, body: body) { ok in
            print("📡 API batch (\(locations.count)): \(ok ? "OK" : "FAIL")")
        }
    }

    // POST /end
    public func sendTripEnd(location: CLLocation) {
        guard isEnabled else { return }
        let body: [String: Any] = [
            "user_Id": config.userId,
            "timestamp": ISO8601DateFormatter().string(from: Date()),
            "latitude": location.coordinate.latitude,
            "longitude": location.coordinate.longitude
        ]
        post(url: config.endURL, body: body) { [weak self] ok in
            print("📡 API trip-end \(ok ? "OK" : "FAIL")")
            if !ok {
                DispatchQueue.global().asyncAfter(deadline: .now() + 5) {
                    self?.post(url: self?.config.endURL ?? "", body: body, completion: nil)
                }
            }
        }
    }

    public func setRouteId(_ id: String) { config.routeId = id }

    private func post(url: String, body: [String: Any], completion: ((Bool) -> Void)?) {
        guard let url = URL(string: url) else { completion?(false); return }
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        if !config.authorizationKey.isEmpty { req.setValue(config.authorizationKey, forHTTPHeaderField: "AuthorizationKey") }
        if !config.apiAuthKey.isEmpty { req.setValue(config.apiAuthKey, forHTTPHeaderField: "api-auth-key") }
        do { req.httpBody = try JSONSerialization.data(withJSONObject: body) } catch { completion?(false); return }
        session.dataTask(with: req) { data, response, error in
            let code = (response as? HTTPURLResponse)?.statusCode ?? 0
            completion?((200...299).contains(code))
        }.resume()
    }
}
