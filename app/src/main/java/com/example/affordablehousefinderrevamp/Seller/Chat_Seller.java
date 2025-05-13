package com.example.affordablehousefinderrevamp.Seller;

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
// import androidx.recyclerview.widget.LinearLayoutManager;
// import androidx.recyclerview.widget.RecyclerView;

// import com.example.affordablehousefinderrevamp.Adapter.MessageAdapter;
import com.example.affordablehousefinderrevamp.Model.Message;
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
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat_Seller extends AppCompatActivity {

    private static final String TAG = "Chat_Seller_Individual";

    // Header UI (from activity_chat_admin_or_seller.xml)
    private TextView tvBuyerName; // ID in XML: tv_name
    private TextView tvBuyerEmail; // ID in XML: tv_email
    private ImageView icBackSeller, icWarningSeller, icMenuSeller;

    // Property Card UI (from activity_chat_admin_or_seller.xml)
    private ImageView imgHouseSeller;
    private TextView tvPropertyTitleSeller, tvPropertyPriceSeller, tvPropertyLocationSeller;
    private Button btnViewPropertyDetailsSeller; // ID in XML: btn_view
    private Button btnAcceptOffer, btnDeclineOffer;

    // Chat messages RecyclerView (You'll need to add this to activity_chat_admin_or_seller.xml)
    // private RecyclerView recyclerViewMessagesSeller;
    // private MessageAdapter messageAdapterSeller;
    // private List<Message> messageListSeller;

    // Chat input UI (from activity_chat_admin_or_seller.xml)
    private EditText etMessageSeller;
    private ImageView icGallerySeller, icAttachSeller;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentSeller;

    private String buyerId;
    private String propertyId; // Property related to this chat
    private String chatId; // Passed from ChatSellerActivity

    private ListenerRegistration chatSessionListenerSeller; // To listen for buyer's messages or status changes


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This is the individual chat screen for seller, using activity_chat_admin_or_seller.xml
        setContentView(R.layout.activity_chat_admin_or_seller);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentSeller = mAuth.getCurrentUser();

        // Initialize Header UI
        tvBuyerName = findViewById(R.id.tv_name);
        tvBuyerEmail = findViewById(R.id.tv_email);
        icBackSeller = findViewById(R.id.ic_back);
        // icWarningSeller = findViewById(R.id.ic_warning);
        // icMenuSeller = findViewById(R.id.ic_menu);

        // Initialize Property Card UI
        imgHouseSeller = findViewById(R.id.img_house);
        tvPropertyTitleSeller = findViewById(R.id.tv_title);
        tvPropertyPriceSeller = findViewById(R.id.tv_price);
        tvPropertyLocationSeller = findViewById(R.id.tv_location);
        btnViewPropertyDetailsSeller = findViewById(R.id.btn_view);
        btnAcceptOffer = findViewById(R.id.btn_accept);
        btnDeclineOffer = findViewById(R.id.btn_decline);

        // Initialize Chat Input UI
        etMessageSeller = findViewById(R.id.et_message);
        // icGallerySeller = findViewById(R.id.ic_gallery);
        // icAttachSeller = findViewById(R.id.ic_attach);

        Intent intent = getIntent();
        chatId = intent.getStringExtra("chatId");
        buyerId = intent.getStringExtra("buyerId");
        // propertyId might also be passed if ChatItem from ChatSellerActivity has it
        propertyId = intent.getStringExtra("propertyId"); // Assuming it's passed

        if (currentSeller == null) {
            Toast.makeText(this, "You need to be logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (buyerId == null || chatId == null) {
            Toast.makeText(this, "Error: Buyer or Chat information missing.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadBuyerDetails();
        if (propertyId != null) { // Only load if propertyId is available
            loadPropertyDetailsForCard();
        } else {
            // Hide or show placeholder for property card if no propertyId
            findViewById(R.id.card_offer).setVisibility(View.GONE);
        }
        setupChatRecyclerViewSeller(); // Placeholder
        // listenToChatMessages(); // Placeholder

        if (icBackSeller != null) {
            icBackSeller.setOnClickListener(v -> onBackPressed());
        }

        if (btnViewPropertyDetailsSeller != null && propertyId != null) {
            btnViewPropertyDetailsSeller.setOnClickListener(v -> {
                Intent detailIntent = new Intent(Chat_Seller.this, SellerPropertyDetails.class); // Seller's view
                detailIntent.putExtra("propertyId", propertyId);
                startActivity(detailIntent);
            });
        } else if (btnViewPropertyDetailsSeller != null) {
            btnViewPropertyDetailsSeller.setVisibility(View.GONE);
        }


        btnAcceptOffer.setOnClickListener(v -> handleOffer("accepted"));
        btnDeclineOffer.setOnClickListener(v -> handleOffer("declined"));

        // Placeholder for send message button
        // Button btnSend = findViewById(R.id.btn_send_message_seller); // Add this to your XML
        // if(btnSend != null) {
        //    btnSend.setOnClickListener(v -> sendMessageSeller());
        // }
    }

    private void loadBuyerDetails() {
        db.collection("users").document(buyerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User buyer = documentSnapshot.toObject(User.class);
                        if (buyer != null) {
                            if (tvBuyerName != null) tvBuyerName.setText(buyer.getName());
                            if (tvBuyerEmail != null) tvBuyerEmail.setText(buyer.getEmail());
                        }
                    } else {
                        Log.w(TAG, "Buyer document does not exist for ID: " + buyerId);
                        if (tvBuyerName != null) tvBuyerName.setText("Buyer Unavailable");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching buyer details", e);
                    if (tvBuyerName != null) tvBuyerName.setText("Error Loading Buyer");
                });
    }

    private void loadPropertyDetailsForCard() {
        if (propertyId == null) {
            findViewById(R.id.card_offer).setVisibility(View.GONE);
            return;
        }
        findViewById(R.id.card_offer).setVisibility(View.VISIBLE);
        db.collection("properties").document(propertyId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Property property = documentSnapshot.toObject(Property.class);
                        if (property != null) {
                            if (tvPropertyTitleSeller != null) tvPropertyTitleSeller.setText(property.getTitle());
                            if (tvPropertyPriceSeller != null) tvPropertyPriceSeller.setText(property.getPrice());
                            if (tvPropertyLocationSeller != null) tvPropertyLocationSeller.setText(property.getLocation());
                            // Load property image into imgHouseSeller using Glide
                        }
                    } else {
                        // Property associated with chat not found, hide card
                        findViewById(R.id.card_offer).setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching property details for card", e);
                    findViewById(R.id.card_offer).setVisibility(View.GONE);
                });
    }

    private void handleOffer(String status) {
        // Update the chat session in Firestore for both seller and buyer.
        // The chatId should uniquely identify this conversation.
        // Seller's perspective: users/{sellerId}/chat_sessions/{chatId}
        // Buyer's perspective: users/{buyerId}/chat_sessions/{chatId} (or a similar structure)

        DocumentReference sellerChatSessionRef = db.collection("users").document(currentSeller.getUid())
                .collection("chat_sessions").document(chatId);
        DocumentReference buyerChatSessionRef = db.collection("users").document(buyerId)
                .collection("chat_sessions").document(chatId); // Assuming same chatId

        Map<String, Object> offerUpdate = new HashMap<>();
        offerUpdate.put("offerStatus", status);
        if ("declined".equals(status)) {
            offerUpdate.put("conversationClosed", true);
        } else if ("accepted".equals(status)) {
            offerUpdate.put("conversationClosed", false); // Ensure it's open or remains open
            // Optionally update property status if this chat is tied to a specific property offer
            if (propertyId != null) {
                // db.collection("properties").document(propertyId).update("status", "Reserved"); // Or "Sold"
            }
        }

        // Update for seller
        sellerChatSessionRef.set(offerUpdate, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Chat_Seller.this, "Offer " + status + ".", Toast.LENGTH_SHORT).show();
                    if ("declined".equals(status)) {
                        etMessageSeller.setEnabled(false);
                        // Disable send button, accept/decline buttons
                        btnAcceptOffer.setEnabled(false);
                        btnDeclineOffer.setEnabled(false);
                    } else {
                        btnAcceptOffer.setEnabled(false); // Can't accept again
                        btnDeclineOffer.setEnabled(true); // Can still decline later if needed, or disable both
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error updating seller chat session for offer", e));

        // Update for buyer (so buyer's app gets the status change)
        buyerChatSessionRef.set(offerUpdate, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Buyer chat session updated for offer status."))
                .addOnFailureListener(e -> Log.e(TAG, "Error updating buyer chat session for offer", e));
    }


    private void setupChatRecyclerViewSeller() {
        Log.d(TAG, "Seller Chat RecyclerView setup placeholder. Implement MessageAdapter and add RecyclerView to XML.");
    }

    // Placeholder for sendMessageSeller
    // private void sendMessageSeller() { ... }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // if (chatSessionListenerSeller != null) {
        //    chatSessionListenerSeller.remove();
        // }
    }
}
