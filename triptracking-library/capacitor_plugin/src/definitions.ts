/**
 * TripTracker Capacitor Plugin
 *
 * Bridges TripTracker iOS native code to Ionic/JavaScript.
 * Provides: tracking status, settings pages, geofencing, notifications, logs.
 */

import type { PluginListenerHandle } from '@capacitor/core';

export interface TripTrackerPlugin {

  // ── Initialize ──

  /**
   * Initialize TripTracker SDK with custom config.
   * Call once at app startup before using any other methods.
   * Only the values you pass are changed — everything else stays at defaults.
   *
   * Defaults:
   *   saveIntervalMinutes: 15, saveDistanceMeters: 30, vehicleThreshold: 6.0 (m/s),
   *   transportType: 0 (Car), autoStopTimeoutMinutes: 5, routeGapMeters: 500,
   *   geofenceEnabled: false, webMonitorEnabled: false, voiceFeedbackEnabled: true,
   *   notifyTripStart: true, notifyTripEnd: true, notifyDistanceKm: true,
   *   notifyGeofenceEnter: true, notifyGeofenceExit: true
   */
  initializeWithConfig(options?: TripTrackerConfigOptions): Promise<{
    initialized: boolean;
    permissionGranted?: boolean;
    trackingStarted?: boolean;
  }>;

  /**
   * Update vehicle_id at runtime.
   */
  updateVehicleId(options: { vehicleId: string }): Promise<{
    updated: boolean;
    vehicleId: string;
  }>;

  // ── Permission & Tracking Control ──

  hasLocationPermission(): Promise<{ granted: boolean }>;
  startTracking(): Promise<{ started: boolean }>;
  stopTracking(): Promise<{ stopped: boolean }>;

  // ── Native Pages ──

  openSettings(): Promise<{ opened: boolean }>;
  openNotificationSettings(): Promise<{ opened: boolean }>;
  openGeofenceManager(): Promise<{ opened: boolean }>;
  openMainView(): Promise<{ opened: boolean }>;
  openHistory(): Promise<{ opened: boolean }>;
  openDailyLocations(): Promise<{ opened: boolean }>;

  // ── Tracking Status ──

  getTrackingStatus(): Promise<TrackingStatus>;
  getCurrentLocation(options?: { timeout?: number }): Promise<LocationResult>;

  // ── Trip History ──

  getTripHistory(options?: { limit?: number }): Promise<TripHistoryResult>;

  // ── Settings ──

  getSettings(): Promise<SettingsResult>;

  /**
   * Update a single setting by key.
   *
   * Tracking (number): vehicleThreshold, saveIntervalMinutes, saveDistanceMeters,
   *   autoEndTimeoutMinutes, routeGapThresholdMeters
   *
   * Feature toggles (boolean): webMonitorEnabled, voiceFeedbackEnabled, geofencingEnabled
   *
   * Notification toggles (boolean):
   *   notifyTrip      — turn trip-start AND trip-end on/off together
   *   notifyTripStart — trip-start push only
   *   notifyTripEnd   — trip-end push only
   */
  updateSetting(options: { key: string; value: number | boolean }): Promise<{ key: string; updated: boolean }>;

  // ── Geofence ──

  getGeofenceZones(): Promise<GeofenceZonesResult>;
  addGeofenceZone(options: AddGeofenceOptions): Promise<{ id: string; added: boolean }>;
  removeGeofenceZone(options: { id: string }): Promise<{ id: string; removed: boolean }>;

  // ── Web Monitor ──

  startWebMonitor(): Promise<{ started: boolean }>;
  stopWebMonitor(): Promise<{ stopped: boolean }>;

  // ── Logs ──

  sendTodayLog(): Promise<{ shared: boolean }>;
  sendAllLogs(): Promise<{ shared: boolean; count: number }>;
  sendRecentLogs(options?: { days?: number }): Promise<{ path: string }>;
  writeLog(options: { message: string }): Promise<void>;

  // ── Trip Control ──

  endTrip(): Promise<{ ended: boolean; tripId?: number; reason?: string }>;
  updateToolId(options: { toolId: string }): Promise<{ updated: boolean; toolId: string }>;
  resetConfig(): Promise<{ reset: boolean }>;

  // ── Heartbeat Timer ──

  /**
   * Start the native heartbeat timer.
   * Fires a "heartbeat" event to JS every `intervalSeconds` (default 10s).
   * Useful to wake Ionic code in background so it can reconnect a BLE dongle.
   * No-op if the timer is already running.
   */
  startHeartbeatTimer(options?: { intervalSeconds?: number }): Promise<{ started: boolean }>;

