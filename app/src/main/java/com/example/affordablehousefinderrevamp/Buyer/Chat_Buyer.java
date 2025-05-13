package com.example.affordablehousefinderrevamp.Buyer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.affordablehousefinderrevamp.ChatItem; // Assuming ChatItem model
import com.example.affordablehousefinderrevamp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Chat_Buyer extends AppCompatActivity {

    private RecyclerView recyclerViewChats;
    private ChatBuyerAdapter chatBuyerAdapter; // Adapter for chat list
    private List<ChatItem> chatList;
    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;

    private DatabaseReference databaseReferenceChats;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // This activity is for the LIST of chats for the buyer.
        setContentView(R.layout.activity_chat_list_buyer);

        // Corrected Toolbar ID to match activity_chat_list_buyer.xml
        toolbar = findViewById(R.id.toolbar_chat_buyer);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Chats");
            // getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Optional
        }

        // Corrected RecyclerView ID to match activity_chat_list_buyer.xml
        recyclerViewChats = findViewById(R.id.rv_chat_list_buyer);
        recyclerViewChats.setLayoutManager(new LinearLayoutManager(this));

        chatList = new ArrayList<>();
        chatBuyerAdapter = new ChatBuyerAdapter(this, chatList, chatItem -> {
            // Handle chat item click: Open individual chat screen (ChatBuyerActivity)
            Intent intent = new Intent(Chat_Buyer.this, ChatBuyerActivity.class);
            intent.putExtra("receiverId", chatItem.getReceiverId());
            intent.putExtra("chatId", chatItem.getChatId());
            intent.putExtra("receiverName", chatItem.getSenderName());
            // intent.putExtra("propertyId", chatItem.getPropertyId()); // If chats are linked to properties
            startActivity(intent);
        });
        recyclerViewChats.setAdapter(chatBuyerAdapter);

        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            databaseReferenceChats = FirebaseDatabase.getInstance().getReference("user_chats").child(currentUser.getUid());
            loadChatList();
        } else {
            Toast.makeText(this, "Please log in to view chats.", Toast.LENGTH_SHORT).show();
            // Consider finishing the activity or redirecting to login
            // finish();
        }

        // ID for BottomNavigationView in activity_chat_list_buyer.xml is correct
        bottomNavigationView = findViewById(R.id.bottom_navigation_buyer);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.navigation_chat); // Set Chat as selected

            bottomNavigationView.setOnItemSelectedListener(item -> {
                Intent intent = null;
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_home) {
                    intent = new Intent(Chat_Buyer.this, Homepage.class);
                } else if (itemId == R.id.navigation_chat) {
                    return true; // Already here
                } else if (itemId == R.id.navigation_profile) {
                    intent = new Intent(Chat_Buyer.this, BuyerProfile.class);
                }

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0,0); // No animation
                    finish(); // Finish current activity to prevent stack buildup
                    return true;
                }
                return false;
            });
        }
    }

    private void loadChatList() {
        // Placeholder: Actual chat list loading from Firebase would be more complex.
        // TODO: Implement actual Firebase chat list loading logic.
        // This might involve querying a 'chats' node where each entry has participants.
        // For now, using placeholder or a simple listener.

        if (databaseReferenceChats == null) { // If not logged in or DB ref not set
            if (chatList.isEmpty()) { // Only add sample if list is empty
                // Ensure ChatItem constructor matches your ChatItem.java model
                chatList.add(new ChatItem("chat1_placeholder", "seller123", "John Seller (Sample)", "Okay, see you then!", "10:30 AM", 0, true, "user_buyer_id_sample", R.drawable.ic_person_placeholder));
                chatList.add(new ChatItem("chat2_placeholder", "seller456", "Jane Realtor (Sample)", "Is the price negotiable?", "Yesterday", 2, false, "user_buyer_id_sample", R.drawable.ic_person_placeholder));
                chatBuyerAdapter.notifyDataSetChanged();
                if (chatList.isEmpty()) { // Should not be empty now if samples added
                    Toast.makeText(this, "No chats yet.", Toast.LENGTH_SHORT).show();
                }
            }
            return;
        }

        // Example of listening to a node that contains chat sessions for the user
        // This assumes a structure like /user_chats/{currentUserId}/{chatId}
        databaseReferenceChats.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    ChatItem chatItem = snapshot.getValue(ChatItem.class);
                    if (chatItem != null) {
                        chatItem.setChatId(snapshot.getKey()); // Set chat ID from Firebase key
                        // You might need to fetch the other user's name/profile pic based on receiverId
                        // For now, senderName from ChatItem is used.
                        chatList.add(chatItem);
                    }
                }
                chatBuyerAdapter.notifyDataSetChanged();
                if (chatList.isEmpty()) {
                    Toast.makeText(Chat_Buyer.this, "No active chats.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(Chat_Buyer.this, "Failed to load chats: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == android.R.id.home) {
            // This is for the Up button if setDisplayHomeAsUpEnabled(true) is called
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
