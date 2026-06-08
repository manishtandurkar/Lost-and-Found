package com.example.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import androidx.annotation.NonNull;
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
    private String searchQuery = "";
    private List<ItemEntity> allItems = new ArrayList<>();
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        sessionManager = new SessionManager(this);
        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupViews();
        setupDrawer(toolbar);
        setupBottomNav();
        setupFab();
        observeData();

        if (NetworkUtils.isNetworkAvailable(this)) {
            FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
                    if (auth.getCurrentUser() != null) {
                        auth.removeAuthStateListener(this);
                        feedViewModel.syncFromFirebase();
                    }
                }
            });
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
        androidx.appcompat.widget.SearchView searchView = findViewById(R.id.searchView);

        if (searchView != null) {
            searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchQuery = query != null ? query.trim().toLowerCase() : "";
                    applyFilter();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    searchQuery = newText != null ? newText.trim().toLowerCase() : "";
                    applyFilter();
                    return true;
                }
            });
        }

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

        if (!searchQuery.isEmpty()) {
            filtered = filtered.stream()
                    .filter(i -> (i.title != null && i.title.toLowerCase().contains(searchQuery))
                            || (i.category != null && i.category.toLowerCase().contains(searchQuery))
                            || (i.description != null && i.description.toLowerCase().contains(searchQuery)))
                    .collect(Collectors.toList());
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

    @Override
    protected void onResume() {
        super.onResume();
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_feed);
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
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_report_options, null);
        sheet.setContentView(view);

        MaterialCardView cardLost = view.findViewById(R.id.cardReportLost);
        MaterialCardView cardFound = view.findViewById(R.id.cardReportFound);

        cardLost.setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, ReportLostActivity.class));
        });
        cardFound.setOnClickListener(v -> {
            sheet.dismiss();
            startActivity(new Intent(this, ReportFoundActivity.class));
        });

        sheet.show();
    }

    private void setupDrawer(MaterialToolbar toolbar) {
        drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navView = findViewById(R.id.navView);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        View header = navView.getHeaderView(0);
        TextView tvName = header.findViewById(R.id.tvNavName);
        TextView tvEmail = header.findViewById(R.id.tvNavEmail);
        ImageView ivPhoto = header.findViewById(R.id.ivNavPhoto);

        tvName.setText(sessionManager.getUserName());
        tvEmail.setText(sessionManager.getUserEmail());

        String photoUrl = sessionManager.getUserPhotoUrl();
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl).circleCrop().into(ivPhoto);
        }

        navView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.drawer_map) {
                startActivity(new Intent(this, MapActivity.class));
            } else if (id == R.id.drawer_my_posts) {
                startActivity(new Intent(this, MyPostsActivity.class));
            } else if (id == R.id.drawer_notifications) {
                startActivity(new Intent(this, NotificationActivity.class));
            } else if (id == R.id.drawer_logout) {
                FirebaseAuth.getInstance().signOut();
                sessionManager.clearSession();
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public void onSyncTriggered() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            feedViewModel.syncFromFirebase();
        }
    }
}
