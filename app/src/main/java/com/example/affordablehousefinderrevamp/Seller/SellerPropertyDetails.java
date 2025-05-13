package com.example.affordablehousefinderrevamp.Seller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
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
import com.google.firebase.firestore.DocumentReference; // Firestore
import com.google.firebase.firestore.FirebaseFirestore;   // Firestore
import com.google.firebase.firestore.ListenerRegistration; // Firestore


public class SellerPropertyDetails extends AppCompatActivity {

    private static final String TAG = "SellerPropertyDetails";

    private ImageView propertyImageView;
    private ImageButton backButton, shareButton, moreOptionsButton, descriptionEditIcon;
    private TextView propertyTitleTextView, propertyPriceTextView, locationTextView,
            conditionTextView, bookmarkTextView, descriptionTextView; // Removed descriptionLabelTextView as it's static
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
        conditionTextView = findViewById(R.id.conditionTextView);
        bookmarkTextView = findViewById(R.id.bookmarkTextView);
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
            Toast.makeText(this, "Authentication error.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        propertyDocRef = db.collection("properties").document(propertyId);

        loadPropertyDetails();

        backButton.setOnClickListener(v -> onBackPressed());
        shareButton.setOnClickListener(v -> shareProperty());
        moreOptionsButton.setOnClickListener(this::showMoreOptionsMenu);
        descriptionEditIcon.setOnClickListener(v -> editProperty());
        viewInsightsButton.setOnClickListener(v -> Toast.makeText(this, "View Insights (not implemented)", Toast.LENGTH_SHORT).show());
        noChatsButton.setOnClickListener(v -> Toast.makeText(this, "No chats (not implemented)", Toast.LENGTH_SHORT).show());
    }

    private void shareProperty() {
        if (currentProperty != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareBody = "Check out this property: " + currentProperty.getTitle() + "\nLocation: " + currentProperty.getLocation();
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Property: " + currentProperty.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } else {
            Toast.makeText(this, "Details not loaded.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMoreOptionsMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        // Ensure you have res/menu/seller_property_options_menu.xml
        popup.getMenuInflater().inflate(R.menu.seller_property_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_property_details) {
                editProperty();
                return true;
            } else if (itemId == R.id.action_delete_property_details) {
                confirmDeleteProperty();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void loadPropertyDetails() {
        propertyListener = propertyDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                Toast.makeText(SellerPropertyDetails.this, "Failed to load details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                currentProperty = snapshot.toObject(Property.class);
                if (currentProperty != null) {
                    // currentProperty.setPropertyId(snapshot.getId()); // @DocumentId handles this
                    if (!currentUser.getUid().equals(currentProperty.getSellerId())) {
                        Toast.makeText(SellerPropertyDetails.this, "Not authorized.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    populateUI();
                } else {
                    Toast.makeText(SellerPropertyDetails.this, "Error converting property data.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Log.d(TAG, "Current data: null");
                Toast.makeText(SellerPropertyDetails.this, "Property details not found.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateUI() {
        if (currentProperty == null) return;

        propertyTitleTextView.setText(currentProperty.getTitle().toUpperCase());
        propertyPriceTextView.setText(currentProperty.getPrice());
        locationTextView.setText(currentProperty.getLocation());
        descriptionTextView.setText(currentProperty.getDescription());
        // Populate new fields if data exists in Property model
        conditionTextView.setText(currentProperty.getPropertyType()); // Example: using propertyType for condition
        bookmarkTextView.setText("N/A Bookmarks"); // Placeholder, bookmark count not in Property model

        String imageUrlToLoad = null;
        if (currentProperty.getImageUrl() != null && !currentProperty.getImageUrl().isEmpty()) {
            imageUrlToLoad = currentProperty.getImageUrl();
        } else if (currentProperty.getImageUrls() != null && !currentProperty.getImageUrls().isEmpty()) {
            imageUrlToLoad = currentProperty.getImageUrls().get(0);
        }

        if (imageUrlToLoad != null) {
            Glide.with(this).load(imageUrlToLoad)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.ic_launcher_background)
                    .into(propertyImageView);
        } else {
            propertyImageView.setImageResource(R.drawable.placeholder);
        }
    }

    private void editProperty() {
        if (currentProperty == null) return;
        Intent editIntent = new Intent(this, UploadActivity.class);
        editIntent.putExtra("propertyIdToEdit", propertyId); // propertyId is already class member
        startActivity(editIntent);
    }

    private void confirmDeleteProperty() {
        if (currentProperty == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Property")
                .setMessage("Are you sure you want to delete this listing?")
                .setPositiveButton("Delete", (dialog, which) -> deletePropertyFromFirestore())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePropertyFromFirestore() {
        if (propertyId == null) return;
        Log.d(TAG, "Attempting to delete property: " + propertyId);
        // Image deletion from Storage is skipped as per prior request.
        propertyDocRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SellerPropertyDetails.this, "Property deleted.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Property deleted successfully: " + propertyId);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SellerPropertyDetails.this, "Failed to delete property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting property", e);
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
