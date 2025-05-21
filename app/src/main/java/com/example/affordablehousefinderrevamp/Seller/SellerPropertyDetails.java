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
import com.google.firebase.firestore.QueryDocumentSnapshot; // Keep for checkForChats

import java.util.ArrayList; // Keep for checkForChats
import java.util.List; // Keep for checkForChats

public class SellerPropertyDetails extends AppCompatActivity {

    private static final String TAG = "SellerPropertyDetails";

    private ImageView propertyImageView;
    private ImageButton backButton, shareButton, moreOptionsButton, descriptionEditIcon;
    private TextView propertyTitleTextView, propertyPriceTextView, locationTextView,
            conditionTextView, bookmarkTextView, descriptionTextView;
    private Button viewInsightsButton, noChatsButton; // viewInsightsButton will now be View Offers

    private FirebaseFirestore db;
    private DocumentReference propertyDocRef;
    private ListenerRegistration propertyListener;
    private FirebaseUser currentUser;

    private String propertyId;
    private Property currentProperty;

    // For handling chats related to this property specifically via noChatsButton
    private List<String> propertySpecificChatIds = new ArrayList<>();
    private List<String> propertySpecificBuyerIds = new ArrayList<>();
    private List<String> propertySpecificBuyerNames = new ArrayList<>();


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
        bookmarkTextView = findViewById(R.id.bookmarkTextView); // This might show bookmark count by buyers
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
        checkForPropertyChats(); // Renamed to avoid confusion with general chat list

        backButton.setOnClickListener(v -> onBackPressed());
        shareButton.setOnClickListener(v -> shareProperty());
        moreOptionsButton.setOnClickListener(this::showMoreOptionsMenu);
        descriptionEditIcon.setOnClickListener(v -> editProperty());

        viewInsightsButton.setText("View Offers"); // Update button text
        viewInsightsButton.setOnClickListener(v -> {
            Intent intent = new Intent(SellerPropertyDetails.this, OffersListActivity.class);
            intent.putExtra("propertyId", propertyId);
            startActivity(intent);
        });

