package com.example.affordablehousefinderrevamp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class Login extends AppCompatActivity {

    DatabaseReference databaseReference;

    // Reference to "Users" node in Firebase
    databaseReference = FirebaseDatabase.getInstance().getReference("Users");

    private EditText editEmailLogin, editPasswordLogin;
    private Button btnLogin, Signupbuttonbottom;
    private TextView textView5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editEmailLogin = findViewById(R.id.editEmailLogin);
        editPasswordLogin = findViewById(R.id.editPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        Signupbuttonbottom = findViewById(R.id.Signupbuttonbottom);
        textView5 = findViewById(R.id.textView5);

        btnLogin.setOnClickListener(v -> {
            String email = editEmailLogin.getText().toString().trim();
            String password = editPasswordLogin.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(Login.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Add your authentication logic here
                Toast.makeText(Login.this, "Login attempt", Toast.LENGTH_SHORT).show();
            }
        });

        Signupbuttonbottom.setOnClickListener(v -> {
            // Redirect to role selection or directly to buyer registration
            Intent intent = new Intent(Login.this, BuyerRegister.class);
            startActivity(intent);
        });

        textView5.setOnClickListener(v -> {
            // Handle forgot password
            Toast.makeText(Login.this, "Forgot password clicked", Toast.LENGTH_SHORT).show();
        });
    }
}