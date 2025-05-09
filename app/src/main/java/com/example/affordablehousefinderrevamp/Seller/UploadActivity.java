package com.example.affordablehousefinderrevamp.Seller;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.affordablehousefinderrevamp.R;

import java.util.ArrayList;
import java.util.List;

public class UploadActivity extends AppCompatActivity implements GalleryImageAdapter.OnNewPhotoClickListener, GalleryImageAdapter.OnImageClickListener {

    private static final String TAG = "UploadActivity";
    private static final int MAX_RECENT_IMAGES = 100; // Max images to load initially

    private RecyclerView galleryRecyclerView;
    private GalleryImageAdapter galleryAdapter;
    private ImageButton closeButton;
    private Button nextButton;

    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String[]> requestPermissionsLauncher; // For multiple permissions if needed

    private final String[] REQUIRED_PERMISSIONS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
            new String[]{Manifest.permission.READ_MEDIA_IMAGES} :
            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        Toolbar toolbar = findViewById(R.id.upload_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Using custom title area
        }

        closeButton = findViewById(R.id.upload_close_button);
        nextButton = findViewById(R.id.upload_next_button);

        closeButton.setOnClickListener(v -> finish());
        nextButton.setOnClickListener(v -> {
            // TODO: Implement what "Next" button does (e.g., proceed with selected images)
            Toast.makeText(UploadActivity.this, "Next button clicked", Toast.LENGTH_SHORT).show();
        });

        galleryRecyclerView = findViewById(R.id.gallery_recyclerview);
        setupRecyclerView();
        initializeLaunchers();

        applyWindowInsets(toolbar);

        // Check permissions and load initial images
        if (allPermissionsGranted()) {
            loadRecentImages();
        } else {
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS);
        }
    }

    private void initializeLaunchers() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            Log.d(TAG, "Image selected via picker: " + imageUri.toString());
                            galleryAdapter.addImageUri(imageUri); // Add to adapter
                            galleryRecyclerView.scrollToPosition(0); // Scroll to show the new item
                            Toast.makeText(this, "Image added!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        requestPermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                permissions -> {
                    boolean allGranted = true;
                    for (Boolean granted : permissions.values()) {
                        if (!granted) {
                            allGranted = false;
                            break;
                        }
                    }
                    if (allGranted) {
                        Log.d(TAG, "All permissions granted.");
                        loadRecentImages();
                    } else {
                        Log.w(TAG, "Storage permission denied.");
                        Toast.makeText(this, R.string.permission_rationale_storage, Toast.LENGTH_LONG).show();
                        // Optionally, show a dialog explaining why permissions are needed or disable functionality.
                        // For now, the gallery will remain empty or only show what was picked.
                        galleryAdapter.setImageUris(new ArrayList<>()); // Show empty or previously picked
                    }
                });
    }

    private void setupRecyclerView() {
        galleryAdapter = new GalleryImageAdapter(this, this, this);
        galleryRecyclerView.setAdapter(galleryAdapter);
        galleryRecyclerView.setLayoutManager(new GridLayoutManager(this, 3)); // 3 columns
        // You can add ItemDecoration for spacing if needed
    }

    private void applyWindowInsets(Toolbar toolbar) {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.upload_main_layout), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            // Apply padding for status bar and navigation bar
            v.setPadding(insets.left, 0, insets.right, 0); // Let toolbar handle top

            // Adjust toolbar's top margin or padding for status bar
            ViewGroup.MarginLayoutParams toolbarParams = (ViewGroup.MarginLayoutParams) toolbar.getLayoutParams();
            toolbarParams.topMargin = insets.top;
            toolbar.setLayoutParams(toolbarParams);

            // Adjust RecyclerView's bottom padding for navigation bar
            galleryRecyclerView.setPadding(galleryRecyclerView.getPaddingLeft(), galleryRecyclerView.getPaddingTop(),
                    galleryRecyclerView.getPaddingRight(), insets.bottom + galleryRecyclerView.getPaddingTop()); // Add to existing top padding

            return WindowInsetsCompat.CONSUMED;
        });
    }


    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void loadRecentImages() {
        Log.d(TAG, "Loading recent images...");
        List<Uri> recentImageUris = new ArrayList<>();
        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.MIME_TYPE
        };
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                collection,
                projection,
                null, // No selection criteria
                null, // No selection arguments
                sortOrder + " LIMIT " + MAX_RECENT_IMAGES // Limit results
        )) {
            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri contentUri = Uri.withAppendedPath(collection, Long.toString(id));
                    recentImageUris.add(contentUri);
                }
                Log.d(TAG, "Loaded " + recentImageUris.size() + " recent images.");
            } else {
                Log.e(TAG, "Cursor was null when loading recent images.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading recent images", e);
        }
        galleryAdapter.setImageUris(recentImageUris);
    }


    @Override
    public void onNewPhotoClick() {
        Log.d(TAG, "New Photo clicked by user.");
        if (allPermissionsGranted()) {
            openSystemGallery();
        } else {
            // This should ideally not be hit if permissions were checked on activity start,
            // but as a fallback:
            requestPermissionsLauncher.launch(REQUIRED_PERMISSIONS);
        }
    }

    private void openSystemGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // For a more general file picker:
        // Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        // intent.setType("image/*");
        // intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            pickImageLauncher.launch(intent);
        } else {
            Toast.makeText(this, "No gallery app found.", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No activity found to handle ACTION_PICK for images.");
        }
    }

    @Override
    public void onImageClick(Uri imageUri, int position) {
        Log.d(TAG, "Image clicked: " + imageUri.toString() + " at position: " + position);
        // TODO: Implement what happens when an image is clicked (e.g., select for upload, view full screen)
        Toast.makeText(this, "Selected: " + imageUri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
    }
}