  /** Stop the native heartbeat timer. */
  stopHeartbeatTimer(): Promise<{ stopped: boolean }>;

  // ── Trip Notifications ──

  /**
   * Enable or disable trip start / end push notifications at runtime.
   *
   * Usage:
   *   { notify: false }              — turn both off together
   *   { start: true, end: false }    — control each independently
   *
   * Returns current state of both flags after the update.
   */
  setTripNotifications(options: {
    /** Set both notifyTripStart and notifyTripEnd at once */
    notify?: boolean;
    /** Set only notifyTripStart */
    start?: boolean;
    /** Set only notifyTripEnd */
    end?: boolean;
  }): Promise<{
    notifyTripStart: boolean;
    notifyTripEnd: boolean;
  }>;

  // ═══════════════════════════════════════════════════════════════════
  // Event Listeners
  // ═══════════════════════════════════════════════════════════════════

  addListener(eventName: 'activityChange', listener: (event: ActivityChangeEvent) => void): Promise<PluginListenerHandle>;
  addListener(eventName: 'locationUpdate', listener: (event: LocationUpdateEvent) => void): Promise<PluginListenerHandle>;
  addListener(eventName: 'trackingStateChange', listener: (event: TrackingStateChangeEvent) => void): Promise<PluginListenerHandle>;
  addListener(eventName: 'statsUpdate', listener: (event: StatsUpdateEvent) => void): Promise<PluginListenerHandle>;

  /**
   * Fired by the native heartbeat timer every intervalSeconds.
   * Wakes the WKWebView JS engine in background — use to run BLE/dongle reconnect logic.
   */
  addListener(eventName: 'heartbeat', listener: (event: HeartbeatEvent) => void): Promise<PluginListenerHandle>;

  /** Fired when the app returns to foreground. */
  addListener(eventName: 'appForeground', listener: () => void): Promise<PluginListenerHandle>;

  removeAllListeners(): Promise<void>;
}

// ── Types ──

export interface TrackingStatus {
  isTracking: boolean;
  speed: number;
  speedKmh: number;
  distance: number;
  duration: number;
  steps: number;
  tripId: number;
  latitude?: number;
  longitude?: number;
}

export interface LocationResult {
  latitude: number;
  longitude: number;
  speed: number;
  speedKmh: number;
}

export interface TripHistoryResult {
  trips: TripInfo[];
  count: number;
}

export interface TripInfo {
  id: number;
  startTime: number;
  endTime: number;
  distance: number;
  duration: number;
  isActive: boolean;
}

export interface SettingsResult {
  vehicleThreshold: number;
  vehicleThresholdKmh: number;
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
  radius?: number;
  notifyOnEnter?: boolean;
  notifyOnExit?: boolean;
  autoStopOnEnter?: boolean;
}

export interface TripTrackerConfigOptions {
  saveIntervalMinutes?: number;
  saveDistanceMeters?: number;
  vehicleThreshold?: number;
  transportType?: number;
  autoStopTimeoutMinutes?: number;
  routeGapMeters?: number;
  geofenceEnabled?: boolean;
  webMonitorEnabled?: boolean;
  voiceFeedbackEnabled?: boolean;
  /** Enable push for trip start (default true) */
  notifyTripStart?: boolean;
  /** Enable push for trip end (default true) */
  notifyTripEnd?: boolean;
  /** Enable push for trip start AND end together (default true) */
  notifyTrip?: boolean;
  notifyDistanceKm?: boolean;
  notifyGeofenceEnter?: boolean;
  notifyGeofenceExit?: boolean;
  pingURL?: string;
  endURL?: string;
  userId?: string;
  vehicleId?: string;
  osInfo?: string;
  routeId?: string;
  authorizationKey?: string;
  apiAuthKey?: string;
  apiAuthToken?: string;
  toolId?: string;
}

export interface ActivityChangeEvent {
  activity: string;
  transition: string;
}

export interface LocationUpdateEvent {
  latitude: number;
  longitude: number;
  speed: number;
  speedKmh: number;
  accuracy: number;
  source: string;
  distance: number;
  timestamp: number;
}

export interface TrackingStateChangeEvent {
  isTracking: boolean;
}

export interface StatsUpdateEvent {
  speed: number;
  speedKmh: number;
  distance: number;
  duration: number;
}

export interface HeartbeatEvent {
  timestamp: number;
}
