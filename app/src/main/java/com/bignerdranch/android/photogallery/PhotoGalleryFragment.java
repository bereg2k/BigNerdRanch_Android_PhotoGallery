package com.bignerdranch.android.photogallery;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static androidx.core.content.ContextCompat.getColor;

public class PhotoGalleryFragment extends Fragment {

    public static final String DIALOG_PHOTO = "DialogPhoto";

    private static final String TAG = "PhotoGalleryFragment";
    private static final boolean IS_RETAIN_FRAGMENT = true;
    private static final int SINGLE_COLUMN_WIDTH = 200;
    private static final int MAX_IMAGE_SIZE = 4_096_000;

    private List<GalleryItem> mItems = new ArrayList<>();
    private int mPage = 1;
    private boolean mIsClearSearch;
    private byte[] mFullSizeImageByteArray;

    private ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener;

    private PhotoAdapter mAdapter;
    private RecyclerView mPhotoRecyclerView;
    private ProgressBar mProgressBar;
    private SearchView mSearchView;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;
    private int mPhotoRecyclerViewWidth;
    private boolean mIsNoConnection;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(IS_RETAIN_FRAGMENT);
        setHasOptionsMenu(true);
        updateItems();

        ActivityManager am = (ActivityManager) getContext().getSystemService(Context.ACTIVITY_SERVICE);
        int cacheSize = am.getMemoryClass() * 1024 / 8;

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
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
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery, menu);

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);
        mSearchView = (SearchView) searchItem.getActionView();
        mSearchView.setQuery(QueryPreferences.getStoredQuery(getContext()), true);

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "onQueryTextSubmit: " + query);
                QueryPreferences.setPrefSearchQuery(getContext(), query);

                hideSoftKeyboard(mSearchView);
                mPage = 1;
                mPhotoRecyclerView.scrollToPosition(0);

                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "onQueryTextChange: " + newText);

                return false;
            }
        });

        mSearchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getContext());
                mSearchView.setQuery(query, false);
            }
        });

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);

        boolean isServiceOn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                PollJobService.isJobScheduled(getActivity()) :
                PollService.isServiceAlarmOn(getActivity());
        if (isServiceOn) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setPrefSearchQuery(getContext(), null);

                mIsClearSearch = true;
                hideSoftKeyboard(mSearchView);
                mSearchView.onActionViewCollapsed();
                mPage = 1;

                updateItems();
                return true;
            case R.id.menu_item_toggle_polling:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    boolean shouldStartAlarm = !PollJobService.isJobScheduled(getActivity());
                    PollJobService.scheduleJob(getActivity(), shouldStartAlarm);
                } else {
                    boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                    PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                }

                getActivity().invalidateOptionsMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
                    updateItems();
                }
            }
        });

        initRecyclerGlobalLayoutListener();
        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(mGlobalLayoutListener);

        mProgressBar = view.findViewById(R.id.progress_bar);
        mProgressBar.getIndeterminateDrawable().setColorFilter(
                getColor(getContext(), R.color.blue), PorterDuff.Mode.SRC_IN);

        updateUI(false);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
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

    private void updateUI(boolean isRefreshThumbs) {
        if (mIsNoConnection) {
            showNoConnectionAlert();
        }

        if (isAdded()) {
            if (mAdapter == null) {
                mAdapter = new PhotoAdapter(mItems);
                mPhotoRecyclerView.setAdapter(mAdapter);
            } else {
                if (isRefreshThumbs) {
                    mAdapter.notifyDataSetChanged();
                } else {
                    mAdapter.notifyItemInserted(mItems.size());
                }
            }
        }
        mProgressBar.setVisibility(View.GONE);
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getContext());
        new FetchItemsTask(query).execute();
    }

    private void hideSoftKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private class PhotoHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView mItemImageView;

        public PhotoHolder(@NonNull View itemView) {
            super(itemView);
            mItemImageView = itemView.findViewById(R.id.item_image_view);
            mItemImageView.setOnClickListener(this);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }

        @Override
        public void onClick(View v) {
            mFullSizeImageByteArray = null;
            new FetchFullSizeItemTask(getAdapterPosition()).execute();

            FragmentManager manager = getFragmentManager();
            while (mFullSizeImageByteArray == null) {
                // wait for mFullSizeImageBitmapArray to load actual image in the background
                if (mIsNoConnection) {
                    showNoConnectionAlert();
                    return;
                }
            }
            FullSizePhotoFragment dialog = FullSizePhotoFragment.newInstance(
                    mFullSizeImageByteArray,
                    mItems.get(getAdapterPosition()).getCaption(),
                    mItems.get(getAdapterPosition()).getUrl(),
                    mItems.get(getAdapterPosition()).getUrlBig()
            );
            dialog.show(manager, DIALOG_PHOTO);
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

    private void showNoConnectionAlert() {
        new AlertDialog.Builder(getContext())
                .setTitle("Error")
                .setMessage("There's a connection problem with your network...")
                .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .setNeutralButton("Reload", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateItems();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // just cancelling the dialog, works out of the box
                    }
                })
                .show();
    }

    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> {
        private String mQuery;

        public FetchItemsTask() {
            this(null);
        }

        public FetchItemsTask(String query) {
            mQuery = query;
        }

        @Override
        protected void onPreExecute() {
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            if (mQuery != null) {
                return new FlickrFetchr().searchPhoto(mQuery, mPage);
            } else {
                return new FlickrFetchr().fetchRecentPhotos(mPage);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> galleryItems) {
            mIsNoConnection = galleryItems.isEmpty();

            boolean isRefreshThumbs = mQuery != null && mPage == 1 || mIsClearSearch;

            if (isRefreshThumbs) {
                mItems.clear();

                if (mIsClearSearch) {
                    mIsClearSearch = false;
                }
            }

            mItems.addAll(galleryItems);
            updateUI(isRefreshThumbs);
        }
    }

    private class FetchFullSizeItemTask extends AsyncTask<Void, Void, byte[]> {
        private final int mItemPosition;

        public FetchFullSizeItemTask(int itemPosition) {
            mItemPosition = itemPosition;
        }

        @Override
        protected void onPreExecute() {
            if (mProgressBar != null) {
                mProgressBar.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected byte[] doInBackground(Void... voids) {
            try {
                byte[] tempImageBuffer;
                tempImageBuffer = new FlickrFetchr().getUrlBytes(mItems.get(mItemPosition).getUrlBig());

                if (tempImageBuffer.length >= MAX_IMAGE_SIZE) {
                    mFullSizeImageByteArray = new FlickrFetchr().getUrlBytes(mItems.get(mItemPosition).getUrl());
                } else {
                    mFullSizeImageByteArray = tempImageBuffer;
                }
                mIsNoConnection = false;
            } catch (IOException e) {
                Log.e(TAG, e.getMessage(), e);
                mIsNoConnection = true;
            }
            return mFullSizeImageByteArray;
        }

        @Override
        protected void onPostExecute(byte[] bytes) {
            mProgressBar.setVisibility(View.GONE);
        }
    }
}