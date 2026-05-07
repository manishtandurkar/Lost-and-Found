package com.example.lostandfound.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lostandfound.R;
import com.example.lostandfound.models.Item;
import com.example.lostandfound.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    private List<Item> items;
    private final OnItemClickListener listener;

    public FeedAdapter(List<Item> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateItems(List<Item> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed_card, parent, false);
        return new FeedViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedViewHolder holder, int position) {
        Item item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class FeedViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivPhoto;
        private final TextView tvTypeBadge, tvTitle, tvCategory, tvLocation, tvDate, tvStatus;

        FeedViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_item_photo);
            tvTypeBadge = itemView.findViewById(R.id.tv_type_badge);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvLocation = itemView.findViewById(R.id.tv_location);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }

        void bind(Item item, OnItemClickListener listener) {
            tvTitle.setText(item.getTitle());
            tvCategory.setText(item.getCategory());
            tvLocation.setText(item.getLocationName() != null ? item.getLocationName() : "Unknown location");

            String dateStr = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    .format(new Date(item.getTimestamp()));
            tvDate.setText(dateStr);

            boolean isLost = Constants.ITEM_TYPE_LOST.equals(item.getType());
            tvTypeBadge.setText(isLost ? "LOST" : "FOUND");

            if (isLost) {
                tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_lost);
            } else {
                tvTypeBadge.setBackgroundResource(R.drawable.bg_badge_found);
            }

            boolean isActive = Constants.STATUS_ACTIVE.equals(item.getStatus());
            tvStatus.setText(isActive ? "Active" : "Resolved");
            tvStatus.setTextColor(itemView.getContext().getResources().getColor(
                    isActive ? R.color.status_active : R.color.status_resolved));

            // Load photo
            if (item.getPhotoUrl() != null && !item.getPhotoUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getPhotoUrl())
                        .placeholder(R.drawable.ic_placeholder_image)
                        .centerCrop()
                        .into(ivPhoto);
            } else {
                ivPhoto.setImageResource(R.drawable.ic_placeholder_image);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
}
