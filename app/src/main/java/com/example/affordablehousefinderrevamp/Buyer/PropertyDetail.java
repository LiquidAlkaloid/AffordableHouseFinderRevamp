package com.example.affordablehousefinderrevamp.Buyer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.affordablehousefinderrevamp.Adapter.ImageSliderAdapter;
import com.example.affordablehousefinderrevamp.Model.Offer;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.User; // Import User model
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

    private Button buttonChatWithSeller, buttonBuyNow; // buttonBuyNow is now Make Offer
    private ImageButton closeButton, shareButtonPropertyDetail, bookmarkButton, feedbackButton;

    private ViewPager2 imageViewPager;
    private TabLayout tabLayoutIndicator;

    private FirebaseFirestore db;
    private DocumentReference propertyDocRef;
    private ListenerRegistration propertyListenerRegistration;
    private DocumentReference userBookmarkDocRef;

    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private User currentBuyerUserDetails; // To store buyer's name for offer

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
        buttonChatWithSeller = findViewById(R.id.chatButton);
        buttonBuyNow = findViewById(R.id.buyButton); // This button will trigger "Make Offer"

        closeButton.setOnClickListener(v -> onBackPressed());
        shareButtonPropertyDetail.setOnClickListener(v -> shareProperty());
        bookmarkButton.setOnClickListener(v -> toggleBookmark());
        feedbackButton.setOnClickListener(v ->
                Toast.makeText(PropertyDetail.this, "Feedback clicked (not implemented)", Toast.LENGTH_SHORT).show()
        );

        buttonChatWithSeller.setOnClickListener(v -> initiateChatWithSeller());
        buttonBuyNow.setOnClickListener(v -> showMakeOfferDialog()); // "Buy Now" button now for making an offer

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
            loadCurrentBuyerDetails(); // Load buyer's details for offer
        }
        loadPropertyDetails();
    }

    private void loadCurrentBuyerDetails() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentBuyerUserDetails = documentSnapshot.toObject(User.class);
                        if (currentBuyerUserDetails != null) {
                            currentBuyerUserDetails.setId(documentSnapshot.getId()); // Ensure ID is set
                        }
                    } else {
                        Log.w(TAG, "Buyer user document does not exist. Cannot make offers with buyer name.");
                        Toast.makeText(this, "Your profile is incomplete. Cannot make offers.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch buyer details", e);
                    Toast.makeText(this, "Failed to load your profile details.", Toast.LENGTH_SHORT).show();
                });
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
                // finish(); // Property might have been deleted, consider finishing
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
                    buttonBuyNow.setEnabled(true); // Can make offer if available
                    buttonChatWithSeller.setEnabled(true);
                    break;
                case "reserved":
                    propertyStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_reserved_color));
                    buttonBuyNow.setEnabled(false); // Cannot make offer if reserved
                    buttonBuyNow.setText("Reserved");
                    buttonChatWithSeller.setEnabled(true); // Still allow chat
                    break;
                case "taken": // Treat 'taken' and 'sold' similarly
                case "sold":
                    propertyStatusTextView.setTextColor(ContextCompat.getColor(this, R.color.status_taken_color));
                    buttonBuyNow.setEnabled(false); // Cannot make offer if sold/taken
                    buttonBuyNow.setText("Sold/Taken");
                    buttonChatWithSeller.setEnabled(false); // Disable chat if sold
                    break;
                default:
                    propertyStatusTextView.setTextColor(Color.DKGRAY);
                    buttonBuyNow.setEnabled(true); // Default to allowing offer
                    buttonChatWithSeller.setEnabled(true);
                    break;
            }
        } else {
            propertyStatusTextView.setText("STATUS: UNKNOWN");
            propertyStatusTextView.setTextColor(Color.DKGRAY);
            buttonBuyNow.setEnabled(true);
            buttonChatWithSeller.setEnabled(true);
        }

        List<String> images = currentProperty.getImageUrls();
        if (images == null || images.isEmpty()) { // Check if list is null or empty
            if (currentProperty.getImageUrl() != null && !currentProperty.getImageUrl().isEmpty()) {
                images = Collections.singletonList(currentProperty.getImageUrl());
            } else {
                images = Collections.emptyList(); // Ensure images is not null
            }
        }


        if (imageViewPager != null && tabLayoutIndicator != null) {
            if (!images.isEmpty()) {
                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, images);
                imageViewPager.setAdapter(sliderAdapter);
                imageViewPager.setVisibility(View.VISIBLE);
                if (images.size() > 1) {
                    new TabLayoutMediator(tabLayoutIndicator, imageViewPager, (tab, position) -> {}).attach();
                    tabLayoutIndicator.setVisibility(View.VISIBLE);
                } else {
                    tabLayoutIndicator.setVisibility(View.GONE);
                }
            } else {
                imageViewPager.setVisibility(View.GONE); // Hide if no images
                tabLayoutIndicator.setVisibility(View.GONE);
            }
        }
        updateBookmarkButtonVisual();
    }

    private void checkIfBookmarked() {
        if (userBookmarkDocRef == null) return;
        userBookmarkDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                isBookmarked = document != null && document.exists();
            } else {
                Log.e(TAG, "Error checking bookmark status", task.getException());
                isBookmarked = false; // Assume not bookmarked on error
            }
            updateBookmarkButtonVisual();
        });
    }

    private void updateBookmarkButtonVisual() {
        if (bookmarkButton == null) return;
        if (isBookmarked) {
            bookmarkButton.setImageResource(R.drawable.baseline_bookmark_50); // Filled icon
        } else {
            bookmarkButton.setImageResource(R.drawable.ic_bookmark_24); // Border icon
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

        if (isBookmarked) { // If currently bookmarked, then unbookmark
            userBookmarkDocRef.delete()
                    .addOnSuccessListener(aVoid -> {
                        isBookmarked = false;
                        updateBookmarkButtonVisual();
                        Toast.makeText(PropertyDetail.this, getString(R.string.removed_from_bookmarks), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(PropertyDetail.this, getString(R.string.failed_to_remove_bookmark) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else { // If not bookmarked, then bookmark
            Map<String, Object> bookmarkData = new HashMap<>();
            bookmarkData.put("propertyId", currentProperty.getPropertyId());
            bookmarkData.put("title", currentProperty.getTitle());
            String imageUrlForBookmark = null;
            if (currentProperty.getImageUrls() != null && !currentProperty.getImageUrls().isEmpty()) {
                imageUrlForBookmark = currentProperty.getImageUrls().get(0);
            } else if (currentProperty.getImageUrl() != null && !currentProperty.getImageUrl().isEmpty()) {
                imageUrlForBookmark = currentProperty.getImageUrl();
            }
            if (imageUrlForBookmark != null) {
                bookmarkData.put("imageUrl", imageUrlForBookmark);
            }
            bookmarkData.put("price", currentProperty.getPrice());
            bookmarkData.put("location", currentProperty.getLocation());
            bookmarkData.put("bookmarkedAt", FieldValue.serverTimestamp());

            userBookmarkDocRef.set(bookmarkData)
                    .addOnSuccessListener(aVoid -> {
                        isBookmarked = true;
                        updateBookmarkButtonVisual();
                        Toast.makeText(PropertyDetail.this, getString(R.string.added_to_bookmarks), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(PropertyDetail.this, getString(R.string.failed_to_add_bookmark) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void shareProperty() {
        if (currentProperty != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            // Ideally, replace with a dynamic link or a web URL for your app
            String propertyUrl = "https://your-app-url.com/property/" + propertyId;
            String shareBody = "Check out this property: " + currentProperty.getTitle() +
                    "\nLocation: " + currentProperty.getLocation() +
                    "\nPrice: " + currentProperty.getPrice() +
                    "\n\nView more details here: " + propertyUrl; // Example link
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Property For Sale: " + currentProperty.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } else {
            Toast.makeText(this, "Property details not loaded yet.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMakeOfferDialog() {
        if (currentProperty == null || currentUser == null || currentBuyerUserDetails == null) {
            Toast.makeText(this, "Cannot make offer. User or property details missing.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "showMakeOfferDialog: Missing currentProperty, currentUser, or currentBuyerUserDetails");
            return;
        }
        if (currentUser.getUid().equals(currentProperty.getSellerId())) {
            Toast.makeText(this, "You cannot make an offer on your own property.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!"Available".equalsIgnoreCase(currentProperty.getStatus())) {
            Toast.makeText(this, "This property is currently " + currentProperty.getStatus() + " and not accepting offers.", Toast.LENGTH_LONG).show();
            return;
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_make_offer, null);
        final EditText editTextOfferAmount = dialogView.findViewById(R.id.editTextOfferAmount);

        builder.setView(dialogView)
                .setPositiveButton("Submit Offer", (dialog, id) -> {
                    String offerAmount = editTextOfferAmount.getText().toString().trim();
                    if (TextUtils.isEmpty(offerAmount)) {
                        Toast.makeText(PropertyDetail.this, "Please enter an offer amount.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Basic validation for offer amount (e.g., must be a number, or use a specific format)
                    // For simplicity, just checking if not empty here.
                    sendOfferToFirestore(offerAmount);
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void sendOfferToFirestore(String offerAmount) {
        if (currentProperty == null || currentUser == null || currentBuyerUserDetails == null ||
                TextUtils.isEmpty(currentProperty.getPropertyId()) || TextUtils.isEmpty(currentProperty.getSellerId()) ||
                TextUtils.isEmpty(currentBuyerUserDetails.getName())) {
            Toast.makeText(this, "Error: Missing information to send offer.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "sendOfferToFirestore - Missing info. PropID: " + (currentProperty != null ? currentProperty.getPropertyId() : "null") +
                    ", SellerID: " + (currentProperty != null ? currentProperty.getSellerId() : "null") +
                    ", BuyerName: " + (currentBuyerUserDetails != null ? currentBuyerUserDetails.getName() : "null"));
            return;
        }

        String propertyImageUrl = null;
        if (currentProperty.getImageUrls() != null && !currentProperty.getImageUrls().isEmpty()) {
            propertyImageUrl = currentProperty.getImageUrls().get(0);
        } else if (currentProperty.getImageUrl() != null && !currentProperty.getImageUrl().isEmpty()) {
            propertyImageUrl = currentProperty.getImageUrl();
        }

        Offer offer = new Offer(
                currentProperty.getPropertyId(),
                currentProperty.getTitle(),
                propertyImageUrl,
                currentUser.getUid(),
                currentBuyerUserDetails.getName(), // Buyer's name
                currentProperty.getSellerId(),
                offerAmount
        );

        DocumentReference newOfferRef = db.collection("offers").document();
        offer.setOfferId(newOfferRef.getId()); // Set the auto-generated ID to the offer object

        newOfferRef.set(offer)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(PropertyDetail.this, "Offer submitted successfully!", Toast.LENGTH_SHORT).show();
                    // Here you could navigate away, disable the offer button for this session,
                    // or send a system message in chat if a chat already exists or is created.
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(PropertyDetail.this, "Failed to submit offer: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error submitting offer", e);
                });
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

        Intent intent = new Intent(this, Chat_Buyer.class);
        intent.putExtra("sellerId", currentProperty.getSellerId());
        intent.putExtra("propertyId", currentProperty.getPropertyId());
        intent.putExtra("propertyName", currentProperty.getTitle());
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (propertyListenerRegistration != null) {
            propertyListenerRegistration.remove();
            propertyListenerRegistration = null; // Good practice
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Re-attach listener if it was removed and propertyDocRef is valid
        if (propertyDocRef != null && propertyListenerRegistration == null) {
            loadPropertyDetails();
        }
        if (currentUser != null && userBookmarkDocRef != null) { // Re-check bookmark status
            checkIfBookmarked();
        }
    }
}