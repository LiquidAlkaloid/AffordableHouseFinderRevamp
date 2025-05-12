package com.example.affordablehousefinderrevamp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {
    public interface OnChatItemClickListener { void onItemClick(ChatItem item); }

    private List<ChatItem> items;
    private OnChatItemClickListener listener;

    public ChatListAdapter(OnChatItemClickListener listener, List<ChatItem> items) {
        this.listener = listener;
        this.items = items;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_chat, parent, false);
        return new ChatViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem item = items.get(position);
        holder.nameTv.setText(item.getSenderName());
        holder.msgTv.setText(item.getLastMessage());
        holder.timeTv.setText(item.getTimestamp());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override public int getItemCount() { return items.size(); }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        TextView nameTv, msgTv, timeTv;
        ImageView profileIv;
        ChatViewHolder(View v) {
            super(v);
            profileIv = v.findViewById(R.id.chat_item_profile_image);
            nameTv = v.findViewById(R.id.chat_item_sender_name);
            msgTv = v.findViewById(R.id.chat_item_last_message);
            timeTv = v.findViewById(R.id.chat_item_timestamp);
        }
    }
}