package com.example.affordablehousefinderrevamp.Seller;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
// import androidx.core.content.FileProvider; // Not used if only caching locally
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
// import android.os.Environment; // Not strictly needed for app-specific cache
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
// import com.google.firebase.storage.FirebaseStorage; // Not using Firebase Storage for images
// import com.google.firebase.storage.StorageReference; // Not using Firebase Storage for images
// import com.google.firebase.storage.UploadTask; // Not using Firebase Storage for images

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

    private ArrayList<Uri> imageUrisForAdapter; // Holds Uris for GalleryImageAdapter (can be content:// or file://)
    private ArrayList<String> imageStringsForFirestore; // Holds String representations of file:// Uris after caching

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
        propertiesCollection = db.collection("Properties"); // Corrected collection name to match adapter

        imageUrisForAdapter = new ArrayList<>();
        imageStringsForFirestore = new ArrayList<>();

        galleryImageAdapter = new GalleryImageAdapter(this, imageUrisForAdapter, (uriToRemove, position) -> {
            if (position >= 0 && position < imageUrisForAdapter.size()) {
                Uri removedAdapterUri = imageUrisForAdapter.remove(position);
                // Also remove from imageStringsForFirestore if it was a cached file URI string
                if (removedAdapterUri != null && "file".equals(removedAdapterUri.getScheme())) {
                    imageStringsForFirestore.remove(removedAdapterUri.toString());
                } else if (removedAdapterUri != null) {
                    // If it was a content URI, it might not be in imageStringsForFirestore directly
                    // This part needs careful handling if you mix original and cached URIs
                    Log.w(TAG, "Removed a non-file URI from adapter, ensure consistency with Firestore strings: " + removedAdapterUri);
                }

                galleryImageAdapter.notifyItemRemoved(position);
                galleryImageAdapter.notifyItemRangeChanged(position, imageUrisForAdapter.size());
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
                            propertyToEdit.setPropertyId(documentSnapshot.getId()); // Ensure ID is set
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
        editTextTitle.setText(propertyToEdit.getName()); // Corrected to getName
        editTextLocation.setText(propertyToEdit.getLocation());
        editTextPrice.setText(propertyToEdit.getPrice());
        editTextDescription.setText(propertyToEdit.getDescription());
        editTextPropertyType.setText(propertyToEdit.getPropertyType());
        editTextBedrooms.setText(propertyToEdit.getNumBedrooms()); // Corrected
        editTextBathrooms.setText(propertyToEdit.getNumBathrooms()); // Corrected
        editTextArea.setText(propertyToEdit.getArea());

        imageUrisForAdapter.clear();
        imageStringsForFirestore.clear();
        if (propertyToEdit.getImageUrls() != null && !propertyToEdit.getImageUrls().isEmpty()) {
            for (String uriString : propertyToEdit.getImageUrls()) {
                if (uriString == null || uriString.isEmpty()) continue;
                try {
                    Uri parsedUri = Uri.parse(uriString);
                    imageUrisForAdapter.add(parsedUri); // Add to adapter list
                    // Assuming these are already file:// URIs from previous saves
                    if ("file".equals(parsedUri.getScheme())) {
                        imageStringsForFirestore.add(uriString);
                    } else {
                        // If it's a content URI from an older version, it should be re-cached
                        // For simplicity, we'll assume they are file URIs or need re-selection
                        Log.w(TAG, "Non-file URI found during edit: " + uriString + ". Consider re-caching or user re-selection.");
                    }
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
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activityResultLauncher.launch(Intent.createChooser(intent, getString(R.string.select_pictures)));
    }

    ActivityResultLauncher<Intent> activityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        // Clear previous selections before adding new ones if that's the desired behavior
                        // If appending, remove these clear() calls. For editing, usually replace.
                        imageUrisForAdapter.clear();
                        imageStringsForFirestore.clear();

                        List<Uri> urisToProcess = new ArrayList<>();
                        if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                urisToProcess.add(clipData.getItemAt(i).getUri());
                            }
                        } else if (data.getData() != null) {
                            urisToProcess.add(data.getData());
                        }

                        if (!urisToProcess.isEmpty()) {
                            progressDialog.setMessage("Processing images...");
                            progressDialog.show();
                            new Thread(() -> {
                                for (Uri uri : urisToProcess) {
                                    if (uri != null) {
                                        try {
                                            // Persist permission for content URIs
                                            if ("content".equals(uri.getScheme())) {
                                                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                            }
                                        } catch (SecurityException e) {
                                            Log.w(TAG, "Failed to take persistable URI permission (might be normal for some URIs): " + uri.toString(), e);
                                        }
                                        Uri cachedFileUri = copyUriToInternalCache(uri);
                                        if (cachedFileUri != null) {
                                            // Run on UI thread because these lists are bound to the adapter
                                            runOnUiThread(() -> {
                                                imageUrisForAdapter.add(cachedFileUri); // For adapter display
                                                imageStringsForFirestore.add(cachedFileUri.toString()); // For Firestore
                                            });
                                        } else {
                                            Log.w(TAG, "Failed to cache image: " + uri.toString());
                                            runOnUiThread(()-> Toast.makeText(UploadActivity.this, "Failed to process one image", Toast.LENGTH_SHORT).show());
                                        }
                                    }
                                }
                                runOnUiThread(() -> {
                                    dismissProgressDialog();
                                    galleryImageAdapter.notifyDataSetChanged(); // Notify after all processing
                                    if (!imageUrisForAdapter.isEmpty()) {
                                        Toast.makeText(this, getString(R.string.images_selected, imageUrisForAdapter.size()), Toast.LENGTH_SHORT).show();
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
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_" + UUID.randomUUID().toString().substring(0, 6);
            File cacheDir = getApplicationContext().getCacheDir();
            File outputFile = new File(cacheDir, imageFileName + ".jpg");

            try (OutputStream outputStream = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[1024 * 4];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                Log.d(TAG, "Image copied to cache: " + outputFile.getAbsolutePath());
                return Uri.fromFile(outputFile); // file:// URI
            }
        } catch (Exception e) { // Catch generic exception for broader error logging
            Log.e(TAG, "Error copying URI to internal cache: " + sourceUri.toString(), e);
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
        // Bedrooms and bathrooms are already strings, no need to parse if model expects strings.
        // If model expects int, then parse here. Our model uses String for numBedrooms/Bathrooms.

        FirebaseUser currentUserData = firebaseAuth.getCurrentUser();
        if (currentUserData == null) {
            Toast.makeText(this, getString(R.string.login_required), Toast.LENGTH_SHORT).show();
            return;
        }
        String sellerId = currentUserData.getUid();
        String sellerName = currentUserData.getDisplayName() != null ? currentUserData.getDisplayName() : "Unknown Seller"; // Get seller name
        String sellerContact = currentUserData.getPhoneNumber() != null ? currentUserData.getPhoneNumber() : ""; // Get seller contact if available

        progressDialog.setMessage(editingPropertyId != null ? getString(R.string.updating_property) : getString(R.string.uploading_property));
        progressDialog.show();

        String currentStatus;
        Date existingTimestamp = null;

        if (editingPropertyId != null && propertyToEdit != null) {
            currentStatus = propertyToEdit.getStatus(); // Preserve existing status
            existingTimestamp = propertyToEdit.getTimestamp(); // Preserve existing timestamp
        } else {
            currentStatus = Property.STATUS_AVAILABLE; // Default for new property
        }

        // Corrected constructor call
        Property property = new Property(title, location, price, description,
                new ArrayList<>(imageStringsForFirestore), // Pass the list of string URIs
                sellerId, propertyType, bedroomsStr, bathroomsStr, area, currentStatus);

        property.setSellerName(sellerName); // Set seller name
        property.setSellerContact(sellerContact); // Set seller contact

        DocumentReference propertyDocRef;
        if (editingPropertyId != null) {
            propertyDocRef = propertiesCollection.document(editingPropertyId);
            property.setPropertyId(editingPropertyId); // Ensure ID is set for existing property
            if (existingTimestamp != null) {
                property.setTimestamp(existingTimestamp); // Preserve original timestamp if editing
            } else {
                // If relying on @ServerTimestamp, Firestore will set it on update.
                // If managing client-side and it was null, it will remain null unless set here.
                // For @ServerTimestamp, we don't need to set it manually for updates unless we want to force a new one.
            }
        } else {
            propertyDocRef = propertiesCollection.document(); // New property, Firestore generates ID
            property.setPropertyId(propertyDocRef.getId());
            // For new properties, @ServerTimestamp will set the timestamp.
            // If not using @ServerTimestamp, set it here: property.setTimestamp(new Date());
        }


        propertyDocRef.set(property, SetOptions.merge()) // Use merge to be safe with existing docs
                .addOnSuccessListener(aVoid -> {
                    dismissProgressDialog();
                    Toast.makeText(UploadActivity.this,
                            editingPropertyId != null ? getString(R.string.property_updated_successfully) : getString(R.string.property_saved_successfully),
                            Toast.LENGTH_SHORT).show();
                    clearForm();
                    Log.i(TAG, "Property " + (editingPropertyId != null ? "updated" : "saved") + " successfully with ID: " + propertyDocRef.getId());
                    finish(); // Go back after successful upload/update
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
        imageUrisForAdapter.clear();
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
            onBackPressed(); // Or finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
