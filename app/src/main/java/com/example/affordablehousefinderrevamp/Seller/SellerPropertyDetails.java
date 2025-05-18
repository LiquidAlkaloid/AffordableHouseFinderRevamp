package com.example.affordablehousefinderrevamp.Seller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SellerPropertyDetails extends AppCompatActivity {

    private static final String TAG = "SellerPropertyDetails";

    private ImageView propertyImageView;
    private ImageButton backButton, shareButton, moreOptionsButton, descriptionEditIcon;
    private TextView propertyTitleTextView, propertyPriceTextView, locationTextView,
            conditionTextView, bookmarkTextView, descriptionTextView;
    private Button viewInsightsButton, noChatsButton;

    private FirebaseFirestore db;
    private DocumentReference propertyDocRef;
    private ListenerRegistration propertyListener;
    private FirebaseUser currentUser;

    private String propertyId;
    private Property currentProperty;
    private List<String> chatIds = new ArrayList<>();
    private List<String> buyerIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_property_details);

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
        descriptionTextView = findViewById(R.id.descriptionTextView);
        viewInsightsButton = findViewById(R.id.viewInsightsButton);
        noChatsButton = findViewById(R.id.noChatsButton);

        propertyId = getIntent().getStringExtra("propertyId");
        if (propertyId == null || propertyId.isEmpty()) {
            Toast.makeText(this, "Property ID not found.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Authentication error.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        propertyDocRef = db.collection("properties").document(propertyId);

        loadPropertyDetails();
        checkForChats();

        backButton.setOnClickListener(v -> onBackPressed());
        shareButton.setOnClickListener(v -> shareProperty());
        moreOptionsButton.setOnClickListener(this::showMoreOptionsMenu);
        descriptionEditIcon.setOnClickListener(v -> editProperty());
        viewInsightsButton.setOnClickListener(v -> Toast.makeText(this, "View Insights (not implemented)", Toast.LENGTH_SHORT).show());

        // Set up the noChatsButton click listener
        noChatsButton.setOnClickListener(v -> {
            if (chatIds.isEmpty()) {
                Toast.makeText(SellerPropertyDetails.this, "No chats available for this property", Toast.LENGTH_SHORT).show();
            } else if (chatIds.size() == 1) {
                // If there's only one chat, go directly to it
                navigateToChat(chatIds.get(0), buyerIds.get(0));
            } else {
                // If there are multiple chats, show a dialog to choose
                showChatSelectionDialog();
            }
        });
    }

    private void checkForChats() {
        // Reset lists
        chatIds.clear();
        buyerIds.clear();

        // Update button text to show loading state
        noChatsButton.setText("Checking for chats...");

        // Query for chat sessions related to this property
        db.collection("users")
                .document(currentUser.getUid())
                .collection("chat_sessions")
                .whereEqualTo("propertyId", propertyId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        noChatsButton.setText("No chats yet");
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String chatId = document.getId();
                            String buyerId = document.getString("otherUserId");

                            if (buyerId != null) {
                                chatIds.add(chatId);
                                buyerIds.add(buyerId);
                            }
                        }

                        // Update button text with chat count
                        noChatsButton.setText(chatIds.size() + " Chat" + (chatIds.size() > 1 ? "s" : ""));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for chats", e);
                    noChatsButton.setText("No chats yet");
                });
    }

    private void showChatSelectionDialog() {
        if (chatIds.isEmpty() || buyerIds.isEmpty()) {
            Toast.makeText(this, "No chats available", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get buyer names for each chat
        List<String> buyerNames = new ArrayList<>();
        for (String buyerId : buyerIds) {
            buyerNames.add("Loading..."); // Placeholder until we load the actual names
        }

        // Create dialog with placeholder names
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a chat");

        // Create and show the dialog with placeholders
        AlertDialog dialog = builder.setItems(buyerNames.toArray(new String[0]), null)
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();

        // Now load the actual buyer names and update the dialog
        for (int i = 0; i < buyerIds.size(); i++) {
            final int index = i;
            db.collection("users").document(buyerIds.get(i))
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String buyerName = documentSnapshot.getString("name");
                            if (buyerName != null) {
                                buyerNames.set(index, buyerName);

                                // Update dialog with new names
                                if (dialog.isShowing()) {
                                    dialog.getListView().setAdapter(
                                            new android.widget.ArrayAdapter<>(
                                                    this,
                                                    android.R.layout.simple_list_item_1,
                                                    buyerNames
                                            )
                                    );

                                    // Set click listener for the items
                                    dialog.getListView().setOnItemClickListener((parent, view, position, id) -> {
                                        dialog.dismiss();
                                        navigateToChat(chatIds.get(position), buyerIds.get(position));
                                    });
                                }
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error loading buyer name", e));
        }
    }

    private void navigateToChat(String chatId, String buyerId) {
        Intent intent = new Intent(SellerPropertyDetails.this, Chat_Seller.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("buyerId", buyerId);
        intent.putExtra("propertyId", propertyId);
        if (currentProperty != null) {
            intent.putExtra("propertyName", currentProperty.getTitle());
        }
        startActivity(intent);
    }

    private void shareProperty() {
        if (currentProperty != null) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareBody = "Check out this property: " + currentProperty.getTitle() + "\nLocation: " + currentProperty.getLocation();
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Property: " + currentProperty.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } else {
            Toast.makeText(this, "Details not loaded.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showMoreOptionsMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.seller_property_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_property) {
                editProperty();
                return true;
            } else if (itemId == R.id.action_remove_property) {
                confirmDeleteProperty();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void loadPropertyDetails() {
        propertyListener = propertyDocRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                Toast.makeText(SellerPropertyDetails.this, "Failed to load details: " + e.getMessage(), Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                currentProperty = snapshot.toObject(Property.class);
                if (currentProperty != null) {
                    if (!currentUser.getUid().equals(currentProperty.getSellerId())) {
                        Toast.makeText(SellerPropertyDetails.this, "Not authorized.", Toast.LENGTH_LONG).show();
                        finish();
                        return;
                    }
                    populateUI();
                } else {
                    Toast.makeText(SellerPropertyDetails.this, "Error converting property data.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Log.d(TAG, "Current data: null");
                Toast.makeText(SellerPropertyDetails.this, "Property details not found.", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void populateUI() {
        if (currentProperty == null) return;

        propertyTitleTextView.setText(currentProperty.getTitle().toUpperCase());
        propertyPriceTextView.setText(currentProperty.getPrice());
        locationTextView.setText(currentProperty.getLocation());
        descriptionTextView.setText(currentProperty.getDescription());
        conditionTextView.setText(currentProperty.getPropertyType());
        bookmarkTextView.setText("N/A Bookmarks");

        String imageUrlToLoad = null;
        if (currentProperty.getImageUrl() != null && !currentProperty.getImageUrl().isEmpty()) {
            imageUrlToLoad = currentProperty.getImageUrl();
        } else if (currentProperty.getImageUrls() != null && !currentProperty.getImageUrls().isEmpty()) {
            imageUrlToLoad = currentProperty.getImageUrls().get(0);
        }

        if (imageUrlToLoad != null) {
            Glide.with(this).load(imageUrlToLoad)
                    .placeholder(R.drawable.placeholder)
                    .error(R.drawable.ic_launcher_background)
                    .into(propertyImageView);
        } else {
            propertyImageView.setImageResource(R.drawable.placeholder);
        }
    }

    private void editProperty() {
        if (currentProperty == null) return;
        Intent editIntent = new Intent(this, UploadActivity.class);
        editIntent.putExtra("propertyIdToEdit", propertyId);
        startActivity(editIntent);
    }

    private void confirmDeleteProperty() {
        if (currentProperty == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Property")
                .setMessage("Are you sure you want to delete this listing?")
                .setPositiveButton("Delete", (dialog, which) -> deletePropertyFromFirestore())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePropertyFromFirestore() {
        if (propertyId == null) return;
        Log.d(TAG, "Attempting to delete property: " + propertyId);
        propertyDocRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SellerPropertyDetails.this, "Property deleted.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Property deleted successfully: " + propertyId);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SellerPropertyDetails.this, "Failed to delete property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error deleting property", e);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh chat status when returning to this screen
        if (currentUser != null && propertyId != null) {
            checkForChats();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (propertyListener != null) {
            propertyListener.remove();
        }
    }
}
