package com.example.lostandfound.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.lostandfound.R;
import com.example.lostandfound.database.ItemEntity;
import com.example.lostandfound.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private List<ItemEntity> items = new ArrayList<>();
    private final OnItemClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(ItemEntity item);
    }

    public ItemAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<ItemEntity> newItems) {
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return items.size(); }

            @Override
            public int getNewListSize() { return newItems.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).id.equals(newItems.get(newPos).id);
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return items.get(oldPos).status.equals(newItems.get(newPos).status);
            }
        });
        items = newItems;
        result.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed_card, parent, false);
        return new ItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        ItemEntity item = items.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    class ItemViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto;
        TextView tvTitle, tvCategory, tvLocation, tvDate, tvStatus, tvType;

        ItemViewHolder(View itemView) {
            super(itemView);
            imgPhoto = itemView.findViewById(R.id.imgItemPhoto);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvCategory = itemView.findViewById(R.id.tvItemCategory);
            tvLocation = itemView.findViewById(R.id.tvItemLocation);
            tvDate = itemView.findViewById(R.id.tvItemDate);
            tvStatus = itemView.findViewById(R.id.tvItemStatus);
            tvType = itemView.findViewById(R.id.tvItemType);
        }

        void bind(ItemEntity item) {
            Context ctx = itemView.getContext();
            tvTitle.setText(item.title);
            tvCategory.setText(item.category);
            tvLocation.setText(item.locationName != null ? item.locationName : "Unknown location");
            tvDate.setText(sdf.format(new Date(item.timestamp)));
            tvStatus.setText(item.status != null ? item.status.toUpperCase() : "");

            boolean isLost = Constants.TYPE_LOST.equals(item.type);
            tvType.setText(isLost ? "LOST" : "FOUND");
            tvType.setTextColor(isLost
                    ? ctx.getColor(R.color.lost_color)
                    : ctx.getColor(R.color.found_color));

            if (item.photoUrl != null && !item.photoUrl.isEmpty()) {
                Glide.with(ctx).load(item.photoUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(imgPhoto);
            } else {
                imgPhoto.setImageResource(R.drawable.ic_image_placeholder);
            }
        }
    }
}
