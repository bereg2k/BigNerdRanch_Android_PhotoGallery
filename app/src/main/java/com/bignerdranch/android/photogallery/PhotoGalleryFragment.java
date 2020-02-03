package com.bignerdranch.android.photogallery;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";
    private static final boolean IS_RETAIN_FRAGMENT = true;
    private static final int SINGLE_COLUMN_WIDTH = 200;

    private List<GalleryItem> mItems = new ArrayList<>();
    private int mPage = 1;

    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener;

    private PhotoAdapter mAdapter;
    private RecyclerView mPhotoRecyclerView;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int mPhotoRecyclerViewWidth;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(IS_RETAIN_FRAGMENT);
        new FetchItemsTask().execute();

        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        int cacheSize = am.getMemoryClass() * 1024 / 8;

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<PhotoHolder>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoHolder photoHolder, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                photoHolder.bindDrawable(drawable);
            }
        });

        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread has started successfully");

        mThumbnailDownloader.setCacheSize(cacheSize);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread has been successfully stopped");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = view.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                    mPage++;
                    Toast.makeText(getActivity(), "Loading results page #" + mPage, Toast.LENGTH_SHORT).show();
                    new FetchItemsTask().execute();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        initRecyclerGlobalLayoutListener();
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);

        updateUI();

        return view;
    }

    /**
     * initiating global layout listener to monitor layout changes (after configuration changes).
     * This is useful to get layout properties in runtime.
     * Here, we calculate spanCount variable dynamically and use to change layout for RecyclerView
     * (change number of columns depending on the view's width)
     */
    private void initRecyclerGlobalLayoutListener() {
        mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mPhotoRecyclerViewWidth = mPhotoRecyclerView.getWidth();
                Log.i(TAG, "mPhotoRecyclerViewWidth = " + mPhotoRecyclerViewWidth);

                int newSpanCount = mPhotoRecyclerViewWidth / SINGLE_COLUMN_WIDTH;
                Log.i(TAG, "spanCount = " + newSpanCount);

                // getting current LayoutManager for RecyclerView, to get Grid spanCount (old columns).
                // if current spanCount != new spanCount, then Adapter needs new layout with different num of columns
                GridLayoutManager gm = (GridLayoutManager) (mPhotoRecyclerView.getLayoutManager());
                int oldSpanCount = gm == null ? 0 : gm.getSpanCount();
                if (newSpanCount != oldSpanCount) {
                    mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), newSpanCount));
                }

                // leaving layout-change listener after processing code above
                // (it's needed for correct processing of configuration changes (e.g., rotation)
                mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);
            }
        };
    }

    private void updateUI() {
        if (isAdded()) {
            if (mAdapter == null) {
                mAdapter = new PhotoAdapter(mItems);
                mPhotoRecyclerView.setAdapter(mAdapter);
            } else {
                mAdapter.notifyItemInserted(mItems.size());
            }
        }
    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {

        private ImageView mItemImageView;

        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            mItemImageView = itemView.findViewById(R.id.item_image_view);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @NonNull
        @Override
        public PhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.list_item_gallery, parent, false);

            return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoHolder holder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);
            Drawable placeholder = getResources().getDrawable(R.drawable.ic_placeholder);
            holder.bindDrawable(placeholder);
            mThumbnailDownloader.queueThumbnail(holder, galleryItem.getUrl());
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems(mPage);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mItems.addAll(galleryItems);
            updateUI();
        }
    }
}