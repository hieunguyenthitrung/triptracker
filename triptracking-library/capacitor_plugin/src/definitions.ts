/**
 * TripTracker Capacitor Plugin
 *
 * Bridges TripTracker iOS native code to Ionic/JavaScript.
 * Provides: tracking status, settings pages, geofencing, notifications, logs.
 */

export interface TripTrackerPlugin {

  // ── Native Pages ──

  /** Open the full native Settings page (sliders, toggles, web monitor, CarPlay). */
  openSettings(): Promise<{ opened: boolean }>;

  /** Open the Notification Settings page (per-type push toggles + voice). */
  openNotificationSettings(): Promise<{ opened: boolean }>;

  /** Open the Geofence Manager page (map + zone list). */
  openGeofenceManager(): Promise<{ opened: boolean }>;

  /** Open the main TripTracker map + tracking view. */
  openMainView(): Promise<{ opened: boolean }>;

  /** Open the Trip History page. */
  openHistory(): Promise<{ opened: boolean }>;

  /** Open Daily Locations page. */
  openDailyLocations(): Promise<{ opened: boolean }>;

  // ── Tracking Status ──

  /** Get current tracking status, speed, distance, trip info. */
  getTrackingStatus(): Promise<TrackingStatus>;

  /** Get current GPS coordinates. */
  getCurrentLocation(): Promise<LocationResult>;

  // ── Trip History ──

  /** Get trip history. */
  getTripHistory(options?: { limit?: number }): Promise<TripHistoryResult>;

  // ── Settings ──

  /** Get all current settings. */
  getSettings(): Promise<SettingsResult>;

  /**
   * Update a single setting.
   * Keys: vehicleThreshold, saveIntervalMinutes, saveDistanceMeters,
   *       autoEndTimeoutMinutes, routeGapThresholdMeters, webMonitorEnabled,
   *       voiceFeedbackEnabled, geofencingEnabled
   */
  updateSetting(options: { key: string; value: number | boolean }): Promise<{ key: string; updated: boolean }>;

  // ── Geofence ──

  /** Get all geofence zones. */
  getGeofenceZones(): Promise<GeofenceZonesResult>;

  /** Add a new geofence zone. */
  addGeofenceZone(options: AddGeofenceOptions): Promise<{ id: string; added: boolean }>;

  /** Remove a geofence zone by ID. */
  removeGeofenceZone(options: { id: string }): Promise<{ id: string; removed: boolean }>;

  // ── Web Monitor ──

  /** Start the web monitor HTTP server on port 8080. */
  startWebMonitor(): Promise<{ started: boolean }>;

  /** Stop the web monitor server to save battery. */
  stopWebMonitor(): Promise<{ stopped: boolean }>;

  // ── Logs ──

  /** Share today's log file via share sheet. */
  sendTodayLog(): Promise<{ shared: boolean }>;

  /** Share all log files via share sheet. */
  sendAllLogs(): Promise<{ shared: boolean; count: number }>;
}

// ── Types ──

export interface TrackingStatus {
  isTracking: boolean;
  speed: number;       // m/s
  speedKmh: number;    // km/h
  distance: number;    // meters
  duration: number;    // seconds
  steps: number;
  tripId: number;
  latitude?: number;
  longitude?: number;
}

export interface LocationResult {
  latitude: number;
  longitude: number;
  speed: number;       // m/s
  speedKmh: number;    // km/h
}

export interface TripHistoryResult {
  trips: TripInfo[];
  count: number;
}

export interface TripInfo {
  id: number;
  startTime: number;   // ms since epoch
  endTime: number;     // ms since epoch (0 if active)
  distance: number;    // meters
  duration: number;    // seconds
  isActive: boolean;
}

export interface SettingsResult {
  vehicleThreshold: number;       // m/s
  vehicleThresholdKmh: number;    // km/h
  saveIntervalMinutes: number;
  saveDistanceMeters: number;
  autoEndTimeoutMinutes: number;
  routeGapThresholdMeters: number;
  webMonitorEnabled: boolean;
  voiceFeedbackEnabled: boolean;
  geofencingEnabled: boolean;
  notifyTripStart: boolean;
  notifyTripEnd: boolean;
  notifyDistanceKm: boolean;
  notifyGeofenceEnter: boolean;
  notifyGeofenceExit: boolean;
}

export interface GeofenceZonesResult {
  zones: GeofenceZoneInfo[];
  count: number;
}

export interface GeofenceZoneInfo {
  id: string;
  name: string;
  latitude: number;
  longitude: number;
  radius: number;
  notifyOnEnter: boolean;
  notifyOnExit: boolean;
  autoStopOnEnter: boolean;
}

export interface AddGeofenceOptions {
  name: string;
  latitude: number;
  longitude: number;
  radius?: number;           // default 200m
  notifyOnEnter?: boolean;   // default true
  notifyOnExit?: boolean;    // default true
  autoStopOnEnter?: boolean; // default false
}
