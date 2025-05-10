package com.example.affordablehousefinderrevamp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SellerRegister extends AppCompatActivity {

    private EditText nameEditText, emailEditText, passwordEditText, phoneEditText, addressEditText;
    private Button signupButton, buyerRegisterButton;
    private TextView loginPromptTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seller_register);

        // Initialize views
        nameEditText = findViewById(R.id.name_edittext);
        emailEditText = findViewById(R.id.email_edittext);
        passwordEditText = findViewById(R.id.password_edittext);
        phoneEditText = findViewById(R.id.phone_edittext_seller);
        addressEditText = findViewById(R.id.address_edittext);
        signupButton = findViewById(R.id.signup_button);
        buyerRegisterButton = findViewById(R.id.buyer_register_button);
        loginPromptTextView = findViewById(R.id.login_prompt_textview);

        signupButton.setOnClickListener(v -> registerSeller());

        buyerRegisterButton.setOnClickListener(v -> {
            Intent intent = new Intent(SellerRegister.this, BuyerRegister.class);
            startActivity(intent);
            finish();
        });

        loginPromptTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SellerRegister.this, Login.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerSeller() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String address = addressEditText.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phone.isEmpty() || address.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a new seller user
        String userId = "user_" + System.currentTimeMillis(); // Generate a simple ID
        User seller = new User(userId, name, email, password, phone, address, "seller");

        // Here you would typically save the user to a database
        // For now, just show a success message
        Toast.makeText(this, "Seller registration successful!", Toast.LENGTH_SHORT).show();

        // Redirect to login
        Intent intent = new Intent(SellerRegister.this, Login.class);
        startActivity(intent);
        finish();
    }
}

/**
 How to Use:

 Save the main XML code as a layout file (e.g., activity_create_account.xml) in your project's res/layout directory.
 Create the three drawable XML files (shape_*.xml) in the res/drawable directory.
 Add the color definitions to your res/values/colors.xml file.
 Add the string definitions to your res/values/strings.xml file.
 (Optional) Add the style definitions to your res/values/styles.xml or themes.xml file.
 Replace placeholder drawable names (@drawable/ic_logo_chargit) with your actual logo file located in res/drawable.
 In your corresponding Activity (e.g., CreateAccountActivity.java or .kt), set this layout using setContentView(R.layout.activity_create_account);.
 You will need to add Java/Kotlin code to handle button clicks (signupButton) and to show a DatePickerDialog when the Date of Birth field (dobEditText) is clicked. You'll also need logic to make the "Log in here" part of the subtitle clickable if desired.
 This code provides a solid starting point based on the visual structure of the image. You may need to fine-tune padding, margins, text sizes, and colors to get an exact match.
 * **/