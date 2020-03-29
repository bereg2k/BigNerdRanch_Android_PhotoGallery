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
    @SerializedName("url_o")
    private String mUrlBig;

    public String getCaption() {
        return mCaption;
    }

    public String getId() {
        return mId;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getUrlBig() {
        return mUrlBig;
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

    public void setUrlBig(String mUrlBig) {
        this.mUrlBig = mUrlBig;
    }

    @NonNull
    @Override
    public String toString() {
        return mCaption;
    }
}
