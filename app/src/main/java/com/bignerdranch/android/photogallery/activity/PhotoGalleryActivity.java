package com.bignerdranch.android.photogallery.activity;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;

import com.bignerdranch.android.photogallery.fragment.PhotoGalleryFragment;

/**
 * Main activity that hosts a fragment with the gallery.
 */
public class PhotoGalleryActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context context) {
        return new Intent(context, PhotoGalleryActivity.class);
    }

    @Override
    public Fragment createFragment() {
        return PhotoGalleryFragment.newInstance();
    }
}