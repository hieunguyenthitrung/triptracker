import { Component, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IonicModule } from '@ionic/angular';
import { registerPlugin } from '@capacitor/core';
import type { TripTrackerPlugin, TrackingStatus, SettingsResult } from 'capacitor-trip-tracker';

const TripTracker = registerPlugin<TripTrackerPlugin>('TripTracker');

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, IonicModule],
  template: `
    <ion-header>
      <ion-toolbar color="primary">
        <ion-title>TripTracker Test</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">

      <!-- Status Card -->
      <ion-card>
        <ion-card-header>
          <ion-card-title>Status</ion-card-title>
        </ion-card-header>
        <ion-card-content>
          <p><strong>SDK:</strong> {{ sdkStatus }}</p>
          <p><strong>Permission:</strong> {{ permStatus }}</p>
          <p><strong>Tracking:</strong> {{ trackingStatus }}</p>
          <p><strong>Speed:</strong> {{ speed }} km/h</p>
          <p><strong>Distance:</strong> {{ distance }} m</p>
          <p><strong>Steps:</strong> {{ steps }}</p>
          <p><strong>Trip #:</strong> {{ tripId }}</p>
        </ion-card-content>
      </ion-card>

      <!-- SDK Control -->
      <ion-item-group>
        <ion-item-divider color="light">
          <ion-label>SDK Control</ion-label>
        </ion-item-divider>
      </ion-item-group>

      <ion-button expand="block" color="success" (click)="initSDK()">
        Initialize SDK
      </ion-button>
      <ion-button expand="block" color="tertiary" (click)="checkPermission()">
        Check Permission
      </ion-button>
      <ion-button expand="block" color="success" (click)="startTracking()">
        Start Tracking
      </ion-button>
      <ion-button expand="block" color="danger" (click)="stopTracking()">
        Stop Tracking
      </ion-button>
      <ion-button expand="block" color="medium" (click)="refreshStatus()">
        Refresh Status
      </ion-button>

      <!-- Native Pages -->
      <ion-item-group>
        <ion-item-divider color="light">
          <ion-label>Native Pages</ion-label>
        </ion-item-divider>
      </ion-item-group>

      <ion-button expand="block" (click)="openPage('mainView')">Open Main Map</ion-button>
      <ion-button expand="block" (click)="openPage('settings')">Open Settings</ion-button>
      <ion-button expand="block" (click)="openPage('history')">Open History</ion-button>
      <ion-button expand="block" (click)="openPage('dailyLocations')">Open Daily Locations</ion-button>
      <ion-button expand="block" (click)="openPage('geofence')">Open Geofence</ion-button>
      <ion-button expand="block" (click)="openPage('notifications')">Open Notifications</ion-button>

      <!-- Data Queries -->
      <ion-item-group>
        <ion-item-divider color="light">
          <ion-label>Data Queries</ion-label>
        </ion-item-divider>
      </ion-item-group>

      <ion-button expand="block" color="secondary" (click)="getSettings()">Get Settings</ion-button>
      <ion-button expand="block" color="secondary" (click)="getLocation()">Get Current Location</ion-button>
      <ion-button expand="block" color="secondary" (click)="getGeofenceZones()">Get Geofence Zones</ion-button>
      <ion-button expand="block" color="secondary" (click)="getTripHistory()">Get Trip History</ion-button>

      <!-- Web Monitor -->
      <ion-item-group>
        <ion-item-divider color="light">
          <ion-label>Web Monitor</ion-label>
        </ion-item-divider>
      </ion-item-group>

      <ion-button expand="block" color="warning" (click)="startWebMonitor()">Start Web Monitor</ion-button>
      <ion-button expand="block" color="medium" (click)="stopWebMonitor()">Stop Web Monitor</ion-button>

      <!-- Logs -->
      <ion-item-group>
        <ion-item-divider color="light">
          <ion-label>Logs</ion-label>
        </ion-item-divider>
      </ion-item-group>

      <ion-button expand="block" color="dark" (click)="sendLogs('today')">Send Today's Log</ion-button>
      <ion-button expand="block" color="dark" (click)="sendLogs('recent')">Send Recent Logs (3 days)</ion-button>
      <ion-button expand="block" color="dark" (click)="sendLogs('all')">Send All Logs</ion-button>

      <!-- Result Output -->
      <ion-card *ngIf="lastResult">
        <ion-card-header [color]="lastSuccess ? 'success' : 'danger'">
          <ion-card-title>{{ lastAction }}</ion-card-title>
        </ion-card-header>
        <ion-card-content>
          <pre style="white-space: pre-wrap; font-size: 12px;">{{ lastResult }}</pre>
        </ion-card-content>
      </ion-card>

    </ion-content>
  `
})
export class HomePage implements OnDestroy {

  // Status display
  sdkStatus = 'Not initialized';
  permStatus = 'Unknown';
  trackingStatus = 'Stopped';
  speed = '0.0';
  distance = '0';
  steps = 0;
  tripId = 0;

  // Result display
  lastAction = '';
  lastResult = '';
  lastSuccess = false;

  // Auto-refresh timer
  private statusTimer: any;

