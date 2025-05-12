package com.example.affordablehousefinderrevamp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {
    private EditText emailEt, passwordEt;
    private Button loginBtn, signupBtn;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEt = findViewById(R.id.editEmailLogin);
        passwordEt = findViewById(R.id.editPasswordLogin);
        loginBtn = findViewById(R.id.btnLogin);
        signupBtn = findViewById(R.id.Signupbuttonbottom);

        loginBtn.setOnClickListener(v -> loginUser());
        signupBtn.setOnClickListener(v -> startActivity(new Intent(this, BuyerRegister.class)));
    }

    private void loginUser() {
        String email = emailEt.getText().toString().trim();
        String pass = passwordEt.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        // Fetch userType from Firestore
                        db.collection("users").document(firebaseUser.getUid()).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc.exists()) {
                                        String type = doc.getString("userType");
                                        if ("buyer".equals(type)) {
                                            startActivity(new Intent(this, com.example.affordablehousefinderrevamp.Buyer.Homepage.class));
                                        } else {
                                            startActivity(new Intent(this, com.example.affordablehousefinderrevamp.Seller.SellerProfile.class));
                                        }
                                        finish();
                                    } else {
                                        Toast.makeText(this, "User record not found", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}