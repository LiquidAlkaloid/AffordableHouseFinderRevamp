package com.example.affordablehousefinderrevamp.Buyer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.affordablehousefinderrevamp.Adapter.MessageAdapter;
import com.example.affordablehousefinderrevamp.Model.Message;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.example.affordablehousefinderrevamp.User; // Assuming User model exists
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat_Buyer extends AppCompatActivity {
    private static final String TAG = "Chat_Buyer_Individual";

    // UI Elements for Header
    private TextView tvSellerNameHeader, tvSellerEmailHeader; // Renamed for clarity
    private ImageButton icBackHeader;

    // UI Elements for Property Card (similar to Seller's chat)
    private ImageView imgHousePropertyCardBuyer;
    private TextView tvPropertyTitleCardBuyer, tvPropertyPriceCardBuyer, tvPropertyLocationCardBuyer;
    private Button btnViewPropertyDetailsCardBuyer;
    private View cardOfferViewPropertyBuyer; // The whole card view

    // UI Elements for Chat
    private RecyclerView recyclerViewMessagesChatBuyer;
    private EditText etMessageInputBuyer;
    private ImageButton btnSendMessageActionBuyer;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseUser currentFirebaseUser;
    private User currentBuyerDetails; // Details of the logged-in buyer
    private User sellerDetails;       // Details of the seller being chatted with

    // Intent Data
    private String sellerId;
    private String propertyId;
    private String propertyNameFromIntent;
    private String propertyNameForDisplay; // Actual property name for display
    private String chatId;

    // Adapter and List
    private MessageAdapter messageAdapterBuyer;
    private List<Message> messageListBuyer;
    private ListenerRegistration messagesListenerRegistrationBuyer;
    private ListenerRegistration chatSessionStatusListenerBuyer; // To listen for offer status changes, etc.

    interface UserDetailsCallback {
        void onCallback();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assuming activity_chat_buyer.xml is similar to activity_chat_admin_or_seller.xml
        // but with IDs specific to the buyer's perspective if needed.
        // For this example, we'll assume it uses the same IDs as activity_chat_admin_or_seller.xml
        // for shared components like message input and RecyclerView.
        setContentView(R.layout.activity_chat_buyer); // Make sure this layout exists and has the IDs

        db = FirebaseFirestore.getInstance();
        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentFirebaseUser == null || TextUtils.isEmpty(currentFirebaseUser.getUid())) {
            Toast.makeText(this, "Authentication required. Please log in.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "No authenticated user");
            finish();
            return;
        }

        initUI();
        readIntentData();

        if (TextUtils.isEmpty(sellerId)) {
            Toast.makeText(this, "Seller information missing; cannot proceed.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Generate chatId if not passed (e.g., starting a new chat from property details)
        if (TextUtils.isEmpty(chatId)) {
            chatId = generateChatId(currentFirebaseUser.getUid(), sellerId,
                    !TextUtils.isEmpty(propertyId) ? propertyId : "general_inquiry");
        }
        if (chatId.startsWith("error_")) {
            Toast.makeText(this, "Invalid chat session parameters.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupChatRecyclerViewBuyer();
        setupUIListeners();

        // Load current buyer's details first, then seller's details for the header
        loadCurrentBuyerDetails(() -> loadSellerDetailsForHeader(() -> {
            if (currentBuyerDetails == null || sellerDetails == null) {
                // Error logged, UI might be partially updated or show placeholders
            }
            if (!TextUtils.isEmpty(propertyId)) {
                loadPropertyDetailsForCard(); // This will also call createOrUpdateChatSessionMetadata
            } else {
                cardOfferViewPropertyBuyer.setVisibility(View.GONE);
                propertyNameForDisplay = "General Inquiry";
                createOrUpdateChatSessionMetadata(); // Call if no property involved
            }
            listenToMessagesBuyer();
            listenToChatSessionStatusBuyer(); // Listen for changes from seller (e.g., offer accepted)
        }));
    }

    private void initUI() {
        // Header
        tvSellerNameHeader = findViewById(R.id.tv_name); // Assuming same ID for seller's name in buyer's chat
        tvSellerEmailHeader = findViewById(R.id.tv_email); // Assuming same ID for seller's email
        icBackHeader = findViewById(R.id.ic_back);

        // Property Card (assuming similar IDs to seller's chat layout for the card part)
        cardOfferViewPropertyBuyer = findViewById(R.id.card_offer);
        imgHousePropertyCardBuyer = findViewById(R.id.img_house);
        tvPropertyTitleCardBuyer = findViewById(R.id.tv_title);
        tvPropertyPriceCardBuyer = findViewById(R.id.tv_price);
        tvPropertyLocationCardBuyer = findViewById(R.id.tv_location);
        btnViewPropertyDetailsCardBuyer = findViewById(R.id.btn_view);
        // Buyer's chat might not have accept/decline buttons directly on this card.
        // Those actions are typically for the seller. Hide them if they exist in the shared layout.
        Button btnAccept = findViewById(R.id.btn_accept);
        Button btnDecline = findViewById(R.id.btn_decline);
        if (btnAccept != null) btnAccept.setVisibility(View.GONE);
        if (btnDecline != null) btnDecline.setVisibility(View.GONE);


        // Chat UI
        recyclerViewMessagesChatBuyer = findViewById(R.id.recycler_view_messages_buyer); // Ensure this ID is in activity_chat_buyer.xml
        btnSendMessageActionBuyer = findViewById(R.id.btn_send_message_buyer); // Ensure this ID is in activity_chat_buyer.xml
    }

    private void readIntentData() {
        Intent intent = getIntent();
        chatId = intent.getStringExtra("chatId"); // Might be null if starting new
        sellerId = intent.getStringExtra("sellerId");
        propertyId = intent.getStringExtra("propertyId");
        propertyNameFromIntent = intent.getStringExtra("propertyName");
        Log.d(TAG, "Intent Data -> chatId:" + chatId + ", sellerId:" + sellerId + ", propertyId:" + propertyId + ", propertyNameFromIntent:" + propertyNameFromIntent);
    }

    private void setupUIListeners() {
        icBackHeader.setOnClickListener(v -> onBackPressed());

        if (!TextUtils.isEmpty(propertyId)) {
            btnViewPropertyDetailsCardBuyer.setVisibility(View.VISIBLE);
            btnViewPropertyDetailsCardBuyer.setOnClickListener(v -> {
                Intent i = new Intent(this, PropertyDetail.class); // Buyer views PropertyDetail
                i.putExtra("propertyId", propertyId);
                startActivity(i);
            });
        } else {
            btnViewPropertyDetailsCardBuyer.setVisibility(View.GONE);
        }
        btnSendMessageActionBuyer.setOnClickListener(v -> sendMessageBuyer());
    }

    private String generateChatId(String u1, String u2, String ctx) {
        if (TextUtils.isEmpty(u1) || TextUtils.isEmpty(u2) || TextUtils.isEmpty(ctx)) {
            Log.e(TAG, "Cannot generate chatId with null/empty components.");
            return "error_invalid_components";
        }
        String sanitizedCtx = ctx.replaceAll("[^a-zA-Z0-9-]", "_");
        if (u1.compareTo(u2) < 0) {
            return u1 + "_" + u2 + "_" + sanitizedCtx;
        } else {
            return u2 + "_" + u1 + "_" + sanitizedCtx;
        }
    }

    private void loadCurrentBuyerDetails(UserDetailsCallback cb) {
        if (currentFirebaseUser == null) {
            cb.onCallback(); // No user to load
            return;
        }
        db.collection("users").document(currentFirebaseUser.getUid())
                .get().addOnSuccessListener(doc -> {
                    currentBuyerDetails = doc.toObject(User.class);
                    cb.onCallback();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading current buyer details", e);
                    cb.onCallback(); // Proceed even if buyer details fail
                });
    }

    private void loadSellerDetailsForHeader(UserDetailsCallback cb) {
        if (TextUtils.isEmpty(sellerId)) {
            cb.onCallback();
            return;
        }
        db.collection("users").document(sellerId)
                .get().addOnSuccessListener(doc -> {
                    sellerDetails = doc.toObject(User.class);
                    if (sellerDetails != null) {
                        tvSellerNameHeader.setText(sellerDetails.getName());
                        tvSellerEmailHeader.setText(sellerDetails.getEmail()); // Assuming User model has getEmail
                    } else {
                        tvSellerNameHeader.setText("Seller"); // Fallback
                    }
                    cb.onCallback();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading seller details for header", e);
                    tvSellerNameHeader.setText("Seller"); // Fallback
                    cb.onCallback(); // Proceed
                });
    }

    private void loadPropertyDetailsForCard() {
        if (TextUtils.isEmpty(propertyId)) {
            cardOfferViewPropertyBuyer.setVisibility(View.GONE);
            propertyNameForDisplay = "General Inquiry";
            createOrUpdateChatSessionMetadata(); // Update metadata even if no property card
            return;
        }
        cardOfferViewPropertyBuyer.setVisibility(View.VISIBLE);
        db.collection("Properties").document(propertyId) // Corrected collection name
                .get().addOnSuccessListener(doc -> {
                    Property p = doc.toObject(Property.class);
                    if (p != null) {
                        propertyNameForDisplay = p.getName(); // Use actual name from property
                        tvPropertyTitleCardBuyer.setText(p.getName()); // Corrected: Use getName()
                        tvPropertyPriceCardBuyer.setText(p.getPrice());
                        tvPropertyLocationCardBuyer.setText(p.getLocation());
                        String img = p.getPrimaryImageUrl(); // Corrected: Use getPrimaryImageUrl()
                        if (!TextUtils.isEmpty(img)) {
                            Glide.with(this).load(img)
                                    .placeholder(R.drawable.placeholder_image)
                                    .error(R.drawable.placeholder_image) // Fallback image
                                    .into(imgHousePropertyCardBuyer);
                        } else {
                            imgHousePropertyCardBuyer.setImageResource(R.drawable.placeholder_image);
                        }
                    } else {
                        propertyNameForDisplay = !TextUtils.isEmpty(propertyNameFromIntent) ? propertyNameFromIntent : "Property Inquiry";
                        tvPropertyTitleCardBuyer.setText(propertyNameForDisplay);
                        // Hide image or show placeholder if property details not fully loaded
                        imgHousePropertyCardBuyer.setImageResource(R.drawable.placeholder_image);
                    }
                    createOrUpdateChatSessionMetadata(); // Update metadata after property details are attempted
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading property card details for buyer chat", e);
                    cardOfferViewPropertyBuyer.setVisibility(View.GONE); // Or show minimal info
                    propertyNameForDisplay = !TextUtils.isEmpty(propertyNameFromIntent) ? propertyNameFromIntent : "Property Inquiry";
                    createOrUpdateChatSessionMetadata(); // Still update metadata
                });
    }

    private void setupChatRecyclerViewBuyer() {
        messageListBuyer = new ArrayList<>();
        // Corrected: MessageAdapter constructor expects (Context, List<Message>, String currentUserId)
        messageAdapterBuyer = new MessageAdapter(this, messageListBuyer, currentFirebaseUser.getUid());
        recyclerViewMessagesChatBuyer.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerViewMessagesChatBuyer.setAdapter(messageAdapterBuyer);
    }

    private void listenToMessagesBuyer() {
        if (chatId.startsWith("error_")) return;
        CollectionReference msgsRef = db.collection("chats")
                .document(chatId).collection("messages");
        messagesListenerRegistrationBuyer = msgsRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Buyer: Messages listen failed", e);
                        Toast.makeText(this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (queryDocumentSnapshots != null) {
                        messageListBuyer.clear();
                        for (Message m : queryDocumentSnapshots.toObjects(Message.class)) {
                            messageListBuyer.add(m);
                        }
                        messageAdapterBuyer.notifyDataSetChanged();
                        if (!messageListBuyer.isEmpty()) {
                            recyclerViewMessagesChatBuyer.smoothScrollToPosition(messageListBuyer.size() - 1);
                        }
                        // Mark messages as read (simplified: all messages from sender in this chat)
                        markMessagesAsRead();
                    }
                });
    }

    private void markMessagesAsRead() {
        // This is a simplified approach. For a robust solution, you'd update specific messages.
        // Here, we update the buyer's chat session unread count.
        DocumentReference buyerSessionRef = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);
        buyerSessionRef.update("unreadCount", 0)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Buyer unread count reset for chat: " + chatId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to reset buyer unread count for chat: " + chatId, e));
    }


    private void sendMessageBuyer() {
        String text = etMessageInputBuyer.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Cannot send empty message", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentFirebaseUser == null || TextUtils.isEmpty(sellerId)) {
            Toast.makeText(this, "Cannot send message. User/Seller info missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Corrected: Message constructor expects (senderId, receiverId, text, timestamp)
        Message newMessage = new Message(currentFirebaseUser.getUid(), sellerId, text, new Date());
        db.collection("chats").document(chatId).collection("messages")
                .add(newMessage).addOnSuccessListener(docRef -> {
                    etMessageInputBuyer.setText("");
                    updateChatSessionMetadataOnSend(text); // Update metadata for both users
                    Log.d(TAG, "Message sent successfully by buyer");
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message by buyer", e);
                    Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                });
    }

    private void createOrUpdateChatSessionMetadata() {
        if (currentFirebaseUser == null || TextUtils.isEmpty(sellerId) || sellerDetails == null) {
            Log.e(TAG, "Cannot create/update chat session metadata for buyer: missing user details.");
            return;
        }
        // For Buyer's own session list
        DocumentReference buyerSessionRef = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);

        Map<String, Object> data = new HashMap<>();
        data.put("otherUserId", sellerId);
        data.put("otherUserName", sellerDetails.getName()); // Store seller's name for buyer's list
        if (!TextUtils.isEmpty(propertyId)) data.put("propertyId", propertyId);
        data.put("propertyName", TextUtils.isEmpty(propertyNameForDisplay) ? "General Inquiry" : propertyNameForDisplay);
        // Buyer doesn't manage offerStatus directly in their own session view in this way
        // data.put("offerStatus", "pending"); // This is more for seller or shared state
        data.put("conversationClosed", false); // Default or fetch existing
        data.put("unreadCount", 0); // Buyer is viewing this chat, so 0 for them initially
        data.put("timestamp", FieldValue.serverTimestamp());

        buyerSessionRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Buyer chat session metadata created/updated."))
                .addOnFailureListener(e -> Log.e(TAG, "Buyer chat session metadata update failed.", e));

        // For Seller's session list (so seller sees this chat appear/update)
        if (currentBuyerDetails != null) {
            DocumentReference sellerSessionRef = db.collection("users")
                    .document(sellerId)
                    .collection("chat_sessions").document(chatId);
            Map<String, Object> sellerMetadata = new HashMap<>(data); // Start with common data
            sellerMetadata.put("otherUserId", currentFirebaseUser.getUid());
            sellerMetadata.put("otherUserName", currentBuyerDetails.getName());
            // unreadCount for seller will be handled by sender (buyer)
            sellerSessionRef.set(sellerMetadata, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Seller chat session metadata also updated for consistency."))
                    .addOnFailureListener(e -> Log.e(TAG, "Seller chat session metadata update (consistency) failed.", e));
        }
    }

    private void updateChatSessionMetadataOnSend(String lastMessage) {
        if (currentBuyerDetails == null || TextUtils.isEmpty(sellerId) || sellerDetails == null) {
            Log.e(TAG, "Cannot update chat metadata on send from buyer: missing user details");
            return;
        }

        String effectivePropertyName = TextUtils.isEmpty(propertyNameForDisplay) ? "General Inquiry" : propertyNameForDisplay;

        // Update buyer's own session (they sent the message)
        DocumentReference buyerSessionRef = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);
        Map<String, Object> buyerData = new HashMap<>();
        buyerData.put("lastMessage", lastMessage);
        buyerData.put("timestamp", FieldValue.serverTimestamp());
        buyerData.put("otherUserId", sellerId);
        buyerData.put("otherUserName", sellerDetails.getName());
        buyerData.put("propertyName", effectivePropertyName);
        if (!TextUtils.isEmpty(propertyId)) buyerData.put("propertyId", propertyId);
        buyerData.put("unreadCount", 0); // Buyer just sent/saw it

        buyerSessionRef.set(buyerData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Buyer session updated successfully on send"))
                .addOnFailureListener(e -> Log.e(TAG, "Buyer session update failed on send", e));

        // Update seller's session (they are the receiver)
        DocumentReference sellerSessionRef = db.collection("users")
                .document(sellerId)
                .collection("chat_sessions").document(chatId);
        Map<String, Object> sellerData = new HashMap<>();
        sellerData.put("lastMessage", lastMessage);
        sellerData.put("timestamp", FieldValue.serverTimestamp());
        sellerData.put("otherUserId", currentFirebaseUser.getUid());
        sellerData.put("otherUserName", currentBuyerDetails.getName());
        sellerData.put("propertyName", effectivePropertyName);
        if (!TextUtils.isEmpty(propertyId)) sellerData.put("propertyId", propertyId);
        sellerData.put("unreadCount", FieldValue.increment(1)); // Increment for receiver

        sellerSessionRef.set(sellerData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Seller session updated successfully for received message"))
                .addOnFailureListener(e -> Log.e(TAG, "Seller session update failed for received message", e));
    }


    private void listenToChatSessionStatusBuyer() {
        // Buyer might want to know if the seller closed the conversation or changed offer status
        DocumentReference buyerSessionViewRef = db.collection("users")
                .document(currentFirebaseUser.getUid()) // Listening to their own session document
                .collection("chat_sessions").document(chatId);

        chatSessionStatusListenerBuyer = buyerSessionViewRef.addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "Buyer: Session status listener failed", e);
                return;
            }
            if (snap != null && snap.exists()) {
                Boolean closed = snap.getBoolean("conversationClosed");
                String offerStatus = snap.getString("offerStatus"); // Could be set by seller's actions

                boolean isConversationClosed = Boolean.TRUE.equals(closed);
                etMessageInputBuyer.setEnabled(!isConversationClosed);
                btnSendMessageActionBuyer.setEnabled(!isConversationClosed);

                if (isConversationClosed) {
                    Toast.makeText(Chat_Buyer.this, "This conversation has been closed by the seller.", Toast.LENGTH_LONG).show();
                } else if (offerStatus != null && !offerStatus.equals("pending")) {
                    // Example: "Offer accepted by seller!"
                    // Toast.makeText(Chat_Buyer.this, "Seller updated offer status to: " + offerStatus, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListenerRegistrationBuyer != null) messagesListenerRegistrationBuyer.remove();
        if (chatSessionStatusListenerBuyer != null) chatSessionStatusListenerBuyer.remove();
    }
}