  async initSDK() {
    try {
      const res = await TripTracker.initializeWithConfig({
        saveIntervalMinutes: 5,
        saveDistanceMeters: 30,
        vehicleThreshold: 6.0,
        autoStopTimeoutMinutes: 5,
        voiceFeedbackEnabled: true,
        webMonitorEnabled: false,
        geofenceEnabled: false,
        notifyTripStart: true,
        notifyTripEnd: true,
        notifyDistanceKm: true,
      });
      this.sdkStatus = 'Initialized';
      this.permStatus = res.permissionGranted ? 'Granted' : 'Not granted';
      this.showResult('Initialize SDK', res, true);

      // Start auto-refresh
      this.startStatusRefresh();
    } catch (e: any) {
      this.showResult('Initialize SDK', e.message, false);
    }
  }

  async checkPermission() {
    try {
      const res = await TripTracker.hasLocationPermission();
      this.permStatus = res.granted ? 'Granted' : 'Not granted';
      this.showResult('Check Permission', res, true);
    } catch (e: any) {
      this.showResult('Check Permission', e.message, false);
    }
  }

  async startTracking() {
    try {
      const res = await TripTracker.startTracking();
      this.trackingStatus = 'Running';
      this.showResult('Start Tracking', res, true);
      this.startStatusRefresh();
    } catch (e: any) {
      this.showResult('Start Tracking', e.message, false);
    }
  }

  async stopTracking() {
    try {
      const res = await TripTracker.stopTracking();
      this.trackingStatus = 'Stopped';
      this.showResult('Stop Tracking', res, true);
      this.stopStatusRefresh();
    } catch (e: any) {
      this.showResult('Stop Tracking', e.message, false);
    }
  }

  async refreshStatus() {
    try {
      const s = await TripTracker.getTrackingStatus();
      this.trackingStatus = s.isTracking ? 'Running' : 'Stopped';
      this.speed = s.speedKmh.toFixed(1);
      this.distance = s.distance.toFixed(0);
      this.steps = s.steps;
      this.tripId = s.tripId;
      this.showResult('Tracking Status', s, true);
    } catch (e: any) {
      this.showResult('Tracking Status', e.message, false);
    }
  }

  async openPage(page: string) {
    try {
      let res: any;
      switch (page) {
        case 'mainView': res = await TripTracker.openMainView(); break;
        case 'settings': res = await TripTracker.openSettings(); break;
        case 'history': res = await TripTracker.openHistory(); break;
        case 'dailyLocations': res = await TripTracker.openDailyLocations(); break;
        case 'geofence': res = await TripTracker.openGeofenceManager(); break;
        case 'notifications': res = await TripTracker.openNotificationSettings(); break;
      }
      this.showResult('Open ' + page, res, true);
    } catch (e: any) {
      this.showResult('Open ' + page, e.message, false);
    }
  }

  async getSettings() {
    try {
      const res = await TripTracker.getSettings();
      this.showResult('Get Settings', res, true);
    } catch (e: any) {
      this.showResult('Get Settings', e.message, false);
    }
  }

  async getLocation() {
    try {
      const res = await TripTracker.getCurrentLocation();
      this.showResult('Current Location', res, true);
    } catch (e: any) {
      this.showResult('Current Location', e.message, false);
    }
  }

  async getGeofenceZones() {
    try {
      const res = await TripTracker.getGeofenceZones();
      this.showResult('Geofence Zones', res, true);
    } catch (e: any) {
      this.showResult('Geofence Zones', e.message, false);
    }
  }

  async getTripHistory() {
    try {
      const res = await TripTracker.getTripHistory({ limit: 10 });
      this.showResult('Trip History', res, true);
    } catch (e: any) {
      this.showResult('Trip History', e.message, false);
    }
  }

  async startWebMonitor() {
    try {
      const res = await TripTracker.startWebMonitor();
      this.showResult('Start Web Monitor', { ...res, hint: 'Open http://<device-ip>:8080 on same WiFi' }, true);
    } catch (e: any) {
      this.showResult('Start Web Monitor', e.message, false);
    }
  }

  async stopWebMonitor() {
    try {
      const res = await TripTracker.stopWebMonitor();
      this.showResult('Stop Web Monitor', res, true);
    } catch (e: any) {
      this.showResult('Stop Web Monitor', e.message, false);
    }
  }

  async sendLogs(type: string) {
    try {
      let res: any;
      switch (type) {
        case 'today': res = await TripTracker.sendTodayLog(); break;
        case 'recent': res = await TripTracker.sendRecentLogs({ days: 3 }); break;
        case 'all': res = await TripTracker.sendAllLogs(); break;
      }
      this.showResult('Send Logs (' + type + ')', res, true);
    } catch (e: any) {
      this.showResult('Send Logs (' + type + ')', e.message, false);
    }
  }

  private showResult(action: string, data: any, success: boolean) {
    this.lastAction = action;
    this.lastSuccess = success;
    this.lastResult = (success ? '✅ ' : '❌ ') +
      (typeof data === 'string' ? data : JSON.stringify(data, null, 2));
  }

  private startStatusRefresh() {
    this.stopStatusRefresh();
    this.statusTimer = setInterval(() => this.refreshStatus(), 3000);
  }

  private stopStatusRefresh() {
    if (this.statusTimer) {
      clearInterval(this.statusTimer);
      this.statusTimer = null;
    }
  }

  ngOnDestroy() {
    this.stopStatusRefresh();
  }
}
