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
import com.example.lostandfound.repository.UserRepository;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ItemRepository itemRepository;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private Item currentItem;
    private String itemId, itemType;
    private GoogleMap googleMap;

    private ImageView imgPhoto;
    private View photoContainer;
    private TextView tvTitle, tvCategory, tvDescription, tvLocation, tvDate, tvStatus, tvPostedBy, tvType;
    private MaterialButton btnContact, btnResolve, btnShare;
    private ProgressBar progressBar;
    private View rootView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Lost & Found");
        }

        itemRepository = new ItemRepository(this);
        userRepository = new UserRepository();
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
        photoContainer = findViewById(R.id.photoContainer);
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
            photoContainer.setVisibility(View.VISIBLE);
            Glide.with(this).load(item.getPhotoUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(imgPhoto);
        } else {
            photoContainer.setVisibility(View.GONE);
        }

        String myId = sessionManager.getUserId();
        boolean isOwner = myId != null && myId.equals(item.getPostedBy());

        String posterName = item.getPostedByName();
        if (isOwner) {
            String myName = sessionManager.getUserName();
            tvPostedBy.setText("Posted by: " + (myName != null ? myName : "You"));
        } else if (posterName != null && !posterName.isEmpty()) {
            tvPostedBy.setText("Posted by: " + posterName);
        } else {
            // Old item without postedByName — look up from /users/{uid}
            tvPostedBy.setText("Posted by: ...");
            userRepository.getUser(item.getPostedBy(), new UserRepository.UserFetchCallback() {
                @Override
                public void onFetched(com.example.lostandfound.models.User user) {
                    String name = (user != null && user.getName() != null) ? user.getName() : "Unknown";
                    tvPostedBy.setText("Posted by: " + name);
                }
                @Override
                public void onError(String message) {
                    tvPostedBy.setText("Posted by: Unknown");
                }
            });
        }

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
            btnContact.setOnClickListener(v -> openWhatsApp(item));
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

    private void openWhatsApp(Item item) {
        String phone = item.getContactPreference();
        if (phone == null || phone.trim().isEmpty()) {
            Snackbar.make(rootView, "No contact number available", Snackbar.LENGTH_SHORT).show();
            return;
        }
        // Strip spaces/dashes; ensure leading + for international format
        phone = phone.replaceAll("[\\s\\-]", "");
        if (!phone.startsWith("+")) phone = "+" + phone;

        boolean isLost = Constants.TYPE_LOST.equals(item.getType());
        String message = isLost
                ? "Hi! I saw your lost item \"" + item.getTitle() + "\" on Campus Lost & Found. I think I may have found it!"
                : "Hi! I saw you found a \"" + item.getTitle() + "\" on Campus Lost & Found. It might be mine!";

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + phone.substring(1)
                            + "?text=" + Uri.encode(message)));
            intent.setPackage("com.whatsapp");
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            // WhatsApp not installed — fall back to any app that handles the URL
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + phone.substring(1)
                            + "?text=" + Uri.encode(message)));
            startActivity(intent);
        }
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
