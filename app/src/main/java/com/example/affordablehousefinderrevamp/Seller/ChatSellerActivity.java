package com.example.affordablehousefinderrevamp.Seller;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.affordablehousefinderrevamp.Adapter.ChatSellerAdapter;
import com.example.affordablehousefinderrevamp.ChatItem;
import com.example.affordablehousefinderrevamp.R;
import com.example.affordablehousefinderrevamp.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;

public class ChatSellerActivity extends AppCompatActivity {

    private static final String TAG = "ChatSellerListActivity";

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
            getSupportActionBar().setTitle("Chat with Buyers");
        }

        recyclerViewChats = findViewById(R.id.rv_chat_list_seller);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));

        chatList = new ArrayList<>();
        chatSellerAdapter = new ChatSellerAdapter(this, chatList, chatItem -> {
            Intent intent = new Intent(ChatSellerActivity.this, Chat_Seller.class);
            intent.putExtra("chatId", chatItem.getChatId());
            intent.putExtra("buyerId", chatItem.getOtherUserId());
            intent.putExtra("buyerName", chatItem.getSenderName());
            if (chatItem.getPropertyId() != null) {
                intent.putExtra("propertyId", chatItem.getPropertyId());
            }
            if (chatItem.getPropertyName() != null) {
                intent.putExtra("propertyName", chatItem.getPropertyName());
            }
            startActivity(intent);
        });
        recyclerViewChats.setAdapter(chatSellerAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (currentUser != null) {
            loadChatList();
        } else {
            Toast.makeText(this, "Not signed in.", Toast.LENGTH_SHORT).show();
        }

        bottomNavigationViewSeller = findViewById(R.id.bottom_navigation_seller);
        if (bottomNavigationViewSeller != null) {
            bottomNavigationViewSeller.setSelectedItemId(R.id.navigation_chat);
            bottomNavigationViewSeller.setOnNavigationItemSelectedListener(item -> {
                Intent intent = null;
                int id = item.getItemId();
                if (id == R.id.navigation_profile) {
                    intent = new Intent(this, SellerProfile.class);
                } else if (id == R.id.navigation_upload) {
                    intent = new Intent(this, HouseListings.class);
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
                .document(currentUser .getUid())
                .collection("chat_sessions");
        chatListenerRegistration = chatSessionsRef
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Failed to load chats.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshots != null) {
                        chatList.clear();
                        for (DocumentSnapshot doc : snapshots.getDocuments()) {
                            ChatItem chatItem = doc.toObject(ChatItem.class);
                            chatList.add(chatItem);
                        }
                        chatSellerAdapter.notifyDataSetChanged();
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
                            if (pos >= 0) chatSellerAdapter.notifyItemChanged(pos);
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
