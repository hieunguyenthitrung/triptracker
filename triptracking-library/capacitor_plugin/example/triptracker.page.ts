/**
 * Example Ionic Angular page using the TripTracker plugin.
 * Copy this into your Ionic project as a starting point.
 *
 * File: src/app/pages/triptracker/triptracker.page.ts
 */

import { Component, OnInit, OnDestroy } from '@angular/core';
import { TripTracker, TrackingStatus, SettingsResult } from 'capacitor-triptracker';

@Component({
  selector: 'app-triptracker',
  template: `
    <ion-header>
      <ion-toolbar color="primary">
        <ion-title>Trip Tracker</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding">

      <!-- Status Card -->
      <ion-card>
        <ion-card-header>
          <ion-card-title>
            <ion-icon [name]="status?.isTracking ? 'car' : 'time'" 
                      [color]="status?.isTracking ? 'success' : 'medium'"></ion-icon>
            {{ status?.isTracking ? 'Recording Trip #' + status?.tripId : 'Waiting for vehicle speed' }}
          </ion-card-title>
        </ion-card-header>
        <ion-card-content>
          <ion-grid>
            <ion-row>
              <ion-col size="4" class="ion-text-center">
                <h2>{{ status?.speedKmh?.toFixed(0) || '0' }}</h2>
                <p>km/h</p>
              </ion-col>
              <ion-col size="4" class="ion-text-center">
                <h2>{{ formatDistance(status?.distance) }}</h2>
                <p>distance</p>
              </ion-col>
              <ion-col size="4" class="ion-text-center">
                <h2>{{ formatDuration(status?.duration) }}</h2>
                <p>duration</p>
              </ion-col>
            </ion-row>
          </ion-grid>
        </ion-card-content>
      </ion-card>

      <!-- Settings Quick Toggles -->
      <ion-card>
        <ion-card-header>
          <ion-card-subtitle>Quick Toggles</ion-card-subtitle>
        </ion-card-header>
        <ion-list>
          <ion-item>
            <ion-icon name="globe-outline" slot="start"></ion-icon>
            <ion-label>Web Monitor</ion-label>
            <ion-toggle [(ngModel)]="webMonitorEnabled" 
                        (ionChange)="toggleWebMonitor()"></ion-toggle>
          </ion-item>
          <ion-item>
            <ion-icon name="volume-high-outline" slot="start"></ion-icon>
            <ion-label>Voice Feedback</ion-label>
            <ion-toggle [(ngModel)]="voiceEnabled" 
                        (ionChange)="toggleVoice()"></ion-toggle>
          </ion-item>
          <ion-item>
            <ion-icon name="pin-outline" slot="start"></ion-icon>
            <ion-label>Geofencing</ion-label>
            <ion-toggle [(ngModel)]="geofencingEnabled" 
                        (ionChange)="toggleGeofencing()"></ion-toggle>
          </ion-item>
        </ion-list>
      </ion-card>

    </ion-content>
  `,
})
export class TripTrackerPage implements OnInit, OnDestroy {
  status: TrackingStatus | null = null;
  geofenceCount = 0;
  webMonitorEnabled = false;
  voiceEnabled = true;
  geofencingEnabled = false;

  private statusInterval: any;

  async ngOnInit() {
    await this.refreshStatus();
    await this.refreshSettings();
    await this.refreshGeofences();

    // Poll tracking status every second
    this.statusInterval = setInterval(() => this.refreshStatus(), 1000);
  }

  ngOnDestroy() {
    if (this.statusInterval) clearInterval(this.statusInterval);
  }

  async refreshStatus() {
    this.status = await TripTracker.getTrackingStatus();
  }

  async refreshSettings() {
    const s = await TripTracker.getSettings();
    this.webMonitorEnabled = s.webMonitorEnabled;
    this.voiceEnabled = s.voiceFeedbackEnabled;
    this.geofencingEnabled = s.geofencingEnabled;
  }

  async refreshGeofences() {
    const { count } = await TripTracker.getGeofenceZones();
    this.geofenceCount = count;
  }

  async toggleWebMonitor() {
    await TripTracker.updateSetting({ key: 'webMonitorEnabled', value: this.webMonitorEnabled });
  }

  async toggleVoice() {
    await TripTracker.updateSetting({ key: 'voiceFeedbackEnabled', value: this.voiceEnabled });
  }

  async toggleGeofencing() {
    await TripTracker.updateSetting({ key: 'geofencingEnabled', value: this.geofencingEnabled });
  }

  formatDistance(meters?: number): string {
    if (!meters) return '0 m';
    return meters < 1000
      ? `${meters.toFixed(0)} m`
      : `${(meters / 1000).toFixed(1)} km`;
  }

  formatDuration(seconds?: number): string {
    if (!seconds) return '00:00';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }
}
