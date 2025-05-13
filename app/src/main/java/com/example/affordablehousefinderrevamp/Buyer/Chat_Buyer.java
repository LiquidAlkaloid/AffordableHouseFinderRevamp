package com.example.affordablehousefinderrevamp.Buyer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// Assuming MessageAdapter is for displaying messages in this individual chat
// import com.example.affordablehousefinderrevamp.Adapter.MessageAdapter;
import com.example.affordablehousefinderrevamp.Model.Message; // You'll need a Message model
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.example.affordablehousefinderrevamp.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat_Buyer extends AppCompatActivity {

    private static final String TAG = "Chat_Buyer_Individual";

    // Header UI (from activity_chat_buyer.xml)
    private TextView tvSellerName; // ID in XML: tv_name
    private TextView tvSellerEmail; // ID in XML: tv_email
    private ImageView icBack, icWarning, icMenu;

    // Property Card UI (from activity_chat_buyer.xml)
    private ImageView imgHouse;
    private TextView tvPropertyTitle, tvPropertyPrice, tvPropertyLocation;
    private Button btnViewPropertyDetails; // ID in XML: btn_view

    // Chat messages RecyclerView (You'll need to add this to activity_chat_buyer.xml)
    // private RecyclerView recyclerViewMessages;
    // private MessageAdapter messageAdapter; // Adapter for messages
    // private List<Message> messageList;

    // Chat input UI (from activity_chat_buyer.xml)
    private EditText etMessage;
    private ImageView icGallery, icAttach; // Assuming send button is part of etMessage or implicit

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentBuyer;

    private String sellerId;
    private String propertyId;
    private String propertyName;
    private String chatId; // Will be generated or fetched

    private ListenerRegistration chatSessionListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_buyer); // This is the individual chat screen for buyer

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentBuyer = mAuth.getCurrentUser();

        // Initialize Header UI
        tvSellerName = findViewById(R.id.tv_name);
        tvSellerEmail = findViewById(R.id.tv_email);
        icBack = findViewById(R.id.ic_back);
        // icWarning = findViewById(R.id.ic_warning); // If needed
        // icMenu = findViewById(R.id.ic_menu); // If needed

        // Initialize Property Card UI
        imgHouse = findViewById(R.id.img_house);
        tvPropertyTitle = findViewById(R.id.tv_title);
        tvPropertyPrice = findViewById(R.id.tv_price);
        tvPropertyLocation = findViewById(R.id.tv_location);
        btnViewPropertyDetails = findViewById(R.id.btn_view);

        // Initialize Chat Input UI
        etMessage = findViewById(R.id.et_message);
        // icGallery = findViewById(R.id.ic_gallery); // If needed
        // icAttach = findViewById(R.id.ic_attach); // If needed
        // Add a send button if not present and wire it up


        // Get data from Intent (passed from PropertyDetail)
        Intent intent = getIntent();
        sellerId = intent.getStringExtra("sellerId");
        propertyId = intent.getStringExtra("propertyId");
        propertyName = intent.getStringExtra("propertyName");

        if (currentBuyer == null) {
            Toast.makeText(this, "You need to be logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (sellerId == null || propertyId == null) {
            Toast.makeText(this, "Error: Seller or Property information missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Generate or get Chat ID (buyerId_sellerId_propertyId for simplicity, or query)
        chatId = generateChatId(currentBuyer.getUid(), sellerId, propertyId);

        loadSellerDetails();
        loadPropertyDetailsForCard();
        setupChatRecyclerView(); // You need to implement this and MessageAdapter
        listenToChatSessionStatus(); // For offer decline popups

        if (icBack != null) {
            icBack.setOnClickListener(v -> onBackPressed());
        }

        if (btnViewPropertyDetails != null) {
            btnViewPropertyDetails.setOnClickListener(v -> {
                Intent detailIntent = new Intent(Chat_Buyer.this, PropertyDetail.class);
                detailIntent.putExtra("propertyId", propertyId);
                startActivity(detailIntent);
            });
        }

        // Placeholder for send message button
        // Button btnSend = findViewById(R.id.btn_send_message); // Add this to your XML
        // if(btnSend != null) {
        //    btnSend.setOnClickListener(v -> sendMessage());
        // }
    }

    private String generateChatId(String userId1, String userId2, String propId) {
        // Ensure consistent order for combined IDs
        if (userId1.compareTo(userId2) > 0) {
            String temp = userId1;
            userId1 = userId2;
            userId2 = temp;
        }
        return userId1 + "_" + userId2 + "_" + propId;
    }


    private void loadSellerDetails() {
        db.collection("users").document(sellerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User seller = documentSnapshot.toObject(User.class);
                        if (seller != null) {
                            if (tvSellerName != null) tvSellerName.setText(seller.getName());
                            if (tvSellerEmail != null) tvSellerEmail.setText(seller.getEmail());
                            // Load seller profile image if available
                        }
                    } else {
                        Log.w(TAG, "Seller document does not exist for ID: " + sellerId);
                        if (tvSellerName != null) tvSellerName.setText("Seller Unavailable");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching seller details", e);
                    if (tvSellerName != null) tvSellerName.setText("Error Loading Seller");
                });
    }

    private void loadPropertyDetailsForCard() {
        if (propertyId == null) return;
        db.collection("properties").document(propertyId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Property property = documentSnapshot.toObject(Property.class);
                        if (property != null) {
                            if (tvPropertyTitle != null) tvPropertyTitle.setText(property.getTitle());
                            if (tvPropertyPrice != null) tvPropertyPrice.setText(property.getPrice());
                            if (tvPropertyLocation != null) tvPropertyLocation.setText(property.getLocation());
                            // Load property image into imgHouse using Glide
                            // if (imgHouse != null && property.getImageUrl() != null) {
                            //    Glide.with(this).load(property.getImageUrl()).into(imgHouse);
                            // }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching property details for card", e));
    }


    private void setupChatRecyclerView() {
        // recyclerViewMessages = findViewById(R.id.your_chat_messages_recyclerview_id); // Add to XML
        // messageList = new ArrayList<>();
        // messageAdapter = new MessageAdapter(this, messageList, currentBuyer.getUid());
        // LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        // layoutManager.setStackFromEnd(true); // To show newest messages at the bottom
        // recyclerViewMessages.setLayoutManager(layoutManager);
        // recyclerViewMessages.setAdapter(messageAdapter);
        // loadMessages(); // Implement this method
        Log.d(TAG, "Chat RecyclerView setup placeholder. Implement MessageAdapter and add RecyclerView to XML.");
    }

    private void listenToChatSessionStatus() {
        // Listen to the chat session document in the *seller's* subcollection,
        // as the seller is the one who accepts/declines.
        // The document ID (chatId) should be the same for both buyer and seller perspective.
        DocumentReference chatSessionRef = db.collection("users").document(sellerId)
                .collection("chat_sessions").document(chatId);

        chatSessionListener = chatSessionRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Chat session listen failed.", e);
                return;
            }

            if (snapshot != null && snapshot.exists()) {
                Boolean conversationClosed = snapshot.getBoolean("conversationClosed");
                String offerStatus = snapshot.getString("offerStatus");

                if (Boolean.TRUE.equals(conversationClosed) && "declined".equals(offerStatus)) {
                    showConversationClosedDialog("The seller has declined the offer and this conversation is now closed.");
                    if(etMessage != null) etMessage.setEnabled(false); // Disable input
                    // Disable send button as well
                }
                // Handle "accepted" status if needed (e.g., change UI, enable further actions)
            }
        });
    }


    private void showConversationClosedDialog(String message) {
        if (!isFinishing() && !isDestroyed()) {
            new AlertDialog.Builder(this)
                    .setTitle("Conversation Update")
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false) // User must acknowledge
                    .show();
        }
    }

    // Placeholder for sendMessage
    // private void sendMessage() {
    //    String messageText = etMessage.getText().toString().trim();
    //    if (TextUtils.isEmpty(messageText)) return;
    //    // Create Message object
    //    // Add to Firestore: /chats/{chatId}/messages/
    //    // Update last message in both buyer's and seller's chat_sessions document
    //    etMessage.setText("");
    // }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatSessionListener != null) {
            chatSessionListener.remove();
        }
    }
}
