package com.example.affordablehousefinderrevamp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class House_Listing_UI extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_house_listing_ui); // Link to your XML layout

        // Example: Setting a click listener for the "Add Listing" button
        Button addListingButton = findViewById(R.id.add_listing_button);
        if (addListingButton != null) {
            addListingButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(House_Listing_UI.this, "Add Listing Clicked!", Toast.LENGTH_SHORT).show();
                    // TODO: Implement add listing functionality
                }
            });
        }

        Button alreadyListedButton = findViewById(R.id.already_listed_button);
        if (alreadyListedButton != null) {
            // Based on the image, this seems to be the "EDIT LISTING" equivalent for the first item.
            // The image showed "ALREADY LISTED" with blue background and "EDIT LISTING" with yellow.
            // I've used "ALREADY LISTED" with blue background as per the image.
            // If it should be "EDIT LISTING", change the text and style reference.
            alreadyListedButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(House_Listing_UI.this, "Already Listed / Edit Clicked!", Toast.LENGTH_SHORT).show();
                    // TODO: Implement edit/view listing functionality
                }
            });
        }

        // You can similarly find other buttons by their IDs and set listeners.
        // Example for one of the remove buttons:
        Button removeListingButton1 = findViewById(R.id.remove_listing_button_item1);
        if (removeListingButton1 != null) {
            removeListingButton1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(House_Listing_UI.this, "Remove Listing (Item 1) Clicked!", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}