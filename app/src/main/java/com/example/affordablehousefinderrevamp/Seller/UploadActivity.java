package com.example.affordablehousefinderrevamp.Seller;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.affordablehousefinderrevamp.Model.Property;
import com.example.affordablehousefinderrevamp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class UploadActivity extends AppCompatActivity {

    private static final String TAG = "UploadActivity";

    private EditText editTextTitle, editTextLocation, editTextPrice, editTextDescription,
            editTextPropertyType, editTextBedrooms, editTextBathrooms, editTextArea;
    private Button buttonSelectImages, buttonUploadProperty;
    private RecyclerView recyclerViewSelectedImages;
    private Toolbar toolbar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private CollectionReference propertiesCollection;

    // imageUris is for the GalleryImageAdapter to display previews using local file:// URIs of cached images
    private ArrayList<Uri> imageUris;
    // imageStringsForFirestore will store Uri.toString() of these file:// URIs for saving to Firestore.
    private ArrayList<String> imageStringsForFirestore;

    private GalleryImageAdapter galleryImageAdapter;
    private ProgressDialog progressDialog;

    private String editingPropertyId = null;
    private Property propertyToEdit = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        toolbar = findViewById(R.id.upload_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize UI elements
        editTextTitle = findViewById(R.id.editTextTitle);
        editTextLocation = findViewById(R.id.editTextLocation);
        editTextPrice = findViewById(R.id.editTextPrice);
        editTextDescription = findViewById(R.id.editTextDescription);
        editTextPropertyType = findViewById(R.id.editTextPropertyType);
        editTextBedrooms = findViewById(R.id.editTextBedrooms);
        editTextBathrooms = findViewById(R.id.editTextBathrooms);
        editTextArea = findViewById(R.id.editTextArea);
        buttonSelectImages = findViewById(R.id.buttonSelectImages);
        buttonUploadProperty = findViewById(R.id.buttonUpload);
        recyclerViewSelectedImages = findViewById(R.id.recyclerViewSelectedImages);

        if (editTextTitle == null || buttonSelectImages == null || recyclerViewSelectedImages == null || buttonUploadProperty == null) {
            Log.e(TAG, "CRITICAL ERROR: UI elements not found. Check activity_upload.xml layout IDs.");
            Toast.makeText(this, "Layout error. Cannot proceed.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        propertiesCollection = db.collection("properties");

        imageUris = new ArrayList<>();
        imageStringsForFirestore = new ArrayList<>();

        galleryImageAdapter = new GalleryImageAdapter(this, imageUris, (uriToRemove, position) -> {
            if (position >= 0 && position < imageUris.size()) {
                Uri removedUri = imageUris.remove(position);
                if (removedUri != null) {
                    imageStringsForFirestore.remove(removedUri.toString());
                    // Optionally, delete the cached file if it's a file URI from our cache
                    if ("file".equals(removedUri.getScheme())) {
                        File fileToDelete = new File(removedUri.getPath());
                        if (fileToDelete.exists()) {
                            if (fileToDelete.delete()) {
                                Log.d(TAG, "Cached image file deleted: " + removedUri.getPath());
                            } else {
                                Log.w(TAG, "Failed to delete cached image file: " + removedUri.getPath());
                            }
                        }
                    }
                }
                galleryImageAdapter.notifyItemRemoved(position);
                galleryImageAdapter.notifyItemRangeChanged(position, imageUris.size());
                Toast.makeText(UploadActivity.this, getString(R.string.image_removed), Toast.LENGTH_SHORT).show();
            }
        });
        recyclerViewSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerViewSelectedImages.setAdapter(galleryImageAdapter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(false);

        if (getIntent().hasExtra("propertyIdToEdit")) {
            editingPropertyId = getIntent().getStringExtra("propertyIdToEdit");
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(getString(R.string.edit_property_title));
            buttonUploadProperty.setText(getString(R.string.button_update_property));
            progressDialog.setMessage(getString(R.string.loading_property_data));
            progressDialog.show();
            loadPropertyDataForEditing();
        } else {
            if (getSupportActionBar() != null) getSupportActionBar().setTitle(getString(R.string.upload_property_title));
        }

        buttonSelectImages.setOnClickListener(v -> openImagePicker());
        buttonUploadProperty.setOnClickListener(v -> validateAndSaveProperty());
    }

    private void loadPropertyDataForEditing() {
        if (editingPropertyId == null) {
            dismissProgressDialog();
            Toast.makeText(this, "Error: No property ID for editing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        propertiesCollection.document(editingPropertyId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    dismissProgressDialog();
                    if (documentSnapshot.exists()) {
                        propertyToEdit = documentSnapshot.toObject(Property.class);
                        if (propertyToEdit != null) {
                            populateFieldsForEditing();
                        } else {
                            Toast.makeText(UploadActivity.this, "Error converting property data.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(UploadActivity.this, getString(R.string.failed_to_load_property_edit), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    Toast.makeText(UploadActivity.this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error loading property for edit", e);
                    finish();
                });
    }

    private void populateFieldsForEditing() {
        if (propertyToEdit == null) return;
        editTextTitle.setText(propertyToEdit.getTitle());
        editTextLocation.setText(propertyToEdit.getLocation());
        editTextPrice.setText(propertyToEdit.getPrice());
        editTextDescription.setText(propertyToEdit.getDescription());
        editTextPropertyType.setText(propertyToEdit.getPropertyType());
        editTextBedrooms.setText(String.valueOf(propertyToEdit.getBedrooms()));
        editTextBathrooms.setText(String.valueOf(propertyToEdit.getBathrooms()));
        editTextArea.setText(propertyToEdit.getArea());

        imageUris.clear();
        imageStringsForFirestore.clear();
        if (propertyToEdit.getImageUrls() != null && !propertyToEdit.getImageUrls().isEmpty()) {
            for (String uriString : propertyToEdit.getImageUrls()) {
                if (uriString == null || uriString.isEmpty()) continue;
                try {
                    // Assuming these are file:// URIs from previous saves or content:// URIs from older versions
                    Uri parsedUri = Uri.parse(uriString);
                    imageUris.add(parsedUri);
                    imageStringsForFirestore.add(uriString);
                } catch (Exception e) {
                    Log.e(TAG, getString(R.string.error_parsing_image_uri) + ": '" + uriString + "'", e);
                }
            }
            galleryImageAdapter.notifyDataSetChanged();
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Request read permission
        activityResultLauncher.launch(Intent.createChooser(intent, getString(R.string.select_pictures)));
    }

    ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        imageUris.clear();
                        imageStringsForFirestore.clear();

                        List<Uri> urisToProcess = new ArrayList<>();
                        if (data.getClipData() != null) { // Multiple images selected
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                urisToProcess.add(clipData.getItemAt(i).getUri());
                            }
                        } else if (data.getData() != null) { // Single image selected
                            urisToProcess.add(data.getData());
                        }

                        if (!urisToProcess.isEmpty()) {
                            progressDialog.setMessage("Processing images...");
                            progressDialog.show();
                            // Process URIs (copy to cache) on a background thread if it's time-consuming
                            new Thread(() -> {
                                for (Uri uri : urisToProcess) {
                                    if (uri != null) {
                                        // Attempt to take persistable permission (might not always be needed if immediately copying)
                                        try {
                                            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        } catch (SecurityException e) {
                                            Log.w(TAG, "Failed to take persistable URI permission (might be normal for some URIs): " + uri.toString(), e);
                                        }
                                        Uri cachedFileUri = copyUriToInternalCache(uri);
                                        if (cachedFileUri != null) {
                                            imageUris.add(cachedFileUri);
                                            imageStringsForFirestore.add(cachedFileUri.toString());
                                        } else {
                                            Log.w(TAG, "Failed to cache image: " + uri.toString());
                                            // Optionally show a toast on the UI thread for the failed image
                                            runOnUiThread(()-> Toast.makeText(UploadActivity.this, "Failed to process one image", Toast.LENGTH_SHORT).show());
                                        }
                                    }
                                }
                                runOnUiThread(() -> {
                                    dismissProgressDialog();
                                    galleryImageAdapter.notifyDataSetChanged();
                                    if (!imageUris.isEmpty()) {
                                        Toast.makeText(this, getString(R.string.images_selected, imageUris.size()), Toast.LENGTH_SHORT).show();
                                    } else if (!urisToProcess.isEmpty()){
                                        Toast.makeText(this, "Failed to process selected images.", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }).start();
                        }
                    }
                }
            });

    private Uri copyUriToInternalCache(Uri sourceUri) {
        try (InputStream inputStream = getContentResolver().openInputStream(sourceUri)) {
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + sourceUri);
                return null;
            }

            // Create a file in the app's cache directory
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_" + UUID.randomUUID().toString().substring(0, 6);
            File cacheDir = getApplicationContext().getCacheDir(); // Or getExternalCacheDir()
            File outputFile = new File(cacheDir, imageFileName + ".jpg");

            try (OutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024 * 4]; // 4KB buffer
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                Log.d(TAG, "Image copied to cache: " + outputFile.getAbsolutePath());
                // For FileProvider, if you were to share this URI outside your app (not needed for Glide internal use)
                // return FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", outputFile);
                return Uri.fromFile(outputFile); // Glide can load file:// URIs directly
            }
        } catch (IOException e) {
            Log.e(TAG, "Error copying URI to internal cache: " + sourceUri.toString(), e);
            return null;
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException while trying to open URI (this might be the original Glide issue): " + sourceUri.toString(), se);
            return null;
        }
    }


    private void validateAndSaveProperty() {
        String title = editTextTitle.getText().toString().trim();
        String location = editTextLocation.getText().toString().trim();
        String price = editTextPrice.getText().toString().trim();
        String description = editTextDescription.getText().toString().trim();
        String propertyType = editTextPropertyType.getText().toString().trim();
        String bedroomsStr = editTextBedrooms.getText().toString().trim();
        String bathroomsStr = editTextBathrooms.getText().toString().trim();
        String area = editTextArea.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(location) || TextUtils.isEmpty(price) ||
                TextUtils.isEmpty(description) || TextUtils.isEmpty(propertyType) || TextUtils.isEmpty(bedroomsStr) ||
                TextUtils.isEmpty(bathroomsStr) || TextUtils.isEmpty(area)) {
            Toast.makeText(this, getString(R.string.all_fields_required), Toast.LENGTH_SHORT).show();
            return;
        }
        if (imageStringsForFirestore.isEmpty()) {
            Toast.makeText(this, getString(R.string.please_select_image), Toast.LENGTH_SHORT).show();
            return;
        }
        int bedrooms, bathrooms;
        try {
            bedrooms = Integer.parseInt(bedroomsStr);
            bathrooms = Integer.parseInt(bathroomsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.bedrooms_bathrooms_must_be_numbers), Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUserData = firebaseAuth.getCurrentUser();
        if (currentUserData == null) {
            Toast.makeText(this, getString(R.string.login_required), Toast.LENGTH_SHORT).show();
            return;
        }
        String sellerId = currentUserData.getUid();

        progressDialog.setMessage(editingPropertyId != null ? getString(R.string.updating_property) : getString(R.string.uploading_property));
        progressDialog.show();

        String mainImageUrlString = imageStringsForFirestore.isEmpty() ? null : imageStringsForFirestore.get(0);

        Log.d(TAG, "Saving property. Main image URI string (now file://): " + mainImageUrlString);
        Log.d(TAG, "All image URI strings for Firestore (now file://): " + imageStringsForFirestore.toString());
        Log.i(TAG, "Note: Storing file:// URIs. These are local to this device and app installation. For sharing or cross-device access, images need to be uploaded to cloud storage and HTTPS URLs stored instead.");


        String currentStatus;
        if (editingPropertyId != null && propertyToEdit != null && propertyToEdit.getStatus() != null) {
            currentStatus = propertyToEdit.getStatus();
        } else {
            currentStatus = "Available";
        }

        Property property = new Property(title, location, price, description, mainImageUrlString,
                new ArrayList<>(imageStringsForFirestore),
                sellerId, propertyType, bedrooms, bathrooms, area, currentStatus);

        DocumentReference propertyDocRef;
        if (editingPropertyId != null) {
            propertyDocRef = propertiesCollection.document(editingPropertyId);
            if (propertyToEdit != null && propertyToEdit.getTimestamp() != null) {
                property.setTimestamp(propertyToEdit.getTimestamp());
            } else {
                property.setTimestamp(null);
            }
        } else {
            propertyDocRef = propertiesCollection.document();
            property.setPropertyId(propertyDocRef.getId());
            property.setTimestamp(null);
        }

        propertyDocRef.set(property, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    dismissProgressDialog();
                    Toast.makeText(UploadActivity.this,
                            editingPropertyId != null ? getString(R.string.property_updated_successfully) : getString(R.string.property_saved_successfully),
                            Toast.LENGTH_SHORT).show();
                    clearForm();
                    Log.i(TAG, "Property " + (editingPropertyId != null ? "updated" : "saved") + " successfully with ID: " + propertyDocRef.getId());
                    finish();
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    Toast.makeText(UploadActivity.this, getString(R.string.failed_to_save_property) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving property to Firestore", e);
                });
    }

    private void clearForm() {
        editTextTitle.setText("");
        editTextLocation.setText("");
        editTextPrice.setText("");
        editTextDescription.setText("");
        editTextPropertyType.setText("");
        editTextBedrooms.setText("");
        editTextBathrooms.setText("");
        editTextArea.setText("");
        imageUris.clear();
        imageStringsForFirestore.clear();
        if (galleryImageAdapter != null) {
            galleryImageAdapter.notifyDataSetChanged();
        }
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
