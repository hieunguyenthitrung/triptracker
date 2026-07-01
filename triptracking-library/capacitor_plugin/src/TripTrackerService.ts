/**
 * TripTrackerService
 *
 * Drop-in Angular/Ionic service that wires up:
 *   1. JS heartbeat timer  — calls jsHeartbeat() every 30 s so the native SDK
 *      knows the webview is alive. Also listens for native "heartbeat" events
 *      so JS can reconnect a BLE dongle while the app is backgrounded.
 *   2. Trip notifications  — listens for trackingStateChange to detect
 *      trip start / end, and exposes helpers to turn those push notifications
 *      on or off at runtime via updateSetting().
 *
 * Usage (Angular):
 *   providers: [TripTrackerService]
 *
 *   constructor(private tracker: TripTrackerService) {}
 *
 *   ngOnInit() {
 *     this.tracker.startHeartbeat((ts) => {
 *       // native heartbeat fired → try BLE reconnect here
 *       this.myBleService.reconnect();
 *     });
 *
 *     this.tracker.onTripStart(() => console.log('trip started'));
 *     this.tracker.onTripEnd(()   => console.log('trip ended'));
 *   }
 *
 *   ngOnDestroy() {
 *     this.tracker.destroy();
 *   }
 */

import { Injectable, OnDestroy } from '@angular/core';
import type { PluginListenerHandle } from '@capacitor/core';
import { TripTracker } from './index';

@Injectable({ providedIn: 'root' })
export class TripTrackerService implements OnDestroy {

  // ── JS-side heartbeat timer ──────────────────────────────────────────
  private jsHeartbeatInterval: ReturnType<typeof setInterval> | null = null;

  // ── Native listener handles (needed for cleanup) ─────────────────────
  private nativeHeartbeatHandle: PluginListenerHandle | null = null;
  private appForegroundHandle: PluginListenerHandle | null = null;
  private trackingStateHandle: PluginListenerHandle | null = null;

  // ── Callbacks set by the caller ──────────────────────────────────────
  private onHeartbeatCb: ((timestamp: number) => void) | null = null;
  private onTripStartCb: (() => void) | null = null;
  private onTripEndCb: (() => void) | null = null;

  // ─────────────────────────────────────────────────────────────────────
  // Heartbeat
  // ─────────────────────────────────────────────────────────────────────

  /**
   * Start the JS heartbeat.
   *
   * - Sends jsHeartbeat() to native every `intervalMs` (default 30 000 ms).
   *   Native side logs a warning if it hasn't heard from JS for 2+ minutes.
   * - Also subscribes to the native "heartbeat" event so your callback fires
   *   whenever native wakes the webview (useful for BLE reconnect logic).
   * - Subscribes to "appForeground" so the same callback fires when the user
   *   returns to the app — a good moment to retry BLE connection too.
   *
   * Safe to call multiple times — subsequent calls are no-ops until destroy().
   */
  async startHeartbeat(
    onNativeHeartbeat?: (timestamp: number) => void,
    intervalMs = 30_000,
  ): Promise<void> {
    if (this.jsHeartbeatInterval) return; // already running

    this.onHeartbeatCb = onNativeHeartbeat ?? null;

    // 1. JS → Native ping every intervalMs
    this.jsHeartbeatInterval = setInterval(async () => {
      try {
        await TripTracker.jsHeartbeat();
      } catch {
        // native not ready yet — ignore
      }
    }, intervalMs);

    // 2. Native → JS heartbeat (wakes webview from background)
    this.nativeHeartbeatHandle = await TripTracker.addListener(
      'heartbeat',
      ({ timestamp }) => {
        this.onHeartbeatCb?.(timestamp);
      },
    );

    // 3. App foregrounded — also a good moment to reconnect BLE
    this.appForegroundHandle = await TripTracker.addListener(
      'appForeground',
      () => {
        this.onHeartbeatCb?.(Date.now());
      },
    );
  }

  /** Stop the JS heartbeat timer and remove native listeners. */
  stopHeartbeat(): void {
    if (this.jsHeartbeatInterval) {
      clearInterval(this.jsHeartbeatInterval);
      this.jsHeartbeatInterval = null;
    }
    this.nativeHeartbeatHandle?.remove();
    this.nativeHeartbeatHandle = null;
    this.appForegroundHandle?.remove();
    this.appForegroundHandle = null;
    this.onHeartbeatCb = null;
  }

  // ─────────────────────────────────────────────────────────────────────
  // Trip start / end events
  // ─────────────────────────────────────────────────────────────────────

  /**
   * Register a callback for trip start.
   * Replaces any previously registered callback.
   * Starts listening on the native trackingStateChange event if not already.
   */
  async onTripStart(callback: () => void): Promise<void> {
    this.onTripStartCb = callback;
    await this.ensureTrackingStateListener();
  }

  /**
   * Register a callback for trip end.
   * Replaces any previously registered callback.
   * Starts listening on the native trackingStateChange event if not already.
   */
  async onTripEnd(callback: () => void): Promise<void> {
    this.onTripEndCb = callback;
    await this.ensureTrackingStateListener();
  }

  private async ensureTrackingStateListener(): Promise<void> {
    if (this.trackingStateHandle) return;
    this.trackingStateHandle = await TripTracker.addListener(
      'trackingStateChange',
      ({ isTracking }) => {
        if (isTracking) {
          this.onTripStartCb?.();
        } else {
          this.onTripEndCb?.();
        }
      },
    );
  }

  // ─────────────────────────────────────────────────────────────────────
  // Notification toggles
  // ─────────────────────────────────────────────────────────────────────

  /** Enable or disable the trip-start push notification. */
  setTripStartNotification(enabled: boolean): Promise<{ key: string; updated: boolean }> {
    return TripTracker.updateSetting({ key: 'notifyTripStart', value: enabled });
  }

  /** Enable or disable the trip-end push notification. */
  setTripEndNotification(enabled: boolean): Promise<{ key: string; updated: boolean }> {
    return TripTracker.updateSetting({ key: 'notifyTripEnd', value: enabled });
  }

  /**
   * Enable or disable BOTH trip-start and trip-end notifications together.
   * Equivalent to the "notifyTrip" toggle in Settings.
   */
  async setTripNotifications(enabled: boolean): Promise<void> {
    await TripTracker.updateSetting({ key: 'notifyTripStart', value: enabled });
    await TripTracker.updateSetting({ key: 'notifyTripEnd', value: enabled });
  }

  // ─────────────────────────────────────────────────────────────────────
  // Cleanup
  // ─────────────────────────────────────────────────────────────────────

  /** Call in ngOnDestroy (or equivalent) to remove all listeners and timers. */
  destroy(): void {
    this.stopHeartbeat();
    this.trackingStateHandle?.remove();
    this.trackingStateHandle = null;
    this.onTripStartCb = null;
    this.onTripEndCb = null;
  }

  ngOnDestroy(): void {
    this.destroy();
  }
}
