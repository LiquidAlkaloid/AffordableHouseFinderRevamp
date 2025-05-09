package com.example.affordablehousefinderrevamp.Seller;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.affordablehousefinderrevamp.R;

import java.util.ArrayList;
import java.util.List;

// For image loading, you'd typically use a library like Glide or Picasso.
// import com.bumptech.glide.Glide;

public class GalleryImageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_NEW_PHOTO = 0;
    private static final int VIEW_TYPE_IMAGE = 1;

    private Context context;
    private List<Uri> imageUris;
    private OnNewPhotoClickListener newPhotoClickListener;
    private OnImageClickListener imageClickListener;

    public interface OnNewPhotoClickListener {
        void onNewPhotoClick();
    }

    public interface OnImageClickListener {
        void onImageClick(Uri imageUri, int position);
    }

    public GalleryImageAdapter(Context context, OnNewPhotoClickListener newPhotoClickListener, OnImageClickListener imageClickListener) {
        this.context = context;
        this.imageUris = new ArrayList<>();
        this.newPhotoClickListener = newPhotoClickListener;
        this.imageClickListener = imageClickListener;
    }

    public void setImageUris(List<Uri> uris) {
        this.imageUris.clear();
        if (uris != null) {
            this.imageUris.addAll(uris);
        }
        // Notify after clearing and adding all, plus one for the "New Photo" item
        notifyDataSetChanged();
    }

    public void addImageUri(Uri uri) {
        if (uri != null) {
            this.imageUris.add(0, uri); // Add new images to the start of the list (after "New Photo")
            notifyItemInserted(1); // "New Photo" is at 0, so new image is at 1
            notifyItemRangeChanged(1, imageUris.size()); // Update positions
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return VIEW_TYPE_NEW_PHOTO;
        }
        return VIEW_TYPE_IMAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        if (viewType == VIEW_TYPE_NEW_PHOTO) {
            View view = inflater.inflate(R.layout.item_new_photo, parent, false);
            return new NewPhotoViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_gallery_image, parent, false);
            return new ImageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_NEW_PHOTO) {
            NewPhotoViewHolder newPhotoViewHolder = (NewPhotoViewHolder) holder;
            newPhotoViewHolder.newPhotoLayout.setOnClickListener(v -> {
                if (newPhotoClickListener != null) {
                    newPhotoClickListener.onNewPhotoClick();
                }
            });
        } else {
            ImageViewHolder imageViewHolder = (ImageViewHolder) holder;
            // Adjust position because "New Photo" is the first item
            final int imagePosition = position - 1;
            Uri imageUri = imageUris.get(imagePosition);

            // Load image using Glide or Picasso or standard ImageView methods
            // Example: Glide.with(context).load(imageUri).centerCrop().into(imageViewHolder.imageView);
            imageViewHolder.imageView.setImageURI(imageUri); // Simple URI setting for now

            imageViewHolder.itemView.setOnClickListener(v -> {
                if (imageClickListener != null) {
                    imageClickListener.onImageClick(imageUri, imagePosition);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return imageUris.size() + 1; // +1 for the "New Photo" item
    }

    // ViewHolder for image items
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        // View selectionOverlay; // If you implement selection
        // ImageView checkmarkIcon; // If you implement selection

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.galleryImageView);
            // selectionOverlay = itemView.findViewById(R.id.selection_overlay);
            // checkmarkIcon = itemView.findViewById(R.id.checkmark_icon);
        }
    }

    // ViewHolder for "New Photo" item
    static class NewPhotoViewHolder extends RecyclerView.ViewHolder {
        LinearLayout newPhotoLayout;
        ImageView cameraIcon;
        TextView newPhotoText;

        public NewPhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            newPhotoLayout = itemView.findViewById(R.id.new_photo_item_layout);
            cameraIcon = itemView.findViewById(R.id.new_photo_icon);
            newPhotoText = itemView.findViewById(R.id.new_photo_text);
        }
    }
}
