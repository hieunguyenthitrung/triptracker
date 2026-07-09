import { WebPlugin } from '@capacitor/core';
export class TripTrackerWeb extends WebPlugin {
    async initializeWithConfig() {
        throw this.unavailable('initializeWithConfig is only available on iOS/Android');
    }
    async updateVehicleId() {
        throw this.unavailable('updateVehicleId is only available on iOS/Android');
    }
    async hasLocationPermission() {
        throw this.unavailable('hasLocationPermission is only available on iOS/Android');
    }
    async startTracking() {
        throw this.unavailable('startTracking is only available on iOS/Android');
    }
    async stopTracking() {
        throw this.unavailable('stopTracking is only available on iOS/Android');
    }
    async getTrackingStatus() {
        throw this.unavailable('getTrackingStatus is only available on iOS');
    }
    async getCurrentLocation() {
        throw this.unavailable('getCurrentLocation is only available on iOS');
    }
    async getTripHistory() {
        throw this.unavailable('getTripHistory is only available on iOS');
    }
    async getSettings() {
        throw this.unavailable('getSettings is only available on iOS');
    }
    async updateSetting() {
        throw this.unavailable('updateSetting is only available on iOS');
    }
    async getGeofenceZones() {
        throw this.unavailable('getGeofenceZones is only available on iOS');
    }
    async addGeofenceZone() {
        throw this.unavailable('addGeofenceZone is only available on iOS');
    }
    async removeGeofenceZone() {
        throw this.unavailable('removeGeofenceZone is only available on iOS');
    }
    async startWebMonitor() {
        throw this.unavailable('startWebMonitor is only available on iOS');
    }
    async stopWebMonitor() {
        throw this.unavailable('stopWebMonitor is only available on iOS');
    }
    async sendTodayLog() {
        throw this.unavailable('sendTodayLog is only available on iOS');
    }
    async sendAllLogs() {
        throw this.unavailable('sendAllLogs is only available on iOS/Android');
    }
    async sendRecentLogs() {
        throw this.unavailable('sendRecentLogs is only available on iOS/Android');
    }
    async writeLog() {
        throw this.unavailable('writeLog is only available on iOS/Android');
    }
    async endTrip() {
        throw this.unavailable('endTrip is only available on iOS/Android');
    }
    async updateToolId() {
        throw this.unavailable('updateToolId is only available on iOS/Android');
    }
    async resetConfig() {
        throw this.unavailable('resetConfig is only available on iOS/Android');
    }
    async startHeartbeatTimer() {
        throw this.unavailable('startHeartbeatTimer is only available on iOS/Android');
    }
    async stopHeartbeatTimer() {
        throw this.unavailable('stopHeartbeatTimer is only available on iOS/Android');
    }
    async setTripNotifications() {
        throw this.unavailable('setTripNotifications is only available on iOS/Android');
    }
}
//# sourceMappingURL=web.js.map