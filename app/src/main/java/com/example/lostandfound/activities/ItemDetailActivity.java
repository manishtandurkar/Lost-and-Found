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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {

    private ItemRepository itemRepository;
    private SessionManager sessionManager;
    private Item currentItem;
    private String itemId, itemType;

    private ImageView imgPhoto;
    private TextView tvTitle, tvCategory, tvDescription, tvLocation, tvDate, tvStatus, tvPostedBy, tvType;
    private MaterialButton btnContact, btnResolve;
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
        FirebaseDatabase.getInstance().getReference(Constants.DB_USERS)
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
    }

    private void openChat(Item item, String myId) {
        if (myId == null) return;
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_ITEM_ID, item.getId());
        intent.putExtra(Constants.EXTRA_ITEM_TYPE, item.getType());
        intent.putExtra(Constants.EXTRA_OTHER_USER_ID, item.getPostedBy());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
