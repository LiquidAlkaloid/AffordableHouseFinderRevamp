package com.example.affordablehousefinderrevamp.Buyer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import com.example.affordablehousefinderrevamp.Adapter.PropertyAdapter;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
// Removed java.util.Date as it's not directly used here for ordering anymore (Firestore handles timestamp)

public class Homepage extends AppCompatActivity {

    private RecyclerView recyclerViewProperties;
    private PropertyAdapter propertyAdapter;
    private List<Property> propertyList;
    private FirebaseFirestore db;
    private ListenerRegistration propertiesListener;

    private BottomNavigationView bottomNavigationViewBuyer;
    private EditText searchBar;
    private TextView textViewNewHomesNearbyTitle;
    private TextView emptyViewProperties;

    private static final String TAG = "HomepageBuyer";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        searchBar = findViewById(R.id.searchBar);
        textViewNewHomesNearbyTitle = findViewById(R.id.textViewNewHomesNearbyTitle);
        recyclerViewProperties = findViewById(R.id.recyclerViewProperties);
        emptyViewProperties = findViewById(R.id.emptyViewProperties); // Make sure this ID is in your activity_homepage.xml
        bottomNavigationViewBuyer = findViewById(R.id.bottomNavigationView);

        db = FirebaseFirestore.getInstance();

        if (recyclerViewProperties != null) {
            recyclerViewProperties.setHasFixedSize(true);
            recyclerViewProperties.setLayoutManager(new LinearLayoutManager(this));
            propertyList = new ArrayList<>();
            // For buyer context, isSellerContext is false, no action listener needed for status change from here
            propertyAdapter = new PropertyAdapter(this, propertyList, false);
            recyclerViewProperties.setAdapter(propertyAdapter);
            fetchProperties();
        } else {
            Toast.makeText(this, "Error: RecyclerView not found.", Toast.LENGTH_LONG).show();
            if (emptyViewProperties != null) emptyViewProperties.setVisibility(View.VISIBLE);
        }

        setupBottomNavigation();
    }

    private void fetchProperties() {
        if (propertyAdapter == null) {
            Log.w(TAG, "PropertyAdapter is null, cannot fetch properties.");
            updateEmptyViewVisibility();
            return;
        }

        if (propertiesListener != null) {
            propertiesListener.remove(); // Remove existing listener
        }

        propertiesListener = db.collection("properties")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for properties.", e);
                        Toast.makeText(Homepage.this, "Failed to load properties: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        updateEmptyViewVisibility();
                        return;
                    }

                    propertyList.clear();
                    if (queryDocumentSnapshots != null) {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Property property = doc.toObject(Property.class);
                            // property.setPropertyId(doc.getId()); // @DocumentId in Property model handles this
                            propertyList.add(property);
                        }
                    }
                    propertyAdapter.notifyDataSetChanged();
                    updateEmptyViewVisibility();
                });
    }

    private void updateEmptyViewVisibility() {
        if (propertyList.isEmpty()) {
            if (emptyViewProperties != null) {
                emptyViewProperties.setVisibility(View.VISIBLE);
                emptyViewProperties.setText(getString(R.string.no_properties_found)); // Ensure this string resource exists
            }
            if (recyclerViewProperties != null) recyclerViewProperties.setVisibility(View.GONE);
        } else {
            if (emptyViewProperties != null) emptyViewProperties.setVisibility(View.GONE);
            if (recyclerViewProperties != null) recyclerViewProperties.setVisibility(View.VISIBLE);
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigationViewBuyer != null) {
            bottomNavigationViewBuyer.setSelectedItemId(R.id.navigation_home);
            bottomNavigationViewBuyer.setOnNavigationItemSelectedListener(item -> {
                Intent intent = null;
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) return true; // Already on this screen
                else if (itemId == R.id.navigation_chat) intent = new Intent(Homepage.this, ChatBuyerActivity.class);
                else if (itemId == R.id.navigation_profile) intent = new Intent(Homepage.this, BuyerProfile.class);
                // Consider adding Bookmarks if it's in your bottom_navigation_menu_buyer.xml
                // else if (itemId == R.id.navigation_bookmarks) intent = new Intent(Homepage.this, Bookmarks.class);


                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0,0);
                    finish(); // Finish current activity to prevent stacking
                    return true;
                }
                return false;
            });
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (propertiesListener != null) {
            propertiesListener.remove();
            propertiesListener = null; // Good practice to nullify after removing
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Re-attach listener if it was removed, to ensure data is fresh if user navigates back.
        if (propertyAdapter != null && propertiesListener == null) { // Check if listener is null before re-fetching
            fetchProperties();
        }
    }
}
