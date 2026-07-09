import { WebPlugin } from '@capacitor/core';

import type { TripTrackerPlugin } from './definitions';

export class TripTrackerWeb extends WebPlugin implements TripTrackerPlugin {

  async initializeWithConfig(): Promise<{ initialized: boolean }> {
    throw this.unavailable('initializeWithConfig is only available on iOS/Android');
  }

  async updateVehicleId(): Promise<{ updated: boolean; vehicleId: string }> {
    throw this.unavailable('updateVehicleId is only available on iOS/Android');
  }

  async hasLocationPermission(): Promise<{ granted: boolean }> {
    throw this.unavailable('hasLocationPermission is only available on iOS/Android');
  }

  async startTracking(): Promise<{ started: boolean }> {
    throw this.unavailable('startTracking is only available on iOS/Android');
  }

  async stopTracking(): Promise<{ stopped: boolean }> {
    throw this.unavailable('stopTracking is only available on iOS/Android');
  }

  async getTrackingStatus(): Promise<any> {
    throw this.unavailable('getTrackingStatus is only available on iOS');
  }

  async getCurrentLocation(): Promise<any> {
    throw this.unavailable('getCurrentLocation is only available on iOS');
  }

  async getTripHistory(): Promise<any> {
    throw this.unavailable('getTripHistory is only available on iOS');
  }

  async getSettings(): Promise<any> {
    throw this.unavailable('getSettings is only available on iOS');
  }

  async updateSetting(): Promise<any> {
    throw this.unavailable('updateSetting is only available on iOS');
  }

  async getGeofenceZones(): Promise<any> {
    throw this.unavailable('getGeofenceZones is only available on iOS');
  }

  async addGeofenceZone(): Promise<any> {
    throw this.unavailable('addGeofenceZone is only available on iOS');
  }

  async removeGeofenceZone(): Promise<any> {
    throw this.unavailable('removeGeofenceZone is only available on iOS');
  }

  async startWebMonitor(): Promise<any> {
    throw this.unavailable('startWebMonitor is only available on iOS');
  }

  async stopWebMonitor(): Promise<any> {
    throw this.unavailable('stopWebMonitor is only available on iOS');
  }

  async sendTodayLog(): Promise<any> {
    throw this.unavailable('sendTodayLog is only available on iOS');
  }

  async sendAllLogs(): Promise<any> {
    throw this.unavailable('sendAllLogs is only available on iOS/Android');
  }

  async sendRecentLogs(): Promise<any> {
    throw this.unavailable('sendRecentLogs is only available on iOS/Android');
  }

  async writeLog(): Promise<void> {
    throw this.unavailable('writeLog is only available on iOS/Android');
  }

  async endTrip(): Promise<{ ended: boolean; tripId?: number; reason?: string }> {
    throw this.unavailable('endTrip is only available on iOS/Android');
  }

  async updateToolId(): Promise<{ updated: boolean; toolId: string }> {
    throw this.unavailable('updateToolId is only available on iOS/Android');
  }

  async resetConfig(): Promise<{ reset: boolean }> {
    throw this.unavailable('resetConfig is only available on iOS/Android');
  }

  async startHeartbeatTimer(): Promise<{ started: boolean }> {
    throw this.unavailable('startHeartbeatTimer is only available on iOS/Android');
  }

  async stopHeartbeatTimer(): Promise<{ stopped: boolean }> {
    throw this.unavailable('stopHeartbeatTimer is only available on iOS/Android');
  }

  async setTripNotifications(): Promise<{ notifyTripStart: boolean; notifyTripEnd: boolean }> {
    throw this.unavailable('setTripNotifications is only available on iOS/Android');
  }
}
