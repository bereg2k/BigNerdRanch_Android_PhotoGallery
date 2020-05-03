package com.bignerdranch.android.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;

import androidx.annotation.IdRes;
import androidx.fragment.app.Fragment;

/**
 * Activity for hosting {@link PhotoPageFragment} - web page view inside the app.
 */
public class PhotoPageActivity extends SingleFragmentActivity {

    @IdRes
    public static final int WEB_VIEW_ID = R.id.web_view;

    public static Intent newIntent(Context context, Uri uri) {
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(uri);

        return intent;
    }

    @Override
    public Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }

    @Override
    public void onBackPressed() {
        // adding support for web history when pressing Android's Back button
        WebView webView = findViewById(WEB_VIEW_ID);

        if (webView == null || !webView.canGoBack()) {
            super.onBackPressed();
        } else if (webView.canGoBack()) {
            webView.goBack();
        }
    }
}
