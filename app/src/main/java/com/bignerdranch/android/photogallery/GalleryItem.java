package com.bignerdranch.android.photogallery;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class GalleryItem {

    @SerializedName("title")
    private String mCaption;
    @SerializedName("id")
    private String mId;
    @SerializedName("url_s")
    private String mUrl;

    public String getCaption() {
        return mCaption;
    }

    public String getId() {
        return mId;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setCaption(String caption) {
        mCaption = caption;
    }

    public void setId(String id) {
        mId = id;
    }

    public void setUrl(String url) {
        mUrl = url;
    }

    @NonNull
    @Override
    public String toString() {
        return mCaption;
    }
}
