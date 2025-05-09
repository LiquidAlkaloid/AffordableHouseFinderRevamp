package com.example.affordablehousefinderrevamp.Seller;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.affordablehousefinderrevamp.R;

public class SellerPropertyDetails extends AppCompatActivity {

    private ImageView propertyImageView;
    private ImageButton backButton, shareButton, moreOptionsButton, descriptionEditIcon;
    private TextView propertyTitleTextView, propertyPriceTextView;
    private TextView locationTextView, conditionTextView, bookmarkTextView;
    private TextView descriptionLabelTextView, descriptionTextView;
    private Button viewInsightsButton, noChatsButton;
    private NestedScrollView nestedScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_property_details);

        // Initialize Views
        propertyImageView = findViewById(R.id.propertyImageView);
        backButton = findViewById(R.id.backButton);
        shareButton = findViewById(R.id.shareButton);
        moreOptionsButton = findViewById(R.id.moreOptionsButton);
        descriptionEditIcon = findViewById(R.id.descriptionEditIcon);

        propertyTitleTextView = findViewById(R.id.propertyTitleTextView);
        propertyPriceTextView = findViewById(R.id.propertyPriceTextView);

        locationTextView = findViewById(R.id.locationTextView);
        conditionTextView = findViewById(R.id.conditionTextView);
        bookmarkTextView = findViewById(R.id.bookmarkTextView);

        descriptionLabelTextView = findViewById(R.id.descriptionLabelTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);

        viewInsightsButton = findViewById(R.id.viewInsightsButton);
        noChatsButton = findViewById(R.id.noChatsButton);

        nestedScrollView = findViewById(R.id.nestedScrollView); // For the scrollable description

        // --- Set Content (Ideally, this would come from a data source) ---
        // propertyImageView.setImageResource(R.drawable.your_actual_image); // Load your image
        propertyTitleTextView.setText("MODERN HOUSE");
        propertyPriceTextView.setText("PHP 20,000,000");
        locationTextView.setText("Bakakeng North");
        conditionTextView.setText("Brand New");
        bookmarkTextView.setText("0 Bookmarks");
        descriptionTextView.setText(
                "This modern house is located within Northern Bakakeng, " +
                        "complete with 1 king-size room and 2 rooms, a balcony, " +
                        "and a lounge, and a toilet and shower on the second " +
                        "floor. On the first floor, there is a large kitchen, living " +
                        "room, storage area, and toilets. The home is close to " +
                        "accessible stores and to the main road where jeeps " +
                        "frequent the area."
        );


        // --- Set Click Listeners ---
        backButton.setOnClickListener(v -> {
            // Handle back button press
            finish(); // Finishes this activity and returns to the previous one
        });

        shareButton.setOnClickListener(v -> {
            // Handle share button press
            Toast.makeText(SellerPropertyDetails.this, "Share clicked", Toast.LENGTH_SHORT).show();
            // Add your sharing logic here (e.g., Intent.ACTION_SEND)
        });

        moreOptionsButton.setOnClickListener(v -> {
            // Handle more options button press
            Toast.makeText(SellerPropertyDetails.this, "More options clicked", Toast.LENGTH_SHORT).show();
            // Add your menu/options logic here (e.g., show a PopupMenu)
        });

        descriptionEditIcon.setOnClickListener(v -> {
            // Handle edit description icon press
            showEditDescriptionDialog();
        });

        viewInsightsButton.setOnClickListener(v -> {
            // Handle view insights button press
            Toast.makeText(SellerPropertyDetails.this, "View Insights clicked", Toast.LENGTH_SHORT).show();
            // Add your logic to show insights
        });

        noChatsButton.setOnClickListener(v -> {
            // Handle no chats button press
            Toast.makeText(SellerPropertyDetails.this, "No chats yet clicked", Toast.LENGTH_SHORT).show();
            // Add your logic for chat functionality
        });
    }

    private void showEditDescriptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Description");

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setText(descriptionTextView.getText().toString());
        input.setHint("Enter description");
        input.setLines(5); // Set initial number of lines
        input.setMaxLines(10); // Set max number of lines
        input.setVerticalScrollBarEnabled(true); // Enable vertical scroll for EditText

        // Set a fixed height or use layout parameters for the EditText if needed
        // For example, to make it take a certain portion of dialog height.
        // FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
        //         ViewGroup.LayoutParams.MATCH_PARENT,
        //         (int) (getResources().getDisplayMetrics().heightPixels * 0.3) // 30% of screen height
        // );
        // input.setLayoutParams(params);


        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newDescription = input.getText().toString().trim();
            if (!newDescription.isEmpty()) {
                descriptionTextView.setText(newDescription);
                Toast.makeText(SellerPropertyDetails.this, "Description updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SellerPropertyDetails.this, "Description cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // --- Helper methods for loading resources (e.g., drawables) ---
    // You would need to create these drawable resources in your res/drawable folder
    // For example, res/drawable/ic_arrow_back.xml (Vector Asset)
    // placeholder_house.png (or .jpg) in res/drawable
    // ic_share.xml
    // ic_more_vert.xml
    // ic_location_on.xml
    // ic_new_releases.xml
    // ic_bookmark_border.xml
    // ic_edit.xml
}
