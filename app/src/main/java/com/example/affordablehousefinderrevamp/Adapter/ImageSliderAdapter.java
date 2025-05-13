package com.example.affordablehousefinderrevamp.Adapter; // Make sure this package is correct

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide; // For image loading
import com.example.affordablehousefinderrevamp.R; // For accessing resources

import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {

    private Context context;
    private List<String> imageUrls; // List of image URLs to display

    /**
     * Constructor for the ImageSliderAdapter.
     * @param context The context from which the adapter is created.
     * @param imageUrls A list of strings, where each string is a URL for an image.
     */
    public ImageSliderAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each item of the ViewPager2/RecyclerView.
        // This layout (item_image_slider.xml) should contain an ImageView.
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return; // Do nothing if there are no image URLs
        }
        String imageUrl = imageUrls.get(position);

        // Load the image from the URL into the ImageView using Glide.
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image) // Placeholder image while loading
                .error(R.drawable.ic_launcher_background)   // Image to display if loading fails
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        // Return the total number of images.
        return imageUrls == null ? 0 : imageUrls.size();
    }

    /**
     * ViewHolder class for the slider items.
     * Holds the ImageView for displaying each image.
     */
    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView; // The ImageView from item_image_slider.xml

        SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize the ImageView from the item layout.
            // Ensure R.id.sliderImageView matches the ID in item_image_slider.xml.
            imageView = itemView.findViewById(R.id.sliderImageView);
        }
    }

    /**
     * Optional: Helper method to update the list of image URLs if it changes.
     * @param newImageUrls The new list of image URLs.
     */
    public void updateImageUrls(List<String> newImageUrls) {
        this.imageUrls.clear();
        if (newImageUrls != null) {
            this.imageUrls.addAll(newImageUrls);
        }
        notifyDataSetChanged(); // Notify the adapter that the data set has changed
    }
}
