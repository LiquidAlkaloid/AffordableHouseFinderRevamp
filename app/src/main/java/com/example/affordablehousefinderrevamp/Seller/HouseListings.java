package com.example.affordablehousefinderrevamp.Seller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log; // For logging
import android.view.View; // For View.VISIBLE/GONE

import com.example.affordablehousefinderrevamp.Adapter.PropertyAdapter;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore; // Firestore
import com.google.firebase.firestore.Query; // Firestore
import com.google.firebase.firestore.QueryDocumentSnapshot; // Firestore
import com.google.firebase.firestore.ListenerRegistration; // Firestore

import java.util.ArrayList;
import java.util.List;

public class HouseListings extends AppCompatActivity {

    private static final String TAG = "HouseListings"; // For logging

    private RecyclerView recyclerViewSellerProperties;
    private PropertyAdapter propertyAdapter;
    private List<Property> sellerPropertyList;
    private FirebaseFirestore db; // Firestore instance
    private ListenerRegistration sellerPropertiesListener; // To remove listener

    private FirebaseUser currentUser;
    private BottomNavigationView bottomNavigationViewSeller;
    private Button btnAddNewListing;
    // private TextView emptyViewSellerListings; // Optional: if you add an empty view

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_house_listings); // Assumes this layout has the RecyclerView and Button

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Login required.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        recyclerViewSellerProperties = findViewById(R.id.recyclerViewSellerProperties);
        // emptyViewSellerListings = findViewById(R.id.emptyViewSellerListings); // Initialize if you add it
        btnAddNewListing = findViewById(R.id.btnAddNewListing);
        bottomNavigationViewSeller = findViewById(R.id.bottom_navigation_seller);

        if (recyclerViewSellerProperties != null) {
            recyclerViewSellerProperties.setHasFixedSize(true);
            recyclerViewSellerProperties.setLayoutManager(new LinearLayoutManager(this));
            sellerPropertyList = new ArrayList<>();
            propertyAdapter = new PropertyAdapter(this, sellerPropertyList, true); // isSellerContext = true
            recyclerViewSellerProperties.setAdapter(propertyAdapter);
            fetchSellerProperties();
        } else {
            Toast.makeText(this, "Error: RecyclerView not found.", Toast.LENGTH_LONG).show();
        }


        if (btnAddNewListing != null) {
            btnAddNewListing.setOnClickListener(v -> {
                Intent uploadIntent = new Intent(HouseListings.this, UploadActivity.class);
                startActivity(uploadIntent);
            });
        }

        if (bottomNavigationViewSeller != null) {
            bottomNavigationViewSeller.setSelectedItemId(R.id.navigation_upload); // Assuming this is the "Listings" or "Upload" tab for seller
            bottomNavigationViewSeller.setOnNavigationItemSelectedListener(item -> {
                Intent intent = null;
                int itemId = item.getItemId();

                // Use IDs from bottom_navigation_menu_seller.xml
                if (itemId == R.id.navigation_upload) { // This might be your "Listings" tab ID
                    return true;
                } else if (itemId == R.id.navigation_chat) {
                    intent = new Intent(HouseListings.this, Chat_Seller.class);
                } else if (itemId == R.id.navigation_profile) {
                    intent = new Intent(HouseListings.this, SellerProfile.class);
                }

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0,0);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void fetchSellerProperties() {
        if (currentUser == null || propertyAdapter == null) {
            Log.w(TAG, "Cannot fetch seller properties: current user or adapter is null.");
            // if (emptyViewSellerListings != null) emptyViewSellerListings.setVisibility(View.VISIBLE);
            // if (recyclerViewSellerProperties != null) recyclerViewSellerProperties.setVisibility(View.GONE);
            return;
        }
        String currentUserId = currentUser.getUid();

        // Query Firestore for properties where sellerId matches currentUserId
        // Order by timestamp, newest first. Ensure Firestore index for sellerId and timestamp.
        sellerPropertiesListener = db.collection("properties")
                .whereEqualTo("sellerId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for seller properties.", e);
                        Toast.makeText(HouseListings.this, "Failed to load your properties: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        // if (emptyViewSellerListings != null) emptyViewSellerListings.setVisibility(View.VISIBLE);
                        // if (recyclerViewSellerProperties != null) recyclerViewSellerProperties.setVisibility(View.GONE);
                        return;
                    }

                    sellerPropertyList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Property property = doc.toObject(Property.class);
                            // property.setPropertyId(doc.getId()); // @DocumentId handles this
                            sellerPropertyList.add(property);
                        }
                    }
                    propertyAdapter.notifyDataSetChanged();

                    if (sellerPropertyList.isEmpty()) {
                        // if (emptyViewSellerListings != null) emptyViewSellerListings.setVisibility(View.VISIBLE);
                        // if (recyclerViewSellerProperties != null) recyclerViewSellerProperties.setVisibility(View.GONE);
                        Toast.makeText(HouseListings.this, "You have not listed any properties yet.", Toast.LENGTH_SHORT).show();
                    } else {
                        // if (emptyViewSellerListings != null) emptyViewSellerListings.setVisibility(View.GONE);
                        // if (recyclerViewSellerProperties != null) recyclerViewSellerProperties.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sellerPropertiesListener != null) {
            sellerPropertiesListener.remove(); // Remove Firestore listener
        }
    }
}
