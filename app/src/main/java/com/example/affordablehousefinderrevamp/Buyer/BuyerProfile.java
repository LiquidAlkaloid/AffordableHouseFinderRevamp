package com.example.affordablehousefinderrevamp.Buyer;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.affordablehousefinderrevamp.Login;
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

import java.util.HashMap;
import java.util.Map;

public class BuyerProfile extends AppCompatActivity {

    private static final String TAG = "BuyerProfileActivity";

    private RelativeLayout logoutRowAlt;
    private RelativeLayout privacyPolicyRowAlt;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private BottomNavigationView bottomNavigationViewBuyer;

    // UI Elements for profile data
    private ImageView profileImageViewAlt;
    private TextView userNameTextViewAlt, welcomeTextViewAlt, verifiedTextViewAlt;
    private TextView emailTextViewAlt, phoneTextViewAlt, addressTextViewAlt;
    private ImageView moreOptionsIconAlt; // Kebab menu icon

    private User loadedUser; // To store the loaded user data

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUser = mAuth.getCurrentUser();

        // Initialize UI elements from activity_buyer_profile.xml
        profileImageViewAlt = findViewById(R.id.profile_image_alt);
        userNameTextViewAlt = findViewById(R.id.user_name_text_alt);
        welcomeTextViewAlt = findViewById(R.id.welcome_text_alt);
        verifiedTextViewAlt = findViewById(R.id.verified_text_alt);
        emailTextViewAlt = findViewById(R.id.email_text_alt);
        phoneTextViewAlt = findViewById(R.id.phone_text_alt);
        addressTextViewAlt = findViewById(R.id.address_text_alt);
        moreOptionsIconAlt = findViewById(R.id.more_options_icon_alt); // Initialize kebab menu

        logoutRowAlt = findViewById(R.id.logout_row_alt);
        privacyPolicyRowAlt = findViewById(R.id.privacy_policy_row_alt);
        bottomNavigationViewBuyer = findViewById(R.id.bottom_navigation_buyer);

        if (moreOptionsIconAlt != null) {
            moreOptionsIconAlt.setOnClickListener(this::showEditProfileMenu);
        }

        if (logoutRowAlt != null) {
            logoutRowAlt.setOnClickListener(v -> confirmLogout());
        }

        if (privacyPolicyRowAlt != null) {
            privacyPolicyRowAlt.setOnClickListener(v -> showPrivacyPolicy());
        }

