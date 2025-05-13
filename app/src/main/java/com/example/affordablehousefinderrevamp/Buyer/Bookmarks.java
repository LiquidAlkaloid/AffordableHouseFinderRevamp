package com.example.affordablehousefinderrevamp.Buyer;

import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;

import android.content.Context; // Added for LayoutInflater in Adapter
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater; // Added for LayoutInflater in Adapter
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton; // For closeButton
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
// Removed Toolbar import as activity_bookmarks.xml uses TextView and ImageButton for header
// import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Bookmarks extends AppCompatActivity {

    private RecyclerView bookmarksRecyclerView;
    private BookmarksAdapter bookmarksAdapter;
    private List<Property> bookmarkList;
    // private Toolbar toolbar; // XML uses TextView and ImageButton instead of a Toolbar
    private TextView titleTextView;
    private ImageButton closeButton;
    private BottomNavigationView bottomNavigationView;

    private DatabaseReference databaseReferenceBookmarks;
    private FirebaseUser currentUser;
    private android.app.ProgressDialog progressDialogInstance;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bookmarks);

        // UI elements from activity_bookmarks.xml
        titleTextView = findViewById(R.id.titleTextView); // Title TextView
        closeButton = findViewById(R.id.closeButton);     // Close ImageButton
        bookmarksRecyclerView = findViewById(R.id.bookmarksRecyclerView);
        // Corrected BottomNavigationView ID from activity_bookmarks.xml
        bottomNavigationView = findViewById(R.id.bottom_navigation);


        // Setup for header elements
        titleTextView.setText("BOOKMARKS"); // Or get from string resource
        closeButton.setOnClickListener(v -> onBackPressed()); // Or finish();

        bookmarksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        bookmarkList = new ArrayList<>();
        bookmarksAdapter = new BookmarksAdapter(this, bookmarkList);
        bookmarksRecyclerView.setAdapter(bookmarksAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            databaseReferenceBookmarks = FirebaseDatabase.getInstance().getReference("bookmarks").child(currentUser.getUid());
            loadBookmarkDataFromFirebase();
        } else {
            Toast.makeText(this, "Please log in to see your bookmarks.", Toast.LENGTH_LONG).show();
        }

        setupBottomNavigation();
    }

    private void loadBookmarkDataFromFirebase() {
        if (databaseReferenceBookmarks == null) return;
        progressDialogShow("Loading bookmarks...");

        databaseReferenceBookmarks.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bookmarkList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Property bookmarkedProperty = snapshot.getValue(Property.class);
                    if (bookmarkedProperty != null) {
                        bookmarkedProperty.setPropertyId(snapshot.getKey());
                        bookmarkList.add(bookmarkedProperty);
                    }
                }
                bookmarksAdapter.notifyDataSetChanged();
                progressDialogHide();
                if (bookmarkList.isEmpty()) {
                    Toast.makeText(Bookmarks.this, "No bookmarks yet.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                progressDialogHide();
                Toast.makeText(Bookmarks.this, "Failed to load bookmarks: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
        // Since 'bookmarks_buyer' is not in 'bottom_navigation_menu_buyer.xml',
        // we should select an existing item or none.
        // For example, if Bookmarks is a sub-screen of Profile, Profile could be selected.
        // Or, if it's independent, no item might be pre-selected, or Home by default.
        // For now, let's assume no specific item is pre-selected unless it's a main tab.
        // If 'Bookmarks' were a main tab with ID R.id.navigation_bookmarks, you'd use that.
        // bottomNavigationView.setSelectedItemId(R.id.navigation_home); // Example: Select home by default

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Intent intent = null;
            int itemId = item.getItemId();

            // Use IDs from bottom_navigation_menu_buyer.xml
            if (itemId == R.id.navigation_home) {
                intent = new Intent(Bookmarks.this, Homepage.class);
            } else if (itemId == R.id.navigation_chat) {
                intent = new Intent(Bookmarks.this, Chat_Buyer.class);
            } else if (itemId == R.id.navigation_profile) {
                intent = new Intent(Bookmarks.this, BuyerProfile.class);
            }
            // No 'bookmarks_buyer' ID in the menu XML, so no direct navigation to itself via bottom nav.

            if (intent != null) {
                startActivity(intent);
                overridePendingTransition(0,0);
                finish();
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
            View view = LayoutInflater.from(context) // Use context for LayoutInflater
                    .inflate(R.layout.list_item_bookmarks, parent, false);
            return new BookmarkViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull BookmarkViewHolder holder, int position) {
            Property currentItem = items.get(position);
            holder.houseNameTextView.setText(currentItem.getTitle());
            holder.housePriceTextView.setText(currentItem.getPrice());
            holder.houseLocationTextView.setText(currentItem.getLocation());

            if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(currentItem.getImageUrl())
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.ic_launcher_background)
                        .into(holder.houseImageView);
            } else {
                holder.houseImageView.setImageResource(R.drawable.placeholder_image);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent detailIntent = new Intent(context, PropertyDetail.class);
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

    // Removed onOptionsItemSelected as Toolbar is not used in the same way.
    // Back press is handled by closeButton and system back.
}
