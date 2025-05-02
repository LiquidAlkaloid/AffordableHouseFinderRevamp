package com.example.affordablehousefinderrevamp;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PropertyDetail extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_property_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}

/**
 How to Use:

 Save the main XML code as activity_house_details.xml in res/layout.
 Create the gradient and button background drawables (gradient_bottom_dark.xml, button_background_pink.xml) in res/drawable.
 Add necessary placeholder images (house_placeholder) and vector icons (ic_close, ic_bookmark_border) to res/drawable. You can get standard icons from Android Studio's Vector Asset tool.
 Add the color definitions to res/values/colors.xml.
 Add the string definitions to res/values/strings.xml.
 Add the RatingBarAccent style to res/values/styles.xml.
 Set this layout in your corresponding Activity (HouseDetailsActivity.java or .kt) using setContentView(R.layout.activity_house_details);.
 "See All" Functionality: You will need to add Java/Kotlin code:
 Set an OnClickListener on seeAllTextView.
 Initially, set detailsDescription.setMaxLines(N) (e.g., N=10) to truncate the text.
 In the click listener, toggle between the max lines and showing the full text (e.g., by setting detailsDescription.setMaxLines(Integer.MAX_VALUE)) and potentially change the "See All" text to "See Less".
 Image Gallery: Replace the ImageView (houseImageView) with a androidx.viewpager2.widget.ViewPager2 and implement its adapter for a functional image gallery. You'll also need to add indicator dots (e.g., using a TabLayout or a custom solution) and potentially the left/right navigation arrows if desired.
 * **/