        noChatsButton.setOnClickListener(v -> {
            if (propertySpecificChatIds.isEmpty()) {
                Toast.makeText(SellerPropertyDetails.this, "No chats available for this property", Toast.LENGTH_SHORT).show();
            } else if (propertySpecificChatIds.size() == 1) {
                navigateToChat(propertySpecificChatIds.get(0), propertySpecificBuyerIds.get(0), propertySpecificBuyerNames.get(0));
            } else {
                showPropertyChatSelectionDialog();
            }
        });
    }

    private void checkForPropertyChats() {
        propertySpecificChatIds.clear();
        propertySpecificBuyerIds.clear();
        propertySpecificBuyerNames.clear();

        noChatsButton.setText("Checking for chats..."); // Loading state

        db.collection("users")
                .document(currentUser.getUid())
                .collection("chat_sessions")
                .whereEqualTo("propertyId", propertyId) // Filter chats by this propertyId
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        noChatsButton.setText("No Property Chats");
                    } else {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String chatId = document.getId();
                            String buyerId = document.getString("otherUserId");
                            String buyerName = document.getString("senderName"); // In seller's chat_session, senderName is buyer's name

                            if (buyerId != null && buyerName != null) {
                                propertySpecificChatIds.add(chatId);
                                propertySpecificBuyerIds.add(buyerId);
                                propertySpecificBuyerNames.add(buyerName);
                            }
                        }
                        noChatsButton.setText(propertySpecificChatIds.size() + " Property Chat" + (propertySpecificChatIds.size() > 1 ? "s" : ""));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking for property-specific chats", e);
                    noChatsButton.setText("Chat (Error)");
                });
    }

    private void showPropertyChatSelectionDialog() {
        if (propertySpecificChatIds.isEmpty()) {
            Toast.makeText(this, "No chats available for this property", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a Chat for this Property");

        // Use buyerNames directly as they are already fetched in checkForPropertyChats
        builder.setItems(propertySpecificBuyerNames.toArray(new String[0]), (dialog, which) -> {
                    navigateToChat(propertySpecificChatIds.get(which), propertySpecificBuyerIds.get(which), propertySpecificBuyerNames.get(which));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void navigateToChat(String chatId, String buyerId, String buyerName) {
        Intent intent = new Intent(SellerPropertyDetails.this, Chat_Seller.class);
        intent.putExtra("chatId", chatId);
        intent.putExtra("buyerId", buyerId);
        intent.putExtra("buyerName", buyerName); // Pass buyer name for header
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
            // Consider generating a dynamic link or a web URL if your app has a web component
            String propertyShareUrl = "https://your-app-domain.com/property/" + propertyId; // Placeholder
            String shareBody = "Check out this property: " + currentProperty.getTitle() +
                    "\nLocation: " + currentProperty.getLocation() +
                    "\nPrice: " + currentProperty.getPrice() +
                    "\nFind it on AffordableHouseFinder: " + propertyShareUrl; // Example
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Property For Sale: " + currentProperty.getTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(shareIntent, "Share via"));
        } else {
            Toast.makeText(this, "Property details not loaded yet.", Toast.LENGTH_SHORT).show();
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
        if (propertyDocRef == null) return;
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
                    currentProperty.setPropertyId(snapshot.getId()); // Ensure propertyId is set
                    if (!currentUser.getUid().equals(currentProperty.getSellerId())) {
                        Toast.makeText(SellerPropertyDetails.this, "You are not authorized to view these seller details.", Toast.LENGTH_LONG).show();
                        // Fallback to buyer view or finish
                        // For now, finish
                        finish();
                        return;
                    }
                    populateUI();
                } else {
                    Toast.makeText(SellerPropertyDetails.this, "Error converting property data.", Toast.LENGTH_SHORT).show();
                    finish();
                }
            } else {
                Log.d(TAG, "Current data: null. Property might have been deleted.");
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
        conditionTextView.setText("Type: " + currentProperty.getPropertyType()); // Changed to Property Type
        // bookmarkTextView could show how many buyers bookmarked this. This requires an aggregation.
        // For now, let's keep it simple or query for it.
        // For simplicity, we'll keep the placeholder or fetch this count if needed.
        fetchBookmarkCount(); // Placeholder for fetching bookmark count

        String imageUrlToLoad = null;
        if (currentProperty.getImageUrls() != null && !currentProperty.getImageUrls().isEmpty()) {
            imageUrlToLoad = currentProperty.getImageUrls().get(0);
        } else if (currentProperty.getImageUrl() != null && !currentProperty.getImageUrl().isEmpty()) {
            imageUrlToLoad = currentProperty.getImageUrl();
        }


        if (imageUrlToLoad != null) {
            Glide.with(this).load(imageUrlToLoad)
                    .placeholder(R.drawable.placeholder_image) // Ensure you have placeholder_image
                    .error(R.drawable.ic_launcher_background) // Or a more generic error placeholder
                    .into(propertyImageView);
        } else {
            propertyImageView.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void fetchBookmarkCount() {
        // This is a more complex query that requires querying the 'users' collection,
        // then for each user, their 'bookmarked_properties' subcollection.
        // Firestore doesn't support direct count queries across collections like this efficiently for the client.
        // A common approach is to maintain a counter on the property document itself,
        // updated via Cloud Functions when a bookmark is added/removed.
        // For simplicity here, we'll just show a placeholder or "N/A".
        db.collectionGroup("bookmarked_properties")
                .whereEqualTo("propertyId", propertyId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    bookmarkTextView.setText(queryDocumentSnapshots.size() + " Bookmarks");
                })
                .addOnFailureListener(e -> {
                    bookmarkTextView.setText("N/A Bookmarks");
                    Log.w(TAG, "Error fetching bookmark count.", e);
                });
    }


    private void editProperty() {
        if (currentProperty == null || propertyId == null) {
            Toast.makeText(this, "Property data not available for editing.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent editIntent = new Intent(this, UploadActivity.class);
        editIntent.putExtra("propertyIdToEdit", propertyId);
        startActivity(editIntent);
    }

    private void confirmDeleteProperty() {
        if (currentProperty == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Delete Property")
                .setMessage("Are you sure you want to delete '" + currentProperty.getTitle() + "' listing?")
                .setPositiveButton("Delete", (dialog, which) -> deletePropertyFromFirestore())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deletePropertyFromFirestore() {
        if (propertyId == null) return;
        Log.d(TAG, "Attempting to delete property: " + propertyId);
        // Consider also deleting associated offers and chat_sessions or notifying users.
        // For now, just deleting the property.
        propertyDocRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SellerPropertyDetails.this, "Property deleted.", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "Property deleted successfully: " + propertyId);
                    // Navigate back to the listings page or finish activity
                    Intent intent = new Intent(SellerPropertyDetails.this, HouseListings.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
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
        if (currentUser != null && propertyId != null) {
            checkForPropertyChats(); // Refresh chat button status
            if (propertyDocRef != null && propertyListener == null) { // Re-attach listener if it was removed
                loadPropertyDetails();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (propertyListener != null) {
            propertyListener.remove();
            propertyListener = null; // Good practice
        }
    }
}