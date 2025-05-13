package com.example.affordablehousefinderrevamp.Seller; // Or your correct adapter package

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // Using Glide for efficient image loading
import com.example.affordablehousefinderrevamp.R; // For accessing resources like layouts and drawables

import java.util.List;

public class GalleryImageAdapter extends RecyclerView.Adapter<GalleryImageAdapter.ViewHolder> {

    private Context context;
    private List<Uri> imageUris;
    private OnImageRemoveListener onImageRemoveListener;

    // Interface for remove click listener
    public interface OnImageRemoveListener {
        void onImageRemove(Uri imageUri, int position);
    }

    public GalleryImageAdapter(Context context, List<Uri> imageUris, OnImageRemoveListener listener) {
        this.context = context;
        this.imageUris = imageUris;
        this.onImageRemoveListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout (item_gallery_image.xml)
        // Make sure you have item_gallery_image.xml in your res/layout folder
        View view = LayoutInflater.from(context).inflate(R.layout.item_gallery_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri imageUri = imageUris.get(position);

        // Load image using Glide
        Glide.with(context)
                .load(imageUri)
                .placeholder(R.drawable.placeholder_image) // Optional: a placeholder drawable
                .error(R.drawable.ic_launcher_background) // Optional: an error drawable
                .centerCrop()
                .into(holder.imageView);

        // Set click listener for the remove button
        if (holder.removeButton != null) { // Check if removeButton exists in the layout
            holder.removeButton.setOnClickListener(v -> {
                if (onImageRemoveListener != null) {
                    // Get the adapter position, as it's more reliable inside a listener
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onImageRemoveListener.onImageRemove(imageUris.get(adapterPosition), adapterPosition);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return imageUris == null ? 0 : imageUris.size();
    }

    // ViewHolder class
    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageView removeButton; // Button/ImageView to remove the image from selection

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views from item_gallery_image.xml
            // Ensure these IDs match your item_gallery_image.xml
            imageView = itemView.findViewById(R.id.galleryImageViewItem);
            removeButton = itemView.findViewById(R.id.removeImageButton);
        }
    }

    // Helper method to update the list of URIs (optional, if you manage list outside)
    public void updateImageUris(List<Uri> newImageUris) {
        this.imageUris.clear();
        this.imageUris.addAll(newImageUris);
        notifyDataSetChanged();
    }
}
