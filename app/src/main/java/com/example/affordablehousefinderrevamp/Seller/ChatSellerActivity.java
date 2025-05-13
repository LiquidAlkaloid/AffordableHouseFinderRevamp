package com.example.affordablehousefinderrevamp.Seller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.affordablehousefinderrevamp.ChatItem;
import com.example.affordablehousefinderrevamp.R;
import com.example.affordablehousefinderrevamp.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;


import java.util.ArrayList;
import java.util.List;

public class ChatSellerActivity extends AppCompatActivity {

    private static final String TAG = "ChatSellerActivity";

    private RecyclerView recyclerViewChats;
    private ChatSellerAdapter chatSellerAdapter;
    private List<ChatItem> chatList;
    private BottomNavigationView bottomNavigationViewSeller;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration chatListenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list_seller);

        toolbar = findViewById(R.id.toolbar_chat_seller);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Chats");
            // Potentially remove logo and custom title view from toolbar if default title is preferred
            // Or, customize programmatically if needed
        }

        recyclerViewChats = findViewById(R.id.rv_chat_list_seller);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));

        chatList = new ArrayList<>();
        chatSellerAdapter = new ChatSellerAdapter(this, chatList, chatItem -> {
            // Handle chat item click: Open individual chat screen (Chat_Seller.java)
            Intent intent = new Intent(ChatSellerActivity.this, Chat_Seller.class);
            intent.putExtra("chatId", chatItem.getChatId());
            intent.putExtra("buyerId", chatItem.getOtherUserId()); // The "other user" is the buyer
            intent.putExtra("buyerName", chatItem.getSenderName()); // The "senderName" in ChatItem is the other user's name
            // Pass propertyId if available and needed by Chat_Seller
            // if (chatItem.getPropertyId() != null) { // Assuming ChatItem has propertyId
            //    intent.putExtra("propertyId", chatItem.getPropertyId());
            // }
            startActivity(intent);
        });
        recyclerViewChats.setAdapter(chatSellerAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            loadChatList();
        } else {
            Toast.makeText(this, "Please log in to view chats.", Toast.LENGTH_SHORT).show();
            // Redirect to login or handle accordingly
            // finish();
        }

        bottomNavigationViewSeller = findViewById(R.id.bottom_navigation_seller);
        if (bottomNavigationViewSeller != null) {
            bottomNavigationViewSeller.setSelectedItemId(R.id.navigation_chat); // Set Chat as selected

            bottomNavigationViewSeller.setOnNavigationItemSelectedListener(item -> {
                Intent intent = null;
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_upload) {
                    intent = new Intent(ChatSellerActivity.this, HouseListings.class);
                } else if (itemId == R.id.navigation_chat) {
                    return true; // Already here
                } else if (itemId == R.id.navigation_profile) {
                    intent = new Intent(ChatSellerActivity.this, SellerProfile.class);
                }

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }

    private void loadChatList() {
        if (currentUser == null) {
            Log.w(TAG, "Current user is null, cannot load chat list.");
            return;
        }

        // Assuming chat sessions for a seller are stored in:
        // users/{sellerId}/chat_sessions/{chatSessionId}
        // Each document in chat_sessions should have fields like:
        // otherUserId (buyerId), otherUserName (buyerName), lastMessage, timestamp (String or Firestore Timestamp),
        // unreadCount, propertyId (optional), offerStatus, conversationClosed.
        CollectionReference chatSessionsRef = db.collection("users")
                .document(currentUser.getUid())
                .collection("chat_sessions");

        // Order by last message timestamp if available. Assuming a field like "lastMessageTimestamp" (Firestore Timestamp)
        Query query = chatSessionsRef.orderBy("timestamp", Query.Direction.DESCENDING); // Or your actual timestamp field

        chatListenerRegistration = query.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Log.w(TAG, "Listen failed.", e);
                Toast.makeText(ChatSellerActivity.this, "Failed to load chats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshots != null) {
                for (DocumentChange dc : snapshots.getDocumentChanges()) {
                    ChatItem chatItem = dc.getDocument().toObject(ChatItem.class);
                    chatItem.setChatId(dc.getDocument().getId()); // Set document ID as chatId

                    // Fetch buyer's details if not already in ChatItem (e.g., if only otherUserId is stored)
                    // This is an example; adapt based on your ChatItem model and Firestore structure
                    if (chatItem.getSenderName() == null && chatItem.getOtherUserId() != null) {
                        fetchOtherUserDetails(chatItem, chatItem.getOtherUserId());
                    }


                    switch (dc.getType()) {
                        case ADDED:
                            if (!chatListContains(chatItem.getChatId())) {
                                chatList.add(chatItem);
                            }
                            break;
                        case MODIFIED:
                            for (int i = 0; i < chatList.size(); i++) {
                                if (chatList.get(i).getChatId().equals(chatItem.getChatId())) {
                                    chatList.set(i, chatItem);
                                    break;
                                }
                            }
                            break;
                        case REMOVED:
                            chatList.removeIf(item -> item.getChatId().equals(chatItem.getChatId()));
                            break;
                    }
                }
                // Sort here if necessary, e.g., by a proper timestamp
                chatSellerAdapter.notifyDataSetChanged();

                if (chatList.isEmpty()) {
                    Toast.makeText(ChatSellerActivity.this, "No active chats.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void fetchOtherUserDetails(ChatItem chatItemToUpdate, String otherUserId) {
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User otherUser = documentSnapshot.toObject(User.class);
                        if (otherUser != null) {
                            chatItemToUpdate.setSenderName(otherUser.getName());
                            // chatItemToUpdate.setProfileImageUrl(otherUser.getProfileImageUrl()); // If you have profile images
                            chatSellerAdapter.notifyItemChanged(chatList.indexOf(chatItemToUpdate));
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error fetching other user details for chat list", e));
    }


    private boolean chatListContains(String chatId) {
        for (ChatItem item : chatList) {
            if (item.getChatId() != null && item.getChatId().equals(chatId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatListenerRegistration != null) {
            chatListenerRegistration.remove();
        }
    }
}
