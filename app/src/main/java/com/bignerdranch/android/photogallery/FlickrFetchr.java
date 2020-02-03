package com.bignerdranch.android.photogallery;

import android.net.Uri;
import android.util.Log;

import com.bignerdranch.android.photogallery.dto.RootJsonDTO;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class FlickrFetchr {

    private static final String TAG = "FlickrFetchr";
    private static final String API_KEY = "33675dac38a8a33ccdf1a30b0e67f21b";
    private static final String SECRET = "6e3ad27ab28b33d3";

    private static final String FLICKR_REST_API_URL = "https://www.flickr.com/services/rest/";
    private static final String GET_RECENT_METHOD = "flickr.photos.getRecent";

    public byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + ": with " + urlSpec);
            }

            int bytesRead = 0;
            byte[] buffer = new byte[4096];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    public String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    public List<GalleryItem> fetchItems(int page) {

        List<GalleryItem> items = new ArrayList<>();

        try {
            String url = Uri.parse(FLICKR_REST_API_URL)
                    .buildUpon()
                    .appendQueryParameter("method", GET_RECENT_METHOD)
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    .appendQueryParameter("extras", "url_s")
                    .appendQueryParameter("page", String.valueOf(page))
                    .build().toString();
            String jsonString = getUrlString(url);
            Log.i(TAG, "Received JSON: " + jsonString);

            parseItemsGson(items, jsonString);
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items!", ioe);
        }

        return items;
    }

    private void parseItemsGson(List<GalleryItem> items, String jsonString) {
        Gson gson = new Gson();
        RootJsonDTO rootJsonDTO = gson.fromJson(jsonString, RootJsonDTO.class);
        GalleryItem[] galleryItemsArray = rootJsonDTO.getPhotos().getGalleryItems();

        // filter out all the items with empty URLs
        for (GalleryItem arrayItem : galleryItemsArray) {
            if (arrayItem.getUrl() == null || arrayItem.getUrl().isEmpty()) {
                continue;
            }
            items.add(arrayItem);
        }
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws
            JSONException {
        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);

            if (!photoJsonObject.has("url_s")) {
                continue;
            }

            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));
            item.setUrl(photoJsonObject.getString("url_s"));

            items.add(item);
        }
    }
}