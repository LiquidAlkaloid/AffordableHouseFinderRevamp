package com.example.affordablehousefinderrevamp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ChatViewHolder> {

    private Context context;
    private List<ChatItem> chatItems;
    private OnChatItemClickListener listener;

    public interface OnChatItemClickListener {
        void onItemClick(ChatItem chatItem);
    }

    public ChatListAdapter(Context context, List<ChatItem> chatItems, OnChatItemClickListener listener) {
        this.context = context;
        this.chatItems = chatItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem currentItem = chatItems.get(position);

        holder.senderNameTextView.setText(currentItem.getSenderName());
        holder.lastMessageTextView.setText(currentItem.getLastMessage());
        holder.timestampTextView.setText(currentItem.getTimestamp());

        // Use a placeholder or actual image loading library (Glide, Picasso) here
        holder.profileImageView.setImageResource(currentItem.getProfileImageResId()); // Example: R.drawable.baseline_person_24

        if (currentItem.getUnreadCount() > 0) {
            holder.unreadCountTextView.setText(String.valueOf(currentItem.getUnreadCount()));
            holder.unreadCountTextView.setVisibility(View.VISIBLE);
            holder.readCheckmarkImageView.setVisibility(View.GONE);
        } else if (currentItem.isRead()) {
            holder.unreadCountTextView.setVisibility(View.GONE);
            holder.readCheckmarkImageView.setVisibility(View.VISIBLE);
        } else { // Message sent but not necessarily read, and no unread count
            holder.unreadCountTextView.setVisibility(View.GONE);
            holder.readCheckmarkImageView.setVisibility(View.GONE); // Or another status icon
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatItems.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView senderNameTextView;
        TextView lastMessageTextView;
        TextView timestampTextView;
        TextView unreadCountTextView;
        ImageView readCheckmarkImageView;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.chat_item_profile_image);
            senderNameTextView = itemView.findViewById(R.id.chat_item_sender_name);
            lastMessageTextView = itemView.findViewById(R.id.chat_item_last_message);
            timestampTextView = itemView.findViewById(R.id.chat_item_timestamp);
            unreadCountTextView = itemView.findViewById(R.id.chat_item_unread_count);
            readCheckmarkImageView = itemView.findViewById(R.id.chat_item_read_checkmark);
        }
    }

    // Helper method to update data
    public void updateChatList(List<ChatItem> newChatItems) {
        this.chatItems.clear();
        this.chatItems.addAll(newChatItems);
        notifyDataSetChanged();
    }
}
