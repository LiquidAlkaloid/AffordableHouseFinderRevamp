package com.example.affordablehousefinderrevamp.Buyer;

import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.google.firebase.firestore.FirebaseFirestore; // Firestore
import com.google.firebase.firestore.QueryDocumentSnapshot; // Firestore

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
// Remove Realtime Database imports if fully migrated
// import com.google.firebase.database.DataSnapshot;
// import com.google.firebase.database.DatabaseError;
// import com.google.firebase.database.DatabaseReference;
// import com.google.firebase.database.FirebaseDatabase;
// import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Bookmarks extends AppCompatActivity {

    private static final String TAG = "BookmarksActivity";

    private RecyclerView bookmarksRecyclerView;
    private BookmarksAdapter bookmarksAdapter;
    private List<Property> bookmarkList;
    private TextView titleTextView;
    private ImageButton closeButton;
    private BottomNavigationView bottomNavigationView;

    // Firestore references
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private android.app.ProgressDialog progressDialogInstance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        titleTextView = findViewById(R.id.titleTextView);
        closeButton = findViewById(R.id.closeButton);
        bookmarksRecyclerView = findViewById(R.id.bookmarksRecyclerView);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        titleTextView.setText("BOOKMARKS");
        closeButton.setOnClickListener(v -> onBackPressed());

        bookmarksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookmarkList = new ArrayList<>();
        bookmarksAdapter = new BookmarksAdapter(this, bookmarkList);
        bookmarksRecyclerView.setAdapter(bookmarksAdapter);

        db = FirebaseFirestore.getInstance(); // Initialize Firestore
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            loadBookmarkDataFromFirestore();
        } else {
            Toast.makeText(this, "Please log in to see your bookmarks.", Toast.LENGTH_LONG).show();
        }

        setupBottomNavigation();
    }

    private void loadBookmarkDataFromFirestore() {
        if (currentUser == null) return;
        progressDialogShow("Loading bookmarks...");

        db.collection("users").document(currentUser.getUid())
                .collection("bookmarked_properties") // Assuming this is your subcollection for bookmarks
                .get()
                .addOnCompleteListener(task -> {
                    progressDialogHide();
                    if (task.isSuccessful()) {
                        bookmarkList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Here, we expect the bookmarked_properties subcollection to store
                            // full Property objects or at least enough data to display.
                            // If it only stores property IDs, you'd need another fetch.
                            // For this example, let's assume it stores partial Property data.
                            Property bookmarkedProperty = document.toObject(Property.class);
                            bookmarkedProperty.setPropertyId(document.getId()); // The document ID in bookmarks is the propertyId

                            // If "bookmarked_properties" only stores IDs and you need full property details:
                            // String actualPropertyId = document.getString("propertyId"); // If the field is named propertyId
                            // Then fetch from "properties/{actualPropertyId}"
                            // This example assumes bookmarked_properties contains enough data directly.

                            bookmarkList.add(bookmarkedProperty);
                        }
                        bookmarksAdapter.notifyDataSetChanged();
                        if (bookmarkList.isEmpty()) {
                            Toast.makeText(Bookmarks.this, "No bookmarks yet.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Error loading bookmarks: ", task.getException());
                        Toast.makeText(Bookmarks.this, "Failed to load bookmarks.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void progressDialogShow(String message) {
        if (progressDialogInstance == null) {
            progressDialogInstance = new android.app.ProgressDialog(this);
            progressDialogInstance.setCancelable(false);
        }
        progressDialogInstance.setMessage(message);
        progressDialogInstance.show();
    }

    private void progressDialogHide() {
        if (progressDialogInstance != null && progressDialogInstance.isShowing()) {
            progressDialogInstance.dismiss();
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;
        // bottomNavigationView.setSelectedItemId(R.id.navigation_home); // Example

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                intent = new Intent(Bookmarks.this, Homepage.class);
            } else if (itemId == R.id.navigation_chat) {
                intent = new Intent(Bookmarks.this, Chat_Buyer.class); // Assuming Chat_Buyer is the list
            } else if (itemId == R.id.navigation_profile) {
                intent = new Intent(Bookmarks.this, BuyerProfile.class);
            }
            // No R.id.navigation_bookmarks in buyer's bottom nav as per typical design

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0,0); // No animation
                finish(); // Finish current activity
                return true;
            }
            return false;
        });
    }

    public static class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarkViewHolder> {
        private List<Property> items;
        private Context context;

        public BookmarksAdapter(Context context, List<Property> items) {
            this.context = context;
            this.items = items;
        }

        @NonNull
        @Override
        public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_bookmarks, parent, false);
            return new BookmarkViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
            Property currentItem = items.get(position);
            if (currentItem == null) return;

            holder.houseNameTextView.setText(currentItem.getName()); // Corrected to getName()
            holder.housePriceTextView.setText(currentItem.getPrice());
            holder.houseLocationTextView.setText(currentItem.getLocation());

            String imageUrl = currentItem.getPrimaryImageUrl(); // Corrected to use getPrimaryImageUrl()
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.ic_launcher_background) // Consider a more generic error placeholder
                        .into(holder.houseImageView);
            } else {
                holder.houseImageView.setImageResource(R.drawable.placeholder_image);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent detailIntent = new Intent(context, PropertyDetail.class);
                // The propertyId for the detail view should be the actual ID of the property,
                // which is assumed to be the document ID in the bookmarked_properties subcollection.
                detailIntent.putExtra("propertyId", currentItem.getPropertyId());
                context.startActivity(detailIntent);
            });
        }

        @Override
        public int getItemCount() {
            return items != null ? items.size() : 0;
        }

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
