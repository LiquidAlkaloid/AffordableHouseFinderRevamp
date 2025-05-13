package com.example.affordablehousefinderrevamp.Adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button; // For edit, listed, remove buttons

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.affordablehousefinderrevamp.Buyer.PropertyDetail;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.example.affordablehousefinderrevamp.Seller.SellerPropertyDetails;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder> {

    private Context context;
    private List<Property> propertyList;
    private boolean isSellerContext;

    public PropertyAdapter(Context context, List<Property> propertyList, boolean isSellerContext) {
        this.context = context;
        this.propertyList = propertyList;
        this.isSellerContext = isSellerContext;
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
        // Assuming 'status' is a field in your Property model, or you derive it
        // holder.houseStatusTextView.setText("STATUS: " + (property.getStatus() != null ? property.getStatus() : "UNKNOWN"));


        if (property.getImageUrl() != null && !property.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(property.getImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.ic_launcher_background) // Consider a more generic error placeholder
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
                // Navigate to UploadActivity in edit mode
                Intent editIntent = new Intent(context, com.example.affordablehousefinderrevamp.Seller.UploadActivity.class);
                editIntent.putExtra("propertyIdToEdit", property.getPropertyId());
                context.startActivity(editIntent);
            });
            holder.buttonAlreadyListed.setOnClickListener(v -> {
                // Handle "Already Listed" logic (e.g., toggle status in Firebase)
                Toast.makeText(context, "Toggle 'listed' status for: " + property.getTitle(), Toast.LENGTH_SHORT).show();
            });
            holder.buttonRemoveListing.setOnClickListener(v -> {
                // Handle "Remove Listing" logic (e.g., show confirmation then delete from Firebase)
                // This would typically involve calling a method in the activity/fragment to handle deletion
                Toast.makeText(context, "Remove listing: " + property.getTitle(), Toast.LENGTH_SHORT).show();
            });
        } else {
            // Hide seller buttons if in buyer context
            holder.buttonEditListing.setVisibility(View.GONE);
            holder.buttonAlreadyListed.setVisibility(View.GONE);
            holder.buttonRemoveListing.setVisibility(View.GONE);
            // The bookmarkIcon from the original PropertyAdapter.java is not in list_item_house.xml
            // If you need a bookmark icon per item for buyers, it should be added to list_item_house.xml
            // and then handled here.
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
        TextView houseStatusTextView; // Added for status
        // ImageView bookmarkIcon; // Removed as not in list_item_house.xml by default
        Button buttonEditListing, buttonAlreadyListed, buttonRemoveListing;


        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            // Corrected IDs to match list_item_house.xml
            houseImageView = itemView.findViewById(R.id.house_image);
            houseTitleTextView = itemView.findViewById(R.id.house_name);
            houseLocationTextView = itemView.findViewById(R.id.house_location);
            housePriceTextView = itemView.findViewById(R.id.house_price);
            houseStatusTextView = itemView.findViewById(R.id.house_status);
            // bookmarkIcon = itemView.findViewById(R.id.bookmarkIcon); // ID does not exist in list_item_house.xml

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
