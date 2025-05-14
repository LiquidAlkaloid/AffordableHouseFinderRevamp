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
import com.google.firebase.firestore.FirebaseFirestore; // Firestore
import com.google.firebase.firestore.Query; // Firestore
import com.google.firebase.firestore.QueryDocumentSnapshot; // Firestore
import com.google.firebase.firestore.ListenerRegistration; // Firestore for removing listener

import java.util.ArrayList;
import java.util.List;
import java.util.Date; // For ordering by timestamp

public class Homepage extends AppCompatActivity {

    private RecyclerView recyclerViewProperties;
    private PropertyAdapter propertyAdapter;
    private List<Property> propertyList;
    private FirebaseFirestore db; // Firestore instance
    private ListenerRegistration propertiesListener; // To remove listener later

    private BottomNavigationView bottomNavigationViewBuyer;
    private EditText searchBar;
    private TextView textViewNewHomesNearbyTitle;
    private TextView emptyViewProperties;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage); // Assumes this is the correct layout with RecyclerView

        searchBar = findViewById(R.id.searchBar);
        textViewNewHomesNearbyTitle = findViewById(R.id.textViewNewHomesNearbyTitle);
        recyclerViewProperties = findViewById(R.id.recyclerViewProperties);
        emptyViewProperties = findViewById(R.id.emptyViewProperties);
        bottomNavigationViewBuyer = findViewById(R.id.bottomNavigationView);

        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        if (recyclerViewProperties != null) {
            recyclerViewProperties.setHasFixedSize(true);
            recyclerViewProperties.setLayoutManager(new LinearLayoutManager(this));
            propertyList = new ArrayList<>();
            propertyAdapter = new PropertyAdapter(this, propertyList, false);
            recyclerViewProperties.setAdapter(propertyAdapter);
            fetchProperties(); // Fetch data after adapter is set
        } else {
            Toast.makeText(this, "Error: RecyclerView not found.", Toast.LENGTH_LONG).show();
            if(emptyViewProperties != null) emptyViewProperties.setVisibility(View.VISIBLE);
        }

        if (bottomNavigationViewBuyer != null) {
            bottomNavigationViewBuyer.setSelectedItemId(R.id.navigation_home);
            bottomNavigationViewBuyer.setOnNavigationItemSelectedListener(item -> {
                Intent intent = null;
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) return true;
                else if (itemId == R.id.navigation_chat) intent = new Intent(Homepage.this, ChatBuyerActivity.class);
                else if (itemId == R.id.navigation_profile) intent = new Intent(Homepage.this, BuyerProfile.class);

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

    private void fetchProperties() {
        if (propertyAdapter == null) return;

        // Query to get properties, ordered by timestamp (newest first)
        // Ensure you have an index in Firestore for 'timestamp' descending if you get an error log.
        propertiesListener = db.collection("properties")
                .orderBy("timestamp", Query.Direction.DESCENDING) // Order by timestamp
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        if (emptyViewProperties != null) emptyViewProperties.setVisibility(View.VISIBLE);
                        if (recyclerViewProperties != null) recyclerViewProperties.setVisibility(View.GONE);
                        Toast.makeText(Homepage.this, "Failed to load properties: " + e.getMessage(), Toast.LENGTH_LONG).show();
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

                    if (propertyList.isEmpty()) {
                        if (emptyViewProperties != null) emptyViewProperties.setVisibility(View.VISIBLE);
                        if (recyclerViewProperties != null) recyclerViewProperties.setVisibility(View.GONE);
                        Toast.makeText(Homepage.this, getString(R.string.no_properties_found), Toast.LENGTH_SHORT).show();
                    } else {
                        if (emptyViewProperties != null) emptyViewProperties.setVisibility(View.GONE);
                        if (recyclerViewProperties != null) recyclerViewProperties.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (propertiesListener != null) {
            propertiesListener.remove(); // Remove Firestore listener to prevent memory leaks
        }
    }

    private static final String TAG = "HomepageBuyer"; // Added TAG for logging
}
