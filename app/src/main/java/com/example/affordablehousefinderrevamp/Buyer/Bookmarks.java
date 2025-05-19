package com.example.affordablehousefinderrevamp.Buyer;

import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Import Log
import android.view.LayoutInflater;
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
import com.google.firebase.firestore.FirebaseFirestore; // Firestore import
import com.google.firebase.firestore.QueryDocumentSnapshot; // Firestore import
import com.google.firebase.firestore.ListenerRegistration; // Firestore import
import com.google.firebase.firestore.Query; // Firestore import

import java.util.ArrayList;
import java.util.List;

public class Bookmarks extends AppCompatActivity {

    private static final String TAG = "BookmarksActivity"; // TAG for logging

    private RecyclerView bookmarksRecyclerView;
    private BookmarksAdapter bookmarksAdapter;
    private List<Property> bookmarkList;
    private TextView titleTextView;
    private ImageButton closeButton;
    private BottomNavigationView bottomNavigationView;

    private FirebaseFirestore db; // Firestore instance
    private FirebaseUser currentUser;
    private ListenerRegistration bookmarksListener; // To remove listener later
    private android.app.ProgressDialog progressDialogInstance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        // UI elements from activity_bookmarks.xml
        titleTextView = findViewById(R.id.titleTextView); // Title TextView
        closeButton = findViewById(R.id.closeButton);     // Close ImageButton
        bookmarksRecyclerView = findViewById(R.id.bookmarksRecyclerView);
        bottomNavigationView = findViewById(R.id.bottom_navigation);


        // Setup for header elements
        titleTextView.setText("BOOKMARKS");
        closeButton.setOnClickListener(v -> onBackPressed());

        bookmarksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookmarkList = new ArrayList<>();
        // Pass the click listener for item clicks and unbookmark clicks
        bookmarksAdapter = new BookmarksAdapter(this, bookmarkList, new BookmarksAdapter.OnBookmarkActionsListener() {
            @Override
            public void onItemClick(Property property) {
                Intent detailIntent = new Intent(Bookmarks.this, PropertyDetail.class);
                detailIntent.putExtra("propertyId", property.getPropertyId());
                startActivity(detailIntent);
            }

            @Override
            public void onUnbookmarkClick(Property property, int position) {
                // Optional: Show confirmation dialog
                // For simplicity, directly unbookmark
                unbookmarkProperty(property.getPropertyId(), position);
            }
        });
        bookmarksRecyclerView.setAdapter(bookmarksAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore

        if (currentUser != null) {
            loadBookmarkDataFromFirestore(); // Changed method name
        } else {
            Toast.makeText(this, "Please log in to see your bookmarks.", Toast.LENGTH_LONG).show();
        }

        setupBottomNavigation();
    }

    private void loadBookmarkDataFromFirestore() {
        if (currentUser == null) {
            Log.w(TAG, "Current user is null, cannot load bookmarks.");
            return;
        }
        progressDialogShow("Loading bookmarks...");

        // Path based on your Firestore rules: users/{userId}/bookmarked_properties
        // Order by 'bookmarkedAt' if you have such a field and want to sort by it.
        // For simplicity, not adding ordering here, but it's a good practice.
        bookmarksListener = db.collection("users").document(currentUser.getUid())
                .collection("bookmarked_properties")
                // Example: .orderBy("bookmarkedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    progressDialogHide();
                    if (e != null) {
                        Log.w(TAG, "Listen failed for bookmarks.", e);
                        Toast.makeText(Bookmarks.this, "Failed to load bookmarks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    bookmarkList.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            // The document in bookmarked_properties might just contain a reference or key fields.
                            // Or it might store a denormalized copy of the property.
                            // Assuming it stores enough data to display in the list (like Property model)
                            // or at least the propertyId to fetch full details if needed.
                            // For this example, assuming the documents in "bookmarked_properties"
                            // can be directly converted to your Property model.
                            // If not, you'll need to adjust this part.
                            Property bookmarkedProperty = doc.toObject(Property.class);
                            // The document ID in bookmarked_properties IS the propertyId.
                            bookmarkedProperty.setPropertyId(doc.getId());
                            bookmarkList.add(bookmarkedProperty);
                        }
                        bookmarksAdapter.notifyDataSetChanged();
                        if (bookmarkList.isEmpty()) {
                            Toast.makeText(Bookmarks.this, "No bookmarks yet.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d(TAG, "Current data: null for bookmarks");
                        Toast.makeText(Bookmarks.this, "No bookmarks found or error.", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void unbookmarkProperty(String propertyIdToUnbookmark, int position) {
        if (currentUser == null || propertyIdToUnbookmark == null || propertyIdToUnbookmark.isEmpty()) {
            Toast.makeText(this, "Error: Cannot unbookmark.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressDialogShow("Removing bookmark...");
        db.collection("users").document(currentUser.getUid())
                .collection("bookmarked_properties").document(propertyIdToUnbookmark)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    progressDialogHide();
                    Toast.makeText(Bookmarks.this, "Bookmark removed.", Toast.LENGTH_SHORT).show();
                    // The listener in loadBookmarkDataFromFirestore should automatically update the list.
                    // If you remove the listener or want explicit removal:
                    // if (position != RecyclerView.NO_POSITION && position < bookmarkList.size()) {
                    //     bookmarkList.remove(position);
                    //     bookmarksAdapter.notifyItemRemoved(position);
                    //     bookmarksAdapter.notifyItemRangeChanged(position, bookmarkList.size());
                    //     if (bookmarkList.isEmpty()) {
                    //         Toast.makeText(Bookmarks.this, "No bookmarks yet.", Toast.LENGTH_SHORT).show();
                    //     }
                    // }
                })
                .addOnFailureListener(e -> {
                    progressDialogHide();
                    Toast.makeText(Bookmarks.this, "Failed to remove bookmark: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Failed to remove bookmark", e);
                });
    }


    private void progressDialogShow(String message) {
        if (progressDialogInstance == null) {
            progressDialogInstance = new android.app.ProgressDialog(this);
            progressDialogInstance.setCancelable(false);
        }
        progressDialogInstance.setMessage(message);
        if (!progressDialogInstance.isShowing()) {
            progressDialogInstance.show();
        }
    }

    private void progressDialogHide() {
        if (progressDialogInstance != null && progressDialogInstance.isShowing()) {
            progressDialogInstance.dismiss();
        }
    }

    private void setupBottomNavigation() {
        if (bottomNavigationView == null) return;
        // bottomNavigationView.setSelectedItemId(R.id.navigation_home); // Example, or another relevant ID if Bookmarks is a main tab

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent = null;
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                intent = new Intent(Bookmarks.this, Homepage.class);
            } else if (itemId == R.id.navigation_chat) {
                intent = new Intent(Bookmarks.this, ChatBuyerActivity.class); // Corrected activity
            } else if (itemId == R.id.navigation_profile) {
                intent = new Intent(Bookmarks.this, BuyerProfile.class);
            }
            // If Bookmarks has its own icon in the bottom nav, handle it to prevent re-launching
            // else if (itemId == R.id.navigation_bookmarks) { // Assuming an ID for bookmarks
            //    return true; // Already on this screen
            // }


            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0,0);
                finish(); // Finish current activity to prevent stacking, adjust if needed
                return true;
            }
            return false;
        });
    }

