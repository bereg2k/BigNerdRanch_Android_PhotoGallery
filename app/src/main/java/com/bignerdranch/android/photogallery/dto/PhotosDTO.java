package com.bignerdranch.android.photogallery.dto;

import com.bignerdranch.android.photogallery.GalleryItem;
import com.google.gson.annotations.SerializedName;

public class PhotosDTO {

    @SerializedName("photo")
    private GalleryItem[] mGalleryItems;

    public GalleryItem[] getGalleryItems() {
        return mGalleryItems;
    }
}
