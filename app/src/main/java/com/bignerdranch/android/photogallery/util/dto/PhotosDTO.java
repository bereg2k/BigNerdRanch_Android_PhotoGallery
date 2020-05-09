package com.bignerdranch.android.photogallery.util.dto;

import com.bignerdranch.android.photogallery.model.GalleryItem;
import com.google.gson.annotations.SerializedName;

/**
 * Data transfer object to hold the data for individual container of photo packages.
 * They are all inside the root of the parsed JSON object with photos (see {@link RootJsonDTO}).
 */
public class PhotosDTO {

    @SerializedName("photo")
    private GalleryItem[] mGalleryItems;

    public GalleryItem[] getGalleryItems() {
        return mGalleryItems;
    }
}
