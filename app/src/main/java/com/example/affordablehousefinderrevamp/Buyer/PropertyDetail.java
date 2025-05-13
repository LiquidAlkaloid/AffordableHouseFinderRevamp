package com.example.affordablehousefinderrevamp.Buyer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
// import androidx.appcompat.widget.Toolbar; // Toolbar not explicitly defined with this ID in XML
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem; // For options item selected
import android.view.View;
import android.widget.Button; // For chatButton, buyButton (AppCompatButton in XML)
import android.widget.ImageButton; // For closeButton, bookmarkButton
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.affordablehousefinderrevamp.Adapter.ImageSliderAdapter;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections; // For Collections.singletonList

public class PropertyDetail extends AppCompatActivity {

    private static final String TAG = "PropertyDetailBuyer";

    // UI Elements - IDs based on activity_property_detail.xml
    private TextView priceTextViewOverlay, houseNameTextViewOverlay, locationTextViewOverlay;
    private TextView detailsTitle, detailsForSale, detailsLotArea, detailsFloorArea, detailsDescription;
    private Button buttonChatWithSeller, buttonBuyNow; // Mapped to chatButton, buyButton in XML
    private ImageButton closeButton, bookmarkButton, feedbackButton; // feedbackButton not used in current Java logic
    private RatingBar ratingBar;
    private TextView ratingTextView;

    // ViewPager2 for image gallery (Note: XML needs update for this)
    private ViewPager2 imageViewPager;
    private TabLayout tabLayoutIndicator;
    // private Toolbar toolbar; // Not directly used as per XML structure, header is part of ConstraintLayout

    private DatabaseReference databaseReferenceProperties, databaseReferenceBookmarks;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;

