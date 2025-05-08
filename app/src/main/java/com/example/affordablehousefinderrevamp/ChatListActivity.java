package com.example.affordablehousefinderrevamp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity implements ChatListAdapter.OnChatItemClickListener {

    private static final String TAG = "ChatListActivity";
    private RecyclerView recyclerViewChatList;
    private ChatListAdapter chatListAdapter;
    private List<ChatItem> chatDataList;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        Toolbar toolbar = findViewById(R.id.toolbar_chat_list);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Toolbar icon listeners
        ImageButton bookmarkButton = findViewById(R.id.toolbar_bookmark_button_chat_list);
        ImageButton newChatButton = findViewById(R.id.toolbar_new_chat_button_chat_list);
        ImageButton menuButton = findViewById(R.id.toolbar_menu_button_chat_list);

        bookmarkButton.setOnClickListener(v -> Toast.makeText(ChatListActivity.this, "Bookmarks clicked", Toast.LENGTH_SHORT).show());
        newChatButton.setOnClickListener(v -> {
            // Example: Start an activity to select a contact or create new chat
            Toast.makeText(ChatListActivity.this, "New Chat clicked", Toast.LENGTH_SHORT).show();
            // Intent intent = new Intent(ChatListActivity.this, NewChatActivity.class);
            // startActivity(intent);
        });
        menuButton.setOnClickListener(v -> Toast.makeText(ChatListActivity.this, "Menu clicked", Toast.LENGTH_SHORT).show());

        // RecyclerView setup
        recyclerViewChatList = findViewById(R.id.recycler_view_chat_list);
        recyclerViewChatList.setLayoutManager(new LinearLayoutManager(this));
        chatDataList = new ArrayList<>();
        chatListAdapter = new ChatListAdapter(this, chatDataList, this);
        recyclerViewChatList.setAdapter(chatListAdapter);
        loadSampleChatData();

        // BottomNavigationView setup
        bottomNavigationView = findViewById(R.id.bottom_navigation_chat_list);
        // Set "Chat" as the selected item
        bottomNavigationView.setSelectedItemId(R.id.navigation_chat);
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_home) {
                    Intent homeIntent = new Intent(ChatListActivity.this, Homepage.class);
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Navigate to existing instance or new
                    startActivity(homeIntent);
                    finish(); // Optional: finish ChatListActivity
                    return true;
                } else if (itemId == R.id.navigation_chat) {
                    // Already on ChatListActivity, maybe refresh or scroll to top
                    Toast.makeText(ChatListActivity.this, "Chat list refreshed (Example)", Toast.LENGTH_SHORT).show();
                    // loadSampleChatData(); // Example: reload data
                    recyclerViewChatList.smoothScrollToPosition(0); // Scroll to top
                    return true;
                } else if (itemId == R.id.navigation_sell) { // Or navigation_buy
                    Intent uploadIntent = new Intent(ChatListActivity.this, UploadActivity.class);
                    // uploadIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(uploadIntent);
                    // finish(); // Optional
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    Toast.makeText(ChatListActivity.this, "Profile Clicked (Implement Navigation)", Toast.LENGTH_SHORT).show();
                    // Intent profileIntent = new Intent(ChatListActivity.this, ProfileActivity.class);
                    // startActivity(profileIntent);
                    // finish(); // Optional
                    return true;
                }
                return false;
            }
        });

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat_list_main_layout), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply top padding to the AppBarLayout to push it down below status bar
            View appBar = findViewById(R.id.app_bar_layout_chat_list);
            appBar.setPadding(insets.left, insets.top, insets.right, 0);

            // Apply bottom padding to the BottomNavigationView
            bottomNavigationView.setPadding(insets.left, 0, insets.right, insets.bottom);

            // The RecyclerView is already constrained between AppBar and BottomNav,
            // so its padding for system bars might not be needed if AppBar and BottomNav handle them.
            // However, if content still overlaps, adjust RecyclerView's padding as well.
            // recyclerViewChatList.setPadding(0,0,0,insets.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void loadSampleChatData() {
        chatDataList.clear(); // Clear existing data before loading new
        chatDataList.add(new ChatItem("John Ever", "We bend so we don't break.", "Now", 2, false, R.drawable.baseline_person_24));
        chatDataList.add(new ChatItem("Theo Bugayong", "Happiness is a habit.", "2 Min", 0, true, R.drawable.baseline_person_24));
        chatDataList.add(new ChatItem("Hitesh Sohal", "Don't just fly. Soar.", "1 Hour", 3, false, R.drawable.baseline_person_24));
        chatDataList.add(new ChatItem("Mark Andrei", "Allow yourself joy.", "5 Hours", 0, true, R.drawable.baseline_person_24));
        chatDataList.add(new ChatItem("Johnred Ivensor", "Keep it simple.", "22 Hours", 4, false, R.drawable.baseline_person_24));
        chatDataList.add(new ChatItem("Gean Llobrera", "You're wonderful", "Yesterday", 0, true, R.drawable.baseline_person_24));
        chatDataList.add(new ChatItem("Ray Janeo", "You got this", "2 Weeks ago", 1, false, R.drawable.baseline_person_24));
        chatListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(ChatItem chatItem) {
        Toast.makeText(this, "Opening chat with: " + chatItem.getSenderName(), Toast.LENGTH_SHORT).show();
        // Example: Navigate to individual chat screen
        // Intent intent = new Intent(this, ChatConversationActivity.class); // Assuming you have ChatConversationActivity
        // intent.putExtra("SENDER_NAME", chatItem.getSenderName()); // Pass necessary data
        // startActivity(intent);
    }
}
