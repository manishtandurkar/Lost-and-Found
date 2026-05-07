package com.example.lostandfound.activities;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.adapters.FeedAdapter;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.receivers.NetworkChangeReceiver;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.NetworkUtils;
import com.example.lostandfound.utils.SessionManager;
import com.example.lostandfound.viewmodels.AuthViewModel;
import com.example.lostandfound.viewmodels.FeedViewModel;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private FeedViewModel feedViewModel;
    private AuthViewModel authViewModel;
    private SessionManager sessionManager;

    private RecyclerView recyclerFeed;
    private FeedAdapter feedAdapter;
    private TextView tvOfflineBanner;
    private View layoutEmpty;
    private View progressBar;

    private NetworkChangeReceiver networkChangeReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sessionManager = new SessionManager(this);

        initViews();
        setupViewModels();
        setupRecyclerView();
        setupBottomNavigation();
        setupFab();
        setupTabLayout();
        observeFeed();

        registerNetworkReceiver();
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerFeed = findViewById(R.id.recycler_feed);
        tvOfflineBanner = findViewById(R.id.tv_offline_banner);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupViewModels() {
        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        String userId = sessionManager.getUserId();
        if (userId != null) {
            feedViewModel.setCurrentUserId(userId);
        }
    }

    private void setupRecyclerView() {
        feedAdapter = new FeedAdapter(new ArrayList<>(), item -> {
            Intent intent = new Intent(this, ItemDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ITEM_ID, item.getId());
            intent.putExtra(Constants.EXTRA_ITEM_TYPE, item.getType());
            startActivity(intent);
        });

        recyclerFeed.setLayoutManager(new LinearLayoutManager(this));
        recyclerFeed.setAdapter(feedAdapter);
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_feed) {
                // Already on feed
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
        ExtendedFloatingActionButton fab = findViewById(R.id.fab_report);
        fab.setOnClickListener(v -> showReportDialog());
    }

    private void showReportDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.fab_label))
                .setItems(new CharSequence[]{
                        getString(R.string.report_lost),
                        getString(R.string.report_found)
                }, (dialog, which) -> {
                    if (which == 0) {
                        startActivity(new Intent(this, ReportLostActivity.class));
                    } else {
                        startActivity(new Intent(this, ReportFoundActivity.class));
                    }
                })
                .show();
    }

    private void setupTabLayout() {
        TabLayout tabLayout = findViewById(R.id.tab_layout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        feedViewModel.setFilter(FeedViewModel.FilterType.ALL);
                        break;
                    case 1:
                        feedViewModel.setFilter(FeedViewModel.FilterType.LOST_ONLY);
                        break;
                    case 2:
                        feedViewModel.setFilter(FeedViewModel.FilterType.FOUND_ONLY);
                        break;
                    case 3:
                        feedViewModel.setFilter(FeedViewModel.FilterType.MY_POSTS);
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void observeFeed() {
        boolean isOnline = NetworkUtils.isNetworkAvailable(this);
        feedViewModel.setOffline(!isOnline);

        if (isOnline) {
            feedViewModel.startListening();
            feedViewModel.getFilteredItems().observe(this, items -> {
                progressBar.setVisibility(View.GONE);
                if (items == null || items.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerFeed.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerFeed.setVisibility(View.VISIBLE);
                    feedAdapter.updateItems(items);
                }
            });
        } else {
            tvOfflineBanner.setVisibility(View.VISIBLE);
            feedViewModel.getOfflineItems().observe(this, items -> {
                progressBar.setVisibility(View.GONE);
                if (items == null || items.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    recyclerFeed.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    recyclerFeed.setVisibility(View.VISIBLE);
                    feedAdapter.updateItems(items);
                }
            });
        }
    }

    private void registerNetworkReceiver() {
        networkChangeReceiver = new NetworkChangeReceiver(isConnected -> {
            feedViewModel.setOffline(!isConnected);
            if (isConnected) {
                tvOfflineBanner.setVisibility(View.GONE);
                Snackbar.make(recyclerFeed,
                        getString(R.string.network_restored), Snackbar.LENGTH_SHORT).show();
                feedViewModel.startListening();
            } else {
                tvOfflineBanner.setVisibility(View.VISIBLE);
            }
        });

        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeReceiver, filter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.fab_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_report_lost) {
            startActivity(new Intent(this, ReportLostActivity.class));
            return true;
        } else if (item.getItemId() == R.id.menu_report_found) {
            startActivity(new Intent(this, ReportFoundActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkChangeReceiver != null) {
            try {
                unregisterReceiver(networkChangeReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver not registered
            }
        }
    }
}
