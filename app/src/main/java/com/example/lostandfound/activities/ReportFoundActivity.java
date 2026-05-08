package com.example.lostandfound.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.lostandfound.R;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.viewmodels.ReportViewModel;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

public class ReportFoundActivity extends AppCompatActivity {

    private ReportViewModel viewModel;
    private TextInputEditText etTitle, etDescription, etLocation, etContact;
    private TextInputLayout tilTitle, tilDescription, tilLocation, tilCategory;
    private AutoCompleteTextView spinnerCategory;
    private ImageView imgPreview;
    private ProgressBar progressBar;
    private Uri selectedPhotoUri;
    private double selectedLat = 0, selectedLng = 0;
    private String selectedLocationName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_found);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Report Found Item");

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));
        }

        bindViews();
        setupCategoryDropdown();
        observeViewModel();
    }

    private void bindViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etContact = findViewById(R.id.etContact);
        tilTitle = findViewById(R.id.tilTitle);
        tilDescription = findViewById(R.id.tilDescription);
        tilLocation = findViewById(R.id.tilLocation);
        tilCategory = findViewById(R.id.tilCategory);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        imgPreview = findViewById(R.id.imgPhotoPreview);
        progressBar = findViewById(R.id.progressBar);

        MaterialButton btnPickPhoto = findViewById(R.id.btnPickPhoto);
        MaterialButton btnPickLocation = findViewById(R.id.btnPickLocation);
        MaterialButton btnSubmit = findViewById(R.id.btnSubmit);

        btnPickPhoto.setOnClickListener(v -> requestPhotoPermissionAndPick());
        btnPickLocation.setOnClickListener(v -> openPlacePicker());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, Constants.CATEGORIES);
        spinnerCategory.setAdapter(adapter);
    }

    private void requestPhotoPermissionAndPick() {
        String permission = android.os.Build.VERSION.SDK_INT >= 33
                ? Manifest.permission.READ_MEDIA_IMAGES
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, Constants.RC_STORAGE_PERMISSION);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, Constants.RC_IMAGE_PICK);
    }

    private void openPlacePicker() {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(intent, Constants.RC_PLACE_PICKER);
    }

    private void validateAndSubmit() {
        boolean valid = true;
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String category = spinnerCategory.getText().toString().trim();
        String description = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        String contact = etContact.getText() != null ? etContact.getText().toString().trim() : "";

        if (title.isEmpty()) {
            tilTitle.setError("Title is required");
            valid = false;
        } else tilTitle.setError(null);

        if (category.isEmpty()) {
            tilCategory.setError("Category is required");
            valid = false;
        } else tilCategory.setError(null);

        if (description.isEmpty()) {
            tilDescription.setError("Description is required");
            valid = false;
        } else tilDescription.setError(null);

        if (selectedLocationName.isEmpty()) {
            tilLocation.setError("Please pick a location");
            valid = false;
        } else tilLocation.setError(null);

        if (!valid) return;

        viewModel.submitItem(title, category, description, selectedLocationName,
                selectedLat, selectedLng, selectedPhotoUri,
                contact.isEmpty() ? Constants.CONTACT_CHAT : contact,
                Constants.TYPE_FOUND);
    }

    private void observeViewModel() {
        viewModel.isLoading.observe(this, loading ->
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.postStatus.observe(this, status -> {
            if (status == null) return;
            if (status.startsWith("success:")) {
                String itemId = status.substring(8);
                triggerMatchingService(itemId);
                Snackbar.make(etTitle, "Found item reported! Looking for matches...", Snackbar.LENGTH_SHORT).show();
                finish();
            } else if (status.startsWith("error:")) {
                Snackbar.make(etTitle, "Failed: " + status.substring(6), Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> validateAndSubmit()).show();
            }
        });
    }

    private void triggerMatchingService(String itemId) {
        Intent serviceIntent = new Intent(this, com.example.lostandfound.services.MatchingService.class);
        serviceIntent.putExtra(Constants.EXTRA_ITEM_ID, itemId);
        serviceIntent.putExtra(Constants.EXTRA_ITEM_TYPE, Constants.TYPE_FOUND);
        startService(serviceIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.RC_IMAGE_PICK && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.getData();
            imgPreview.setImageURI(selectedPhotoUri);
            imgPreview.setVisibility(View.VISIBLE);
        } else if (requestCode == Constants.RC_PLACE_PICKER && resultCode == Activity.RESULT_OK && data != null) {
            Place place = Autocomplete.getPlaceFromIntent(data);
            selectedLocationName = place.getName();
            if (place.getLatLng() != null) {
                selectedLat = place.getLatLng().latitude;
                selectedLng = place.getLatLng().longitude;
            }
            etLocation.setText(selectedLocationName);
            tilLocation.setError(null);
        } else if (requestCode == Constants.RC_PLACE_PICKER && resultCode == AutocompleteActivity.RESULT_ERROR && data != null) {
            Snackbar.make(etTitle, "Place picker error", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Constants.RC_STORAGE_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
