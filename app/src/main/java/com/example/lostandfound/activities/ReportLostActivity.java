package com.example.lostandfound.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.lostandfound.R;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.example.lostandfound.viewmodels.ReportViewModel;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportLostActivity extends AppCompatActivity {

    private ReportViewModel reportViewModel;
    private SessionManager sessionManager;

    private TextInputLayout tilTitle, tilCategory, tilDescription, tilLocation,
            tilContactPreference, tilPhone;
    private TextInputEditText etTitle, etDescription, etLocation, etPhone;
    private AutoCompleteTextView actvCategory, actvContactPreference;
    private ImageView ivPhotoPreview;
    private FrameLayout framePhoto;
    private View layoutAddPhoto;
    private MaterialButton btnSubmit;
    private View progressBar;

    private Uri cameraImageUri;

    // Gallery picker
    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    reportViewModel.setSelectedPhotoUri(uri);
                    showPhotoPreview(uri);
                }
            });

    // Camera capture
    private final ActivityResultLauncher<Uri> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
                if (success && cameraImageUri != null) {
                    reportViewModel.setSelectedPhotoUri(cameraImageUri);
                    showPhotoPreview(cameraImageUri);
                }
            });

    // Location picker result
    private final ActivityResultLauncher<Intent> locationPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    double lat = result.getData().getDoubleExtra("lat", 0.0);
                    double lng = result.getData().getDoubleExtra("lng", 0.0);
                    String name = result.getData().getStringExtra("location_name");
                    if (name == null) name = lat + ", " + lng;
                    reportViewModel.setSelectedLocation(lat, lng, name);
                    etLocation.setText(name);
                    tilLocation.setError(null);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_lost);

        reportViewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        sessionManager = new SessionManager(this);

        initViews();
        setupDropdowns();
        observeViewModel();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        tilTitle = findViewById(R.id.til_title);
        tilCategory = findViewById(R.id.til_category);
        tilDescription = findViewById(R.id.til_description);
        tilLocation = findViewById(R.id.til_location);
        tilContactPreference = findViewById(R.id.til_contact_preference);
        tilPhone = findViewById(R.id.til_phone);

        etTitle = findViewById(R.id.et_title);
        etDescription = findViewById(R.id.et_description);
        etLocation = findViewById(R.id.et_location);
        etPhone = findViewById(R.id.et_phone);
        actvCategory = findViewById(R.id.actv_category);
        actvContactPreference = findViewById(R.id.actv_contact_preference);

        ivPhotoPreview = findViewById(R.id.iv_photo_preview);
        framePhoto = findViewById(R.id.frame_photo);
        layoutAddPhoto = framePhoto.findViewById(R.id.layout_add_photo);

        btnSubmit = findViewById(R.id.btn_submit);
        progressBar = findViewById(R.id.progress_bar);

        framePhoto.setOnClickListener(v -> showPhotoOptions());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());

        tilLocation.setEndIconOnClickListener(v -> openLocationPicker());
        findViewById(R.id.btn_pick_location).setOnClickListener(v -> openLocationPicker());
    }

    private void setupDropdowns() {
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, Constants.CATEGORIES);
        actvCategory.setAdapter(categoryAdapter);

        ArrayAdapter<String> contactAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line,
                new String[]{Constants.CONTACT_IN_APP_CHAT, Constants.CONTACT_PHONE});
        actvContactPreference.setAdapter(contactAdapter);

        actvContactPreference.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            tilPhone.setVisibility(Constants.CONTACT_PHONE.equals(selected)
                    ? View.VISIBLE : View.GONE);
        });
    }

    private void observeViewModel() {
        reportViewModel.getSubmittedItemId().observe(this, itemId -> {
            if (itemId != null) {
                Snackbar.make(btnSubmit, getString(R.string.report_submitted),
                        Snackbar.LENGTH_LONG).show();
                finish();
            }
        });

        reportViewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Snackbar.make(btnSubmit, getString(R.string.report_failed),
                        Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.retry), v -> validateAndSubmit())
                        .show();
            }
        });

        reportViewModel.getIsLoading().observe(this, isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
                btnSubmit.setEnabled(!isLoading);
            }
        });
    }

    private boolean validateAndSubmit() {
        boolean valid = true;

        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        if (title.length() < 3) {
            tilTitle.setError(getString(R.string.error_title_too_short));
            valid = false;
        } else {
            tilTitle.setError(null);
        }

        String category = actvCategory.getText().toString().trim();
        if (category.isEmpty()) {
            tilCategory.setError(getString(R.string.error_required));
            valid = false;
        } else {
            tilCategory.setError(null);
        }

        if (!reportViewModel.hasLocation()) {
            tilLocation.setError(getString(R.string.error_location_required));
            valid = false;
        } else {
            tilLocation.setError(null);
        }

        String contactPref = actvContactPreference.getText().toString().trim();
        if (contactPref.isEmpty()) {
            tilContactPreference.setError(getString(R.string.error_required));
            valid = false;
        } else {
            tilContactPreference.setError(null);
        }

        if (Constants.CONTACT_PHONE.equals(contactPref)) {
            String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
            if (phone.length() < 7) {
                tilPhone.setError(getString(R.string.error_invalid_phone));
                valid = false;
            } else {
                tilPhone.setError(null);
            }
        }

        if (valid) {
            String userId = sessionManager.getUserId();
            if (userId == null) {
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return false;
            }

            String description = etDescription.getText() != null
                    ? etDescription.getText().toString().trim() : "";

            reportViewModel.submitLostItem(
                    title, category, description,
                    reportViewModel.getSelectedLocationName(),
                    reportViewModel.getSelectedLatitude(),
                    reportViewModel.getSelectedLongitude(),
                    contactPref, userId
            );
        }

        return valid;
    }

    private void showPhotoOptions() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.photo_options_title))
                .setItems(new CharSequence[]{
                        getString(R.string.take_photo),
                        getString(R.string.choose_from_gallery),
                        getString(R.string.cancel)
                }, (dialog, which) -> {
                    if (which == 0) {
                        checkCameraPermissionAndCapture();
                    } else if (which == 1) {
                        checkStoragePermissionAndPick();
                    }
                })
                .show();
    }

    private void checkCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    private void checkStoragePermissionAndPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            galleryLauncher.launch("image/*");
        } else if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            galleryLauncher.launch("image/*");
        } else {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        }
    }

    private void launchCamera() {
        try {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photoFile);
            cameraLauncher.launch(cameraImageUri);
        } catch (IOException e) {
            Snackbar.make(btnSubmit, "Failed to create image file", Snackbar.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    private void showPhotoPreview(Uri uri) {
        layoutAddPhoto.setVisibility(View.GONE);
        ivPhotoPreview.setVisibility(View.VISIBLE);
        Glide.with(this).load(uri).centerCrop().into(ivPhotoPreview);
    }

    private void openLocationPicker() {
        // Launch MapActivity as a location picker
        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("picker_mode", true);
        locationPickerLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 100) launchCamera();
            else if (requestCode == 101) galleryLauncher.launch("image/*");
        }
    }
}
