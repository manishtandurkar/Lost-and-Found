package com.example.lostandfound.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lostandfound.R;
import com.example.lostandfound.utils.Constants;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private TextView tvAddress;
    private MaterialButton btnConfirm;
    private double pickedLat = 0, pickedLng = 0;
    private String pickedAddress = "";

    private static final LatLng DEFAULT_CENTER = new LatLng(12.9231, 77.4987);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        tvAddress = findViewById(R.id.tvAddress);
        btnConfirm = findViewById(R.id.btnConfirm);

        btnConfirm.setOnClickListener(v -> {
            Intent result = new Intent();
            result.putExtra(Constants.EXTRA_LAT, pickedLat);
            result.putExtra(Constants.EXTRA_LNG, pickedLng);
            result.putExtra(Constants.EXTRA_LOCATION_NAME, pickedAddress);
            setResult(RESULT_OK, result);
            finish();
        });

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, 16));

        googleMap.setOnMapClickListener(latLng -> {
            googleMap.clear();
            googleMap.addMarker(new MarkerOptions().position(latLng));
            pickedLat = latLng.latitude;
            pickedLng = latLng.longitude;
            reverseGeocode(latLng);
        });
    }

    private void reverseGeocode(LatLng latLng) {
        tvAddress.setText("Getting address...");
        tvAddress.setVisibility(View.VISIBLE);
        btnConfirm.setEnabled(false);

        new Thread(() -> {
            String address = getAddressFromLatLng(latLng);
            runOnUiThread(() -> {
                pickedAddress = address;
                tvAddress.setText(address);
                btnConfirm.setEnabled(true);
            });
        }).start();
    }

    private String getAddressFromLatLng(LatLng latLng) {
        if (!Geocoder.isPresent()) return latLng.latitude + ", " + latLng.longitude;
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (results != null && !results.isEmpty()) {
                Address address = results.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(address.getAddressLine(i));
                }
                return sb.toString();
            }
        } catch (IOException e) {
            // fall through
        }
        return latLng.latitude + ", " + latLng.longitude;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
