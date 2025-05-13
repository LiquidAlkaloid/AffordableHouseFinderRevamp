package com.example.affordablehousefinderrevamp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
// Removed HashMap and Map imports as User object is used directly
// import java.util.HashMap;
// import java.util.Map;

public class BuyerRegister extends AppCompatActivity {
    private EditText nameEt, emailEt, passwordEt, phoneEt, addressEt;
    private Button signupBtn;
    private TextView loginPrompt;
    private Button sellerRegisterButton; // Added for the new button
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameEt = findViewById(R.id.name_edittext_buyer);
        emailEt = findViewById(R.id.email_edittext_buyer);
        passwordEt = findViewById(R.id.password_edittext_buyer);
        phoneEt = findViewById(R.id.phone_edittext_buyer);
        addressEt = findViewById(R.id.address_edittext_buyer);
        signupBtn = findViewById(R.id.signup_button_buyer);
        loginPrompt = findViewById(R.id.login_prompt_textview_buyer);
        // Initialize the "Seller Register" button
        sellerRegisterButton = findViewById(R.id.seller_register_button_buyer);


        signupBtn.setOnClickListener(v -> registerBuyer());
        loginPrompt.setOnClickListener(v -> {
            startActivity(new Intent(BuyerRegister.this, Login.class));
            // It's good practice to finish the current registration activity
            // if the user navigates to Login, to prevent stacking.
            // However, this depends on your desired navigation flow.
            // finish();
        });

        // Set OnClickListener for the "Seller Register" button
        if (sellerRegisterButton != null) {
            sellerRegisterButton.setOnClickListener(v -> {
                Intent intent = new Intent(BuyerRegister.this, SellerRegister.class);
                startActivity(intent);
                // Optionally finish this activity if you don't want users to go back to it
                // after navigating to seller registration.
                // finish();
            });
        }
    }

    private void registerBuyer() {
        String name = nameEt.getText().toString().trim();
        String email = emailEt.getText().toString().trim();
        String pass = passwordEt.getText().toString(); // No trim for password
        String phone = phoneEt.getText().toString().trim();
        String addr = addressEt.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)
                || TextUtils.isEmpty(phone) || TextUtils.isEmpty(addr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Basic password validation (e.g., length)
        if (pass.length() < 6) {
            passwordEt.setError("Password must be at least 6 characters");
            passwordEt.requestFocus();
            return;
        }

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) { // Check if firebaseUser is not null
                            String uid = firebaseUser.getUid();
                            // Save to Firestore using the User model
                            User user = new User(uid, name, email, phone, addr, "buyer");
                            // Note: The User model has a 'password' field, but it's generally not good practice
                            // to store plain text passwords in Firestore. Firebase Auth handles password storage securely.
                            // The 'password' field in your User model might be redundant or for a different purpose.
                            // If it's for storing the password, consider removing it or hashing it if absolutely necessary
                            // (though Firebase Auth already does this). For now, I'll assume the User constructor handles it.

                            db.collection("users").document(uid).set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(BuyerRegister.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                        // Send email verification
                                        firebaseUser.sendEmailVerification()
                                                .addOnCompleteListener(verificationTask -> {
                                                    if (verificationTask.isSuccessful()) {
                                                        Toast.makeText(BuyerRegister.this, "Verification email sent.", Toast.LENGTH_LONG).show();
                                                    } else {
                                                        Toast.makeText(BuyerRegister.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                        // Navigate to Login screen after successful registration and Firestore save
                                        Intent intent = new Intent(BuyerRegister.this, Login.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear back stack
                                        startActivity(intent);
                                        finish(); // Finish BuyerRegister activity
                                    })
                                    .addOnFailureListener(e -> {
                                        // If Firestore save fails, you might want to delete the Firebase Auth user
                                        // to allow re-registration, or handle it differently.
                                        Toast.makeText(BuyerRegister.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(BuyerRegister.this, "Registration failed: User not created.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(BuyerRegister.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
