package com.carmd.triptracking.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.carmd.triptracking.ui.DailyLocationsActivity;

/**
 * Fires at 6:00 AM daily via AlarmManager.
 * Shows a push notification reminding the user to review yesterday's route.
 */
public class DailyReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "DailyReminderReceiver";
    private static final String CHANNEL_TRIP_EVENTS = "trip_events";
    private static final int    NOTIF_DAILY_REMINDER = 3001;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Daily reminder fired at 6:00 AM — notification suppressed");
        // Only trip start/end notifications are shown.

    }
}
