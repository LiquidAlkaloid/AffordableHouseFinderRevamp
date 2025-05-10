package com.example.affordablehousefinderrevamp;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class BuyerRegister extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText, phoneEditText, addressEditText;
    private Button signupButton, sellerRegisterButton;
    private TextView loginPromptTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_register);

        // Initialize views
        nameEditText = findViewById(R.id.name_edittext_buyer);
        emailEditText = findViewById(R.id.email_edittext_buyer);
        passwordEditText = findViewById(R.id.password_edittext_buyer);
        phoneEditText = findViewById(R.id.phone_edittext_buyer);
        addressEditText = findViewById(R.id.address_edittext_buyer);
        signupButton = findViewById(R.id.signup_button_buyer);
        sellerRegisterButton = findViewById(R.id.seller_register_button_buyer);
        loginPromptTextView = findViewById(R.id.login_prompt_textview_buyer);

        signupButton.setOnClickListener(v -> registerBuyer());

        sellerRegisterButton.setOnClickListener(v -> {
            Intent intent = new Intent(BuyerRegister.this, SellerRegister.class);
            startActivity(intent);
            finish();
        });

        loginPromptTextView.setOnClickListener(v -> {
            Intent intent = new Intent(BuyerRegister.this, Login.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerBuyer() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new buyer user
        String userId = "user_" + System.currentTimeMillis(); // Generate a simple ID
        User buyer = new User(userId, name, email, password, phone, address, "buyer");

        // Here you would typically save the user to a database
        // For now, just show a success message
        Toast.makeText(this, "Buyer registration successful!", Toast.LENGTH_SHORT).show();

        // Redirect to login
        Intent intent = new Intent(BuyerRegister.this, Login.class);
        startActivity(intent);
        finish();
    }
}