package com.example.affordablehousefinderrevamp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge; // If you use this
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class Homepage extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this); // Uncomment if you are using EdgeToEdge
        setContentView(R.layout.activity_homepage);

        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int itemId = item.getItemId();
                    if (itemId == R.id.navigation_home) {
                        Toast.makeText(Homepage.this, "Home selected", Toast.LENGTH_SHORT).show();
                        return true;
                    } else if (itemId == R.id.navigation_chat) {
                        // Navigate to the new ChatListActivity
                        Intent chatListIntent = new Intent(Homepage.this, ChatListActivity.class);
                        startActivity(chatListIntent);
                        return true;
                    } else if (itemId == R.id.navigation_sell) { // Or navigation_buy
                        Intent uploadIntent = new Intent(Homepage.this, UploadActivity.class);
                        startActivity(uploadIntent);
                        return true;
                    } else if (itemId == R.id.navigation_profile) {
                        Toast.makeText(Homepage.this, "Profile selected", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                    // Handle other items like bookmarks if they exist
                    // else if (itemId == R.id.navigation_bookmarks) {
                    //    Intent bookmarksIntent = new Intent(Homepage.this, Bookmarks.class);
                    //    startActivity(bookmarksIntent);
                    //    return true;
                    // }
                    return false;
                }
            });
        } else {
            Log.e("Homepage", "BottomNavigationView not found. Check ID in activity_homepage.xml");
        }

        // Handle window insets - ensure rootLayout is the ID of your root view in activity_homepage.xml
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootLayout), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply paddings to the root view to avoid drawing under system bars
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return WindowInsetsCompat.CONSUMED; // Consume the insets
        });
    }
}
