package com.example.affordablehousefinderrevamp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity
        implements ChatListAdapter.OnChatItemClickListener {

    private RecyclerView recyclerView;
    private ChatListAdapter adapter;
    private List<ChatItem> chatItems;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        // Firebase instances
        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // RecyclerView setup
        recyclerView = findViewById(R.id.recycler_view_chat_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatItems = new ArrayList<>();
        // Adapter now takes (listener, items)
        adapter = new ChatListAdapter(this, chatItems);
        recyclerView.setAdapter(adapter);

        // Load chats from Firestore
        loadChats();

        // Bottom nav (if present)
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation_chat_list);
        bottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                // handle home/chat/sell/profile...
                return false;
            }
        });
    }

    private void loadChats() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        db.collection("chats")
                .whereArrayContains("participants", user.getUid())
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(ChatListActivity.this,
                                    "Failed to load chats: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        chatItems.clear();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            ChatItem item = doc.toObject(ChatItem.class);
                            chatItems.add(item);
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    @Override
    public void onItemClick(ChatItem chatItem) {
        Toast.makeText(this,
                "Opening chat with: " + chatItem.getSenderName(),
                Toast.LENGTH_SHORT).show();
        // e.g. startActivity(new Intent(this, ChatConversationActivity.class));
    }
}
