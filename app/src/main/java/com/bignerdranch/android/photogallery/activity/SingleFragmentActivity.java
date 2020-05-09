package com.bignerdranch.android.photogallery.activity;

import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.bignerdranch.android.photogallery.R;

/**
 * Parent class for all activities with layouts to hold a "container" for a single fragment.
 */
public abstract class SingleFragmentActivity extends AppCompatActivity {

    @LayoutRes
    private static final int ACTIVITY_LAYOUT_RES = R.layout.activity_fragment;
    @IdRes
    private static final int FRAGMENT_CONTAINER_RES_ID = R.id.fragment_container;

    public abstract Fragment createFragment();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ACTIVITY_LAYOUT_RES);

        FragmentManager fm = getSupportFragmentManager();
        Fragment fragment = fm.findFragmentById(FRAGMENT_CONTAINER_RES_ID);

        if (fragment == null) {
            fragment = createFragment();

            fm.beginTransaction()
                    .add(FRAGMENT_CONTAINER_RES_ID, fragment)
                    .commit();
        }
    }
}
