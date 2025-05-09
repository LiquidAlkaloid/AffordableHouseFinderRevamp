package com.example.affordablehousefinderrevamp.Seller;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.affordablehousefinderrevamp.R;

public class HouseListings extends AppCompatActivity {

    private LinearLayout listingsContainer;
    private Button mainAddListingButton;
    private int listingCount = 0;
    private static final int MAX_LISTINGS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the content view to your main layout file
        setContentView(R.layout.activity_house_listings);

        // Initialize views
        listingsContainer = findViewById(R.id.listings_container);
        mainAddListingButton = findViewById(R.id.button_add_new_listing_main);

        // Add the initial listing based on the provided image
        addInitialListing();

        // Set click listener for the main "ADD LISTING" button
        mainAddListingButton.setOnClickListener(v -> addNewListingCard());

        // Update the state of the "ADD LISTING" button (e.g., disable if max reached)
        updateAddButtonState();
    }

    /**
     * Adds the first, pre-filled listing to the layout.
     */
    private void addInitialListing() {
        if (listingCount < MAX_LISTINGS) {
            LayoutInflater inflater = LayoutInflater.from(this);
            // Inflate the layout for a single listing item
            View listingView = inflater.inflate(R.layout.list_item_house, listingsContainer, false);

            // Get references to views within the inflated layout
            ImageView houseImage = listingView.findViewById(R.id.house_image);
            TextView houseName = listingView.findViewById(R.id.house_name);
            TextView housePrice = listingView.findViewById(R.id.house_price);
            TextView houseLocation = listingView.findViewById(R.id.house_location);
            TextView houseStatus = listingView.findViewById(R.id.house_status);

            // Set data for the initial listing
            houseImage.setImageResource(R.drawable.placeholder); // Use placeholder
            houseImage.setBackgroundColor(ContextCompat.getColor(this, R.color.placeholder_image_bg)); // Example background

            houseName.setText("Modern House");
            housePrice.setText("P 20,000,000");
            houseLocation.setText("Bakakeng North");
            houseStatus.setText("STATUS : AVAILABLE");
            houseStatus.setTextColor(ContextCompat.getColor(this, R.color.status_available_color));

            // Setup buttons for this specific listing
            Button editButton = listingView.findViewById(R.id.button_edit_listing);
            Button alreadyListedButton = listingView.findViewById(R.id.button_already_listed);
            Button removeButton = listingView.findViewById(R.id.button_remove_listing);

            // Set click listeners for buttons within the listing item
            // In a real app, these would trigger actual edit/status change/remove logic
            editButton.setOnClickListener(view -> Toast.makeText(HouseListings.this, "Edit: " + houseName.getText(), Toast.LENGTH_SHORT).show());
            alreadyListedButton.setOnClickListener(view -> Toast.makeText(HouseListings.this, "Status button clicked for: " + houseName.getText(), Toast.LENGTH_SHORT).show());
            removeButton.setOnClickListener(view -> removeListing(listingView)); // Pass the CardView to remove

            // Add the configured listing view to the container
            listingsContainer.addView(listingView);
            listingCount++;
            // updateAddButtonState(); // This will be called in onCreate after this method
        }
    }

    /**
     * Adds a new, empty/template listing card to the scroll view.
     */
    private void addNewListingCard() {
        if (listingCount < MAX_LISTINGS) {
            LayoutInflater inflater = LayoutInflater.from(this);
            View listingView = inflater.inflate(R.layout.list_item_house, listingsContainer, false);

            // Get references to views for the new listing
            ImageView houseImage = listingView.findViewById(R.id.house_image);
            TextView houseName = listingView.findViewById(R.id.house_name);
            TextView housePrice = listingView.findViewById(R.id.house_price);
            TextView houseLocation = listingView.findViewById(R.id.house_location);
            TextView houseStatus = listingView.findViewById(R.id.house_status);

            // Set placeholder/default data for the new listing
            houseImage.setImageResource(R.drawable.placeholder);
            houseImage.setBackgroundColor(ContextCompat.getColor(this, R.color.placeholder_image_bg_new));

            houseName.setText("New Listing " + (listingCount + 1)); // Differentiate new listings
            housePrice.setText("P Enter Price");
            houseLocation.setText("Enter Location");
            houseStatus.setText("STATUS : PENDING");
            houseStatus.setTextColor(ContextCompat.getColor(this, R.color.status_pending_color));

            Button editButton = listingView.findViewById(R.id.button_edit_listing);
            Button alreadyListedButton = listingView.findViewById(R.id.button_already_listed);
            Button removeButton = listingView.findViewById(R.id.button_remove_listing);

            final String currentHouseName = houseName.getText().toString(); // For use in lambda
            editButton.setOnClickListener(view -> Toast.makeText(HouseListings.this, "Edit: " + currentHouseName, Toast.LENGTH_SHORT).show());
            alreadyListedButton.setOnClickListener(view -> Toast.makeText(HouseListings.this, "Status button clicked for: " + currentHouseName, Toast.LENGTH_SHORT).show());
            removeButton.setOnClickListener(view -> removeListing(listingView));

            listingsContainer.addView(listingView);
            listingCount++;
            updateAddButtonState();
        } else {
            Toast.makeText(this, "Maximum of " + MAX_LISTINGS + " listings reached.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Removes a listing view from the container.
     * @param listingViewToRemove The CardView of the listing to remove.
     */
    private void removeListing(View listingViewToRemove) {
        listingsContainer.removeView(listingViewToRemove);
        listingCount--;
        updateAddButtonState();
        Toast.makeText(this, "Listing removed.", Toast.LENGTH_SHORT).show();
    }

    /**
     * Updates the state (enabled/disabled, text) of the main "ADD LISTING" button
     * based on the current number of listings.
     */
    private void updateAddButtonState() {
        if (listingCount >= MAX_LISTINGS) {
            mainAddListingButton.setEnabled(false);
            // Apply a specific tint for the disabled state using the selector
            mainAddListingButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.button_add_listing_tint));
            mainAddListingButton.setText("MAX LISTINGS REACHED (" + listingCount + "/" + MAX_LISTINGS + ")");
        } else {
            mainAddListingButton.setEnabled(true);
            // Apply the tint selector which handles enabled state
            mainAddListingButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.button_add_listing_tint));
            mainAddListingButton.setText("ADD LISTING (" + listingCount + "/" + MAX_LISTINGS + ")");
        }
    }
}