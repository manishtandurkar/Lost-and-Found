package com.example.lostandfound.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.models.NotificationItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationItem> items = new ArrayList<>();
    private final OnClickListener listener;
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());

    public interface OnClickListener {
        void onClick(NotificationItem item);
    }

    public NotificationAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<NotificationItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = items.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvBody.setText(item.getBody());
        holder.tvTime.setText(sdf.format(new Date(item.getTimestamp())));
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvBody, tvTime;

        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvNotifTitle);
            tvBody = v.findViewById(R.id.tvNotifBody);
            tvTime = v.findViewById(R.id.tvNotifTime);
        }
    }
}
