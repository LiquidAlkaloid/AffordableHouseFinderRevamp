package com.example.affordablehousefinderrevamp.Seller;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import com.google.firebase.firestore.CollectionReference; // Firestore
import com.google.firebase.firestore.DocumentReference;  // Firestore
import com.google.firebase.firestore.FirebaseFirestore;  // Firestore
import com.google.firebase.firestore.SetOptions; // For merging data

import java.util.ArrayList;
import java.util.Date; // For timestamp
import java.util.List;

public class UploadActivity extends AppCompatActivity {

    private static final String TAG = "UploadActivity";

    private EditText editTextTitle, editTextLocation, editTextPrice, editTextDescription,
            editTextPropertyType, editTextBedrooms, editTextBathrooms, editTextArea;
    private Button buttonSelectImages, buttonUploadProperty;
    private RecyclerView recyclerViewSelectedImages;
    private Toolbar toolbar;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db; // Firestore instance
    private CollectionReference propertiesCollection; // Firestore collection reference

    private ArrayList<Uri> imageUris;
    private ArrayList<String> imageUriStrings;
    private GalleryImageAdapter galleryImageAdapter;
    private ProgressDialog progressDialog;

    private String editingPropertyId = null;
    private Property propertyToEdit = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload); // Assumes this is the FORM layout

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
            Log.e(TAG, "CRITICAL ERROR: UI elements not found. Ensure R.layout.activity_upload is the form layout.");
            Toast.makeText(this, "Layout error.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // Initialize Firestore
        propertiesCollection = db.collection("properties"); // Reference to "properties" collection

        imageUris = new ArrayList<>();
        imageUriStrings = new ArrayList<>();
        galleryImageAdapter = new GalleryImageAdapter(this, imageUris, (uriToRemove, position) -> {
            if (position >= 0 && position < imageUris.size()) {
                Uri removedUriObject = imageUris.get(position);
                imageUris.remove(position);
                imageUriStrings.remove(removedUriObject.toString());
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
            return;
        }
        propertiesCollection.document(editingPropertyId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    dismissProgressDialog();
                    if (documentSnapshot.exists()) {
                        propertyToEdit = documentSnapshot.toObject(Property.class);
                        // propertyToEdit.setPropertyId(documentSnapshot.getId()); // @DocumentId handles this
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
        // (Same as before, but ensure propertyToEdit.getTimestamp() is handled if you need to display it)
        editTextTitle.setText(propertyToEdit.getTitle());
        editTextLocation.setText(propertyToEdit.getLocation());
        editTextPrice.setText(propertyToEdit.getPrice());
        editTextDescription.setText(propertyToEdit.getDescription());
        editTextPropertyType.setText(propertyToEdit.getPropertyType());
        editTextBedrooms.setText(String.valueOf(propertyToEdit.getBedrooms()));
        editTextBathrooms.setText(String.valueOf(propertyToEdit.getBathrooms()));
        editTextArea.setText(propertyToEdit.getArea());

        imageUris.clear();
        imageUriStrings.clear();
        if (propertyToEdit.getImageUrls() != null && !propertyToEdit.getImageUrls().isEmpty()) {
            for (String uriString : propertyToEdit.getImageUrls()) {
                if (uriString == null || uriString.isEmpty()) continue;
                try {
                    Uri parsedUri = Uri.parse(uriString);
                    imageUris.add(parsedUri);
                    imageUriStrings.add(uriString);
                } catch (Exception e) {
                    Log.e(TAG, getString(R.string.error_parsing_image_uri) + ": '" + uriString + "'", e);
                }
            }
            galleryImageAdapter.notifyDataSetChanged();
        }
    }

    private void openImagePicker() { // Same as before
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        activityResultLauncher.launch(Intent.createChooser(intent, getString(R.string.select_pictures)));
    }

    ActivityResultLauncher<Intent> activityResultLauncher = // Same as before
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        imageUris.clear();
                        imageUriStrings.clear();
                        if (data.getClipData() != null) {
                            ClipData clipData = data.getClipData();
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                Uri imageUri = clipData.getItemAt(i).getUri();
                                if (imageUri != null) {
                                    imageUris.add(imageUri);
                                    imageUriStrings.add(imageUri.toString());
                                }
                            }
                        } else if (data.getData() != null) {
                            Uri imageUri = data.getData();
                            if (imageUri != null) {
                                imageUris.add(imageUri);
                                imageUriStrings.add(imageUri.toString());
                            }
                        }
                        galleryImageAdapter.notifyDataSetChanged();
                        if (!imageUris.isEmpty()) {
                            Toast.makeText(this, getString(R.string.images_selected, imageUris.size()), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private void validateAndSaveProperty() { // Mostly same, Firebase save logic changes
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
        if (imageUriStrings.isEmpty()) {
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

        String mainImageUrl = imageUriStrings.isEmpty() ? null : imageUriStrings.get(0);

        // Create Property object (timestamp will be set by @ServerTimestamp)
        Property property = new Property(title, location, price, description, mainImageUrl,
                new ArrayList<>(imageUriStrings), sellerId, propertyType, bedrooms, bathrooms, area);

        // If editing, set the existing propertyId to ensure we update the correct document.
        // The @DocumentId annotation in Property.java handles reading it, but for saving an update,
        // we need to specify which document to update.
        // If creating new, Firestore will generate an ID if we use .add(), or we can generate one.

        DocumentReference propertyDocRef;
        if (editingPropertyId != null) {
            propertyDocRef = propertiesCollection.document(editingPropertyId);
            // For an update, we might want to explicitly set the timestamp to null
            // if we want @ServerTimestamp to re-evaluate. Or, keep the existing one if not changing.
            // If Property model's timestamp is Date, and it's null, @ServerTimestamp will fill it.
            // If it's not null (e.g. from propertyToEdit), it will be overwritten by server if field exists.
            // For simplicity, @ServerTimestamp will handle it if the field is written.
            // If you want to preserve the original creation timestamp on edit, ensure it's part of the 'property' object
            // and not set to null.
            if (propertyToEdit != null && propertyToEdit.getTimestamp() != null) {
                property.setTimestamp(propertyToEdit.getTimestamp()); // Preserve original creation timestamp
            } else {
                property.setTimestamp(null); // Let server update modification time if that's the logic, or create if new
            }

        } else {
            propertyDocRef = propertiesCollection.document(); // Generate new ID for new property
            property.setPropertyId(propertyDocRef.getId()); // Store the auto-generated ID in our object
            property.setTimestamp(null); // Ensure @ServerTimestamp populates it on creation
        }

        // Use set() to save the object. If editingPropertyId is set, it overwrites.
        // If propertyDocRef was from .document() without an ID, this creates a new doc with that ID.
        propertyDocRef.set(property)
                .addOnSuccessListener(aVoid -> {
                    dismissProgressDialog();
                    Toast.makeText(UploadActivity.this,
                            editingPropertyId != null ? getString(R.string.property_updated_successfully) : getString(R.string.property_saved_successfully),
                            Toast.LENGTH_SHORT).show();
                    clearForm();
                    finish();
                })
                .addOnFailureListener(e -> {
                    dismissProgressDialog();
                    Toast.makeText(UploadActivity.this, getString(R.string.failed_to_save_property) + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error saving property", e);
                });
    }


    private void clearForm() { // Same as before
        editTextTitle.setText("");
        editTextLocation.setText("");
        editTextPrice.setText("");
        editTextDescription.setText("");
        editTextPropertyType.setText("");
        editTextBedrooms.setText("");
        editTextBathrooms.setText("");
        editTextArea.setText("");
        imageUris.clear();
        imageUriStrings.clear();
        if (galleryImageAdapter != null) {
            galleryImageAdapter.notifyDataSetChanged();
        }
    }

    private void dismissProgressDialog() { // Same as before
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { // Same as before
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
