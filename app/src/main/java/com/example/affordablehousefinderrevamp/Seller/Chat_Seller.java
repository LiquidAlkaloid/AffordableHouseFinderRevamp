package com.example.affordablehousefinderrevamp.Seller;

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
import com.example.affordablehousefinderrevamp.User;
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
import java.util.Date; // Added import for java.util.Date
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Chat_Seller extends AppCompatActivity {
    private static final String TAG = "Chat_Seller_Individual";

    private TextView tvBuyerNameHeader, tvBuyerEmailHeader;
    private ImageButton icBackHeader;
    private ImageView imgHousePropertyCardSeller;
    private TextView tvPropertyTitleCardSeller, tvPropertyPriceCardSeller, tvPropertyLocationCardSeller;
    private Button btnViewPropertyDetailsCardSeller, btnAcceptOfferAction, btnDeclineOfferAction;
    private View cardOfferViewPropertySeller;
    private RecyclerView recyclerViewMessagesChatSeller;
    private EditText etMessageInputSeller;
    private ImageButton btnSendMessageActionSeller;

    private FirebaseFirestore db;
    private FirebaseUser currentFirebaseUser;
    private User currentSellerDetails;
    private User buyerDetails;

    private String buyerId;
    private String propertyId;
    private String propertyNameFromIntent;
    private String propertyNameForDisplay;
    private String chatId;

    private MessageAdapter messageAdapterSeller;
    private List<Message> messageListSeller;
    private ListenerRegistration messagesListenerRegistrationSeller;
    private ListenerRegistration chatSessionStatusListenerSeller;

    interface UserDetailsCallback { void onCallback(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_admin_or_seller);

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

        if (TextUtils.isEmpty(buyerId)) {
            Toast.makeText(this, "Buyer information missing; cannot proceed.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (TextUtils.isEmpty(chatId)) {
            chatId = generateChatId(currentFirebaseUser.getUid(), buyerId,
                    !TextUtils.isEmpty(propertyId) ? propertyId : "general_inquiry");
        }
        if (chatId.startsWith("error_")) {
            Toast.makeText(this, "Invalid chat session parameters.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupChatRecyclerViewSeller();
        setupUIListeners();

        loadCurrentSellerDetails(() -> loadBuyerDetailsForHeader(() -> {
            if (currentSellerDetails == null || buyerDetails == null) {
                // Error logged, UI might be partially updated or show placeholders
            }
            if (!TextUtils.isEmpty(propertyId)) {
                loadPropertyDetailsForCard();
            } else {
                cardOfferViewPropertySeller.setVisibility(View.GONE);
                propertyNameForDisplay = "General Inquiry";
                createOrUpdateChatSessionMetadata();
            }
            listenToMessages();
            listenToChatSessionStatusSeller();
        }));
    }

    private void initUI() {
        tvBuyerNameHeader    = findViewById(R.id.tv_name);
        tvBuyerEmailHeader   = findViewById(R.id.tv_email);
        icBackHeader         = findViewById(R.id.ic_back);
        cardOfferViewPropertySeller = findViewById(R.id.card_offer);
        imgHousePropertyCardSeller  = findViewById(R.id.img_house);
        tvPropertyTitleCardSeller   = findViewById(R.id.tv_title);
        tvPropertyPriceCardSeller   = findViewById(R.id.tv_price);
        tvPropertyLocationCardSeller= findViewById(R.id.tv_location);
        btnViewPropertyDetailsCardSeller = findViewById(R.id.btn_view);
        btnAcceptOfferAction     = findViewById(R.id.btn_accept);
        btnDeclineOfferAction    = findViewById(R.id.btn_decline);
        recyclerViewMessagesChatSeller = findViewById(R.id.recycler_view_messages_seller);
        etMessageInputSeller     = findViewById(R.id.et_message);
        btnSendMessageActionSeller = findViewById(R.id.btn_send_message_seller);
    }

    private void readIntentData() {
        Intent intent = getIntent();
        chatId      = intent.getStringExtra("chatId");
        buyerId     = intent.getStringExtra("buyerId");
        propertyId  = intent.getStringExtra("propertyId");
        propertyNameFromIntent = intent.getStringExtra("propertyName");
        Log.d(TAG, "Intent → chatId:" + chatId + " buyerId:" + buyerId + " propertyId:" + propertyId + " propNameIntent:" + propertyNameFromIntent);
    }

    private void setupUIListeners() {
        icBackHeader.setOnClickListener(v -> onBackPressed());

        if (!TextUtils.isEmpty(propertyId)) {
            btnViewPropertyDetailsCardSeller.setVisibility(View.VISIBLE);
            btnViewPropertyDetailsCardSeller.setOnClickListener(v -> {
                Intent i = new Intent(this, SellerPropertyDetails.class);
                i.putExtra("propertyId", propertyId);
                startActivity(i);
            });
        } else {
            btnViewPropertyDetailsCardSeller.setVisibility(View.GONE);
        }

        btnAcceptOfferAction.setOnClickListener(v -> handleOffer("accepted"));
        btnDeclineOfferAction.setOnClickListener(v -> handleOffer("declined"));
        btnSendMessageActionSeller.setOnClickListener(v -> sendMessageSeller());
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

    private void loadCurrentSellerDetails(UserDetailsCallback cb) {
        db.collection("users").document(currentFirebaseUser.getUid())
                .get().addOnSuccessListener(doc -> {
                    currentSellerDetails = doc.toObject(User.class);
                    cb.onCallback();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading seller details", e);
                    cb.onCallback();
                });
    }

    private void loadBuyerDetailsForHeader(UserDetailsCallback cb) {
        db.collection("users").document(buyerId)
                .get().addOnSuccessListener(doc -> {
                    buyerDetails = doc.toObject(User.class);
                    if (buyerDetails != null) {
                        tvBuyerNameHeader.setText(buyerDetails.getName());
                        tvBuyerEmailHeader.setText(buyerDetails.getEmail());
                    } else {
                        tvBuyerNameHeader.setText("Buyer");
                    }
                    cb.onCallback();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading buyer details", e);
                    tvBuyerNameHeader.setText("Buyer");
                    cb.onCallback();
                });
    }

    private void loadPropertyDetailsForCard() {
        cardOfferViewPropertySeller.setVisibility(View.VISIBLE);
        db.collection("Properties").document(propertyId)
                .get().addOnSuccessListener(doc -> {
                    Property p = doc.toObject(Property.class);
                    if (p != null) {
                        propertyNameForDisplay = p.getName();
                        tvPropertyTitleCardSeller.setText(p.getName());
                        tvPropertyPriceCardSeller.setText(p.getPrice());
                        tvPropertyLocationCardSeller.setText(p.getLocation());
                        String img = p.getPrimaryImageUrl();
                        if (!TextUtils.isEmpty(img)) {
                            Glide.with(this).load(img)
                                    .placeholder(R.drawable.placeholder_image)
                                    .error(R.drawable.placeholder_image)
                                    .into(imgHousePropertyCardSeller);
                        } else {
                            imgHousePropertyCardSeller.setImageResource(R.drawable.placeholder_image);
                        }
                    } else {
                        propertyNameForDisplay = !TextUtils.isEmpty(propertyNameFromIntent) ? propertyNameFromIntent : "Property Inquiry";
                        tvPropertyTitleCardSeller.setText(propertyNameForDisplay);
                    }
                    createOrUpdateChatSessionMetadata();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading property card details", e);
                    cardOfferViewPropertySeller.setVisibility(View.GONE);
                    propertyNameForDisplay = !TextUtils.isEmpty(propertyNameFromIntent) ? propertyNameFromIntent : "Property Inquiry";
                    createOrUpdateChatSessionMetadata();
                });
    }

    private void setupChatRecyclerViewSeller() {
        messageListSeller   = new ArrayList<>();
        // Corrected: MessageAdapter constructor now expects (Context, List<Message>, String currentUserId)
        messageAdapterSeller = new MessageAdapter(this, messageListSeller, currentFirebaseUser.getUid());
        recyclerViewMessagesChatSeller.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerViewMessagesChatSeller.setAdapter(messageAdapterSeller);
    }

    private void listenToMessages() {
        if (chatId.startsWith("error_")) return;
        CollectionReference msgsRef = db.collection("chats")
                .document(chatId).collection("messages");
        messagesListenerRegistrationSeller = msgsRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Messages listen failed", e);
                        Toast.makeText(this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (queryDocumentSnapshots != null) {
                        messageListSeller.clear();
                        for (Message m : queryDocumentSnapshots.toObjects(Message.class)) {
                            messageListSeller.add(m);
                        }
                        messageAdapterSeller.notifyDataSetChanged();
                        if (!messageListSeller.isEmpty()) {
                            recyclerViewMessagesChatSeller.smoothScrollToPosition(messageListSeller.size() - 1);
                        }
                    }
                });
    }

    private void sendMessageSeller() {
        String text = etMessageInputSeller.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Cannot send empty message", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentFirebaseUser == null || TextUtils.isEmpty(buyerId)) {
            Toast.makeText(this, "Cannot send message. User info missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Corrected: Message constructor now expects (senderId, receiverId, text, timestamp as Date)
        Message newMessage = new Message(currentFirebaseUser.getUid(), buyerId, text, new Date());
        db.collection("chats").document(chatId).collection("messages")
                .add(newMessage).addOnSuccessListener(docRef -> {
                    etMessageInputSeller.setText("");
                    updateChatSessionMetadataOnSend(text);
                    Log.d(TAG, "Message sent successfully by seller");
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message by seller", e);
                    Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                });
    }

    private void createOrUpdateChatSessionMetadata() {
        if (currentFirebaseUser == null || TextUtils.isEmpty(buyerId) || buyerDetails == null) {
            Log.e(TAG, "Cannot create/update chat session metadata: missing user details.");
            return;
        }

        DocumentReference sellerSessionRef = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);

        Map<String,Object> data = new HashMap<>();
        data.put("otherUserId", buyerId);
        data.put("otherUserName", buyerDetails.getName());
        if (!TextUtils.isEmpty(propertyId)) data.put("propertyId", propertyId);
        data.put("propertyName", TextUtils.isEmpty(propertyNameForDisplay) ? "General Inquiry" : propertyNameForDisplay);
        data.put("offerStatus", "pending");
        data.put("conversationClosed", false);
        data.put("unreadCount", 0);
        data.put("timestamp", FieldValue.serverTimestamp());

        sellerSessionRef.set(data, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Seller chat session metadata created/updated."))
                .addOnFailureListener(e -> Log.e(TAG, "Seller chat session metadata update failed.", e));

        if (currentSellerDetails != null) {
            DocumentReference buyerSessionRef = db.collection("users")
                    .document(buyerId)
                    .collection("chat_sessions").document(chatId);
            Map<String, Object> buyerMetadata = new HashMap<>(data);
            buyerMetadata.put("otherUserId", currentFirebaseUser.getUid());
            buyerMetadata.put("otherUserName", currentSellerDetails.getName());
            buyerSessionRef.set(buyerMetadata, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Buyer chat session metadata also updated for consistency."))
                    .addOnFailureListener(e -> Log.e(TAG, "Buyer chat session metadata update consistency failed.", e));
        }
    }


    private void updateChatSessionMetadataOnSend(String lastMessage) {
        if (currentSellerDetails == null || TextUtils.isEmpty(buyerId) || buyerDetails == null) {
            Log.e(TAG, "Cannot update chat metadata on send: missing user details");
            return;
        }

        String effectivePropertyName = TextUtils.isEmpty(propertyNameForDisplay) ? "General Inquiry" : propertyNameForDisplay;

        DocumentReference sellerSessionRef = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);
        Map<String, Object> sellerData = new HashMap<>();
        sellerData.put("lastMessage", lastMessage);
        sellerData.put("timestamp", FieldValue.serverTimestamp());
        sellerData.put("otherUserId", buyerId);
        sellerData.put("otherUserName", buyerDetails.getName());
        sellerData.put("propertyName", effectivePropertyName);
        if (!TextUtils.isEmpty(propertyId)) sellerData.put("propertyId", propertyId);
        sellerData.put("unreadCount", 0);

        sellerSessionRef.set(sellerData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Seller session updated successfully on send"))
                .addOnFailureListener(e -> Log.e(TAG, "Seller session update failed on send", e));

        DocumentReference buyerSessionRef = db.collection("users")
                .document(buyerId)
                .collection("chat_sessions").document(chatId);
        Map<String, Object> buyerData = new HashMap<>();
        buyerData.put("lastMessage", lastMessage);
        buyerData.put("timestamp", FieldValue.serverTimestamp());
        buyerData.put("otherUserId", currentFirebaseUser.getUid());
        buyerData.put("otherUserName", currentSellerDetails.getName());
        buyerData.put("propertyName", effectivePropertyName);
        if (!TextUtils.isEmpty(propertyId)) buyerData.put("propertyId", propertyId);
        buyerData.put("unreadCount", FieldValue.increment(1));

        buyerSessionRef.set(buyerData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Buyer session updated successfully for received message"))
                .addOnFailureListener(e -> Log.e(TAG, "Buyer session update failed for received message", e));
    }


    private void handleOffer(String status) {
        String offerMsg = "Seller has " + status + " the offer on “"
                + (TextUtils.isEmpty(propertyNameForDisplay) ? "this property" : propertyNameForDisplay) + "”.";

        DocumentReference sellerSessRef = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);
        Map<String,Object> updSeller = new HashMap<>();
        updSeller.put("offerStatus", status);
        updSeller.put("conversationClosed", true);
        updSeller.put("lastMessage", offerMsg);
        updSeller.put("timestamp", FieldValue.serverTimestamp());
        updSeller.put("unreadCount", 0);

        sellerSessRef.set(updSeller, SetOptions.merge())
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Offer " + status, Toast.LENGTH_SHORT).show();

                    DocumentReference buyerSessRef  = db.collection("users")
                            .document(buyerId)
                            .collection("chat_sessions").document(chatId);
                    Map<String,Object> updBuyer = new HashMap<>(updSeller);
                    updBuyer.put("otherUserId", currentFirebaseUser.getUid());
                    if (currentSellerDetails != null) {
                        updBuyer.put("otherUserName", currentSellerDetails.getName());
                    }
                    updBuyer.put("unreadCount", FieldValue.increment(1));

                    buyerSessRef.set(updBuyer, SetOptions.merge())
                            .addOnFailureListener(e -> Log.e(TAG, "Buyer offer session update failed", e));

                    // Corrected: Message constructor now expects (senderId, receiverId, text, timestamp as Date)
                    Message sysMessage = new Message(currentFirebaseUser.getUid(), buyerId, offerMsg + " (system notification)", new Date());
                    db.collection("chats").document(chatId).collection("messages")
                            .add(sysMessage)
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to log system message for offer", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Seller offer session update failed", e));
    }

    private void listenToChatSessionStatusSeller() {
        DocumentReference statusRef = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);

        chatSessionStatusListenerSeller = statusRef.addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "Session status listener failed", e);
                return;
            }
            if (snap != null && snap.exists()) {
                Boolean closed = snap.getBoolean("conversationClosed");
                boolean isConversationClosed = Boolean.TRUE.equals(closed);
                etMessageInputSeller.setEnabled(!isConversationClosed);
                btnSendMessageActionSeller.setEnabled(!isConversationClosed);
                btnAcceptOfferAction.setEnabled(!isConversationClosed);
                btnDeclineOfferAction.setEnabled(!isConversationClosed);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messagesListenerRegistrationSeller != null) messagesListenerRegistrationSeller.remove();
        if (chatSessionStatusListenerSeller != null)   chatSessionStatusListenerSeller.remove();
    }
}
