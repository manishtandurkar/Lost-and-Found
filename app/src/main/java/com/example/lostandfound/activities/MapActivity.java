package com.example.lostandfound.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashMap;
import java.util.Map;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final LatLng RVCE = new LatLng(12.9231, 77.4987);

    private GoogleMap googleMap;
    private FeedViewModel feedViewModel;
    private final Map<Marker, ItemEntity> markerItemMap = new HashMap<>();

    private BitmapDescriptor lostMarkerIcon;
    private BitmapDescriptor foundMarkerIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("Item Map");

        lostMarkerIcon = createMarkerIcon(Color.parseColor("#D32F2F"), "?");
        foundMarkerIcon = createMarkerIcon(Color.parseColor("#388E3C"), "✓");

        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(RVCE, 16));

        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                ItemEntity item = markerItemMap.get(marker);
                if (item == null) return null;

                View view = getLayoutInflater().inflate(R.layout.map_info_window, null);
                TextView tvTitle = view.findViewById(R.id.tvInfoTitle);
                TextView tvType = view.findViewById(R.id.tvInfoType);
                TextView tvCategory = view.findViewById(R.id.tvInfoCategory);
                TextView tvLocation = view.findViewById(R.id.tvInfoLocation);

                tvTitle.setText(item.title);
                tvCategory.setText(item.category != null ? item.category : "");
                tvLocation.setText(item.locationName != null ? item.locationName : "");

                boolean isLost = Constants.TYPE_LOST.equals(item.type);
                tvType.setText(isLost ? "LOST" : "FOUND");
                tvType.setBackgroundColor(isLost
                        ? Color.parseColor("#D32F2F")
                        : Color.parseColor("#388E3C"));

                return view;
            }
        });

        googleMap.setOnInfoWindowClickListener(marker -> {
            ItemEntity item = markerItemMap.get(marker);
            if (item != null) {
                Intent intent = new Intent(this, ItemDetailActivity.class);
                intent.putExtra(Constants.EXTRA_ITEM_ID, item.id);
                intent.putExtra(Constants.EXTRA_ITEM_TYPE, item.type);
                startActivity(intent);
            }
        });

        feedViewModel.getAllCachedItems().observe(this, items -> {
            if (items == null) return;
            googleMap.clear();
            markerItemMap.clear();

            for (ItemEntity item : items) {
                if (item.latitude == 0 && item.longitude == 0) continue;
                LatLng pos = new LatLng(item.latitude, item.longitude);
                BitmapDescriptor icon = Constants.TYPE_LOST.equals(item.type)
                        ? lostMarkerIcon : foundMarkerIcon;

                Marker marker = googleMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(item.title)
                        .icon(icon));
                if (marker != null) markerItemMap.put(marker, item);
            }
        });
    }

    private BitmapDescriptor createMarkerIcon(int color, String label) {
        int size = 96;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Filled circle
        Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, circlePaint);

        // White border
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4, borderPaint);

        // Label text
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(42);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        float textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f;
        canvas.drawText(label, size / 2f, textY, textPaint);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
