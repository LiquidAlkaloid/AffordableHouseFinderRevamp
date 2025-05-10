package com.example.affordablehousefinderrevamp.Seller;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;


import java.util.ArrayList;
import java.util.List;

import com.example.affordablehousefinderrevamp.R;

public class SellerProfile extends AppCompatActivity {

    private ImageView profileImageView;
    private ImageView houseImageView1;
    private ImageView houseImageView2;
    private ImageView houseImageView3;
    private ImageView houseImageView4;
    private ImageView houseImageView5;
    private ImageView houseImageView6;
    private Button viewPolicyButton; // Corrected to Button
    private Button logoutButton;    // Corrected to Button
    private Button showAllListingsButton;
    private List<ImageView> houseImageViewList;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private int selectedImageIndex = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);


        // Initialize UI elements
        profileImageView = findViewById(R.id.profile_image);
        houseImageView1 = findViewById(R.id.houseImageView1);
        houseImageView2 = findViewById(R.id.houseImageView2);
        houseImageView3 = findViewById(R.id.houseImageView3);
        houseImageView4 = findViewById(R.id.houseImageView4);
        houseImageView5 = findViewById(R.id.houseImageView5);
        houseImageView6 = findViewById(R.id.houseImageView6);
        viewPolicyButton = findViewById(R.id.viewPolicyButton);
        logoutButton = findViewById(R.id.logoutButton);
        showAllListingsButton = findViewById(R.id.show_all_button);


        houseImageViewList = new ArrayList<>();
        houseImageViewList.add(houseImageView1);
        houseImageViewList.add(houseImageView2);
        houseImageViewList.add(houseImageView3);
        houseImageViewList.add(houseImageView4);
        houseImageViewList.add(houseImageView5);
        houseImageViewList.add(houseImageView6);


        //Set image click listeners
        for (int i = 0; i < houseImageViewList.size(); i++) {
            final int index = i;
            houseImageViewList.get(i).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedImageIndex = index;
                    openImagePicker();
                }
            });
        }




        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<androidx.activity.result.ActivityResult>() {
                    @Override
                    public void onActivityResult(androidx.activity.result.ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Intent data = result.getData();
                            if (data != null && data.getData() != null) {
                                Uri selectedImageUri = data.getData();
                                if (selectedImageIndex >= 0 && selectedImageIndex < houseImageViewList.size()) {
                                    houseImageViewList.get(selectedImageIndex).setImageURI(selectedImageUri);
                                    houseImageViewList.get(selectedImageIndex).setScaleType(ImageView.ScaleType.CENTER_CROP);
                                }
                            }
                        } else {
                            Toast.makeText(SellerProfile.this, "Image selection cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
                });


        // Set click listeners for the buttons
        viewPolicyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrivacyPolicy(v);
            }
        });


        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Handle logout
                Toast.makeText(SellerProfile.this, "Logging out...", Toast.LENGTH_SHORT).show();
            }
        });




        showAllListingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SellerProfile.this, HouseListings.class);
                startActivity(intent);
            }
        });


    }


    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }


    public void showPrivacyPolicy(View view) {
        // Create an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Privacy Policy");
        builder.setMessage(R.string.privacy_policy_text);


        // Add an OK button to dismiss the dialog
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });




        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