        if (currentUser != null) {
            loadUserProfile();
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
                        Toast.makeText(BuyerProfile.this, "Failed to parse profile data.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d(TAG, "No such document for user profile");
                    Toast.makeText(BuyerProfile.this, "Profile not found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e(TAG, "Failed to fetch user profile", task.getException());
                Toast.makeText(BuyerProfile.this, "Failed to load profile: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateProfileUI(User user) {
        if (user == null) return;
        if (userNameTextViewAlt != null) {
            userNameTextViewAlt.setText(user.getName());
        }
        if (emailTextViewAlt != null) {
            emailTextViewAlt.setText(user.getEmail());
        }
        if (phoneTextViewAlt != null) {
            phoneTextViewAlt.setText(user.getPhone());
        }
        if (addressTextViewAlt != null) {
            addressTextViewAlt.setText(user.getAddress());
        }

        if (verifiedTextViewAlt != null) {
            if (currentUser != null && currentUser.isEmailVerified()) {
                verifiedTextViewAlt.setText("Verified");
                verifiedTextViewAlt.setTextColor(getResources().getColor(R.color.colorAccent)); // Or your verified color
            } else {
                verifiedTextViewAlt.setText("Not Verified");
                verifiedTextViewAlt.setTextColor(getResources().getColor(android.R.color.holo_red_dark)); // Or your unverified color
            }
        }
    }

    private void showEditProfileMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
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

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(currentValue);
        input.setHint("Enter new " + fieldKey);
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newValue = input.getText().toString().trim();
            if (TextUtils.isEmpty(newValue)) {
                Toast.makeText(BuyerProfile.this, "Field cannot be empty.", Toast.LENGTH_SHORT).show();
                return;
            }
            updateFirestoreField(fieldKey, newValue);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void updateFirestoreField(String fieldKey, String newValue) {
        if (currentUser == null) return;
        DocumentReference userDocRef = db.collection("users").document(currentUser.getUid());

        Map<String, Object> updates = new HashMap<>();
        updates.put(fieldKey, newValue);

        userDocRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(BuyerProfile.this, fieldKey.substring(0, 1).toUpperCase() + fieldKey.substring(1) + " updated.", Toast.LENGTH_SHORT).show();
                    loadUserProfile(); // Refresh UI
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(BuyerProfile.this, "Failed to update " + fieldKey + ".", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error updating " + fieldKey, e);
                });
    }

    private void promptForPasswordAndChangeEmail() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Re-authenticate to Change Email");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.dialog_reauthenticate, (RelativeLayout) findViewById(android.R.id.content), false);
        final EditText inputPassword = viewInflated.findViewById(R.id.current_password_input);
        final EditText inputNewEmail = viewInflated.findViewById(R.id.new_value_input);
        inputNewEmail.setHint("Enter new email");
        inputNewEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        builder.setView(viewInflated);

        builder.setPositiveButton("Proceed", (dialog, which) -> {
            String password = inputPassword.getText().toString();
            String newEmail = inputNewEmail.getText().toString().trim();

            if (TextUtils.isEmpty(password) || TextUtils.isEmpty(newEmail)) {
                Toast.makeText(BuyerProfile.this, "Password and new email are required.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                Toast.makeText(BuyerProfile.this, "Enter a valid email address.", Toast.LENGTH_SHORT).show();
                return;
            }

            reauthenticateAndChangeEmail(password, newEmail);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void reauthenticateAndChangeEmail(String password, final String newEmail) {
        if (currentUser == null || currentUser.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), password);
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        currentUser.verifyBeforeUpdateEmail(newEmail) // Firebase handles verification email
                                .addOnCompleteListener(updateEmailTask -> {
                                    if (updateEmailTask.isSuccessful()) {
                                        updateFirestoreField("email", newEmail); // Update in Firestore as well
                                        Toast.makeText(BuyerProfile.this, "Verification email sent to new address. Email will update after verification.", Toast.LENGTH_LONG).show();
                                        // UI will update email once user re-verifies and possibly re-logs in or when loadUserProfile is called next.
                                    } else {
                                        Toast.makeText(BuyerProfile.this, "Failed to update email: " + updateEmailTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(BuyerProfile.this, "Re-authentication failed: " + reauthTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }


    private void promptForPasswordAndChangePassword() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Password");

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
                Toast.makeText(BuyerProfile.this, "All password fields are required.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                Toast.makeText(BuyerProfile.this, "New passwords do not match.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (newPassword.length() < 6) {
                Toast.makeText(BuyerProfile.this, "New password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
                return;
            }

            reauthenticateAndChangePassword(currentPassword, newPassword);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void reauthenticateAndChangePassword(String currentPassword, final String newPassword) {
        if (currentUser == null || currentUser.getEmail() == null) return;

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPassword);
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(reauthTask -> {
                    if (reauthTask.isSuccessful()) {
                        currentUser.updatePassword(newPassword)
                                .addOnCompleteListener(updatePasswordTask -> {
                                    if (updatePasswordTask.isSuccessful()) {
                                        Toast.makeText(BuyerProfile.this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(BuyerProfile.this, "Failed to update password: " + updatePasswordTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        Toast.makeText(BuyerProfile.this, "Re-authentication failed: " + reauthTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
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
        Toast.makeText(BuyerProfile.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
        navigateToLogin();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(BuyerProfile.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    public void showPrivacyPolicy() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Privacy Policy");
        builder.setMessage(R.string.privacy_policy_text); // Ensure this string exists
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void setupBottomNavigation() {
        if (bottomNavigationViewBuyer != null) {
            bottomNavigationViewBuyer.setSelectedItemId(R.id.navigation_profile);

            bottomNavigationViewBuyer.setOnNavigationItemSelectedListener(item -> {
                Intent intent = null;
                int itemId = item.getItemId();

                if (itemId == R.id.navigation_home) {
                    intent = new Intent(BuyerProfile.this, Homepage.class);
                } else if (itemId == R.id.navigation_chat) {
                    intent = new Intent(BuyerProfile.this, Chat_Buyer.class);
                } else if (itemId == R.id.navigation_profile) {
                    return true; // Already on this screen
                }

                if (intent != null) {
                    startActivity(intent);
                    overridePendingTransition(0,0);
                    finish();
                    return true;
                }
                return false;
            });
        }
    }
}
