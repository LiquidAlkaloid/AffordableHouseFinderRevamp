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

public class SellerRegister extends AppCompatActivity {
    private EditText nameEt, emailEt, passwordEt, phoneEt, addressEt;
    private Button signupBtn;
    private TextView loginPrompt;
    private Button buyerRegisterButton; // Added for the new button
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameEt = findViewById(R.id.name_edittext);
        emailEt = findViewById(R.id.email_edittext);
        passwordEt = findViewById(R.id.password_edittext);
        phoneEt = findViewById(R.id.phone_edittext_seller);
        addressEt = findViewById(R.id.address_edittext);
        signupBtn = findViewById(R.id.signup_button);
        loginPrompt = findViewById(R.id.login_prompt_textview);
        // Initialize the "Buyer Register" button
        buyerRegisterButton = findViewById(R.id.buyer_register_button);

        signupBtn.setOnClickListener(v -> registerSeller());
        loginPrompt.setOnClickListener(v -> {
            startActivity(new Intent(SellerRegister.this, Login.class));
            // finish(); // Optional: finish current activity
        });

        // Set OnClickListener for the "Buyer Register" button
        if (buyerRegisterButton != null) {
            buyerRegisterButton.setOnClickListener(v -> {
                Intent intent = new Intent(SellerRegister.this, BuyerRegister.class);
                startActivity(intent);
                // finish(); // Optional: finish current activity
            });
        }
    }

    private void registerSeller() {
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
                            User user = new User(uid, name, email, phone, addr, "seller");
                            // As noted in BuyerRegister, the 'password' field in User model is generally not needed
                            // when using Firebase Auth.

                            db.collection("users").document(uid).set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(SellerRegister.this, "Registration successful", Toast.LENGTH_SHORT).show();
                                        // Send email verification
                                        firebaseUser.sendEmailVerification()
                                                .addOnCompleteListener(verificationTask -> {
                                                    if (verificationTask.isSuccessful()) {
                                                        Toast.makeText(SellerRegister.this, "Verification email sent.", Toast.LENGTH_LONG).show();
                                                    } else {
                                                        Toast.makeText(SellerRegister.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                        // Navigate to Login screen after successful registration
                                        Intent intent = new Intent(SellerRegister.this, Login.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Clear back stack
                                        startActivity(intent);
                                        finish(); // Finish SellerRegister activity
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(SellerRegister.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(SellerRegister.this, "Registration failed: User not created.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(SellerRegister.this, "Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
