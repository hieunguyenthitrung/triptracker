import { WebPlugin } from '@capacitor/core';
import type { TripTrackerPlugin } from './definitions';
export declare class TripTrackerWeb extends WebPlugin implements TripTrackerPlugin {
    initializeWithConfig(): Promise<{
        initialized: boolean;
    }>;
    updateVehicleId(): Promise<{
        updated: boolean;
        vehicleId: string;
    }>;
    hasLocationPermission(): Promise<{
        granted: boolean;
    }>;
    startTracking(): Promise<{
        started: boolean;
    }>;
    stopTracking(): Promise<{
        stopped: boolean;
    }>;
    getTrackingStatus(): Promise<any>;
    getCurrentLocation(): Promise<any>;
    getTripHistory(): Promise<any>;
    getSettings(): Promise<any>;
    updateSetting(): Promise<any>;
    getGeofenceZones(): Promise<any>;
    addGeofenceZone(): Promise<any>;
    removeGeofenceZone(): Promise<any>;
    startWebMonitor(): Promise<any>;
    stopWebMonitor(): Promise<any>;
    sendTodayLog(): Promise<any>;
    sendAllLogs(): Promise<any>;
    sendRecentLogs(): Promise<any>;
    writeLog(): Promise<void>;
    endTrip(): Promise<{
        ended: boolean;
        tripId?: number;
        reason?: string;
    }>;
    updateToolId(): Promise<{
        updated: boolean;
        toolId: string;
    }>;
    resetConfig(): Promise<{
        reset: boolean;
    }>;
    startHeartbeatTimer(): Promise<{
        started: boolean;
    }>;
    stopHeartbeatTimer(): Promise<{
        stopped: boolean;
    }>;
    setTripNotifications(): Promise<{
        notifyTripStart: boolean;
        notifyTripEnd: boolean;
    }>;
}
