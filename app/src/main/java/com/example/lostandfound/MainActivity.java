package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.activities.LoginActivity;
import com.example.lostandfound.activities.MapActivity;
import com.example.lostandfound.activities.MyPostsActivity;
import com.example.lostandfound.activities.NotificationActivity;
import com.example.lostandfound.activities.ReportFoundActivity;
import com.example.lostandfound.activities.ReportLostActivity;
import com.example.lostandfound.adapters.ItemAdapter;
import com.example.lostandfound.database.ItemEntity;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.NetworkUtils;
import com.example.lostandfound.utils.SessionManager;
import com.example.lostandfound.viewmodels.FeedViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private FeedViewModel feedViewModel;
    private ItemAdapter adapter;
    private SessionManager sessionManager;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvOfflineBanner, tvEmptyState;
    private String currentFilter = Constants.FILTER_ALL;
    private List<ItemEntity> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);
        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        setupViews();
        setupBottomNav();
        setupFab();
        observeData();

        if (NetworkUtils.isNetworkAvailable(this)) {
            feedViewModel.syncFromFirebase();
        } else {
            tvOfflineBanner.setVisibility(View.VISIBLE);
        }
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.rvFeed);
        progressBar = findViewById(R.id.progressBarFeed);
        tvOfflineBanner = findViewById(R.id.tvOfflineBanner);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        ChipGroup chipGroup = findViewById(R.id.chipGroupFilter);

        adapter = new ItemAdapter(item -> openItemDetail(item));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        int[] chipIds = {R.id.chipAll, R.id.chipLost, R.id.chipFound, R.id.chipMine};
        String[] filters = {Constants.FILTER_ALL, Constants.FILTER_LOST, Constants.FILTER_FOUND, Constants.FILTER_MY_POSTS};
        for (int i = 0; i < chipIds.length; i++) {
            final String filter = filters[i];
            Chip chip = findViewById(chipIds[i]);
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    currentFilter = filter;
                    applyFilter();
                });
            }
        }
    }

    private void observeData() {
        feedViewModel.getAllCachedItems().observe(this, items -> {
            progressBar.setVisibility(View.GONE);
            allItems = items != null ? items : new ArrayList<>();
            applyFilter();
        });

        feedViewModel.getSyncStatus().observe(this, status -> {
            if (status == null) return;
            if (status.equals("syncing")) {
                progressBar.setVisibility(View.VISIBLE);
            } else if (status.equals("success")) {
                progressBar.setVisibility(View.GONE);
                tvOfflineBanner.setVisibility(View.GONE);
            } else if (status.startsWith("error:")) {
                progressBar.setVisibility(View.GONE);
                Snackbar.make(recyclerView, "Sync failed. Showing cached data.", Snackbar.LENGTH_LONG)
                        .setAction("Retry", v -> feedViewModel.syncFromFirebase())
                        .show();
            }
        });
    }

    private void applyFilter() {
        List<ItemEntity> filtered;
        String userId = sessionManager.getUserId();

        switch (currentFilter) {
            case Constants.FILTER_LOST:
                filtered = allItems.stream()
                        .filter(i -> Constants.TYPE_LOST.equals(i.type))
                        .collect(Collectors.toList());
                break;
            case Constants.FILTER_FOUND:
                filtered = allItems.stream()
                        .filter(i -> Constants.TYPE_FOUND.equals(i.type))
                        .collect(Collectors.toList());
                break;
            case Constants.FILTER_MY_POSTS:
                filtered = allItems.stream()
                        .filter(i -> userId != null && userId.equals(i.postedBy))
                        .collect(Collectors.toList());
                break;
            default:
                filtered = allItems;
        }

        adapter.setItems(filtered);
        tvEmptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openItemDetail(ItemEntity item) {
        Intent intent = new Intent(this, com.example.lostandfound.activities.ItemDetailActivity.class);
        intent.putExtra(Constants.EXTRA_ITEM_ID, item.id);
        intent.putExtra(Constants.EXTRA_ITEM_TYPE, item.type);
        startActivity(intent);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_feed) {
                return true;
            } else if (id == R.id.nav_map) {
                startActivity(new Intent(this, MapActivity.class));
                return true;
            } else if (id == R.id.nav_my_posts) {
                startActivity(new Intent(this, MyPostsActivity.class));
                return true;
            } else if (id == R.id.nav_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> showReportDialog());
    }

    private void showReportDialog() {
        String[] options = {"Report Lost Item", "Report Found Item"};
        new AlertDialog.Builder(this)
                .setTitle("What do you want to report?")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(this, ReportLostActivity.class));
                    } else {
                        startActivity(new Intent(this, ReportFoundActivity.class));
                    }
                })
                .show();
    }

    public void onSyncTriggered() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            feedViewModel.syncFromFirebase();
        }
    }
}
