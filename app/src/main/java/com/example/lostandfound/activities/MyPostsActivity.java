package com.example.lostandfound.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.adapters.MyPostsAdapter;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.example.lostandfound.viewmodels.FeedViewModel;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;

public class MyPostsActivity extends AppCompatActivity {

    private FeedViewModel feedViewModel;
    private SessionManager sessionManager;

    private RecyclerView recyclerMyPosts;
    private MyPostsAdapter adapter;
    private View layoutEmpty;
    private View progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        sessionManager = new SessionManager(this);
        feedViewModel = new ViewModelProvider(this).get(FeedViewModel.class);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerMyPosts = findViewById(R.id.recycler_my_posts);
        layoutEmpty = findViewById(R.id.layout_empty);
        progressBar = findViewById(R.id.progress_bar);

        String userId = sessionManager.getUserId();
        if (userId == null) {
            finish();
            return;
        }

        adapter = new MyPostsAdapter(new ArrayList<>(), item -> {
            Intent intent = new Intent(this, ItemDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ITEM_ID, item.getId());
            intent.putExtra(Constants.EXTRA_ITEM_TYPE, item.getType());
            startActivity(intent);
        });
        recyclerMyPosts.setLayoutManager(new LinearLayoutManager(this));
        recyclerMyPosts.setAdapter(adapter);

        progressBar.setVisibility(View.VISIBLE);
        feedViewModel.setCurrentUserId(userId);
        feedViewModel.setFilter(FeedViewModel.FilterType.MY_POSTS);
        feedViewModel.startListening();

        feedViewModel.getFilteredItems().observe(this, items -> {
            progressBar.setVisibility(View.GONE);
            if (items == null || items.isEmpty()) {
                layoutEmpty.setVisibility(View.VISIBLE);
                recyclerMyPosts.setVisibility(View.GONE);
            } else {
                layoutEmpty.setVisibility(View.GONE);
                recyclerMyPosts.setVisibility(View.VISIBLE);
                adapter.updateItems(items);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        feedViewModel.stopListening();
    }
}
