package com.bignerdranch.android.photogallery.receiver;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.bignerdranch.android.photogallery.service.PollService;

/**
 * Special standalone broadcast receiver to listen to broadcast messages from the app itself.
 * Normally those broadcasts are sent to show a notification to the user.
 * Here, a notification will only be shown if the user is not watching the app at the moment.
 * <p></p>
 * The main fragment's receiver when triggered by a app's own broadcast sends a result code = 0
 * ({@link Activity#RESULT_CANCELED}). In other cases a background service sends a result code = -1
 * ({@link Activity#RESULT_OK}). Only in this case, the notification will be shown (app is not on the screen).
 */
public class NotificationReceiver extends BroadcastReceiver {
    private static final String TAG = NotificationReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "received result: " + getResultCode());

        // a foreground activity cancels the broadcast and its notification
        if (getResultCode() != Activity.RESULT_OK) {
            return;
        }

        int requestCode = intent.getIntExtra(PollService.REQUEST_CODE, 0);
        Notification notification = intent.getParcelableExtra(PollService.NOTIFICATION);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(requestCode, notification);
    }
}
