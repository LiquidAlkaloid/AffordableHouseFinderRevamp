package com.example.affordablehousefinderrevamp.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.Buyer.PropertyDetail; // Assuming this is the detail view for buyers
import com.example.affordablehousefinderrevamp.R;
import com.example.affordablehousefinderrevamp.Seller.SellerPropertyDetails; // Assuming this is the detail view for sellers
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class PropertyAdapter extends RecyclerView.Adapter<PropertyAdapter.PropertyViewHolder> {

    private Context context;
    private List<Property> propertyList;
    private boolean isSellerView; // To differentiate between buyer and seller views
    private CollectionReference mFirestorePropertiesCollection; // Firestore collection reference
    private FirebaseUser currentUser;

    private static final String TAG = "PropertyAdapter";

    public PropertyAdapter(Context context, List<Property> propertyList, boolean isSellerView) {
        this.context = context;
        this.propertyList = propertyList;
        this.isSellerView = isSellerView;
        // Initialize Firestore collection reference
        this.mFirestorePropertiesCollection = FirebaseFirestore.getInstance().collection("Properties");
        this.currentUser = FirebaseAuth.getInstance().getCurrentUser();
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

        if (property == null) {
            Log.e(TAG, "Property object at position " + position + " is null.");
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        holder.itemView.setVisibility(View.VISIBLE);

        holder.houseName.setText(property.getName());
        holder.houseLocation.setText(property.getLocation());

        // Format price
        try {
            if (property.getPrice() != null && !property.getPrice().isEmpty()) {
                double priceDouble = Double.parseDouble(property.getPrice().replaceAll("[^\\d.]", "")); // Remove non-numeric characters
                NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("fil", "PH")); // Filipino Peso
                holder.housePrice.setText(format.format(priceDouble));
            } else {
                holder.housePrice.setText("N/A");
            }
        } catch (NumberFormatException | NullPointerException e) {
            holder.housePrice.setText("₱" + property.getPrice()); // Fallback
            Log.e(TAG, "Error formatting price: " + property.getPrice(), e);
        }

        // Set current status
        String currentStatus = property.getStatus();
        if (currentStatus == null || currentStatus.isEmpty()) {
            currentStatus = Property.STATUS_AVAILABLE; // Default if null or empty
            property.setStatus(currentStatus); // Update model instance
        }
        updateStatusTextColor(holder.houseStatus, currentStatus); // Call this before setting text to ensure color is applied

        if (property.getImageUrls() != null && !property.getImageUrls().isEmpty()) {
            Glide.with(context)
                    .load(property.getImageUrls().get(0)) // Load first image
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image) // Error placeholder
                    .into(holder.houseImage);
        } else {
            Glide.with(context).load(R.drawable.placeholder_image).into(holder.houseImage); // Default placeholder
        }

        // Handle visibility of seller-specific buttons
        // This client-side check ensures that only the owner of the property sees these controls.
        // It complements the Firestore security rules which enforce this on the server-side.
        // Firestore rule: allow write: if request.auth != null && request.resource.data.sellerId == request.auth.uid;
        // (or resource.data.sellerId == request.auth.uid for update/delete)
        if (isSellerView && currentUser != null && property.getSellerId() != null && currentUser.getUid().equals(property.getSellerId())) {
            holder.sellerActionsLayout.setVisibility(View.VISIBLE);
            updateButtonAlreadyListedTextAndColor(holder.buttonAlreadyListed, currentStatus);

            holder.buttonAlreadyListed.setOnClickListener(v -> handleChangeStatus(property, holder));
            holder.buttonRemoveListing.setOnClickListener(v -> handleRemoveListing(property, holder.getAdapterPosition()));
        } else {
            holder.sellerActionsLayout.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (property.getPropertyId() == null || property.getPropertyId().isEmpty()) {
                Toast.makeText(context, "Property details are not available.", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Property ID is null or empty, cannot open details.");
                return;
            }
            Intent intent;
            if (isSellerView) {
                intent = new Intent(context, SellerPropertyDetails.class);
            } else {
                intent = new Intent(context, PropertyDetail.class);
            }
            intent.putExtra("propertyId", property.getPropertyId());
            // Add other necessary extras like sellerId, etc., if needed by the detail activity
            intent.putExtra("sellerId", property.getSellerId());
            context.startActivity(intent);
        });
    }

    private void handleChangeStatus(Property property, PropertyViewHolder holder) {
        if (property.getPropertyId() == null || property.getPropertyId().isEmpty()) {
            Toast.makeText(context, "Cannot update status: Property ID missing.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Property ID is null, cannot update status.");
            return;
        }

        String currentStatus = property.getStatus();
        String newStatus;

        switch (currentStatus) {
            case Property.STATUS_AVAILABLE:
                newStatus = Property.STATUS_RESERVED;
                break;
            case Property.STATUS_RESERVED:
                newStatus = Property.STATUS_TAKEN;
                break;
            case Property.STATUS_TAKEN:
                newStatus = Property.STATUS_AVAILABLE;
                break;
            default:
                newStatus = Property.STATUS_AVAILABLE; // Fallback
                Log.w(TAG, "Unknown status: " + currentStatus + ", defaulting to Available.");
                break;
        }

        // Update Firestore: only update the 'status' field.
        // The client-side check in onBindViewHolder ensures this button is only visible to the seller.
        // Firestore security rules (e.g., checking if resource.data.sellerId == request.auth.uid)
        // provide server-side enforcement that only the authenticated owner can perform this update.
        mFirestorePropertiesCollection.document(property.getPropertyId()).update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    property.setStatus(newStatus); // Update local model
                    updateStatusTextColor(holder.houseStatus, newStatus); // Update text and color
                    updateButtonAlreadyListedTextAndColor(holder.buttonAlreadyListed, newStatus);
                    Toast.makeText(context, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Status updated to " + newStatus + " for property ID: " + property.getPropertyId());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to update status: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to update status for " + property.getPropertyId(), e);
                });
    }

    private void handleRemoveListing(Property property, int position) {
        if (property.getPropertyId() == null || property.getPropertyId().isEmpty()) {
            Toast.makeText(context, "Cannot remove listing: Property ID missing.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Property ID is null, cannot remove listing.");
            return;
        }
        if (position == RecyclerView.NO_POSITION) {
            Log.e(TAG, "Invalid position for remove: " + position);
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("Remove Listing")
                .setMessage("Are you sure you want to remove '" + property.getName() + "'? This action cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) -> {
                    // Attempt to delete the property from Firestore.
                    // The client-side check in onBindViewHolder ensures this button is only visible to the seller.
                    // Firestore security rules (e.g., checking if resource.data.sellerId == request.auth.uid)
                    // provide server-side enforcement that only the authenticated owner can delete the property.
                    mFirestorePropertiesCollection.document(property.getPropertyId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                // Check if the item still exists at the position and matches the ID
                                // This prevents crashes if the list changes rapidly
                                if (position < propertyList.size() && propertyList.get(position).getPropertyId().equals(property.getPropertyId())) {
                                    propertyList.remove(position);
                                    notifyItemRemoved(position);
                                    // notifyItemRangeChanged is important to update subsequent items' positions
                                    notifyItemRangeChanged(position, propertyList.size() - position);
                                    Toast.makeText(context, "Listing removed successfully", Toast.LENGTH_SHORT).show();
                                    Log.d(TAG, "Listing removed: " + property.getPropertyId());
                                } else {
                                    Toast.makeText(context, "Listing already removed or list updated.", Toast.LENGTH_LONG).show();
                                    Log.w(TAG, "Tried to remove item at position " + position + " but it was not found or ID mismatch.");
                                    // Consider refreshing the list from Firestore if this happens frequently
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to remove listing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.e(TAG, "Failed to remove listing " + property.getPropertyId(), e);
                            });
                })
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    private void updateButtonAlreadyListedTextAndColor(Button button, String currentStatus) {
        // Ensure context is not null before accessing resources
        if (context == null) {
            Log.e(TAG, "Context is null in updateButtonAlreadyListedTextAndColor");
            return;
        }
        switch (currentStatus) {
            case Property.STATUS_AVAILABLE:
                button.setText("Mark as Reserved");
                button.setBackgroundColor(context.getResources().getColor(R.color.button_yellow));
                break;
            case Property.STATUS_RESERVED:
                button.setText("Mark as Taken");
                button.setBackgroundColor(context.getResources().getColor(R.color.button_orange));
                break;
            case Property.STATUS_TAKEN:
                button.setText("Mark as Available");
                button.setBackgroundColor(context.getResources().getColor(R.color.button_green));
                break;
            default:
                button.setText("Change Status");
                button.setBackgroundColor(context.getResources().getColor(R.color.grey));
                break;
        }
        button.setTextColor(Color.WHITE);
    }

    private void updateStatusTextColor(TextView statusTextView, String currentStatus) {
        // Ensure context is not null before accessing resources
        if (context == null) {
            Log.e(TAG, "Context is null in updateStatusTextColor");
            statusTextView.setText("Status: " + (currentStatus != null ? currentStatus : "Unknown"));
            statusTextView.setTextColor(Color.DKGRAY);
            return;
        }
        statusTextView.setText("Status: " + currentStatus);
        switch (currentStatus) {
            case Property.STATUS_AVAILABLE:
                statusTextView.setTextColor(context.getResources().getColor(R.color.status_available_green));
                break;
            case Property.STATUS_RESERVED:
                statusTextView.setTextColor(context.getResources().getColor(R.color.status_reserved_orange));
                break;
            case Property.STATUS_TAKEN:
                statusTextView.setTextColor(context.getResources().getColor(R.color.status_taken_red));
                break;
            default:
                statusTextView.setTextColor(Color.DKGRAY);
                break;
        }
    }


    @Override
    public int getItemCount() {
        return propertyList == null ? 0 : propertyList.size();
    }

    // Method to update the adapter's data
    public void updateData(List<Property> newPropertyList) {
        this.propertyList.clear();
        if (newPropertyList != null) {
            this.propertyList.addAll(newPropertyList);
        }
        notifyDataSetChanged(); // Consider using DiffUtil for better performance
        Log.d(TAG, "Adapter data updated. New size: " + (propertyList != null ? propertyList.size() : 0));
    }


    public static class PropertyViewHolder extends RecyclerView.ViewHolder {
        ImageView houseImage;
        TextView houseName, houseLocation, housePrice, houseStatus;
        Button buttonAlreadyListed, buttonRemoveListing;
        LinearLayout sellerActionsLayout;

        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            houseImage = itemView.findViewById(R.id.house_image);
            houseName = itemView.findViewById(R.id.house_name);
            houseLocation = itemView.findViewById(R.id.house_location);
            housePrice = itemView.findViewById(R.id.house_price);
            houseStatus = itemView.findViewById(R.id.house_status);
            buttonAlreadyListed = itemView.findViewById(R.id.button_already_listed);
            buttonRemoveListing = itemView.findViewById(R.id.button_remove_listing);
            sellerActionsLayout = itemView.findViewById(R.id.seller_actions_layout);
        }
    }

    // Reminder: Define these colors in your app/src/main/res/values/colors.xml:
    // <color name="button_yellow">#FFC107</color>
    // <color name="button_orange">#FF9800</color>
    // <color name="button_green">#4CAF50</color>
    // <color name="grey">#BDBDBD</color>
    // <color name="status_available_green">#388E3C</color>
    // <color name="status_reserved_orange">#F57C00</color>
    // <color name="status_taken_red">#D32F2F</color>
}
