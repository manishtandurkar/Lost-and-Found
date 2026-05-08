package com.example.lostandfound.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lostandfound.R;
import com.example.lostandfound.models.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getSenderId().equals(currentUserId)
                ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_SENT) {
            return new SentViewHolder(inflater.inflate(R.layout.item_chat_sent, parent, false));
        } else {
            return new ReceivedViewHolder(inflater.inflate(R.layout.item_chat_received, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        String time = sdf.format(new Date(msg.getTimestamp()));
        if (holder instanceof SentViewHolder) {
            ((SentViewHolder) holder).bind(msg.getText(), time);
        } else {
            ((ReceivedViewHolder) holder).bind(msg.getText(), time);
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        SentViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessageText);
            tvTime = v.findViewById(R.id.tvMessageTime);
        }

        void bind(String text, String time) {
            tvMessage.setText(text);
            tvTime.setText(time);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        ReceivedViewHolder(View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessageText);
            tvTime = v.findViewById(R.id.tvMessageTime);
        }

        void bind(String text, String time) {
            tvMessage.setText(text);
            tvTime.setText(time);
        }
    }
}
