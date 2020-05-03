package com.bignerdranch.android.photogallery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.net.URISyntaxException;

/**
 * Fragment for showing photo's web pages in WebView format (seamless navigation).
 */
public class PhotoPageFragment extends VisibleFragment {
    private static final String TAG = PhotoPageFragment.class.getSimpleName();

    private static final String ARG_URI = "photo_page_url";

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private Uri mUri;

    public static PhotoPageFragment newInstance(Uri uri) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);

        PhotoPageFragment fragment = new PhotoPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mUri = getArguments().getParcelable(ARG_URI);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_page, container, false);

        mProgressBar = view.findViewById(R.id.progress_bar);
        mProgressBar.setMax(100);

        mWebView = view.findViewById(R.id.web_view);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress == 100) {
                    mProgressBar.setVisibility(View.GONE);
                } else {
                    mProgressBar.setVisibility(View.VISIBLE);
                    mProgressBar.setProgress(newProgress);
                }
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                AppCompatActivity activity = (AppCompatActivity) getActivity();
                activity.getSupportActionBar().setSubtitle(title);
            }

        });
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Context context = view.getContext();

                if (url.startsWith("http")) {
                    // same effect as view.loadUrl(url)
                    return false;
                } else {
                    // if there's a link with scheme that's NOT "http/https", try to find an app for it
                    // if there's no app -- extract a web link with "http/https" scheme and reload
                    try {
                        Intent intent = Intent.parseUri(url, 0);
                        PackageManager packageManager = context.getPackageManager();
                        ResolveInfo resolveInfo = packageManager.resolveActivity(intent,
                                PackageManager.MATCH_DEFAULT_ONLY);

                        if (resolveInfo != null) {
                            view.stopLoading();
                            context.startActivity(intent);
                            return true;
                        } else {
                            view.loadUrl(intent.getDataString());
                        }
                    } catch (URISyntaxException e) {
                        Log.e(TAG, "Can't parse the URL: " + url, e);
                    }
                }
                return false;
            }
        });
        mWebView.loadUrl(mUri.toString());

        return view;
    }
}
