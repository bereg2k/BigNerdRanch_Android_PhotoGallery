package com.bignerdranch.android.photogallery;

import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class FullSizePhotoFragment extends DialogFragment {
    private static final String ARG_BYTE_ARRAY = "byte_array";
    private static final String ARG_NAME = "item_name";
    private static final String ARG_URL = "item_url";
    private static final String ARG_URL_BIG = "item_url_big";

    private ImageView mImageView;
    private TextView mNameTextView;
    private TextView mThumbLinkTextView;
    private TextView mOriginalLinkTextView;

    public static FullSizePhotoFragment newInstance(byte[] bitmapByteArray, String name, String url, String urlBig) {
        Bundle args = new Bundle();
        args.putByteArray(ARG_BYTE_ARRAY, bitmapByteArray);
        args.putString(ARG_NAME, name);
        args.putString(ARG_URL, url);
        args.putString(ARG_URL_BIG, urlBig);

        FullSizePhotoFragment fragment = new FullSizePhotoFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fullsize_item_gallery, container, false);

        mImageView = view.findViewById(R.id.full_size_item_image_view);
        byte[] imageArray = getArguments().getByteArray(ARG_BYTE_ARRAY);
        mImageView.setImageBitmap(BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length));

        mNameTextView = view.findViewById(R.id.full_size_item_caption);
        mNameTextView.setText(getString(R.string.photo_name, getArguments().getString(ARG_NAME)));
        mThumbLinkTextView = view.findViewById(R.id.full_size_item_link_thumb);
        mThumbLinkTextView.setText(getString(R.string.photo_link_thumb, getArguments().getString(ARG_URL)));
        mOriginalLinkTextView = view.findViewById(R.id.full_size_item_link_original);
        mOriginalLinkTextView.setText(getString(R.string.photo_link_original, getArguments().getString(ARG_URL_BIG)));

        return view;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.fullsize_item_gallery, null, false);

        mImageView = view.findViewById(R.id.full_size_item_image_view);
        byte[] imageArray = getArguments().getByteArray(ARG_BYTE_ARRAY);
        mImageView.setImageBitmap(BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length));

        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });

        mNameTextView = view.findViewById(R.id.full_size_item_caption);
        mNameTextView.setText(getString(R.string.photo_name, getArguments().getString(ARG_NAME)));
        mThumbLinkTextView = view.findViewById(R.id.full_size_item_link_thumb);
        mThumbLinkTextView.setText(getString(R.string.photo_link_thumb, getArguments().getString(ARG_URL)));
        mOriginalLinkTextView = view.findViewById(R.id.full_size_item_link_original);
        mOriginalLinkTextView.setText(getString(R.string.photo_link_original, getArguments().getString(ARG_URL_BIG)));

        return new AlertDialog.Builder(getContext())
                .setView(view)
                .create();
    }
}
