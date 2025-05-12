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

        signupBtn.setOnClickListener(v -> registerSeller());
        loginPrompt.setOnClickListener(v -> startActivity(new Intent(this, Login.class)));
    }

    private void registerSeller() {
        String name = nameEt.getText().toString().trim();
        String email = emailEt.getText().toString().trim();
        String pass = passwordEt.getText().toString();
        String phone = phoneEt.getText().toString().trim();
        String addr = addressEt.getText().toString().trim();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)
                || TextUtils.isEmpty(phone) || TextUtils.isEmpty(addr)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        String uid = firebaseUser.getUid();
                        User user = new User(uid, name, email, phone, addr, "seller");
                        db.collection("users").document(uid).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, Login.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Auth failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}