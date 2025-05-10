package com.example.affordablehousefinderrevamp.Buyer;

import com.example.affordablehousefinderrevamp.R;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

// Assuming your package name is com.example.yourapp
// import com.example.yourapp.R; // Import your R file

public class Bookmarks extends AppCompatActivity {

    private RecyclerView bookmarksRecyclerView;
    private BookmarksAdapter bookmarksAdapter;
    private List<BookmarkItem> bookmarkList; // Your data model list
    private ImageButton closeButton;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. Set the content view using the layout from the Canvas
        setContentView(R.layout.activity_bookmarks); // Make sure this matches your XML file name

        // 2. Get references to the views
        bookmarksRecyclerView = findViewById(R.id.bookmarksRecyclerView);
        closeButton = findViewById(R.id.closeButton);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // 3. Create and set a LayoutManager
        bookmarksRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 4. Prepare your data (replace with actual data loading)
        loadBookmarkData(); // Method to populate bookmarkList

        // 5. Create and set the custom RecyclerView.Adapter
        bookmarksAdapter = new BookmarksAdapter(bookmarkList);
        bookmarksRecyclerView.setAdapter(bookmarksAdapter);

        // 6. Set up listeners
        setupListeners();

        // Optional: Set the "Home" item as selected initially if this is the main screen
        // bottomNavigationView.setSelectedItemId(R.id.navigation_home);
    }

    // Placeholder method to load sample data
    private void loadBookmarkData() {
        bookmarkList = new ArrayList<>();
        // Add sample items - Replace with your actual data source (database, API, etc.)
        bookmarkList.add(new BookmarkItem(R.drawable.placeholder, "NEW HOUSE", "P 5,000,000", "Bakakeng North"));
        bookmarkList.add(new BookmarkItem(R.drawable.placeholder, "BRAND NEW HOUSE", "P 5,000,000", "Bakakeng Sur"));
        bookmarkList.add(new BookmarkItem(R.drawable.placeholder, "COZY BUNGALOW", "P 3,500,000", "Camp 7"));
        // Add more items as needed
    }

    private void setupListeners() {
        // Close Button Listener
        closeButton.setOnClickListener(v -> {
            // Handle close action (e.g., finish the activity)
            Toast.makeText(Bookmarks.this, "Close clicked", Toast.LENGTH_SHORT).show();
            finish(); // Example: Close the activity
        });

        // Bottom Navigation Listener
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    // Handle Home navigation
                    Toast.makeText(Bookmarks.this, "Home selected", Toast.LENGTH_SHORT).show();
                    // Example: startActivity(new Intent(BookmarksActivity.this, HomeActivity.class));
                    return true;
                } else if (itemId == R.id.navigation_chat) {
                    // Handle Chat navigation
                    Toast.makeText(Bookmarks.this, "Chat selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.navigation_sell) {
                    // Handle Buy navigation
                    Toast.makeText(Bookmarks.this, "Buy selected", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    // Handle Profile navigation
                    Toast.makeText(Bookmarks.this, "Profile selected", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false; // Item not handled
            }
        });
    }

    // --- RecyclerView Adapter and ViewHolder ---

    // Data Model Class (replace with your actual data structure)
    public static class BookmarkItem {
        int imageResId;
        String name;
        String price;
        String location;

        public BookmarkItem(int imageResId, String name, String price, String location) {
            this.imageResId = imageResId;
            this.name = name;
            this.price = price;
            this.location = location;
        }

        // Getters...
        public int getImageResId() { return imageResId; }
        public String getName() { return name; }
        public String getPrice() { return price; }
        public String getLocation() { return location; }
    }


    // Adapter for the RecyclerView
    public static class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarkViewHolder> {

        private List<BookmarkItem> items;

        public BookmarksAdapter(List<BookmarkItem> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Inflate the list_item_bookmark.xml layout
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_bookmarks, parent, false); // Ensure this layout exists
            return new BookmarkViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
            // Get the data item for this position
            BookmarkItem currentItem = items.get(position);

            // Bind the data to the views in the ViewHolder
            holder.houseImageView.setImageResource(currentItem.getImageResId()); // Use a library like Glide or Picasso for real apps
            holder.houseNameTextView.setText(currentItem.getName());
            holder.housePriceTextView.setText(currentItem.getPrice());
            holder.houseLocationTextView.setText(currentItem.getLocation());

            // --- Set OnClickListener for the item itself ---
            holder.itemView.setOnClickListener(v -> {
                // Handle item click - e.g., open details screen
                Toast.makeText(v.getContext(), "Clicked on: " + currentItem.getName(), Toast.LENGTH_SHORT).show();
                // Intent intent = new Intent(v.getContext(), HouseDetailsActivity.class);
                // intent.putExtra("HOUSE_ID", currentItem.getId()); // Assuming items have IDs
                // v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        // ViewHolder class to hold references to the views in list_item_bookmark.xml
        public static class BookmarkViewHolder extends RecyclerView.ViewHolder {
            ImageView houseImageView;
            TextView houseNameTextView;
            TextView housePriceTextView;
            TextView houseLocationTextView;

            public BookmarkViewHolder(@NonNull View itemView) {
                super(itemView);
                houseImageView = itemView.findViewById(R.id.houseImageViewItem);
                houseNameTextView = itemView.findViewById(R.id.houseNameTextViewItem);
                housePriceTextView = itemView.findViewById(R.id.housePriceTextViewItem);
                houseLocationTextView = itemView.findViewById(R.id.houseLocationTextViewItem);
            }
        }
    }
}


/**
 How to Use:

 Save the main layout as activity_bookmarks.xml in res/layout.
 Save the list item layout as list_item_bookmark.xml in res/layout.
 Add the required drawable icons and placeholder image to res/drawable.
 Add the color definitions to res/values/colors.xml.
 Create the color selector file bottom_nav_item_selector.xml in res/color.
 Create the menu file bottom_navigation_menu.xml in res/menu.
 Add the string definitions to res/values/strings.xml.
 In your BookmarksActivity.java or .kt file:
 Set the content view: setContentView(R.layout.activity_bookmarks);
 Get a reference to the RecyclerView: RecyclerView recyclerView = findViewById(R.id.bookmarksRecyclerView);
 Create and set a LayoutManager: recyclerView.setLayoutManager(new LinearLayoutManager(this));
 Create a custom RecyclerView.Adapter that inflates list_item_bookmark.xml for each item and binds the data (house image, name, price, location) to the views within the item layout.
 Set the adapter on the RecyclerView: recyclerView.setAdapter(yourAdapter);
 Set up listeners for the close button and the BottomNavigationView.
 This structure provides a scrollable list of bookmarks using RecyclerView within a ConstraintLayout, matching the UI provided.


 ソースと関連コンテンツ

 * **/