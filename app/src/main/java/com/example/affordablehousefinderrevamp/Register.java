package com.example.affordablehousefinderrevamp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Register extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
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