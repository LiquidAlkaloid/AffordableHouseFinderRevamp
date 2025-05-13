package com.example.affordablehousefinderrevamp.Buyer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.affordablehousefinderrevamp.Adapter.ImageSliderAdapter;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class PropertyDetail extends AppCompatActivity {

    private static final String TAG = "PropertyDetailBuyer";

    private TextView propertyTitleTextView, propertyPriceTextView, locationTextViewPropertyDetail,
            propertyStatusTextView, descriptionTextViewPropertyDetail,
            bedroomsTextViewPropertyDetail, bathroomsTextViewPropertyDetail, areaTextViewPropertyDetail;

    private Button buttonChatWithSeller, buttonBuyNow;
    private ImageButton closeButton, shareButtonPropertyDetail, bookmarkButton, feedbackButton;

    private ViewPager2 imageViewPager;
    private TabLayout tabLayoutIndicator;

    private FirebaseFirestore db;
    private DocumentReference propertyDocRef;
    private ListenerRegistration propertyListenerRegistration;
    private DocumentReference userBookmarkDocRef;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    private String propertyId;
    private Property currentProperty;
    private boolean isBookmarked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail);

        imageViewPager = findViewById(R.id.imageViewPagerPropertyDetail);
        tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);
        closeButton = findViewById(R.id.closeButton);
        shareButtonPropertyDetail = findViewById(R.id.shareButtonPropertyDetail);
        propertyTitleTextView = findViewById(R.id.propertyTitleTextView);
        propertyPriceTextView = findViewById(R.id.propertyPriceTextView);
        bookmarkButton = findViewById(R.id.bookmarkButton);
        feedbackButton = findViewById(R.id.feedbackButton);
        locationTextViewPropertyDetail = findViewById(R.id.locationTextViewPropertyDetail);
        propertyStatusTextView = findViewById(R.id.propertyStatusTextView);
        descriptionTextViewPropertyDetail = findViewById(R.id.descriptionTextViewPropertyDetail);
        bedroomsTextViewPropertyDetail = findViewById(R.id.bedroomsTextViewPropertyDetail);
        bathroomsTextViewPropertyDetail = findViewById(R.id.bathroomsTextViewPropertyDetail);
        areaTextViewPropertyDetail = findViewById(R.id.areaTextViewPropertyDetail);
        buttonChatWithSeller = findViewById(R.id.chatButton); // Corrected ID from XML
        buttonBuyNow = findViewById(R.id.buyButton);       // Corrected ID from XML

        closeButton.setOnClickListener(v -> onBackPressed());
        shareButtonPropertyDetail.setOnClickListener(v -> shareProperty());
        bookmarkButton.setOnClickListener(v -> toggleBookmark());
        feedbackButton.setOnClickListener(v ->
                Toast.makeText(PropertyDetail.this, "Feedback clicked (not implemented)", Toast.LENGTH_SHORT).show()
        );

        // Both buttons will now initiate a chat
        buttonChatWithSeller.setOnClickListener(v -> initiateChatWithSeller());
        buttonBuyNow.setOnClickListener(v -> initiateChatWithSeller()); // "Buy Now" also initiates chat

        propertyId = getIntent().getStringExtra("propertyId");
        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(this, "Property ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        propertyDocRef = db.collection("properties").document(propertyId);

        if (currentUser != null) {
            userBookmarkDocRef = db.collection("users").document(currentUser.getUid())
                    .collection("bookmarked_properties").document(propertyId);
            checkIfBookmarked();
        }
        loadPropertyDetails();
    }

    private void loadPropertyDetails() {
        if (propertyDocRef == null) return;
        propertyListenerRegistration = propertyDocRef.addSnapshotListener((documentSnapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                Toast.makeText(PropertyDetail.this, "Failed to load property: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                currentProperty = documentSnapshot.toObject(Property.class);
                if (currentProperty != null) {
                    currentProperty.setPropertyId(documentSnapshot.getId()); // Ensure propertyId is set
                    populateUI();
                } else {
                    Toast.makeText(PropertyDetail.this, "Error converting property data.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(PropertyDetail.this, "Property details not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI() {
        if (currentProperty == null) return;

        propertyTitleTextView.setText(currentProperty.getTitle().toUpperCase());
        propertyPriceTextView.setText(currentProperty.getPrice());
        locationTextViewPropertyDetail.setText(currentProperty.getLocation());
        descriptionTextViewPropertyDetail.setText(currentProperty.getDescription());
        bedroomsTextViewPropertyDetail.setText("Bedrooms: " + currentProperty.getBedrooms());
        bathroomsTextViewPropertyDetail.setText("Bathrooms: " + currentProperty.getBathrooms());
        areaTextViewPropertyDetail.setText("Area: " + currentProperty.getArea());

        if (currentProperty.getStatus() != null && !currentProperty.getStatus().isEmpty()) {
            String statusText = "STATUS: " + currentProperty.getStatus().toUpperCase();
            propertyStatusTextView.setText(statusText);
            switch (currentProperty.getStatus().toLowerCase()) {
                case "available":
                    propertyStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_available_color));
                    break;
                case "taken":
                case "sold": // Assuming "sold" is similar to "taken"
                    propertyStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_taken_color));
                    break;
                case "reserved":
                    propertyStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_reserved_color));
                    break;
                default:
                    propertyStatusTextView.setTextColor(Color.DKGRAY);
                    break;
            }
        } else {
            propertyStatusTextView.setText("STATUS: UNKNOWN");
            propertyStatusTextView.setTextColor(Color.DKGRAY);
        }

        List<String> images = currentProperty.getImageUrls();
        if (images == null && currentProperty.getImageUrl() != null) {
            images = Collections.singletonList(currentProperty.getImageUrl());
        }

        if (imageViewPager != null && tabLayoutIndicator != null) {
            if (images != null && !images.isEmpty()) {
                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, images);
                imageViewPager.setAdapter(sliderAdapter);
                if (images.size() > 1) {
                    new TabLayoutMediator(tabLayoutIndicator, imageViewPager, (tab, position) -> {}).attach();
                    tabLayoutIndicator.setVisibility(View.VISIBLE);
                } else {
                    tabLayoutIndicator.setVisibility(View.GONE);
                }
            } else {
                imageViewPager.setVisibility(View.GONE);
                tabLayoutIndicator.setVisibility(View.GONE);
            }
        }
        buttonChatWithSeller.setEnabled(currentProperty.getSellerId() != null && !currentProperty.getSellerId().isEmpty());
        buttonBuyNow.setEnabled(currentProperty.getSellerId() != null && !currentProperty.getSellerId().isEmpty());
        updateBookmarkButtonVisual();
    }

    private void checkIfBookmarked() {
        if (userBookmarkDocRef == null) return;
        userBookmarkDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                isBookmarked = document.exists();
            } else {
                Log.e(TAG, "Error checking bookmark status", task.getException());
                isBookmarked = false;
            }
            updateBookmarkButtonVisual();
        });
    }

    private void updateBookmarkButtonVisual() {
        if (bookmarkButton == null) return;
        if (isBookmarked) {
            bookmarkButton.setImageResource(R.drawable.baseline_bookmark_50);
        } else {
            bookmarkButton.setImageResource(R.drawable.ic_bookmark_24);
        }
    }

    private void toggleBookmark() {
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.login_to_bookmark), Toast.LENGTH_SHORT).show();
            return;
        }
        if (propertyId == null || currentProperty == null || userBookmarkDocRef == null) {
            Toast.makeText(this, getString(R.string.cannot_bookmark_now), Toast.LENGTH_SHORT).show();
            return;
        }

        if (isBookmarked) {
            userBookmarkDocRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        isBookmarked = false;
                        updateBookmarkButtonVisual();
                        Toast.makeText(PropertyDetail.this, getString(R.string.removed_from_bookmarks), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(PropertyDetail.this, getString(R.string.failed_to_remove_bookmark), Toast.LENGTH_SHORT).show());
        } else {
            Map<String, Object> bookmarkData = new HashMap<>();
            bookmarkData.put("propertyId", currentProperty.getPropertyId());
            bookmarkData.put("title", currentProperty.getTitle());
            bookmarkData.put("imageUrl", currentProperty.getImageUrl() != null ? currentProperty.getImageUrl() : (currentProperty.getImageUrls() != null && !currentProperty.getImageUrls().isEmpty() ? currentProperty.getImageUrls().get(0) : null));
            bookmarkData.put("price", currentProperty.getPrice());
            bookmarkData.put("location", currentProperty.getLocation());
            bookmarkData.put("bookmarkedAt", FieldValue.serverTimestamp());

            userBookmarkDocRef.set(bookmarkData)
                    .addOnSuccessListener(aVoid -> {
                        isBookmarked = true;
                        updateBookmarkButtonVisual();
                        Toast.makeText(PropertyDetail.this, getString(R.string.added_to_bookmarks), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(PropertyDetail.this, getString(R.string.failed_to_add_bookmark), Toast.LENGTH_SHORT).show());
        }
    }

    private void shareProperty() {
        if (currentProperty != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String propertyUrl = "https://your-app-url.com/property/" + propertyId;
            String shareBody = "Check out this property: " + currentProperty.getTitle() +
                    "\nLocation: " + currentProperty.getLocation() +
                    "\nPrice: " + currentProperty.getPrice() +
                    "\n" + propertyUrl;
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Property For Sale: " + currentProperty.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } else {
            Toast.makeText(this, "Property details not loaded yet.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initiateChatWithSeller() {
        if (currentProperty == null || currentProperty.getSellerId() == null || currentProperty.getSellerId().isEmpty()) {
            Toast.makeText(this, getString(R.string.seller_info_unavailable), Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.login_to_chat), Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser.getUid().equals(currentProperty.getSellerId())) {
            Toast.makeText(this, getString(R.string.cannot_chat_with_self), Toast.LENGTH_SHORT).show();
            return;
        }

        // Navigate to Chat_Buyer.java (individual chat screen for buyer)
        Intent intent = new Intent(this, Chat_Buyer.class);
        intent.putExtra("sellerId", currentProperty.getSellerId()); // Pass Seller's User ID
        intent.putExtra("propertyId", currentProperty.getPropertyId());
        intent.putExtra("propertyName", currentProperty.getTitle());
        // Chat_Buyer will then fetch seller's name/email for its header
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (propertyListenerRegistration != null) {
            propertyListenerRegistration.remove();
        }
    }
}
