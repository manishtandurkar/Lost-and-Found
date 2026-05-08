package com.example.lostandfound.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.adapters.ItemAdapter;
import com.example.lostandfound.utils.Constants;
import com.example.lostandfound.utils.SessionManager;
import com.example.lostandfound.viewmodels.FeedViewModel;

public class MyPostsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_posts);

        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle("My Posts");

        SessionManager session = new SessionManager(this);
        FeedViewModel vm = new ViewModelProvider(this).get(FeedViewModel.class);
        RecyclerView rv = findViewById(R.id.rvMyPosts);
        TextView tvEmpty = findViewById(R.id.tvEmptyMyPosts);

        ItemAdapter adapter = new ItemAdapter(item -> {
            Intent intent = new Intent(this, ItemDetailActivity.class);
            intent.putExtra(Constants.EXTRA_ITEM_ID, item.id);
            intent.putExtra(Constants.EXTRA_ITEM_TYPE, item.type);
            startActivity(intent);
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        vm.getMyItems(session.getUserId()).observe(this, items -> {
            adapter.setItems(items);
            tvEmpty.setVisibility(items == null || items.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
