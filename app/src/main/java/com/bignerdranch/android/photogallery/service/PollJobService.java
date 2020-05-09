package com.bignerdranch.android.photogallery.service;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.bignerdranch.android.photogallery.receiver.NotificationReceiver;
import com.bignerdranch.android.photogallery.util.QueryPreferences;
import com.bignerdranch.android.photogallery.R;
import com.bignerdranch.android.photogallery.activity.PhotoGalleryActivity;
import com.bignerdranch.android.photogallery.model.GalleryItem;
import com.bignerdranch.android.photogallery.util.FlickrFetchr;

import java.util.List;

import static com.bignerdranch.android.photogallery.service.PollService.ACTION_SHOW_NOTIFICATION;
import static com.bignerdranch.android.photogallery.service.PollService.NOTIFICATION;
import static com.bignerdranch.android.photogallery.service.PollService.PERM_PRIVATE;
import static com.bignerdranch.android.photogallery.service.PollService.REQUEST_CODE;

/**
 * Service for background polling for new results.
 * Works with devices with Android API >= LOLLIPOP (21).
 * <p>
 * !!! Important note: the task for polling is periodic but due to inner logic, it cannot be started
 * too often -- not less than every 15 minutes!
 */
public class PollJobService extends JobService {
    private static final String TAG = PollJobService.class.getSimpleName();
    private static final int JOB_ID = 1;

    private PollTask mPollTask;

    /**
     * schedule a current background job (if parameter turnOn = true)
     * or cancelling it (if parameter turnOn = false)
     *
     * @param context current context
     * @param turnOn  true, if job needs to be scheduled, false -- if job needs to be cancelled
     */
    public static void scheduleJob(Context context, boolean turnOn) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        if (turnOn) {
            JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, PollJobService.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
                    .setPeriodic(1000 * 60 * 15)
                    .setPersisted(true)
                    .build();
            jobScheduler.schedule(jobInfo);
        } else {
            jobScheduler.cancel(JOB_ID);
        }

        QueryPreferences.setAlarmOn(context, turnOn);
    }

    /**
     * check if the job with {@link PollJobService#JOB_ID}
     * is already scheduled or not.
     *
     * @param context current context
     * @return true, if the job is already scheduled
     */
    public static boolean isJobScheduled(Context context) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == JOB_ID) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        mPollTask = new PollTask();
        mPollTask.execute(params);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        if (mPollTask != null) {
            mPollTask.cancel(true);
        }
        return true;
    }

    /**
     * Background task to handle the actual scheduled background job.
     */
    private class PollTask extends AsyncTask<JobParameters, Void, Void> {

        @Override
        protected Void doInBackground(JobParameters... jobParameters) {
            // app stores the last result's id to determine on the next request
            // if there're new photos available for downloading.
            // This logic compares ids and fires up a notification if anything new
            // is available among queried or all requests.

            String query = QueryPreferences.getStoredQuery(getApplicationContext());
            String lastResultId = QueryPreferences.getLastResultId(getApplicationContext());
            List<GalleryItem> items;

            if (query == null) {
                items = new FlickrFetchr().fetchRecentPhotos(0);
            } else {
                items = new FlickrFetchr().searchPhotos(query, 0);
            }

            if (items.isEmpty()) {
                return null;
            }

            String resultId = items.get(0).getId();
            if (resultId.equals(lastResultId)) {
                Log.i(TAG, "Got an old result: " + resultId);
            } else {
                Log.i(TAG, "Got a new result: " + resultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(getApplicationContext());
                PendingIntent pi = PendingIntent.getActivity(getApplicationContext(), 0, i, 0);

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

                // devices with Android API >= Oreo (26) needs Notification channel to be created
                String channelId = "newPhotosAvailable";
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(
                            channelId,
                            "New Photos Available Updates",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    notificationManager.createNotificationChannel(channel);
                }

                Notification notification = new NotificationCompat.Builder(getApplicationContext(), channelId)
                        .setTicker(resources.getString(R.string.new_pictures_title))
                        .setSmallIcon(android.R.drawable.ic_menu_report_image)
                        .setContentTitle(resources.getString(R.string.new_pictures_title))
                        .setContentText(resources.getString(R.string.new_pictures_text))
                        .setContentIntent(pi)
                        .setAutoCancel(true)
                        .build();

                showBackgroundNotification(0, notification);
            }

            QueryPreferences.setLastResultId(getApplicationContext(), resultId);

            return null;
        }

        /**
         * Send an ordered broadcast to notification receiver ({@link NotificationReceiver}.
         * If main app's fragment is currently active, then notification will be cancelled.
         * Otherwise, it will be shown among notifications as usual
         *
         * @param requestCode  code for a notification
         * @param notification notification object to send in case the app is not active
         */
        private void showBackgroundNotification(int requestCode, Notification notification) {
            Intent intent = new Intent(ACTION_SHOW_NOTIFICATION);
            intent.putExtra(REQUEST_CODE, requestCode);
            intent.putExtra(NOTIFICATION, notification);
            sendOrderedBroadcast(intent, PERM_PRIVATE,
                    null, null,
                    Activity.RESULT_OK,
                    null, null);
        }
    }
}
