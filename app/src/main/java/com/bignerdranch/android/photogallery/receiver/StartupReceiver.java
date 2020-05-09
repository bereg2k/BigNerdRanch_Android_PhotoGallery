package com.bignerdranch.android.photogallery.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.bignerdranch.android.photogallery.service.PollJobService;
import com.bignerdranch.android.photogallery.service.PollService;
import com.bignerdranch.android.photogallery.util.QueryPreferences;

import static com.bignerdranch.android.photogallery.fragment.PhotoGalleryFragment.IS_JOB_SERVICE_ACTIVE;

/**
 * Standalone broadcast receiver that controls the device going power OFF and ON.
 * When device is powered off and turned on again later, this class receives a system's broadcast
 * intent with action = "android.intent.action.BOOT_COMPLETED".
 * After that, it can start up the background alarm for polling service again
 * (or not, if it was OFF when device was powered off).
 */
public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = StartupReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast intent: " + intent.getAction());

        boolean isAlarmOn = QueryPreferences.isAlarmOn(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
                IS_JOB_SERVICE_ACTIVE) {
            PollJobService.scheduleJob(context, isAlarmOn);
        } else {
            PollService.setServiceAlarm(context, isAlarmOn);
        }
    }
}
