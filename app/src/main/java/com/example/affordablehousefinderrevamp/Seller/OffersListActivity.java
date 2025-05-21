package com.example.affordablehousefinderrevamp.Seller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.affordablehousefinderrevamp.Adapter.OfferAdapter;
import com.example.affordablehousefinderrevamp.Model.Offer;
import com.example.affordablehousefinderrevamp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OffersListActivity extends AppCompatActivity implements OfferAdapter.OnOfferActionListener {

    private static final String TAG = "OffersListActivity";

    private RecyclerView recyclerViewOffers;
    private OfferAdapter offerAdapter;
    private List<Offer> offerList;
    private TextView textViewNoOffers;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private String propertyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offers_list);

        toolbar = findViewById(R.id.toolbar_offers_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Offers Received");
        }

        propertyId = getIntent().getStringExtra("propertyId");
        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(this, "Property ID missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        recyclerViewOffers = findViewById(R.id.recyclerViewOffers);
        textViewNoOffers = findViewById(R.id.textViewNoOffers);
        recyclerViewOffers.setLayoutManager(new LinearLayoutManager(this));

        offerList = new ArrayList<>();
        offerAdapter = new OfferAdapter(this, offerList, this);
        recyclerViewOffers.setAdapter(offerAdapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Authentication required.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        loadOffers();
    }

    private void loadOffers() {
        db.collection("offers")
                .whereEqualTo("sellerId", currentUser.getUid())
                .whereEqualTo("propertyId", propertyId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    offerList.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        textViewNoOffers.setVisibility(View.VISIBLE);
                        recyclerViewOffers.setVisibility(View.GONE);
                    } else {
                        textViewNoOffers.setVisibility(View.GONE);
                        recyclerViewOffers.setVisibility(View.VISIBLE);
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Offer offer = document.toObject(Offer.class);
                            offer.setOfferId(document.getId());
                            offerList.add(offer);
                        }
                        offerAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading offers", e);
                    Toast.makeText(this, "Failed to load offers: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    textViewNoOffers.setVisibility(View.VISIBLE);
                    textViewNoOffers.setText("Failed to load offers.");
                    recyclerViewOffers.setVisibility(View.GONE);
                });
    }

    @Override
    public void onAcceptOffer(Offer offer, int position) {
        updateOfferStatus(offer, "accepted", position);
    }

    @Override
    public void onDeclineOffer(Offer offer, int position) {
        updateOfferStatus(offer, "declined", position);
    }

    private void updateOfferStatus(Offer offer, String newStatus, int position) {
        if (offer.getOfferId() == null) {
            Toast.makeText(this, "Offer ID is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference offerRef = db.collection("offers").document(offer.getOfferId());
        DocumentReference propertyRef = db.collection("properties").document(offer.getPropertyId());

        WriteBatch batch = db.batch();

        Map<String, Object> offerUpdate = new HashMap<>();
        offerUpdate.put("status", newStatus);
        batch.update(offerRef, offerUpdate);

        if ("accepted".equals(newStatus)) {
            Map<String, Object> propertyUpdate = new HashMap<>();
            propertyUpdate.put("status", "Reserved"); // Or "Sold", depending on your workflow
            // propertyUpdate.put("buyerId", offer.getBuyerId()); // Optionally store the buyer who reserved/bought
            batch.update(propertyRef, propertyUpdate);
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(OffersListActivity.this, "Offer " + newStatus + ".", Toast.LENGTH_SHORT).show();
                    offer.setStatus(newStatus);
                    offerAdapter.notifyItemChanged(position);

                    // If an offer is accepted, other pending offers for this property might need to be auto-declined (optional)
                    // For now, only the property status is updated.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(OffersListActivity.this, "Failed to update offer: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating offer status", e);
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}