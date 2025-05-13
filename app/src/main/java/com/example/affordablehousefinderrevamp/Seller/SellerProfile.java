package com.example.affordablehousefinderrevamp.Seller;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.affordablehousefinderrevamp.Login;
import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.example.affordablehousefinderrevamp.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SellerProfile extends AppCompatActivity {

    private static final String TAG = "SellerProfileActivity";

    // UI Elements for profile display
    private ImageView profileImageView;
    private TextView userNameTextView, welcomeTextView, verifiedTextView;
    private TextView emailTextView, phoneTextView, addressTextView;
    private ImageView moreOptionsIcon; // Kebab menu icon for SellerProfile

    // UI elements for listings preview
    private ImageView houseImageView1, houseImageView2, houseImageView3, houseImageView4, houseImageView5, houseImageView6;
    private List<ImageView> houseImageViewList;

    // Other UI elements
    private RelativeLayout viewPolicyRow;
    private RelativeLayout logoutRow;
    private Button showAllListingsButton;
    private BottomNavigationView bottomNavigationViewSeller;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    // To store loaded user data for editing
    private User loadedUser;

    // ActivityResultLauncher for image picking (if used for listing placeholders, not profile pic)
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private int selectedImageIndex = -1; // Tracks which of the 6 ImageViews is being updated

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_profile);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI elements for profile display
        profileImageView = findViewById(R.id.profile_image);
        userNameTextView = findViewById(R.id.user_name_text);
        welcomeTextView = findViewById(R.id.welcome_text); // Can be dynamic if needed
        verifiedTextView = findViewById(R.id.verified_text);
        emailTextView = findViewById(R.id.email_text);
        phoneTextView = findViewById(R.id.phone_text);
        addressTextView = findViewById(R.id.address_text);
        moreOptionsIcon = findViewById(R.id.more_options_icon); // Kebab menu

        // Initialize UI elements for listings preview
        houseImageView1 = findViewById(R.id.houseImageView1);
        houseImageView2 = findViewById(R.id.houseImageView2);
        houseImageView3 = findViewById(R.id.houseImageView3);
        houseImageView4 = findViewById(R.id.houseImageView4);
        houseImageView5 = findViewById(R.id.houseImageView5);
        houseImageView6 = findViewById(R.id.houseImageView6);

        houseImageViewList = new ArrayList<>();
        houseImageViewList.add(houseImageView1);
        houseImageViewList.add(houseImageView2);
        houseImageViewList.add(houseImageView3);
        houseImageViewList.add(houseImageView4);
        houseImageViewList.add(houseImageView5);
        houseImageViewList.add(houseImageView6);

        // Initialize other UI elements
        viewPolicyRow = findViewById(R.id.privacy_policy_row);
        logoutRow = findViewById(R.id.logout_row);
        showAllListingsButton = findViewById(R.id.show_all_button);
        bottomNavigationViewSeller = findViewById(R.id.bottom_navigation_seller);

        // Setup image picker launcher (if these ImageViews were meant to be pickable for some other reason)
        // For now, they are primarily for displaying fetched listings.
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri selectedImageUri = data.getData();
                            if (selectedImageIndex >= 0 && selectedImageIndex < houseImageViewList.size()) {
                                ImageView targetImageView = houseImageViewList.get(selectedImageIndex);
                                if (targetImageView != null) {
                                    Glide.with(this).load(selectedImageUri).into(targetImageView);
                                    targetImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                                }
                            }
                        }
                    } else {
                        Toast.makeText(SellerProfile.this, "Image selection cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

        // Setup Listeners
        if (moreOptionsIcon != null) {
            moreOptionsIcon.setOnClickListener(this::showEditProfileMenu);
        }

        if (profileImageView != null) {
            profileImageView.setOnClickListener(v -> {
                // Since Firebase Storage for profile pic upload was removed, this is view-only.
                Toast.makeText(SellerProfile.this, "Profile Image (View Only)", Toast.LENGTH_SHORT).show();
            });
        }

        // Click listeners for house image views (now primarily for display)
        for (ImageView iv : houseImageViewList) {
            if (iv != null) {
                iv.setOnClickListener(v -> Toast.makeText(SellerProfile.this, "Listing Image (View Only)", Toast.LENGTH_SHORT).show());
            }
        }

        if (viewPolicyRow != null) {
            viewPolicyRow.setOnClickListener(v -> showPrivacyPolicy());
        }
        if (logoutRow != null) {
            logoutRow.setOnClickListener(v -> confirmLogout());
        }
        if (showAllListingsButton != null) {
            showAllListingsButton.setOnClickListener(v -> {
                Intent intent = new Intent(SellerProfile.this, HouseListings.class);
                startActivity(intent);
            });
        }

        // Load data if user is logged in
        if (currentUser != null) {
            loadUserProfile();
            loadRecentListings();
        } else {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            navigateToLogin();
        }
        setupBottomNavigation();
    }

    private void loadUserProfile() {
        if (currentUser == null) return;
        DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());
        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    loadedUser = document.toObject(User.class); // Store loaded user
                    if (loadedUser != null) {
                        populateProfileUI(loadedUser);
                    } else {
                        Log.e(TAG, "User data could not be parsed.");
                        Toast.makeText(SellerProfile.this, "Failed to parse profile data.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "No such document for user profile");
                    // Toast.makeText(SellerProfile.this, "Profile not found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Failed to fetch user profile", task.getException());
                Toast.makeText(SellerProfile.this, "Failed to load profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateProfileUI(User user) {
        if (user == null) return; // Guard against null user object

        if (userNameTextView != null) {
            userNameTextView.setText(user.getName());
        }
        if (emailTextView != null) {
            emailTextView.setText(user.getEmail());
        }
        if (phoneTextView != null) {
            phoneTextView.setText(user.getPhone());
        }
        if (addressTextView != null) {
            addressTextView.setText(user.getAddress());
        }


        // Verification status display
        if (verifiedTextView != null) {
            if (currentUser != null && currentUser.isEmailVerified()) {
                verifiedTextView.setText("Verified");
                verifiedTextView.setTextColor(getResources().getColor(R.color.colorAccent)); // Use your theme's accent color
            } else {
                verifiedTextView.setText("Not Verified");
                verifiedTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark)); // Or a suitable color for "not verified"
            }
        }
    }

    private void showEditProfileMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        // Ensure you have res/menu/profile_edit_options_menu.xml
        popup.getMenuInflater().inflate(R.menu.profile_edit_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_edit_name) {
                showEditDialog("Edit Name", loadedUser != null ? loadedUser.getName() : "", "name");
                return true;
            } else if (itemId == R.id.action_edit_phone) {
                showEditDialog("Edit Phone Number", loadedUser != null ? loadedUser.getPhone() : "", "phone");
                return true;
            } else if (itemId == R.id.action_edit_address) {
                showEditDialog("Edit Address", loadedUser != null ? loadedUser.getAddress() : "", "address");
                return true;
            } else if (itemId == R.id.action_change_email) {
                promptForPasswordAndChangeEmail();
                return true;
            } else if (itemId == R.id.action_change_password) {
                promptForPasswordAndChangePassword();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showEditDialog(String title, String currentValue, final String fieldKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        // Set up the input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT); // General text input
        if ("phone".equals(fieldKey)) {
            input.setInputType(InputType.TYPE_CLASS_PHONE);
        }
        input.setText(currentValue);
        input.setHint("Enter new " + fieldKey);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (TextUtils.isEmpty(newValue)) {
                Toast.makeText(SellerProfile.this, "Field cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateFirestoreField(fieldKey, newValue);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateFirestoreField(String fieldKey, String newValue) {
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());

        Map<String, Object> updates = new HashMap<>();
        updates.put(fieldKey, newValue);

        userDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SellerProfile.this, fieldKey.substring(0, 1).toUpperCase() + fieldKey.substring(1) + " updated successfully.", Toast.LENGTH_SHORT).show();
                    loadUserProfile(); // Refresh UI with new data
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SellerProfile.this, "Failed to update " + fieldKey + ".", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating " + fieldKey, e);
                });
    }


    private void promptForPasswordAndChangeEmail() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Re-authenticate to Change Email");

        // Inflate custom layout for dialog
        // Ensure you have R.layout.dialog_reauthenticate with EditTexts:
        // current_password_input and new_value_input
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_reauthenticate, (RelativeLayout)findViewById(android.R.id.content), false);
        final EditText inputPassword = viewInflated.findViewById(R.id.current_password_input);
        final EditText inputNewEmail = viewInflated.findViewById(R.id.new_value_input);
        inputNewEmail.setHint("Enter new email");
        inputNewEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        builder.setView(viewInflated);

        builder.setPositiveButton("Proceed", (dialog, which) -> {
            String password = inputPassword.getText().toString();
            String newEmail = inputNewEmail.getText().toString().trim();

            if (TextUtils.isEmpty(password) || TextUtils.isEmpty(newEmail)) {
                Toast.makeText(SellerProfile.this, "Password and new email are required.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) { // Basic email validation
                Toast.makeText(SellerProfile.this, "Enter a valid email address.", Toast.LENGTH_SHORT).show();
                return;
            }
            reauthenticateAndChangeEmail(password, newEmail);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void reauthenticateAndChangeEmail(String password, final String newEmail) {
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "User not properly signed in or email is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        currentUser.verifyBeforeUpdateEmail(newEmail) // Firebase sends verification email
                                .addOnCompleteListener(updateEmailTask -> {
                                    if (updateEmailTask.isSuccessful()) {
                                        // Update email in Firestore after Firebase Auth update is initiated
                                        updateFirestoreField("email", newEmail);
                                        Toast.makeText(SellerProfile.this, "Verification email sent to new address. Email will update after verification.", Toast.LENGTH_LONG).show();
                                        // UI will reflect change after re-verification and possibly re-login or next profile load.
                                    } else {
                                        Toast.makeText(SellerProfile.this, "Failed to initiate email update: " + updateEmailTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        Log.e(TAG, "Failed to update email in Auth: ", updateEmailTask.getException());
                                    }
                                });
                    } else {
                        Toast.makeText(SellerProfile.this, "Re-authentication failed: " + reauthTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Re-authentication failed: ", reauthTask.getException());
                    }
                });
    }

    private void promptForPasswordAndChangePassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

        // Inflate custom layout for dialog
        // Ensure you have R.layout.dialog_change_password with EditTexts:
        // current_password_input_cp, new_password_input_cp, confirm_password_input_cp
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, (RelativeLayout)findViewById(android.R.id.content), false);
        final EditText inputCurrentPassword = viewInflated.findViewById(R.id.current_password_input_cp);
        final EditText inputNewPassword = viewInflated.findViewById(R.id.new_password_input_cp);
        final EditText inputConfirmPassword = viewInflated.findViewById(R.id.confirm_password_input_cp);

        builder.setView(viewInflated);

        builder.setPositiveButton("Change Password", (dialog, which) -> {
            String currentPassword = inputCurrentPassword.getText().toString();
            String newPassword = inputNewPassword.getText().toString();
            String confirmPassword = inputConfirmPassword.getText().toString();

            if (TextUtils.isEmpty(currentPassword) || TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(SellerProfile.this, "All password fields are required.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(SellerProfile.this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPassword.length() < 6) { // Basic password validation
                Toast.makeText(SellerProfile.this, "New password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                return;
            }

            reauthenticateAndChangePassword(currentPassword, newPassword);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void reauthenticateAndChangePassword(String currentPassword, final String newPassword) {
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "User not properly signed in or email is null.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updatePasswordTask -> {
                                    if (updatePasswordTask.isSuccessful()) {
                                        Toast.makeText(SellerProfile.this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(SellerProfile.this, "Failed to update password: " + updatePasswordTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        Log.e(TAG, "Failed to update password in Auth: ", updatePasswordTask.getException());
                                    }
                                });
                    } else {
                        Toast.makeText(SellerProfile.this, "Re-authentication failed: " + reauthTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Re-authentication failed: ", reauthTask.getException());
                    }
                });
    }


    private void loadRecentListings() {
        if (currentUser == null) return;
        db.collection("properties")
                .whereEqualTo("sellerId", currentUser.getUid())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(6) // Load up to 6 recent listings
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No listings found for this seller.");
                        // Set all placeholders if no listings
                        for(ImageView iv : houseImageViewList) {
                            if (iv != null) iv.setImageResource(R.drawable.placeholder);
                        }
                        return;
                    }
                    int i = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        if (i < houseImageViewList.size()) {
                            Property property = doc.toObject(Property.class);
                            ImageView currentImageView = houseImageViewList.get(i);
                            if (currentImageView != null) {
                                String imageUrl = null;
                                // Prioritize list of imageUrls, then single imageUrl
                                if (property.getImageUrls() != null && !property.getImageUrls().isEmpty()) {
                                    imageUrl = property.getImageUrls().get(0); // Get the first image
                                } else if (property.getImageUrl() != null && !property.getImageUrl().isEmpty()) {
                                    imageUrl = property.getImageUrl();
                                }

                                if (imageUrl != null) {
                                    Glide.with(SellerProfile.this)
                                            .load(imageUrl)
                                            .placeholder(R.drawable.placeholder) // Placeholder while loading
                                            .error(R.drawable.placeholder) // Fallback if image fails to load
                                            .centerCrop()
                                            .into(currentImageView);
                                } else {
                                    currentImageView.setImageResource(R.drawable.placeholder); // Default if no URL
                                }
                            }
                            i++;
                        } else {
                            break; // Stop if we've filled all available ImageViews
                        }
                    }
                    // If fewer than 6 listings were found, set remaining ImageViews to a placeholder
                    for (int j = i; j < houseImageViewList.size(); j++) {
                        ImageView remainingImageView = houseImageViewList.get(j);
                        if (remainingImageView != null) {
                            remainingImageView.setImageResource(R.drawable.placeholder);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading recent listings", e);
                    Toast.makeText(SellerProfile.this, "Failed to load listings.", Toast.LENGTH_SHORT).show();
                });
    }

    // This was for the grid ImageViews, if they were pickable. Kept for potential future use.
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    public void showPrivacyPolicy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Privacy Policy");
        builder.setMessage(R.string.privacy_policy_text); // Ensure this string exists
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        mAuth.signOut();
        Toast.makeText(SellerProfile.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(SellerProfile.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupBottomNavigation() {
        if (bottomNavigationViewSeller != null) {
            bottomNavigationViewSeller.setSelectedItemId(R.id.navigation_profile); // Set Profile as selected
            bottomNavigationViewSeller.setOnNavigationItemSelectedListener(item -> {
                Intent intent = null;
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_upload) {
                    intent = new Intent(SellerProfile.this, HouseListings.class);
                } else if (itemId == R.id.navigation_chat) {
                    intent = new Intent(SellerProfile.this, Chat_Seller.class); // Ensure Chat_Seller is implemented
                } else if (itemId == R.id.navigation_profile) {
                    return true; // Already on this screen
                }

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0,0); // No animation
                    finish(); // Finish current activity
                    return true;
                }
                return false;
            });
        }
    }
}
