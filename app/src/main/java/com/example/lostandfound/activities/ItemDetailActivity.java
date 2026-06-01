package com.example.lostandfound.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.lostandfound.R;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.repository.ItemRepository;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ItemRepository itemRepository;
    private SessionManager sessionManager;
    private Item currentItem;
    private String itemId, itemType;
    private GoogleMap googleMap;

    private ImageView imgPhoto;
    private TextView tvTitle, tvCategory, tvDescription, tvLocation, tvDate, tvStatus, tvPostedBy, tvType;
    private MaterialButton btnContact, btnResolve, btnShare;
    private ProgressBar progressBar;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        itemRepository = new ItemRepository(this);
        sessionManager = new SessionManager(this);

        itemId = getIntent().getStringExtra(Constants.EXTRA_ITEM_ID);
        itemType = getIntent().getStringExtra(Constants.EXTRA_ITEM_TYPE);

        if (itemId == null || itemType == null) {
            Uri data = getIntent().getData();
            if (data != null) {
                itemId = data.getQueryParameter("id");
                itemType = data.getQueryParameter("type");
            }
        }

        bindViews();

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapDetail);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (itemId != null && itemType != null) {
            loadItem();
        } else {
            finish();
        }
    }

    private void bindViews() {
        rootView = findViewById(R.id.rootDetailLayout);
        imgPhoto = findViewById(R.id.imgDetailPhoto);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvLocation = findViewById(R.id.tvDetailLocation);
        tvDate = findViewById(R.id.tvDetailDate);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvPostedBy = findViewById(R.id.tvDetailPostedBy);
        tvType = findViewById(R.id.tvDetailType);
        btnContact = findViewById(R.id.btnContact);
        btnResolve = findViewById(R.id.btnMarkResolved);
        btnShare = findViewById(R.id.btnShare);
        progressBar = findViewById(R.id.progressBarDetail);
    }

    private void loadItem() {
        progressBar.setVisibility(View.VISIBLE);
        itemRepository.getItemFromFirebase(itemId, itemType, new ItemRepository.ItemFetchCallback() {
            @Override
            public void onFetched(Item item) {
                progressBar.setVisibility(View.GONE);
                if (item != null) {
                    currentItem = item;
                    populateUI(item);
                } else {
                    Snackbar.make(rootView, "Item not found", Snackbar.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                Snackbar.make(rootView, "Error loading item", Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> loadItem()).show();
            }
        });
    }

    private void populateUI(Item item) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        tvTitle.setText(item.getTitle());
        tvCategory.setText(item.getCategory());
        tvDescription.setText(item.getDescription());
        tvLocation.setText(item.getLocationName());
        tvDate.setText(sdf.format(new Date(item.getTimestamp())));
        tvStatus.setText(item.getStatus() != null ? item.getStatus().toUpperCase() : "");

        boolean isLost = Constants.TYPE_LOST.equals(item.getType());
        tvType.setText(isLost ? "LOST" : "FOUND");
        tvType.setTextColor(getColor(isLost ? R.color.lost_color : R.color.found_color));

        if (item.getPhotoUrl() != null && !item.getPhotoUrl().isEmpty()) {
            Glide.with(this).load(item.getPhotoUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(imgPhoto);
        }

        String myId = sessionManager.getUserId();
        boolean isOwner = myId != null && myId.equals(item.getPostedBy());

        // Fetch poster name
        FirebaseDatabase.getInstance("https://lost-and-found-d65bc-default-rtdb.firebaseio.com").getReference(Constants.DB_USERS)
                .child(item.getPostedBy()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String name = snapshot.child("name").getValue(String.class);
                        tvPostedBy.setText("Posted by: " + (name != null ? name : "Unknown"));
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvPostedBy.setText("Posted by: Unknown");
                    }
                });

        if (isOwner) {
            btnContact.setVisibility(View.GONE);
            btnResolve.setVisibility(View.VISIBLE);
            btnResolve.setEnabled(!Constants.STATUS_RESOLVED.equals(item.getStatus()));
            btnResolve.setOnClickListener(v -> {
                itemRepository.markResolved(item.getId(), item.getType());
                btnResolve.setEnabled(false);
                btnResolve.setText("Resolved");
                tvStatus.setText("RESOLVED");
            });
        } else {
            btnContact.setVisibility(View.VISIBLE);
            btnResolve.setVisibility(View.GONE);
            btnContact.setOnClickListener(v -> openChat(item, myId));
        }
        updateMapPin();
        btnShare.setOnClickListener(v -> shareItemDetails());
    }

    @Override
    public void onMapReady(GoogleMap map) {
        this.googleMap = map;
        updateMapPin();
    }

    private void updateMapPin() {
        if (googleMap == null || currentItem == null) return;
        if (currentItem.getLatitude() == 0 && currentItem.getLongitude() == 0) {
            View mapFragmentView = findViewById(R.id.mapDetail);
            if (mapFragmentView != null) {
                mapFragmentView.setVisibility(View.GONE);
            }
            return;
        }

        LatLng pos = new LatLng(currentItem.getLatitude(), currentItem.getLongitude());
        float color = Constants.TYPE_LOST.equals(currentItem.getType())
                ? BitmapDescriptorFactory.HUE_RED
                : BitmapDescriptorFactory.HUE_GREEN;

        googleMap.clear();
        googleMap.addMarker(new MarkerOptions()
                .position(pos)
                .title(currentItem.getTitle())
                .snippet(currentItem.getLocationName())
                .icon(BitmapDescriptorFactory.defaultMarker(color)));

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f));
        googleMap.getUiSettings().setAllGesturesEnabled(false);
    }

    private void openChat(Item item, String myId) {
        if (myId == null) return;
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_ITEM_ID, item.getId());
        intent.putExtra(Constants.EXTRA_ITEM_TYPE, item.getType());
        intent.putExtra(Constants.EXTRA_OTHER_USER_ID, item.getPostedBy());
        startActivity(intent);
    }

    private void shareItemDetails() {
        if (currentItem == null) return;

        String typeLabel = Constants.TYPE_LOST.equals(currentItem.getType()) ? "LOST" : "FOUND";
        String shareText = "📢 *" + typeLabel + " ITEM ALERT* 📢\n\n"
                + "*Title:* " + currentItem.getTitle() + "\n"
                + "*Category:* " + currentItem.getCategory() + "\n"
                + "*Location:* " + currentItem.getLocationName() + "\n"
                + "*Description:* " + currentItem.getDescription() + "\n\n"
                + "Help recover this item by checking the Campus Lost & Found Hub! 🎓";

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, "Share Item Details via");
        startActivity(shareIntent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
