package com.bignerdranch.android.photogallery.dto;

import com.google.gson.annotations.SerializedName;

public class RootJsonDTO {

    @SerializedName("photos")
    private PhotosDTO photos;

    public PhotosDTO getPhotos() {
        return photos;
    }
}
