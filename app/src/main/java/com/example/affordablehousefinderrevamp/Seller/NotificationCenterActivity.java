package com.example.affordablehousefinderrevamp.Seller;


import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;// Import Toast for the back button example
import com.example.affordablehousefinderrevamp.R;


public class NotificationCenterActivity extends AppCompatActivity {

    private LinearLayout notificationItemsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_center); // Use the new layout file

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the back button click listener
        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Example: Go back to the previous activity (e.g., the Login activity)
                onBackPressed();
                // Or navigate to a specific activity using an Intent if needed
                // Intent intent = new Intent(NotificationCenterActivity.this, Login.class);
                // startActivity(intent);
                // finish(); // Optional: close this activity
            }
        });

        // Hide the default action bar title if using a custom TextView in Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // You can also remove the default back arrow if you are using a custom ImageView
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }

        notificationItemsContainer = findViewById(R.id.notificationItemsContainer);

        // Add notification items programmatically
        addNotificationItem("You have a new buyer");
        addNotificationItem("You have a new buyer");
        addNotificationItem("Theo Bugayong - Commented \"You're house is filthy\"");
        addNotificationItem("Theo Bugayong - Bookmarked");
        // Add more items as needed to match the image
        addNotificationItem(""); // Empty items to fill the space like in the image
        addNotificationItem("");
        addNotificationItem("");
        addNotificationItem("");
        addNotificationItem("");
        addNotificationItem("");
        addNotificationItem("");


    }

    /**
     * Helper method to inflate the notification item layout and add it to the container.
     * @param text The text to display in the notification item.
     */
    private void addNotificationItem(String text) {
        // Inflate the list item layout
        LayoutInflater inflater = LayoutInflater.from(this);
        View notificationItemView = inflater.inflate(R.layout.list_item_notification, notificationItemsContainer, false);

        // Find the TextView in the inflated layout and set its text
        TextView textView = notificationItemView.findViewById(R.id.textViewNotificationText);
        textView.setText(text);

        // Add the inflated view to the container LinearLayout
        notificationItemsContainer.addView(notificationItemView);

        // Optional: Add a click listener to each notification item
        notificationItemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle notification item click (e.g., open details)
                Toast.makeText(NotificationCenterActivity.this, "Clicked: " + text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
