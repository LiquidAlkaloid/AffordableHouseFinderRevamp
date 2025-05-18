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
import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.affordablehousefinderrevamp.Adapter.MessageAdapter;
import com.example.affordablehousefinderrevamp.Model.Message;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.example.affordablehousefinderrevamp.User;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat_Buyer extends AppCompatActivity {

    private static final String TAG = "Chat_Buyer_Individual";

    private TextView tvSellerNameHeader, tvSellerEmailHeader;
    private ImageView icBackHeader, imgHousePropertyCard;
    private TextView tvPropertyTitleCard, tvPropertyPriceCard, tvPropertyLocationCard;
    private Button btnViewPropertyDetailsCard;
    private View cardOfferViewProperty;
    private RecyclerView recyclerViewMessagesChat;
    private EditText etMessageInput;
    private ImageButton btnSendMessageAction;

    private FirebaseFirestore db;
    private FirebaseUser currentFirebaseUser;
    private User currentBuyerDetails;
    private User sellerDetails;

    private String sellerId;
    private String propertyId;
    private String propertyName; // Name of the property, fetched if propertyId exists
    private String chatId;       // Unique ID for the chat session

    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private ListenerRegistration chatSessionStatusListener;
    private ListenerRegistration messagesListenerRegistration;

    // Callback interface for asynchronous user detail loading
    interface UserDetailsCallback {
        void onCallback();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_buyer);

        db = FirebaseFirestore.getInstance();
        currentFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // Critical check: Ensure user is authenticated
        if (currentFirebaseUser == null || TextUtils.isEmpty(currentFirebaseUser.getUid())) {
            Toast.makeText(this, "Authentication error. Please log in again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "CRITICAL: currentFirebaseUser or UID is null/empty at onCreate. Cannot proceed.");
            finish();
            return;
        }
        Log.d(TAG, "Authenticated user UID: " + currentFirebaseUser.getUid());

        initializeUI();
        getIntentData(); // Get sellerId, propertyId, etc.

        // Critical check: Ensure sellerId is present
        if (TextUtils.isEmpty(sellerId)) {
            Toast.makeText(this, "Seller information is missing. Cannot proceed.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "CRITICAL: Seller ID is null or empty from intent. Cannot proceed.");
            finish();
            return;
        }
        Log.d(TAG, "Seller ID from intent: " + sellerId);

        // Determine contextId for chat. Use a safe default if propertyId is missing.
        String contextId = !TextUtils.isEmpty(propertyId) ? propertyId : "general_inquiry";
        Log.d(TAG, "Context ID for chat: " + contextId);


        // Generate chatId if not passed via intent or if it's invalid
        // It's crucial that currentFirebaseUser.getUid() and sellerId are valid here.
        if (TextUtils.isEmpty(chatId)) {
            chatId = generateChatId(currentFirebaseUser.getUid(), sellerId, contextId);
        }
        Log.i(TAG, "FINAL chatId: " + chatId + " (Buyer: " + currentFirebaseUser.getUid() + ", Seller: " + sellerId + ", Context: " + contextId + ")");

        if (chatId.startsWith("error_")) {
            Toast.makeText(this, "Failed to initialize chat session due to invalid chat ID components.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "CRITICAL: ChatId generation resulted in an error state: " + chatId + ". Aborting chat setup.");
            finish();
            return;
        }

        setupChatRecyclerView();
        setupUIListeners();

        // Chain loading of details: Buyer -> Seller -> Property -> Metadata -> Messages
        loadCurrentBuyerDetails(() -> {
            loadSellerDetailsForHeader(() -> {
                // Ensure both buyer and seller details are loaded before proceeding
                if (currentBuyerDetails == null) {
                    Log.e(TAG, "CRITICAL: CurrentBuyerDetails not loaded. Cannot proceed with chat setup.");
                    Toast.makeText(Chat_Buyer.this, "Failed to load your user details. Please try again.", Toast.LENGTH_LONG).show();
                    // Consider finishing or providing a retry mechanism
                    return;
                }
                if (sellerDetails == null) {
                    Log.e(TAG, "CRITICAL: SellerDetails not loaded. Cannot proceed with chat setup.");
                    Toast.makeText(Chat_Buyer.this, "Failed to load seller details. Please try again.", Toast.LENGTH_LONG).show();
                    // Consider finishing or providing a retry mechanism
                    return;
                }

                if (propertyId != null && !propertyId.isEmpty()) {
                    loadPropertyDetailsForCard(); // This will call createOrUpdateChatSessionMetadata after loading
                } else {
                    if (cardOfferViewProperty != null) cardOfferViewProperty.setVisibility(View.GONE);
                    createOrUpdateChatSessionMetadata(); // Call if no property involved
                }
                // Load messages and listen to status only after essential details and metadata attempt
                loadMessages();
                listenToChatSessionStatus();
            });
        });
    }

    private void initializeUI() {
        tvSellerNameHeader = findViewById(R.id.tv_name);
        tvSellerEmailHeader = findViewById(R.id.tv_email);
        icBackHeader = findViewById(R.id.ic_back);
        cardOfferViewProperty = findViewById(R.id.card_offer);
        imgHousePropertyCard = findViewById(R.id.img_house);
        tvPropertyTitleCard = findViewById(R.id.tv_title);
        tvPropertyPriceCard = findViewById(R.id.tv_price);
        tvPropertyLocationCard = findViewById(R.id.tv_location);
        btnViewPropertyDetailsCard = findViewById(R.id.btn_view);
        recyclerViewMessagesChat = findViewById(R.id.recycler_view_messages_buyer);
        etMessageInput = findViewById(R.id.et_message);
        btnSendMessageAction = findViewById(R.id.btn_send_message_buyer);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        sellerId = intent.getStringExtra("sellerId");
        propertyId = intent.getStringExtra("propertyId");
        propertyName = intent.getStringExtra("propertyName");
        chatId = intent.getStringExtra("chatId");
        Log.d(TAG, "Intent data - sellerId: " + sellerId + ", propertyId: " + propertyId + ", propertyName: " + propertyName + ", chatId: " + chatId);
    }

    private void setupUIListeners() {
        if (icBackHeader != null) icBackHeader.setOnClickListener(v -> onBackPressed());

        if (btnViewPropertyDetailsCard != null) {
            if (propertyId != null && !propertyId.isEmpty()) {
                btnViewPropertyDetailsCard.setVisibility(View.VISIBLE);
                btnViewPropertyDetailsCard.setOnClickListener(v -> {
                    Intent detailIntent = new Intent(Chat_Buyer.this, PropertyDetail.class);
                    detailIntent.putExtra("propertyId", propertyId);
                    startActivity(detailIntent);
                });
            } else {
                btnViewPropertyDetailsCard.setVisibility(View.GONE);
            }
        }
        if (btnSendMessageAction != null) btnSendMessageAction.setOnClickListener(v -> sendMessage());
    }

    private String generateChatId(String userId1, String userId2, String contextId) {
        Log.d(TAG, "generateChatId inputs - userId1: " + userId1 + ", userId2: " + userId2 + ", contextId: " + contextId);
        if (TextUtils.isEmpty(userId1) || TextUtils.isEmpty(userId2) || TextUtils.isEmpty(contextId)) {
            Log.e(TAG, "generateChatId: Cannot generate chatId with null or empty UIDs or contextId.");
            return "error_chat_id_" + System.currentTimeMillis(); // Return an error state
        }
        String u1, u2;
        // Ensure consistent ordering of UIDs
        if (userId1.compareTo(userId2) < 0) {
            u1 = userId1;
            u2 = userId2;
        } else {
            u1 = userId2;
            u2 = userId1;
        }
        // Sanitize contextId to prevent characters that might interfere with Firestore paths or rule splitting.
        String sanitizedContextId = contextId.replaceAll("[^a-zA-Z0-9-]", "-");
        if (TextUtils.isEmpty(sanitizedContextId)) { // Fallback if sanitization results in empty
            sanitizedContextId = "general"; // Use a default if context becomes empty after sanitization
            Log.w(TAG, "generateChatId: Sanitized contextId was empty, using 'general'. Original: " + contextId);
        }
        String generatedId = u1 + "_" + u2 + "_" + sanitizedContextId;
        Log.d(TAG, "generateChatId output: " + generatedId);
        return generatedId;
    }

    private void loadCurrentBuyerDetails(UserDetailsCallback callback) {
        // currentFirebaseUser and UID checked in onCreate
        Log.d(TAG, "Loading current buyer details for UID: " + currentFirebaseUser.getUid());
        db.collection("users").document(currentFirebaseUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentBuyerDetails = documentSnapshot.toObject(User.class);
                        Log.i(TAG, "Current buyer details loaded: " + (currentBuyerDetails != null ? currentBuyerDetails.getName() : "Name N/A"));
                    } else {
                        Log.w(TAG, "Current buyer's user document not found in Firestore. UID: " + currentFirebaseUser.getUid());
                        Toast.makeText(this, "Your user profile is incomplete.", Toast.LENGTH_SHORT).show();
                    }
                    if (callback != null) callback.onCallback();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch current buyer's details for UID: " + currentFirebaseUser.getUid(), e);
                    Toast.makeText(this, "Failed to load your profile.", Toast.LENGTH_SHORT).show();
                    if (callback != null) callback.onCallback(); // Proceed even on failure to allow other parts to attempt loading
                });
    }

    private void loadSellerDetailsForHeader(UserDetailsCallback callback) {
        // sellerId checked in onCreate
        Log.d(TAG, "Loading seller details for header, ID: " + sellerId);
        db.collection("users").document(sellerId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        sellerDetails = documentSnapshot.toObject(User.class);
                        if (sellerDetails != null) {
                            if (tvSellerNameHeader != null) tvSellerNameHeader.setText(sellerDetails.getName());
                            if (tvSellerEmailHeader != null) tvSellerEmailHeader.setText(sellerDetails.getEmail());
                            Log.i(TAG, "Seller details loaded for header: " + sellerDetails.getName());
                        } else {
                            Log.w(TAG, "Seller details could not be parsed for ID: " + sellerId);
                            if (tvSellerNameHeader != null) tvSellerNameHeader.setText("Seller"); // Fallback
                        }
                    } else {
                        Log.w(TAG, "Seller document does not exist for ID: " + sellerId);
                        if (tvSellerNameHeader != null) tvSellerNameHeader.setText("Seller Not Found");
                    }
                    if (callback != null) callback.onCallback();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching seller details for header, ID: " + sellerId, e);
                    if (tvSellerNameHeader != null) tvSellerNameHeader.setText("Error Loading Seller");
                    if (callback != null) callback.onCallback();
                });
    }

    private void loadPropertyDetailsForCard() {
        if (TextUtils.isEmpty(propertyId)) {
            if(cardOfferViewProperty != null) cardOfferViewProperty.setVisibility(View.GONE);
            createOrUpdateChatSessionMetadata(); // Still ensure buyer's metadata exists
            return;
        }
        if(cardOfferViewProperty != null) cardOfferViewProperty.setVisibility(View.VISIBLE);
        Log.d(TAG, "Loading property details for card, ID: " + propertyId);

        db.collection("properties").document(propertyId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Property property = documentSnapshot.toObject(Property.class);
                        if (property != null) {
                            if (TextUtils.isEmpty(propertyName)) { // If not passed via intent
                                propertyName = property.getTitle();
                            }
                            if (tvPropertyTitleCard != null) tvPropertyTitleCard.setText(property.getTitle());
                            if (tvPropertyPriceCard != null) tvPropertyPriceCard.setText(property.getPrice());
                            if (tvPropertyLocationCard != null) tvPropertyLocationCard.setText(property.getLocation());
                            if (imgHousePropertyCard != null) {
                                String imageUrl = null;
                                if (property.getImageUrls() != null && !property.getImageUrls().isEmpty()) {
                                    imageUrl = property.getImageUrls().get(0); // Get the first image URL
                                } else if (property.getImageUrl() != null && !property.getImageUrl().isEmpty()) {
                                    imageUrl = property.getImageUrl();
                                }

                                if (!TextUtils.isEmpty(imageUrl)) {
                                    Glide.with(this)
                                            .load(imageUrl) // Load direct URL
                                            .placeholder(R.drawable.placeholder_image)
                                            .error(R.drawable.placeholder_image)
                                            .into(imgHousePropertyCard);
                                } else {
                                    imgHousePropertyCard.setImageResource(R.drawable.placeholder_image);
                                }
                            }
                        } else {
                            Log.w(TAG, "Property data could not be parsed for card: " + propertyId);
                            if(cardOfferViewProperty != null) cardOfferViewProperty.setVisibility(View.GONE);
                        }
                    } else {
                        Log.w(TAG, "Property document not found for card: " + propertyId);
                        if(cardOfferViewProperty != null) cardOfferViewProperty.setVisibility(View.GONE);
                    }
                    createOrUpdateChatSessionMetadata(); // Call after property details attempt
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching property details for card", e);
                    if(cardOfferViewProperty != null) cardOfferViewProperty.setVisibility(View.GONE);
                    createOrUpdateChatSessionMetadata(); // Still try to create buyer's metadata
                });
    }

    private void setupChatRecyclerView() {
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerViewMessagesChat.setLayoutManager(layoutManager);
        recyclerViewMessagesChat.setAdapter(messageAdapter);
    }

    private void loadMessages() {
        if (TextUtils.isEmpty(chatId) || chatId.startsWith("error_")) {
            Log.e(TAG, "loadMessages: Cannot load messages, invalid or missing chatId: " + chatId);
            return;
        }
        CollectionReference messagesRef = db.collection("chats").document(chatId).collection("messages");
        Log.i(TAG, "LOAD_MESSAGES: Attaching listener to: " + messagesRef.getPath() + " for current user: " + currentFirebaseUser.getUid());

        if (messagesListenerRegistration != null) { // Remove previous listener if any
            messagesListenerRegistration.remove();
        }
        messagesListenerRegistration = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "LOAD_MESSAGES_ERROR: Listen failed for messages. Path: " + messagesRef.getPath() + ". Error: " + e.getMessage(), e);
                        if (e.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            Toast.makeText(Chat_Buyer.this, "Permission denied to read messages. Check Firestore rules and chatId.", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "LOAD_MESSAGES_ERROR: PERMISSION_DENIED. ChatId: " + chatId + ", Auth UID: " + currentFirebaseUser.getUid() + ". Ensure this UID is a participant in the chatId and rules allow read.");
                        } else {
                            Toast.makeText(Chat_Buyer.this, "Error loading messages.", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    if (snapshots != null) {
                        messageList.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Message message = doc.toObject(Message.class);
                            message.setMessageId(doc.getId()); // Store document ID
                            messageList.add(message);
                        }
                        messageAdapter.notifyDataSetChanged();
                        if (!messageList.isEmpty()) {
                            recyclerViewMessagesChat.smoothScrollToPosition(messageList.size() - 1);
                        }
                        Log.d(TAG, "LOAD_MESSAGES_SUCCESS: " + messageList.size() + " messages loaded for chatId: " + chatId);
                    } else {
                        Log.d(TAG, "LOAD_MESSAGES: Message snapshots are null for chatId: " + chatId);
                    }
                });
    }

    private void sendMessage() {
        String messageText = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(messageText)) {
            Toast.makeText(this, "Cannot send an empty message.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Ensure currentUser is initialized
        if (currentBuyerDetails == null || TextUtils.isEmpty(sellerId) || TextUtils.isEmpty(chatId)) {
            Toast.makeText(this, "Cannot send message: session or user details missing.", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = currentBuyerDetails.getId();
        Message message = new Message(currentUserId, sellerId, messageText);
        db.collection("chats").document(chatId).collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    etMessageInput.setText("");
                    updateChatSessionMetadataOnSend(messageText);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Chat_Buyer.this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                });
    }


    private void createOrUpdateChatSessionMetadata() {
        // Critical info checked in onCreate and user detail loading callbacks
        if (currentFirebaseUser == null || TextUtils.isEmpty(currentFirebaseUser.getUid()) || TextUtils.isEmpty(sellerId) || TextUtils.isEmpty(chatId) || chatId.startsWith("error_") || sellerDetails == null) {
            Log.w(TAG, "createOrUpdateChatSessionMetadata (Buyer): Skipping metadata update, critical information or sellerDetails missing. ChatID: " + chatId);
            return;
        }

        String effectivePropertyName = !TextUtils.isEmpty(propertyName) ? propertyName : "General Inquiry";
        String sellerNameForBuyerList = !TextUtils.isEmpty(sellerDetails.getName()) ? sellerDetails.getName() : "Seller";

        DocumentReference buyerSessionRef = db.collection("users").document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);

        Map<String, Object> buyerSessionData = new HashMap<>();
        buyerSessionData.put("otherUserId", sellerId);
        buyerSessionData.put("senderName", sellerNameForBuyerList);
        // Profile image URL handling removed
        if (!TextUtils.isEmpty(propertyId)) {
            buyerSessionData.put("propertyId", propertyId);
        }
        buyerSessionData.put("propertyName", effectivePropertyName);
        buyerSessionData.put("offerStatus", "pending");
        buyerSessionData.put("conversationClosed", false);
        buyerSessionData.put("unreadCount", 0);

        Log.i(TAG, "METADATA_BUYER_CREATE_UPDATE: Path: " + buyerSessionRef.getPath() + ", Data: " + buyerSessionData.toString());
        buyerSessionRef.set(buyerSessionData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Buyer chat session metadata ensured/updated for " + chatId))
                .addOnFailureListener(e -> Log.e(TAG, "Error ensuring buyer session metadata for " + chatId, e));
    }

    private void updateChatSessionMetadataOnSend(String lastMessage) {
        // Critical info checked in onCreate and user detail loading callbacks
        if (TextUtils.isEmpty(chatId) || currentFirebaseUser == null || TextUtils.isEmpty(sellerId) ||
                chatId.startsWith("error_") || currentBuyerDetails == null || sellerDetails == null) {
            Log.e(TAG, "updateChatSessionMetadataOnSend (Buyer): Critical info missing. ChatId:" + chatId);
            return;
        }

        String buyerName = !TextUtils.isEmpty(currentBuyerDetails.getName()) ? currentBuyerDetails.getName() : "Buyer";
        String sellerName = !TextUtils.isEmpty(sellerDetails.getName()) ? sellerDetails.getName() : "Seller";

        Map<String, Object> commonUpdateData = new HashMap<>();
        commonUpdateData.put("lastMessage", lastMessage);
        commonUpdateData.put("timestamp", FieldValue.serverTimestamp());

        // Add property information to both buyer and seller metadata
        if (!TextUtils.isEmpty(propertyId)) {
            commonUpdateData.put("propertyId", propertyId);
        }
        if (!TextUtils.isEmpty(propertyName)) {
            commonUpdateData.put("propertyName", propertyName);
        }

        // 1. Update Buyer's (current user's) chat session document
        DocumentReference buyerSessionRef = db.collection("users").document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);
        Map<String, Object> buyerUpdateData = new HashMap<>(commonUpdateData);
        buyerUpdateData.put("senderName", sellerName);
        buyerUpdateData.put("otherUserId", sellerId); // Ensure otherUserId is set
        buyerUpdateData.put("unreadCount", 0);

        Log.i(TAG, "METADATA_BUYER_ON_SEND: Path: " + buyerSessionRef.getPath() + ", Data: " + buyerUpdateData.toString());
        buyerSessionRef.set(buyerUpdateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Buyer chat metadata updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update buyer's session on send for " + chatId, e));

        // 2. Update Seller's (other user's) chat session document
        DocumentReference sellerSessionRef = db.collection("users").document(sellerId)
                .collection("chat_sessions").document(chatId);
        Map<String, Object> sellerUpdateData = new HashMap<>(commonUpdateData);
        sellerUpdateData.put("senderName", buyerName);
        sellerUpdateData.put("otherUserId", currentFirebaseUser.getUid()); // Ensure otherUserId is set
        sellerUpdateData.put("unreadCount", FieldValue.increment(1));
        sellerUpdateData.put("offerStatus", "pending"); // Ensure offerStatus is set
        sellerUpdateData.put("conversationClosed", false); // Ensure conversationClosed is set

        Log.i(TAG, "METADATA_SELLER_ON_BUYER_SEND: Path: " + sellerSessionRef.getPath() + ", Data: " + sellerUpdateData.toString());
        sellerSessionRef.set(sellerUpdateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Seller chat metadata updated successfully"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update seller's session on buyer's send for " + chatId, e);
                    if (e instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.e(TAG, "PERMISSION_DENIED updating seller's chat_session by buyer. This might be due to Firestore rules restricting which fields a non-owner can write. Buyer: " + currentFirebaseUser.getUid() + " Seller's session: " + sellerId);
                    }
                });
    }


    private void listenToChatSessionStatus() {
        if (TextUtils.isEmpty(chatId) || currentFirebaseUser == null || chatId.startsWith("error_")){
            Log.e(TAG, "Cannot listen to chat session status, invalid chatId or user.");
            return;
        }
        DocumentReference buyerChatSessionForStatusRef = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);
        Log.d(TAG, "Listening to buyer's session status at: " + buyerChatSessionForStatusRef.getPath());

        if (chatSessionStatusListener != null) {
            chatSessionStatusListener.remove();
        }
        chatSessionStatusListener = buyerChatSessionForStatusRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.w(TAG, "Buyer's chat session status listen failed on path " + buyerChatSessionForStatusRef.getPath(), e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Buyer's chat session status data: " + snapshot.getData());
                Boolean conversationClosed = snapshot.getBoolean("conversationClosed");
                String offerStatus = snapshot.getString("offerStatus");

                if (Boolean.TRUE.equals(conversationClosed)) {
                    if ("declined".equals(offerStatus)) {
                        showConversationClosedDialog("The seller has declined the offer and this conversation is now closed.");
                    } else if ("accepted".equals(offerStatus)){
                        showConversationClosedDialog("The offer has been accepted and this conversation is now concluded.");
                    } else {
                        showConversationClosedDialog("This conversation has been closed.");
                    }
                    if(etMessageInput != null) etMessageInput.setEnabled(false);
                    if(btnSendMessageAction != null) btnSendMessageAction.setEnabled(false);
                } else {
                    // Ensure input is enabled if conversation is not closed
                    if(etMessageInput != null) etMessageInput.setEnabled(true);
                    if(btnSendMessageAction != null) btnSendMessageAction.setEnabled(true);
                }

                // Reset unread count as buyer is viewing the chat
                Long unreadCount = snapshot.getLong("unreadCount");
                if (unreadCount != null && unreadCount > 0) {
                    buyerChatSessionForStatusRef.update("unreadCount", 0)
                            .addOnSuccessListener(aVoid -> Log.d(TAG, "Buyer unread count reset for " + chatId))
                            .addOnFailureListener(err -> Log.e(TAG, "Failed to reset buyer unread count for " + chatId, err));
                }

            } else {
                Log.d(TAG, "Buyer's chat session document for status listener does not exist or is null: " + buyerChatSessionForStatusRef.getPath());
            }
        });
    }

    private void showConversationClosedDialog(String message) {
        if (!isFinishing() && !isDestroyed()) { // Ensure activity is active
            new AlertDialog.Builder(this)
                    .setTitle("Conversation Update")
                    .setMessage(message)
                    .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                    .setCancelable(false) // User must acknowledge
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatSessionStatusListener != null) {
            chatSessionStatusListener.remove();
        }
        if (messagesListenerRegistration != null) {
            messagesListenerRegistration.remove();
        }
    }
}
