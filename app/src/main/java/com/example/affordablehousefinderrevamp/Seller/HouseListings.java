package com.example.affordablehousefinderrevamp.Seller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog; // Import AlertDialog
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.DialogInterface; // Import DialogInterface
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.view.View;

import com.example.affordablehousefinderrevamp.Adapter.PropertyAdapter;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference; // Import DocumentReference
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch; // Import WriteBatch for atomic operations if needed

import java.util.ArrayList;
import java.util.List;

public class HouseListings extends AppCompatActivity implements PropertyAdapter.OnPropertyActionListener {

    private static final String TAG = "HouseListings";

    private RecyclerView recyclerViewSellerProperties;
    private PropertyAdapter propertyAdapter;
    private List<Property> sellerPropertyList;
    private FirebaseFirestore db;
    private ListenerRegistration sellerPropertiesListener;

    private FirebaseUser currentUser;
    private BottomNavigationView bottomNavigationViewSeller;
    private Button btnAddNewListing;
    private TextView emptyViewSellerListings; // Added for empty state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_house_listings);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Login required.", Toast.LENGTH_LONG).show();
            finish(); // Close activity if user is not logged in
            return;
        }

        db = FirebaseFirestore.getInstance();

        recyclerViewSellerProperties = findViewById(R.id.recyclerViewSellerProperties);
        emptyViewSellerListings = findViewById(R.id.emptyViewSellerListings); // Initialize empty view
        btnAddNewListing = findViewById(R.id.btnAddNewListing);
        bottomNavigationViewSeller = findViewById(R.id.bottom_navigation_seller);

        if (recyclerViewSellerProperties != null) {
            recyclerViewSellerProperties.setHasFixedSize(true);
            recyclerViewSellerProperties.setLayoutManager(new LinearLayoutManager(this));
            sellerPropertyList = new ArrayList<>();
            // Pass 'this' as the OnPropertyActionListener
            propertyAdapter = new PropertyAdapter(this, sellerPropertyList, true, this);
            recyclerViewSellerProperties.setAdapter(propertyAdapter);
            fetchSellerProperties();
        } else {
            Toast.makeText(this, "Error: RecyclerView not found.", Toast.LENGTH_LONG).show();
            if (emptyViewSellerListings != null) emptyViewSellerListings.setVisibility(View.VISIBLE);
        }


        if (btnAddNewListing != null) {
            btnAddNewListing.setOnClickListener(v -> {
                Intent uploadIntent = new Intent(HouseListings.this, UploadActivity.class);
                startActivity(uploadIntent);
            });
        }

        setupBottomNavigation();
    }

    private void fetchSellerProperties() {
        if (currentUser == null || propertyAdapter == null) {
            Log.w(TAG, "Cannot fetch seller properties: current user or adapter is null.");
            updateEmptyViewVisibility();
            return;
        }
        String currentUserId = currentUser.getUid();

        if (sellerPropertiesListener != null) {
            sellerPropertiesListener.remove(); // Remove existing listener before attaching a new one
        }

        sellerPropertiesListener = db.collection("properties")
                .whereEqualTo("sellerId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for seller properties.", e);
                        Toast.makeText(HouseListings.this, "Failed to load your properties: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        updateEmptyViewVisibility();
                        return;
                    }

                    sellerPropertyList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Property property = doc.toObject(Property.class);
                            // property.setPropertyId(doc.getId()); // @DocumentId in Property model handles this
                            sellerPropertyList.add(property);
                        }
                    }
                    propertyAdapter.notifyDataSetChanged();
                    updateEmptyViewVisibility();
                });
    }

    private void updateEmptyViewVisibility() {
        if (sellerPropertyList.isEmpty()) {
            if (emptyViewSellerListings != null) emptyViewSellerListings.setVisibility(View.VISIBLE);
            if (recyclerViewSellerProperties != null) recyclerViewSellerProperties.setVisibility(View.GONE);
            // Toast.makeText(HouseListings.this, "You have not listed any properties yet.", Toast.LENGTH_SHORT).show(); // Optional toast
        } else {
            if (emptyViewSellerListings != null) emptyViewSellerListings.setVisibility(View.GONE);
            if (recyclerViewSellerProperties != null) recyclerViewSellerProperties.setVisibility(View.VISIBLE);
        }
    }


    @Override
    public void onEditClick(Property property) {
        Intent editIntent = new Intent(this, UploadActivity.class);
        editIntent.putExtra("propertyIdToEdit", property.getPropertyId());
        startActivity(editIntent);
    }

    @Override
    public void onRemoveClick(Property property) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Listing")
                .setMessage("Are you sure you want to remove '" + property.getTitle() + "'?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    deletePropertyFromFirestore(property.getPropertyId());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePropertyFromFirestore(String propertyIdToDelete) {
        if (propertyIdToDelete == null || propertyIdToDelete.isEmpty()) {
            Toast.makeText(this, "Error: Property ID missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("properties").document(propertyIdToDelete)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(HouseListings.this, "Property removed.", Toast.LENGTH_SHORT).show();
                    // The snapshot listener will automatically update the list.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HouseListings.this, "Failed to remove property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error removing property", e);
                });
    }

    @Override
    public void onChangeStatusClick(Property property) {
        if (property == null || property.getPropertyId() == null) {
            Toast.makeText(this, "Error: Property data missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentStatus = property.getStatus();
        String newStatus;

        if ("Available".equalsIgnoreCase(currentStatus)) {
            newStatus = "Taken";
        } else if ("Taken".equalsIgnoreCase(currentStatus) || "Sold".equalsIgnoreCase(currentStatus)) {
            newStatus = "Available";
        } else {
            // Default to "Available" if status is unknown or something else
            newStatus = "Available";
        }

        DocumentReference propertyRef = db.collection("properties").document(property.getPropertyId());
        propertyRef.update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(HouseListings.this, "Status changed to " + newStatus, Toast.LENGTH_SHORT).show();
                    // Firestore listener will update the UI automatically.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(HouseListings.this, "Failed to change status: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error updating property status", e);
                });
    }


    private void setupBottomNavigation() {
        if (bottomNavigationViewSeller != null) {
            bottomNavigationViewSeller.setSelectedItemId(R.id.navigation_upload);
            bottomNavigationViewSeller.setOnNavigationItemSelectedListener(item -> {
                Intent intent = null;
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_upload) {
                    return true; // Already on this screen
                } else if (itemId == R.id.navigation_chat) {
                    intent = new Intent(HouseListings.this, ChatSellerActivity.class); // Corrected from Chat_Seller to ChatSellerActivity
                } else if (itemId == R.id.navigation_profile) {
                    intent = new Intent(HouseListings.this, SellerProfile.class);
                }

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sellerPropertiesListener != null) {
            sellerPropertiesListener.remove();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Re-attach listener if it was removed, to ensure data is fresh if user navigates back.
        if (currentUser != null && propertyAdapter != null && sellerPropertiesListener == null) {
            fetchSellerProperties();
        }
    }
}
