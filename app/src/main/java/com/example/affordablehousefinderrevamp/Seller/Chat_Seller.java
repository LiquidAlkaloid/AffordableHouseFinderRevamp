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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
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
    private String propertyName;
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
            Toast.makeText(this, "Invalid chat session.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setupChatRecyclerViewSeller();
        setupUIListeners();

        loadCurrentSellerDetails(() -> loadBuyerDetailsForHeader(() -> {
            if (currentSellerDetails == null || buyerDetails == null) {
                Toast.makeText(this, "Failed to load profiles.", Toast.LENGTH_LONG).show();
                return;
            }
            if (!TextUtils.isEmpty(propertyId)) {
                loadPropertyDetailsForCard();
            } else {
                cardOfferViewPropertySeller.setVisibility(View.GONE);
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
        propertyName= intent.getStringExtra("propertyName");
        Log.d(TAG, "Intent → chatId:" + chatId
                + " buyerId:" + buyerId
                + " propertyId:" + propertyId);
    }

    private void setupUIListeners() {
        icBackHeader.setOnClickListener(v -> onBackPressed());

        if (!TextUtils.isEmpty(propertyId)) {
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
        if (u1.compareTo(u2) < 0) {
            return u1 + "_" + u2 + "_" + ctx.replaceAll("[^a-zA-Z0-9-]", "-");
        } else {
            return u2 + "_" + u1 + "_" + ctx.replaceAll("[^a-zA-Z0-9-]", "-");
        }
    }

    private void loadCurrentSellerDetails(UserDetailsCallback cb) {
        db.collection("users").document(currentFirebaseUser.getUid())
                .get().addOnSuccessListener(doc -> {
                    currentSellerDetails = doc.toObject(User.class);
                    cb.onCallback();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading seller", e);
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
                    }
                    cb.onCallback();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading buyer", e);
                    cb.onCallback();
                });
    }

    private void loadPropertyDetailsForCard() {
        cardOfferViewPropertySeller.setVisibility(View.VISIBLE);
        db.collection("properties").document(propertyId)
                .get().addOnSuccessListener(doc -> {
                    Property p = doc.toObject(Property.class);
                    if (p != null) {
                        propertyName = TextUtils.isEmpty(propertyName) ? p.getTitle() : propertyName;
                        tvPropertyTitleCardSeller   .setText(p.getTitle());
                        tvPropertyPriceCardSeller   .setText(p.getPrice());
                        tvPropertyLocationCardSeller.setText(p.getLocation());
                        String img = (p.getImageUrls()!=null && !p.getImageUrls().isEmpty())
                                ? p.getImageUrls().get(0)
                                : p.getImageUrl();
                        if (!TextUtils.isEmpty(img)) {
                            Glide.with(this).load(img)
                                    .placeholder(R.drawable.placeholder_image)
                                    .into(imgHousePropertyCardSeller);
                        }
                    }
                    createOrUpdateChatSessionMetadata();
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading property card", e);
                    cardOfferViewPropertySeller.setVisibility(View.GONE);
                    createOrUpdateChatSessionMetadata();
                });
    }

    private void setupChatRecyclerViewSeller() {
        messageListSeller   = new ArrayList<>();
        messageAdapterSeller= new MessageAdapter(this, messageListSeller);
        recyclerViewMessagesChatSeller.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerViewMessagesChatSeller.setAdapter(messageAdapterSeller);
    }

    private void listenToMessages() {
        if (TextUtils.isEmpty(chatId)) {
            return;
        }
        CollectionReference messagesRef = db.collection("chats").document(chatId).collection("messages");
        messagesListenerRegistrationSeller = messagesRef.orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to load messages.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        messageListSeller.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Message message = doc.toObject(Message.class);
                            messageListSeller.add(message);
                        }
                        messageAdapterSeller.notifyDataSetChanged();
                        recyclerViewMessagesChatSeller.smoothScrollToPosition(messageListSeller.size() - 1);
                    }
                });
    }

    private void sendMessageSeller() {
        String text = etMessageInputSeller.getText().toString().trim();
        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "Cannot send empty message", Toast.LENGTH_SHORT).show();
            return;
        }

        Message m = new Message(currentFirebaseUser.getUid(), buyerId, text);
        db.collection("chats").document(chatId).collection("messages")
                .add(m).addOnSuccessListener(doc -> {
                    etMessageInputSeller.setText("");
                    updateChatSessionMetadataOnSend(text);
                    Log.d(TAG, "Message sent successfully");
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to send message", e);
                    Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                });
    }

    private void createOrUpdateChatSessionMetadata() {
        DocumentReference sellerSession = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);

        Map<String,Object> data = new HashMap<>();
        data.put("otherUserId", buyerId);
        data.put("senderName", buyerDetails.getName());
        if (!TextUtils.isEmpty(propertyId)) data.put("propertyId", propertyId);
        data.put("propertyName", TextUtils.isEmpty(propertyName) ? "General Inquiry" : propertyName);
        data.put("offerStatus", "pending");
        data.put("conversationClosed", false);
        data.put("unreadCount", 0);

        sellerSession.set(data, SetOptions.merge())
                .addOnFailureListener(e -> Log.e(TAG, "Meta create/update fail", e));
    }


    private void updateChatSessionMetadataOnSend(String lastMessage) {
        if (currentSellerDetails == null || TextUtils.isEmpty(buyerId)) {
            Log.e(TAG, "Cannot update chat metadata: missing user details");
            return;
        }

        String sellerName = currentSellerDetails.getName();
        String buyerName = buyerDetails != null ? buyerDetails.getName() : "Buyer";

        Map<String, Object> common = new HashMap<>();
        common.put("lastMessage", lastMessage);
        common.put("timestamp", FieldValue.serverTimestamp());

        // Add property information if available
        if (!TextUtils.isEmpty(propertyId)) {
            common.put("propertyId", propertyId);
        }
        if (!TextUtils.isEmpty(propertyName)) {
            common.put("propertyName", propertyName);
        }

        // Update seller's own session
        DocumentReference sellerSessionRef = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);

        Map<String, Object> sellerData = new HashMap<>(common);
        sellerData.put("senderName", buyerName);
        sellerData.put("otherUserId", buyerId);
        sellerData.put("unreadCount", 0);

        sellerSessionRef.set(sellerData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Seller session updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Seller session update failed", e));

        // Update buyer's session so they see the new message
        DocumentReference buyerSessionRef = db.collection("users")
                .document(buyerId)
                .collection("chat_sessions").document(chatId);

        Map<String, Object> buyerData = new HashMap<>(common);
        buyerData.put("senderName", sellerName);
        buyerData.put("otherUserId", currentFirebaseUser.getUid());
        buyerData.put("unreadCount", FieldValue.increment(1));

        buyerSessionRef.set(buyerData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Buyer session updated successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Buyer session update failed", e));
    }


    private void handleOffer(String status) {
        String offerMsg = "Seller has " + status + " the offer on “"
                + (TextUtils.isEmpty(propertyName) ? "this property" : propertyName) + "”.";
        DocumentReference sellerSess = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);
        DocumentReference buyerSess  = db.collection("users")
                .document(buyerId)
                .collection("chat_sessions").document(chatId);

        Map<String,Object> updSeller = new HashMap<>();
        updSeller.put("offerStatus", status);
        updSeller.put("conversationClosed", true);
        updSeller.put("lastMessage", offerMsg);
        updSeller.put("timestamp", FieldValue.serverTimestamp());
        updSeller.put("unreadCount", 0);

        sellerSess.set(updSeller, SetOptions.merge())
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Offer " + status, Toast.LENGTH_SHORT).show();

                    Map<String,Object> updBuyer = new HashMap<>(updSeller);
                    updBuyer.put("senderName", currentSellerDetails.getName());
                    updBuyer.put("unreadCount", FieldValue.increment(1));

                    buyerSess.set(updBuyer, SetOptions.merge())
                            .addOnFailureListener(e -> Log.e(TAG, "Buyer offer update failed", e));

                    // log a system message in chat history
                    Message sys = new Message(currentFirebaseUser.getUid(), buyerId,
                            offerMsg + " (system)");
                    db.collection("chats").document(chatId).collection("messages")
                            .add(sys)
                            .addOnFailureListener(e -> Log.e(TAG, "Failed to log system message", e));
                })
                .addOnFailureListener(e -> Log.e(TAG, "Seller offer update failed", e));
    }

    private void listenToChatSessionStatusSeller() {
        DocumentReference statusRef = db.collection("users")
                .document(currentFirebaseUser.getUid())
                .collection("chat_sessions").document(chatId);

        chatSessionStatusListenerSeller = statusRef.addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "Session status listener fail", e);
                return;
            }
            if (snap != null && snap.exists()) {
                Boolean closed = snap.getBoolean("conversationClosed");
                String offerS = snap.getString("offerStatus");
                // update UI buttons accordingly…
                etMessageInputSeller.setEnabled(!Boolean.TRUE.equals(closed));
                btnSendMessageActionSeller.setEnabled(!Boolean.TRUE.equals(closed));
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
