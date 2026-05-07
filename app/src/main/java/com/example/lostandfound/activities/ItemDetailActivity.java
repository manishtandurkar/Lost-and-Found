package com.example.lostandfound.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.lostandfound.R;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.repository.ItemRepository;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private SessionManager sessionManager;
    private ItemRepository itemRepository;

    private ImageView ivItemPhoto;
    private TextView tvStatusBadge, tvTitle, tvCategory, tvDescription,
            tvLocation, tvPosterName, tvPostedDate, tvResolvedMessage;
    private ImageView ivPosterAvatar;
    private MaterialButton btnContact, btnResolve;
    private View progressBar;
    private MapView mapView;

    private String itemId;
    private String itemType;
    private Item currentItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        sessionManager = new SessionManager(this);
        itemRepository = new ItemRepository(this);

        itemId = getIntent().getStringExtra(Constants.EXTRA_ITEM_ID);
        itemType = getIntent().getStringExtra(Constants.EXTRA_ITEM_TYPE);

        // Handle deep link from notification
        if (itemId == null && getIntent().getData() != null) {
            itemId = getIntent().getData().getLastPathSegment();
        }

        initViews();
        initMap(savedInstanceState);
        loadItemDetails();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivItemPhoto = findViewById(R.id.iv_item_photo);
        tvStatusBadge = findViewById(R.id.tv_status_badge);
        tvTitle = findViewById(R.id.tv_item_title);
        tvCategory = findViewById(R.id.tv_category);
        tvDescription = findViewById(R.id.tv_description);
        tvLocation = findViewById(R.id.tv_location);
        tvPosterName = findViewById(R.id.tv_poster_name);
        tvPostedDate = findViewById(R.id.tv_posted_date);
        ivPosterAvatar = findViewById(R.id.iv_poster_avatar);
        btnContact = findViewById(R.id.btn_contact);
        btnResolve = findViewById(R.id.btn_resolve);
        tvResolvedMessage = findViewById(R.id.tv_resolved_message);
        progressBar = findViewById(R.id.progress_bar);
        mapView = findViewById(R.id.map_view);

        btnContact.setOnClickListener(v -> openChat());
        btnResolve.setOnClickListener(v -> confirmResolve());
    }

    private void initMap(Bundle savedInstanceState) {
        if (mapView != null) {
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(this);
        }
    }

    private void loadItemDetails() {
        if (itemId == null) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        String node = Constants.ITEM_TYPE_LOST.equals(itemType)
                ? Constants.NODE_LOST_ITEMS : Constants.NODE_FOUND_ITEMS;

        // Try to determine node from itemType, otherwise try both
        if (itemType != null) {
            fetchItemFromNode(node, itemId);
        } else {
            // Determine type by trying lost first
            fetchItemFromNode(Constants.NODE_LOST_ITEMS, itemId);
        }
    }

    private void fetchItemFromNode(String node, String id) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference(node).child(id);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentItem = snapshot.getValue(Item.class);
                    if (currentItem != null) {
                        currentItem.setId(id);
                        currentItem.setType(Constants.NODE_LOST_ITEMS.equals(node)
                                ? Constants.ITEM_TYPE_LOST : Constants.ITEM_TYPE_FOUND);
                        progressBar.setVisibility(View.GONE);
                        populateUI(currentItem);
                    }
                } else if (itemType == null && Constants.NODE_LOST_ITEMS.equals(node)) {
                    // Try found items
                    fetchItemFromNode(Constants.NODE_FOUND_ITEMS, id);
                } else {
                    progressBar.setVisibility(View.GONE);
                    Snackbar.make(ivItemPhoto, "Item not found", Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Snackbar.make(ivItemPhoto, "Error loading item: " + error.getMessage(),
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void populateUI(Item item) {
        // Photo
        if (item.getPhotoUrl() != null && !item.getPhotoUrl().isEmpty()) {
            Glide.with(this)
                    .load(item.getPhotoUrl())
                    .placeholder(R.drawable.ic_placeholder_image)
                    .centerCrop()
                    .into(ivItemPhoto);
        }

        // Status badge
        boolean isLost = Constants.ITEM_TYPE_LOST.equals(item.getType());
        tvStatusBadge.setText(isLost ? "LOST" : "FOUND");
        tvStatusBadge.setBackgroundResource(isLost
                ? R.drawable.bg_badge_lost : R.drawable.bg_badge_found);

        tvTitle.setText(item.getTitle());
        tvCategory.setText("Category: " + item.getCategory());
        tvDescription.setText(item.getDescription() != null && !item.getDescription().isEmpty()
                ? item.getDescription() : "No description provided.");
        tvLocation.setText(item.getLocationName());

        // Date
        String date = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(new Date(item.getTimestamp()));
        tvPostedDate.setText(date);

        // Show resolve/contact buttons based on ownership
        String currentUserId = sessionManager.getUserId();
        if (item.getPostedBy() != null && item.getPostedBy().equals(currentUserId)) {
            // Owner view
            if (!item.isResolved()) {
                btnResolve.setVisibility(View.VISIBLE);
            } else {
                tvResolvedMessage.setVisibility(View.VISIBLE);
            }
        } else {
            // Viewer — show contact button if not resolved
            if (!item.isResolved()) {
                btnContact.setVisibility(View.VISIBLE);
            }
        }

        // Load poster info
        loadPosterInfo(item.getPostedBy());
    }

    private void loadPosterInfo(String userId) {
        if (userId == null) return;
        FirebaseDatabase.getInstance()
                .getReference(Constants.NODE_USERS)
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String name = snapshot.child("name").getValue(String.class);
                            String photoUrl = snapshot.child("profilePhotoUrl").getValue(String.class);
                            tvPosterName.setText(name != null ? name : "Unknown User");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(ItemDetailActivity.this)
                                        .load(photoUrl)
                                        .circleCrop()
                                        .placeholder(R.drawable.ic_placeholder_image)
                                        .into(ivPosterAvatar);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {}
                });
    }

    private void openChat() {
        if (currentItem == null) return;
        String currentUserId = sessionManager.getUserId();
        String otherUserId = currentItem.getPostedBy();

        if (currentUserId == null || otherUserId == null) return;

        String chatId = com.example.lostandfound.viewmodels.ChatViewModel
                .generateChatId(currentUserId, otherUserId, currentItem.getId());

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_CHAT_ID, chatId);
        intent.putExtra(Constants.EXTRA_ITEM_ID, currentItem.getId());
        intent.putExtra(Constants.EXTRA_OTHER_USER_ID, otherUserId);
        startActivity(intent);
    }

    private void confirmResolve() {
        new MaterialAlertDialogBuilder(this)
                .setMessage(getString(R.string.confirm_resolve))
                .setPositiveButton(getString(R.string.yes), (dialog, which) -> resolveItem())
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }

    private void resolveItem() {
        if (currentItem == null) return;
        progressBar.setVisibility(View.VISIBLE);
        btnResolve.setEnabled(false);

        itemRepository.resolveItem(currentItem.getId(), currentItem.getType(), success -> {
            progressBar.setVisibility(View.GONE);
            if (success) {
                btnResolve.setVisibility(View.GONE);
                tvResolvedMessage.setVisibility(View.VISIBLE);
                Snackbar.make(ivItemPhoto, getString(R.string.resolved_success),
                        Snackbar.LENGTH_SHORT).show();
            } else {
                btnResolve.setEnabled(true);
                Snackbar.make(ivItemPhoto, getString(R.string.report_failed),
                        Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (currentItem != null && (currentItem.getLatitude() != 0 || currentItem.getLongitude() != 0)) {
            LatLng position = new LatLng(currentItem.getLatitude(), currentItem.getLongitude());
            float markerColor = Constants.ITEM_TYPE_LOST.equals(currentItem.getType())
                    ? BitmapDescriptorFactory.HUE_RED
                    : BitmapDescriptorFactory.HUE_GREEN;

            googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title(currentItem.getTitle())
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f));
        }
    }

    @Override
    protected void onResume() { super.onResume(); if (mapView != null) mapView.onResume(); }

    @Override
    protected void onPause() { super.onPause(); if (mapView != null) mapView.onPause(); }

    @Override
    protected void onStop() { super.onStop(); if (mapView != null) mapView.onStop(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mapView != null) mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        if (mapView != null) mapView.onLowMemory();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mapView != null) mapView.onSaveInstanceState(outState);
    }
}
