package com.example.affordablehousefinderrevamp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color; // Import Color
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Import ContextCompat
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.affordablehousefinderrevamp.Buyer.PropertyDetail;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.example.affordablehousefinderrevamp.Seller.SellerPropertyDetails;
// Removed Firebase Auth and User imports as they are not directly used in this adapter
// import com.google.firebase.auth.FirebaseAuth;
// import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder> {

    private Context context;
    private List<Property> propertyList;
    private boolean isSellerContext;
    private OnPropertyActionListener propertyActionListener; // Listener for seller actions

    // Interface for handling actions from the adapter, specifically for seller context
    public interface OnPropertyActionListener {
        void onEditClick(Property property);
        void onRemoveClick(Property property);
        void onChangeStatusClick(Property property); // Callback for changing status
    }

    // Constructor for Buyer context (no action listener needed for these actions from buyer)
    public PropertyAdapter(Context context, List<Property> propertyList, boolean isSellerContext) {
        this.context = context;
        this.propertyList = propertyList;
        this.isSellerContext = isSellerContext;
        // propertyActionListener will be null for buyer context
    }

    // Constructor for Seller context (requires action listener)
    public PropertyAdapter(Context context, List<Property> propertyList, boolean isSellerContext, OnPropertyActionListener listener) {
        this.context = context;
        this.propertyList = propertyList;
        this.isSellerContext = isSellerContext;
        this.propertyActionListener = listener;
    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_house, parent, false);
        return new PropertyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Property property = propertyList.get(position);

        // Bind data to the views using IDs from list_item_house.xml
        holder.houseTitleTextView.setText(property.getTitle());
        holder.houseLocationTextView.setText(property.getLocation());
        holder.housePriceTextView.setText(property.getPrice());

        // Display and style the property status
        if (property.getStatus() != null && !property.getStatus().isEmpty()) {
            holder.houseStatusTextView.setText("STATUS: " + property.getStatus().toUpperCase());
            if ("Available".equalsIgnoreCase(property.getStatus())) {
                holder.houseStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.status_available_color)); // Make sure this color is defined
            } else if ("Taken".equalsIgnoreCase(property.getStatus()) || "Sold".equalsIgnoreCase(property.getStatus())) {
                holder.houseStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.status_taken_color)); // Make sure this color is defined
            } else if ("Reserved".equalsIgnoreCase(property.getStatus())) {
                holder.houseStatusTextView.setTextColor(ContextCompat.getColor(context, R.color.status_reserved_color)); // Make sure this color is defined
            }
            else {
                holder.houseStatusTextView.setTextColor(Color.DKGRAY); // Default color
            }
        } else {
            holder.houseStatusTextView.setText("STATUS: UNKNOWN");
            holder.houseStatusTextView.setTextColor(Color.DKGRAY);
        }


        // Load image using Glide
        String imageUrlToLoad = null;
        if (property.getImageUrls() != null && !property.getImageUrls().isEmpty()) {
            imageUrlToLoad = property.getImageUrls().get(0); // Get the first image URL from the list
        } else if (property.getImageUrl() != null && !property.getImageUrl().isEmpty()) {
            imageUrlToLoad = property.getImageUrl(); // Fallback to the single imageUrl field
        }

        if (imageUrlToLoad != null) {
            Glide.with(context)
                    .load(imageUrlToLoad)
                    .placeholder(R.drawable.placeholder_image) // Ensure placeholder_image exists
                    .error(R.drawable.ic_launcher_background) // Or a more generic error placeholder
                    .into(holder.houseImageView);
        } else {
            holder.houseImageView.setImageResource(R.drawable.placeholder_image);
        }

        // Handle visibility and listeners for seller-specific buttons
        if (isSellerContext) {
            holder.buttonEditListing.setVisibility(View.VISIBLE);
            holder.buttonAlreadyListed.setVisibility(View.VISIBLE);
            holder.buttonRemoveListing.setVisibility(View.VISIBLE);

            holder.buttonEditListing.setOnClickListener(v -> {
                if (propertyActionListener != null) {
                    propertyActionListener.onEditClick(property);
                } else {
                    // Fallback or direct navigation if listener not used for this specific action
                    Intent editIntent = new Intent(context, com.example.affordablehousefinderrevamp.Seller.UploadActivity.class);
                    editIntent.putExtra("propertyIdToEdit", property.getPropertyId());
                    context.startActivity(editIntent);
                }
            });

            holder.buttonAlreadyListed.setOnClickListener(v -> {
                if (propertyActionListener != null) {
                    propertyActionListener.onChangeStatusClick(property);
                } else {
                    Toast.makeText(context, "Action listener not set for status change.", Toast.LENGTH_SHORT).show();
                }
            });

            holder.buttonRemoveListing.setOnClickListener(v -> {
                if (propertyActionListener != null) {
                    propertyActionListener.onRemoveClick(property);
                } else {
                    Toast.makeText(context, "Action listener not set for remove.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Hide seller buttons if in buyer context
            holder.buttonEditListing.setVisibility(View.GONE);
            holder.buttonAlreadyListed.setVisibility(View.GONE);
            holder.buttonRemoveListing.setVisibility(View.GONE);
        }


        holder.itemView.setOnClickListener(v -> {
            Intent detailIntent;
            if (isSellerContext) {
                detailIntent = new Intent(context, SellerPropertyDetails.class);
            } else {
                detailIntent = new Intent(context, PropertyDetail.class);
            }
            detailIntent.putExtra("propertyId", property.getPropertyId());
            context.startActivity(detailIntent);
        });
    }

    @Override
    public int getItemCount() {
        return propertyList != null ? propertyList.size() : 0;
    }

    public static class PropertyViewHolder extends RecyclerView.ViewHolder {
        ImageView houseImageView;
        TextView houseTitleTextView;
        TextView houseLocationTextView;
        TextView housePriceTextView;
        TextView houseStatusTextView; // For status
        Button buttonEditListing, buttonAlreadyListed, buttonRemoveListing;


        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            // Corrected IDs to match list_item_house.xml (assuming they are correct)
            houseImageView = itemView.findViewById(R.id.house_image);
            houseTitleTextView = itemView.findViewById(R.id.house_name);
            houseLocationTextView = itemView.findViewById(R.id.house_location);
            housePriceTextView = itemView.findViewById(R.id.house_price);
            houseStatusTextView = itemView.findViewById(R.id.house_status); // Ensure this ID exists

            buttonEditListing = itemView.findViewById(R.id.button_edit_listing);
            buttonAlreadyListed = itemView.findViewById(R.id.button_already_listed);
            buttonRemoveListing = itemView.findViewById(R.id.button_remove_listing);
        }
    }

    public void updateProperties(List<Property> newProperties) {
        this.propertyList.clear();
        if (newProperties != null) {
            this.propertyList.addAll(newProperties);
        }
        notifyDataSetChanged();
    }
}
