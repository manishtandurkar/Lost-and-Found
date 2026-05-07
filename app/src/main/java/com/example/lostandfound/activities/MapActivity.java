package com.example.lostandfound.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.lostandfound.R;
import com.example.lostandfound.utils.Constants;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private View progressBar;
    private boolean isPickerMode = false;
    private LatLng selectedLatLng;
    private Marker selectedMarker;

    // Map from marker ID to item ID for click handling
    private final Map<String, String> markerItemMap = new HashMap<>();
    private final Map<String, String> markerTypeMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        isPickerMode = getIntent().getBooleanExtra("picker_mode", false);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            if (isPickerMode) {
                getSupportActionBar().setTitle("Pick Location");
            }
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        progressBar = findViewById(R.id.progress_bar);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMyLocationButtonEnabled(true);

        // Default camera position — can be set to campus center
        LatLng defaultPosition = new LatLng(0.0, 0.0);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultPosition, 2f));

        if (isPickerMode) {
            setupLocationPicker();
        } else {
            loadAllItemPins();
        }
    }

    /**
     * Picker mode: user taps on map to select a location.
     */
    private void setupLocationPicker() {
        Toast.makeText(this, "Tap on the map to select a location", Toast.LENGTH_LONG).show();

        googleMap.setOnMapClickListener(latLng -> {
            selectedLatLng = latLng;

            if (selectedMarker != null) selectedMarker.remove();

            selectedMarker = googleMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));

            // Return result after brief delay so user sees the marker
            Intent resultIntent = new Intent();
            resultIntent.putExtra("lat", latLng.latitude);
            resultIntent.putExtra("lng", latLng.longitude);
            resultIntent.putExtra("location_name",
                    String.format("%.4f, %.4f", latLng.latitude, latLng.longitude));
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    /**
     * Normal mode: load all active items and show them as pins.
     */
    private void loadAllItemPins() {
        progressBar.setVisibility(View.VISIBLE);

        // Load lost items (red pins)
        FirebaseDatabase.getInstance()
                .getReference(Constants.NODE_LOST_ITEMS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String status = child.child("status").getValue(String.class);
                            if (Constants.STATUS_RESOLVED.equals(status)) continue;

                            Double lat = child.child("latitude").getValue(Double.class);
                            Double lng = child.child("longitude").getValue(Double.class);
                            String title = child.child("title").getValue(String.class);

                            if (lat != null && lng != null && lat != 0.0) {
                                LatLng position = new LatLng(lat, lng);
                                Marker marker = googleMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .title(title != null ? title : "Lost Item")
                                        .snippet("Lost")
                                        .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_RED)));
                                if (marker != null) {
                                    markerItemMap.put(marker.getId(), child.getKey());
                                    markerTypeMap.put(marker.getId(), Constants.ITEM_TYPE_LOST);
                                }
                            }
                        }
                        progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                    }
                });

        // Load found items (green pins)
        FirebaseDatabase.getInstance()
                .getReference(Constants.NODE_FOUND_ITEMS)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String status = child.child("status").getValue(String.class);
                            if (Constants.STATUS_RESOLVED.equals(status)) continue;

                            Double lat = child.child("latitude").getValue(Double.class);
                            Double lng = child.child("longitude").getValue(Double.class);
                            String title = child.child("title").getValue(String.class);

                            if (lat != null && lng != null && lat != 0.0) {
                                LatLng position = new LatLng(lat, lng);
                                Marker marker = googleMap.addMarker(new MarkerOptions()
                                        .position(position)
                                        .title(title != null ? title : "Found Item")
                                        .snippet("Found")
                                        .icon(BitmapDescriptorFactory.defaultMarker(
                                                BitmapDescriptorFactory.HUE_GREEN)));
                                if (marker != null) {
                                    markerItemMap.put(marker.getId(), child.getKey());
                                    markerTypeMap.put(marker.getId(), Constants.ITEM_TYPE_FOUND);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });

        // Tap on marker opens item detail
        googleMap.setOnMarkerClickListener(marker -> {
            String id = markerItemMap.get(marker.getId());
            String type = markerTypeMap.get(marker.getId());
            if (id != null) {
                Intent intent = new Intent(this, ItemDetailActivity.class);
                intent.putExtra(Constants.EXTRA_ITEM_ID, id);
                intent.putExtra(Constants.EXTRA_ITEM_TYPE, type);
                startActivity(intent);
            }
            return false;
        });
    }
}
