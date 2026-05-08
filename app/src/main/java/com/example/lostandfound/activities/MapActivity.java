package com.example.lostandfound.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.lostandfound.R;
import com.example.lostandfound.database.ItemEntity;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.viewmodels.FeedViewModel;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private FeedViewModel feedViewModel;
    private final Map<Marker, ItemEntity> markerItemMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Item Map");

        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        googleMap.setOnMarkerClickListener(marker -> {
            ItemEntity item = markerItemMap.get(marker);
            if (item != null) {
                Intent intent = new Intent(this, ItemDetailActivity.class);
                intent.putExtra(Constants.EXTRA_ITEM_ID, item.id);
                intent.putExtra(Constants.EXTRA_ITEM_TYPE, item.type);
                startActivity(intent);
            }
            return true;
        });

        feedViewModel.getAllCachedItems().observe(this, items -> {
            if (items == null) return;
            googleMap.clear();
            markerItemMap.clear();
            LatLng lastPos = null;

            for (ItemEntity item : items) {
                if (item.latitude == 0 && item.longitude == 0) continue;
                LatLng pos = new LatLng(item.latitude, item.longitude);
                float color = Constants.TYPE_LOST.equals(item.type)
                        ? BitmapDescriptorFactory.HUE_RED
                        : BitmapDescriptorFactory.HUE_GREEN;

                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(item.title)
                        .snippet(item.locationName)
                        .icon(BitmapDescriptorFactory.defaultMarker(color)));
                if (marker != null) markerItemMap.put(marker, item);
                lastPos = pos;
            }

            if (lastPos != null) {
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPos, 14));
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
