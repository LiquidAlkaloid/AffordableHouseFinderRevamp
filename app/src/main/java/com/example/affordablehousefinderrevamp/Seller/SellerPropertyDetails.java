package com.example.affordablehousefinderrevamp.Seller;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

// Removed import for java.util.Map as it's not directly used in this corrected version's populateUI for amenities.
// If you re-add detailed amenity display, you might need it.

public class SellerPropertyDetails extends AppCompatActivity {

    private static final String TAG = "SellerPropertyDetails";

    private ImageView propertyImageView;
    private ImageButton backButton, shareButton, moreOptionsButton, descriptionEditIcon;
    private TextView propertyTitleTextView, propertyPriceTextView, locationTextView,
            conditionTextView, descriptionTextView; // Removed bookmarkTextView as it's repurposed for status

    // Views for displaying property status (formerly bookmarkIcon and bookmarkTextView)
    private ImageView statusIconImageView;
    private TextView statusTextView;

    private Button viewInsightsButton, noChatsButton;

    private FirebaseFirestore db;
    private DocumentReference propertyDocRef;
    private ListenerRegistration propertyListener;
    private FirebaseUser currentUser;

    private String propertyId;
    private Property currentProperty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_property_details);

        propertyImageView = findViewById(R.id.propertyImageView);
        backButton = findViewById(R.id.backButton);
        shareButton = findViewById(R.id.shareButton);
        moreOptionsButton = findViewById(R.id.moreOptionsButton);
        descriptionEditIcon = findViewById(R.id.descriptionEditIcon);
        propertyTitleTextView = findViewById(R.id.propertyTitleTextView);
        propertyPriceTextView = findViewById(R.id.propertyPriceTextView);
        locationTextView = findViewById(R.id.locationTextView);
        conditionTextView = findViewById(R.id.conditionTextView); // Represents propertyType

        // Initialize the status views using the IDs from the XML
        // (previously bookmarkIcon and bookmarkTextView)
        statusIconImageView = findViewById(R.id.statusIcon);
        statusTextView = findViewById(R.id.statusTextView);

        descriptionTextView = findViewById(R.id.descriptionTextView);
        viewInsightsButton = findViewById(R.id.viewInsightsButton);
        noChatsButton = findViewById(R.id.noChatsButton);


        propertyId = getIntent().getStringExtra("propertyId");
        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(this, "Property ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error. Please log in.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        propertyDocRef = db.collection("Properties").document(propertyId);

        loadPropertyDetails();

        backButton.setOnClickListener(v -> onBackPressed());
        shareButton.setOnClickListener(v -> shareProperty());
        moreOptionsButton.setOnClickListener(this::showMoreOptionsMenu);
        if (descriptionEditIcon != null) {
            descriptionEditIcon.setOnClickListener(v -> editProperty());
        } else {
            Log.w(TAG, "descriptionEditIcon is null. Check layout ID in activity_seller_property_details.xml.");
        }
        viewInsightsButton.setOnClickListener(v -> Toast.makeText(this, "View Insights (not implemented)", Toast.LENGTH_SHORT).show());
        noChatsButton.setOnClickListener(v -> Toast.makeText(this, "No chats (not implemented)", Toast.LENGTH_SHORT).show());
    }

    private void shareProperty() {
        if (currentProperty != null && currentProperty.getName() != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String propertyShareUrl = "https://your-app-domain.com/property/" + propertyId; // Replace with actual domain/path
            String shareBody = "Check out this property: " + currentProperty.getName() +
                    "\nLocation: " + currentProperty.getLocation() +
                    "\nPrice: " + currentProperty.getPrice() +
                    "\n" + propertyShareUrl;
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Property Listing: " + currentProperty.getName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } else {
            Toast.makeText(this, "Property details not fully loaded yet.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMoreOptionsMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.seller_property_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_property) {
                editProperty();
                return true;
            } else if (itemId == R.id.action_remove_property) {
                confirmDeleteProperty();
                return true;
            }
            // You can add R.id.action_change_status here if you add it to the menu XML
            // and want to trigger status change from the menu.
            return false;
        });
        popup.show();
    }

    private void loadPropertyDetails() {
        if (propertyDocRef == null) return;
        propertyListener = propertyDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                Toast.makeText(SellerPropertyDetails.this, "Failed to load property details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                currentProperty = snapshot.toObject(Property.class);
                if (currentProperty != null) {
                    currentProperty.setPropertyId(snapshot.getId());
                    if (!currentUser.getUid().equals(currentProperty.getSellerId())) {
                        Toast.makeText(SellerPropertyDetails.this, "You are not authorized to view these details.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    populateUI();
                } else {
                    Toast.makeText(SellerPropertyDetails.this, "Error: Could not parse property data.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Log.d(TAG, "Current data: null or property deleted.");
                Toast.makeText(SellerPropertyDetails.this, "Property details not found or deleted.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateUI() {
        if (currentProperty == null) return;

        propertyTitleTextView.setText(currentProperty.getName() != null ? currentProperty.getName().toUpperCase() : "N/A");
        propertyPriceTextView.setText(currentProperty.getPrice());
        locationTextView.setText(currentProperty.getLocation());
        descriptionTextView.setText(currentProperty.getDescription());
        conditionTextView.setText("Type: " + (currentProperty.getPropertyType() != null ? currentProperty.getPropertyType() : "N/A"));

        // Update status display using statusIconImageView and statusTextView
        String status = currentProperty.getStatus();
        if (statusTextView != null) {
            statusTextView.setText("Status: " + status);
        }
        if (statusIconImageView != null) {
            switch (status.toLowerCase()) { // Use toLowerCase for case-insensitive matching
                case "available":
                    statusIconImageView.setImageResource(R.drawable.ic_check_circle_green_24dp); // Ensure this drawable exists
                    if (statusTextView != null) statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_available_green));
                    break;
                case "reserved":
                    statusIconImageView.setImageResource(R.drawable.ic_hourglass_orange_24dp); // Ensure this drawable exists
                    if (statusTextView != null) statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_reserved_orange));
                    break;
                case "taken":
                case "sold": // Handle "sold" as well if it's a possible status
                    statusIconImageView.setImageResource(R.drawable.ic_cancel_red_24dp); // Ensure this drawable exists
                    if (statusTextView != null) statusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_taken_red));
                    break;
                default:
                    statusIconImageView.setImageResource(R.drawable.ic_info_outline); // Default icon
                    if (statusTextView != null) statusTextView.setTextColor(Color.DKGRAY);
                    break;
            }
        }


        String imageUrlToLoad = currentProperty.getPrimaryImageUrl();
        if (imageUrlToLoad != null) {
            Glide.with(this).load(imageUrlToLoad)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(propertyImageView);
        } else {
            propertyImageView.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void editProperty() {
        if (currentProperty == null || propertyId == null) {
            Toast.makeText(this, "Property data not available for editing.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent editIntent = new Intent(this, UploadActivity.class);
        editIntent.putExtra("propertyIdToEdit", propertyId);
        startActivity(editIntent);
    }

    private void confirmDeleteProperty() {
        if (currentProperty == null) {
            Toast.makeText(this, "Property details not loaded.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Delete Property")
                .setMessage("Are you sure you want to delete '" + currentProperty.getName() + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deletePropertyFromFirestore())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deletePropertyFromFirestore() {
        if (propertyId == null || propertyDocRef == null) {
            Toast.makeText(SellerPropertyDetails.this, "Error: Cannot delete property.", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Attempting to delete property: " + propertyId);

        propertyDocRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SellerPropertyDetails.this, "Property deleted successfully.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Property deleted successfully from Firestore: " + propertyId);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SellerPropertyDetails.this, "Failed to delete property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting property from Firestore", e);
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (propertyListener != null) {
            propertyListener.remove();
        }
    }
}