    // Adapter needs to be static or in its own file.
    // If it's an inner class, it should be static to avoid memory leaks.
    public static class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarkViewHolder> {
        private List<Property> items;
        private Context context;
        private OnBookmarkActionsListener listener; // Listener for actions

        // Interface for click actions
        public interface OnBookmarkActionsListener {
            void onItemClick(Property property);
            void onUnbookmarkClick(Property property, int position);
        }

        public BookmarksAdapter(Context context, List<Property> items, OnBookmarkActionsListener listener) {
            this.context = context;
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public BookmarkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context)
                    .inflate(R.layout.list_item_bookmarks, parent, false); // Ensure list_item_bookmarks.xml is correct
            return new BookmarkViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
            Property currentItem = items.get(position);
            holder.houseNameTextView.setText(currentItem.getTitle());
            holder.housePriceTextView.setText(currentItem.getPrice());
            holder.houseLocationTextView.setText(currentItem.getLocation());

            String imageUrlToLoad = null;
            if (currentItem.getImageUrls() != null && !currentItem.getImageUrls().isEmpty()) {
                imageUrlToLoad = currentItem.getImageUrls().get(0); // Use first image from list
            } else if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
                imageUrlToLoad = currentItem.getImageUrl(); // Fallback to single imageUrl
            }


            if (imageUrlToLoad != null) {
                Glide.with(context)
                        .load(imageUrlToLoad)
                        .placeholder(R.drawable.placeholder_image) // Ensure you have this drawable
                        .error(R.drawable.ic_launcher_background) // Or a more generic error placeholder
                        .into(holder.houseImageView);
            } else {
                holder.houseImageView.setImageResource(R.drawable.placeholder_image);
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(currentItem);
                }
            });

            // Example: Add an unbookmark button inside list_item_bookmarks.xml
            // If you have an ImageButton with id 'unbookmark_button_item' in your list_item_bookmarks.xml
            if (holder.unbookmarkButton != null) {
                holder.unbookmarkButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUnbookmarkClick(currentItem, holder.getAdapterPosition());
                    }
                });
            }
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
            ImageButton unbookmarkButton; // Example: for unbookmarking directly from the list

            public BookmarkViewHolder(@NonNull View itemView) {
                super(itemView);
                // Ensure these IDs match your list_item_bookmarks.xml
                houseImageView = itemView.findViewById(R.id.houseImageViewItem);
                houseNameTextView = itemView.findViewById(R.id.houseNameTextViewItem);
                housePriceTextView = itemView.findViewById(R.id.housePriceTextViewItem);
                houseLocationTextView = itemView.findViewById(R.id.houseLocationTextViewItem);
                // unbookmarkButton = itemView.findViewById(R.id.unbookmark_button_item); // Example ID
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bookmarksListener != null) {
            bookmarksListener.remove(); // Remove Firestore listener to prevent memory leaks
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookmarksListener != null) {
            bookmarksListener.remove();
        }
        progressDialogHide(); // Dismiss dialog if activity is destroyed
    }
}
