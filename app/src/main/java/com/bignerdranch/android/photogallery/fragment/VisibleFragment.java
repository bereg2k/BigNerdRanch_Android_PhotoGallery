package com.bignerdranch.android.photogallery.fragment;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.fragment.app.Fragment;

import com.bignerdranch.android.photogallery.service.PollService;

/**
 * Parent class for app's main fragment ({@link PhotoGalleryFragment}).
 * It's needed to encapsulate a logic to register/unregister a broadcast receiver.
 * This broadcast receiver is used to get broadcast messages for the app itself,
 * but this way it can be directly aware whether the app is currently on screen or not.
 * <p>
 * All this is needed to send notifications to the user only when the app is not on the screen.
 */
public class VisibleFragment extends Fragment {
    private static final String TAG = VisibleFragment.class.getSimpleName();

    private BroadcastReceiver mOnShowNotification = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // if received, it means a fragment is currently seen by the user
            // therefore, cancel the notification
            Log.i(TAG, "cancelling notification with action: " + intent.getAction());
            setResultCode(Activity.RESULT_CANCELED);
        }
    };

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        getActivity().registerReceiver(mOnShowNotification, intentFilter,
                PollService.PERM_PRIVATE, null);
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mOnShowNotification);
    }
}