    private String propertyId;
    private Property currentProperty;
    private boolean isBookmarked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_property_detail);

        // --- XML Discrepancy Note for Image Slider ---
        // The current activity_property_detail.xml has a single ImageView (R.id.houseImageView).
        // This Java code is set up for a ViewPager2 (imageViewPager) and TabLayout (tabLayoutIndicator)
        // to display multiple images with an ImageSliderAdapter.
        // For the image slider to work, activity_property_detail.xml needs to be updated:
        // 1. Replace R.id.houseImageView with a ViewPager2 element:
        //    <androidx.viewpager2.widget.ViewPager2
        //        android:id="@+id/imageViewPagerPropertyDetail"
        //        android:layout_width="0dp"
        //        android:layout_height="280dp" //        app:layout_constraintTop_toTopOf="parent"
        //        app:layout_constraintStart_toStartOf="parent"
        //        app:layout_constraintEnd_toEndOf="parent" />
        // 2. Add a TabLayout for indicators below the ViewPager2:
        //    <com.google.android.material.tabs.TabLayout
        //        android:id="@+id/tabLayoutIndicator"
        //        android:layout_width="match_parent"
        //        android:layout_height="wrap_content"
        //        app:tabBackground="@drawable/tab_selector" //        app:tabGravity="center"
        //        app:tabIndicatorHeight="0dp"
        //        app:layout_constraintTop_toBottomOf="@id/imageViewPagerPropertyDetail" />
        // The gradientOverlay would then apply to this ViewPager2.

        // Initialize UI elements from activity_property_detail.xml
        imageViewPager = findViewById(R.id.imageViewPagerPropertyDetail); // Requires this ID in XML
        tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);       // Requires this ID in XML

        priceTextViewOverlay = findViewById(R.id.priceTextView);
        houseNameTextViewOverlay = findViewById(R.id.houseNameTextView);
        locationTextViewOverlay = findViewById(R.id.locationTextView);
        closeButton = findViewById(R.id.closeButton);

        ratingBar = findViewById(R.id.ratingBar);
        ratingTextView = findViewById(R.id.ratingTextView);
        bookmarkButton = findViewById(R.id.bookmarkButton); // This is an ImageButton in XML
        feedbackButton = findViewById(R.id.feedbackButton); // Present in XML, not used in current Java logic

        detailsTitle = findViewById(R.id.detailsTitle);
        detailsForSale = findViewById(R.id.detailsForSale);
        detailsLotArea = findViewById(R.id.detailsLotArea);
        detailsFloorArea = findViewById(R.id.detailsFloorArea);
        detailsDescription = findViewById(R.id.detailsDescription);

        buttonChatWithSeller = findViewById(R.id.chatButton); // Mapped from chatButton (AppCompatButton)
        buttonBuyNow = findViewById(R.id.buyButton);         // Mapped from buyButton (AppCompatButton)

        // The XML doesn't have a dedicated Toolbar with R.id.toolbar_property_detail.
        // The "header" is composed of the image and overlayed text.
        // If a Toolbar is desired for a consistent action bar, it should be added to the XML.
        // For now, the closeButton handles back navigation.
        closeButton.setOnClickListener(v -> onBackPressed());


        propertyId = getIntent().getStringExtra("propertyId");
        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(this, "Property ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();
        databaseReferenceProperties = FirebaseDatabase.getInstance().getReference("properties").child(propertyId);
        if (currentUser != null) {
            databaseReferenceBookmarks = FirebaseDatabase.getInstance().getReference("bookmarks").child(currentUser.getUid());
            checkIfBookmarked(); // Check bookmark status after initializing DB ref
        }

        loadPropertyDetails();

        bookmarkButton.setOnClickListener(v -> toggleBookmark());
        buttonChatWithSeller.setOnClickListener(v -> chatWithSeller());
        buttonBuyNow.setOnClickListener(v -> {
            // Handle "Buy Now" action
            Toast.makeText(PropertyDetail.this, "Buy Now clicked (not implemented)", Toast.LENGTH_SHORT).show();
        });
        if (feedbackButton != null) {
            feedbackButton.setOnClickListener(v -> {
                Toast.makeText(PropertyDetail.this, "Feedback clicked (not implemented)", Toast.LENGTH_SHORT).show();
            });
        }

    }

    private void loadPropertyDetails() {
        databaseReferenceProperties.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                currentProperty = dataSnapshot.getValue(Property.class);
                if (currentProperty != null) {
                    currentProperty.setPropertyId(dataSnapshot.getKey());
                    populateUI();
                    // Assuming sellerId is part of Property model to fetch seller name
                    // fetchSellerName(currentProperty.getSellerId());
                } else {
                    Toast.makeText(PropertyDetail.this, "Property details not found.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PropertyDetail.this, "Failed to load property: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void populateUI() {
        if (currentProperty == null) return;

        // Populate overlay texts
        priceTextViewOverlay.setText(currentProperty.getPrice()); // Assumes price is formatted string
        houseNameTextViewOverlay.setText(currentProperty.getTitle());
        locationTextViewOverlay.setText(currentProperty.getLocation());

        // Populate detail section texts
        detailsTitle.setText(currentProperty.getTitle().toUpperCase()); // As per XML example
        detailsForSale.setText("FOR SALE: " + currentProperty.getTitle().toUpperCase());
        detailsLotArea.setText("Lot Area: " + currentProperty.getArea()); // Assuming area is lot area
        detailsFloorArea.setText("Floor Area: " + currentProperty.getArea() + ", more or less"); // Example, adjust if separate floor area field
        detailsDescription.setText(currentProperty.getDescription());

        // Rating - static for now as per XML, can be made dynamic
        ratingBar.setRating(5.0f);
        ratingTextView.setText("5.0 Rating (200)");


        // Image Slider Setup (requires ViewPager2 and TabLayout in XML)
        if (imageViewPager != null && tabLayoutIndicator != null) {
            if (currentProperty.getImageUrls() != null && !currentProperty.getImageUrls().isEmpty()) {
                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, currentProperty.getImageUrls());
                imageViewPager.setAdapter(sliderAdapter);
                new TabLayoutMediator(tabLayoutIndicator, imageViewPager, (tab, position) -> {
                }).attach();
                tabLayoutIndicator.setVisibility(currentProperty.getImageUrls().size() > 1 ? View.VISIBLE : View.GONE);
            } else if (currentProperty.getImageUrl() != null && !currentProperty.getImageUrl().isEmpty()) {
                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(this, Collections.singletonList(currentProperty.getImageUrl()));
                imageViewPager.setAdapter(sliderAdapter);
                tabLayoutIndicator.setVisibility(View.GONE);
            } else {
                imageViewPager.setVisibility(View.GONE);
                tabLayoutIndicator.setVisibility(View.GONE);
                // If using the single R.id.houseImageView from original XML, load image into it:
                // ImageView singleHouseImage = findViewById(R.id.houseImageView);
                // if(singleHouseImage != null) {
                //     Glide.with(this).load(R.drawable.placeholder_image).into(singleHouseImage);
                // }
            }
        }


        buttonChatWithSeller.setEnabled(currentProperty.getSellerId() != null && !currentProperty.getSellerId().isEmpty());
        updateBookmarkButtonVisual(); // Update bookmark icon based on isBookmarked state
    }

    // Removed fetchSellerName as textViewSellerInfo is not in the provided XML.
    // If needed, add a TextView for seller info in XML and uncomment/adapt this.

    private void checkIfBookmarked() {
        if (currentUser == null || propertyId == null || databaseReferenceBookmarks == null) return;
        databaseReferenceBookmarks.child(propertyId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                isBookmarked = dataSnapshot.exists();
                updateBookmarkButtonVisual();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error checking bookmark: " + databaseError.getMessage());
            }
        });
    }

    private void updateBookmarkButtonVisual() {
        if (isBookmarked) {
            // Assuming baseline_bookmark_50 is a filled bookmark icon
            bookmarkButton.setImageResource(R.drawable.baseline_bookmark_50);
            // bookmarkButton.setSupportImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.pink_accent_color))); // If you want to tint
        } else {
            // Assuming baseline_bookmark_border_50 is an outline icon (you'll need to add this drawable)
            // If you don't have border version, use the same and rely on a different visual cue or just toggle state
            bookmarkButton.setImageResource(R.drawable.ic_bookmark_24); // ADD THIS DRAWABLE
            // bookmarkButton.setSupportImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.your_default_icon_color)));
        }
    }


    private void toggleBookmark() {
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to bookmark.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (propertyId == null || currentProperty == null || databaseReferenceBookmarks == null) return;

        if (isBookmarked) {
            databaseReferenceBookmarks.child(propertyId).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(PropertyDetail.this, "Removed from bookmarks", Toast.LENGTH_SHORT).show();
                        // isBookmarked will be updated by listener, or set isBookmarked = false; updateBookmarkButtonVisual();
                    })
                    .addOnFailureListener(e -> Toast.makeText(PropertyDetail.this, "Failed to remove bookmark", Toast.LENGTH_SHORT).show());
        } else {
            Map<String, Object> bookmarkData = new HashMap<>();
            bookmarkData.put("title", currentProperty.getTitle());
            bookmarkData.put("imageUrl", currentProperty.getImageUrl());
            bookmarkData.put("price", currentProperty.getPrice());
            bookmarkData.put("location", currentProperty.getLocation());
            bookmarkData.put("timestamp", System.currentTimeMillis());

            databaseReferenceBookmarks.child(propertyId).setValue(bookmarkData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(PropertyDetail.this, "Added to bookmarks", Toast.LENGTH_SHORT).show();
                        // isBookmarked will be updated by listener, or set isBookmarked = true; updateBookmarkButtonVisual();
                    })
                    .addOnFailureListener(e -> Toast.makeText(PropertyDetail.this, "Failed to add bookmark", Toast.LENGTH_SHORT).show());
        }
    }

    private void chatWithSeller() {
        if (currentProperty == null || currentProperty.getSellerId() == null || currentProperty.getSellerId().isEmpty()) {
            Toast.makeText(this, "Seller info not available.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to chat.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser.getUid().equals(currentProperty.getSellerId())) {
            Toast.makeText(this, "You cannot chat with yourself.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ChatBuyerActivity.class);
        intent.putExtra("receiverId", currentProperty.getSellerId());
        intent.putExtra("propertyId", currentProperty.getPropertyId());
        intent.putExtra("propertyName", currentProperty.getTitle());
        // Fetch actual seller name if needed to pass to ChatBuyerActivity
        // intent.putExtra("receiverName", "Seller of " + currentProperty.getTitle());
        startActivity(intent);
    }

    // Removed onSupportNavigateUp as Toolbar is not explicitly managed here.
    // Back navigation is handled by closeButton.
    // If a Toolbar were added and set as support action bar, then onSupportNavigateUp or onOptionsItemSelected for android.R.id.home would be used.
}
