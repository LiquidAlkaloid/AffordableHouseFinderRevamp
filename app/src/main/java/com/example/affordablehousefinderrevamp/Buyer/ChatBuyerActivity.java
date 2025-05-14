package com.example.affordablehousefinderrevamp.Buyer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.affordablehousefinderrevamp.Adapter.ChatBuyerAdapter;
import com.example.affordablehousefinderrevamp.ChatItem;
import com.example.affordablehousefinderrevamp.R;
import com.example.affordablehousefinderrevamp.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.*;

public class ChatBuyerActivity extends AppCompatActivity {
    private static final String TAG = "ChatBuyerListActivity";

    private RecyclerView recyclerViewChats;
    private ChatBuyerAdapter chatBuyerAdapter;
    private List<ChatItem> chatList;
    private BottomNavigationView bottomNavigationViewBuyer;
    private Toolbar toolbar;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private ListenerRegistration chatListenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list_buyer);

        toolbar = findViewById(R.id.toolbar_chat_buyer);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Chats");
        }

        recyclerViewChats = findViewById(R.id.rv_chat_list_buyer);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));

        chatList = new ArrayList<>();
        chatBuyerAdapter = new ChatBuyerAdapter(this, chatList, chatItem -> {
            Intent intent = new Intent(ChatBuyerActivity.this, Chat_Buyer.class);
            intent.putExtra("chatId", chatItem.getChatId());
            intent.putExtra("sellerId", chatItem.getOtherUserId());
            intent.putExtra("sellerName", chatItem.getSenderName());
            if (chatItem.getPropertyId() != null) {
                intent.putExtra("propertyId", chatItem.getPropertyId());
            }
            if (chatItem.getPropertyName() != null) {
                intent.putExtra("propertyName", chatItem.getPropertyName());
            }
            startActivity(intent);
        });
        recyclerViewChats.setAdapter(chatBuyerAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            loadChatList();
        } else {
            Toast.makeText(this, "Please log in to view chats.", Toast.LENGTH_SHORT).show();
        }

        bottomNavigationViewBuyer = findViewById(R.id.bottom_navigation_buyer);
        if (bottomNavigationViewBuyer != null) {
            bottomNavigationViewBuyer.setSelectedItemId(R.id.navigation_chat);
            bottomNavigationViewBuyer.setOnNavigationItemSelectedListener(item -> {
                Intent intent = null;
                int id = item.getItemId();
                if (id == R.id.navigation_home) {
                    intent = new Intent(this, Homepage.class);
                } else if (id == R.id.navigation_profile) {
                    intent = new Intent(this, BuyerProfile.class);
                } else {
                    return true; // already on chat
                }
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            });
        }
    }

    private void loadChatList() {
        CollectionReference chatSessionsRef = db.collection("users")
                .document(currentUser.getUid())
                .collection("chat_sessions");

        chatListenerRegistration = chatSessionsRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed for buyer chat list.", e);
                        Toast.makeText(this, "Failed to load chats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            DocumentSnapshot doc = dc.getDocument();

                            // Manually map fields into ChatItem
                            ChatItem chatItem = new ChatItem();
                            chatItem.setChatId(doc.getId());
                            chatItem.setOtherUserId(doc.getString("otherUserId"));
                            chatItem.setSenderName(doc.getString("senderName"));
                            chatItem.setLastMessage(doc.getString("lastMessage"));
                            Number uc = doc.getLong("unreadCount");
                            chatItem.setUnreadCount(uc != null ? uc.intValue() : 0);

                            Timestamp ts = doc.getTimestamp("timestamp");
                            chatItem.setTimestampDate(ts != null ? ts.toDate() : null);

                            chatItem.setPropertyId(doc.getString("propertyId"));
                            chatItem.setPropertyName(doc.getString("propertyName"));
                            chatItem.setOfferStatus(doc.getString("offerStatus"));
                            chatItem.setConversationClosed(doc.getBoolean("conversationClosed"));

                            // Ensure senderName is populated
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
                                    int idx = findChatItemIndex(chatItem.getChatId());
                                    if (idx >= 0) {
                                        chatList.set(idx, chatItem);
                                    } else {
                                        chatList.add(chatItem);
                                    }
                                    break;
                                case REMOVED:
                                    chatList.removeIf(item -> item.getChatId().equals(chatItem.getChatId()));
                                    break;
                            }
                        }

                        // Sort descending by timestampDate
                        chatList.sort((a, b) -> {
                            Date d1 = a.getTimestampDate(), d2 = b.getTimestampDate();
                            if (d1 == null && d2 == null) return 0;
                            if (d1 == null) return 1;
                            if (d2 == null) return -1;
                            return d2.compareTo(d1);
                        });

                        chatBuyerAdapter.notifyDataSetChanged();

                        if (chatList.isEmpty()) {
                            Toast.makeText(this, "No active chats.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void fetchOtherUserDetails(ChatItem item, String otherUserId) {
        db.collection("users").document(otherUserId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User u = doc.toObject(User.class);
                        if (u != null) {
                            item.setSenderName(u.getName());
                            int pos = findChatItemIndex(item.getChatId());
                            if (pos >= 0) chatBuyerAdapter.notifyItemChanged(pos);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error fetching user details", e));
    }

    private boolean chatListContains(String chatId) {
        for (ChatItem c : chatList) {
            if (chatId.equals(c.getChatId())) return true;
        }
        return false;
    }

    private int findChatItemIndex(String chatId) {
        for (int i = 0; i < chatList.size(); i++) {
            if (chatId.equals(chatList.get(i).getChatId())) return i;
        }
        return -1;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (chatListenerRegistration != null) {
            chatListenerRegistration.remove();
        }
    }
}
