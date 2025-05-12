package com.example.affordablehousefinderrevamp.Buyer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.affordablehousefinderrevamp.ChatItem;
import com.example.affordablehousefinderrevamp.R;

import java.util.List;

// Assuming ChatItem.java is the same model class used before
// import com.bumptech.glide.Glide; // Example for image loading

public class ChatBuyerAdapter extends RecyclerView.Adapter<ChatBuyerAdapter.ChatViewHolder> {

    private Context context;
    private List<ChatItem> chatItems;
    private OnChatItemClickListener listener;

    // Interface for click events
    public interface OnChatItemClickListener {
        void onItemClick(ChatItem chatItem);
    }

    // Constructor
    public ChatBuyerAdapter(Context context, List<ChatItem> chatItems, OnChatItemClickListener listener) {
        this.context = context;
        this.chatItems = chatItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_buyer, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem currentItem = chatItems.get(position);

        // Bind data to the views
        holder.senderName.setText(currentItem.getSenderName());
        holder.lastMessage.setText(currentItem.getLastMessage());
        holder.timestamp.setText(currentItem.getTimestamp());

        // Handle profile image (using a placeholder for now)
        // Example: Glide.with(context).load(currentItem.getProfileImageUrl()).placeholder(R.drawable.ic_profile_placeholder).into(holder.profilePic);
        holder.profilePic.setImageResource(R.drawable.ic_person_placeholder); // Placeholder

        // Handle unread count and read tick visibility
        if (currentItem.getUnreadCount() > 0) {
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText(String.valueOf(currentItem.getUnreadCount()));
            holder.readTick.setVisibility(View.GONE);
        } else if (currentItem.isRead()) {
            holder.unreadCount.setVisibility(View.GONE);
            holder.readTick.setVisibility(View.VISIBLE);
        } else {
            holder.unreadCount.setVisibility(View.GONE);
            holder.readTick.setVisibility(View.GONE);
        }

        // Set click listener for the item
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

    // ViewHolder class
    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePic;
        TextView senderName;
        TextView lastMessage;
        TextView timestamp;
        TextView unreadCount;
        ImageView readTick;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ensure these IDs match those in item_chat_buyer.xml
            profilePic = itemView.findViewById(R.id.iv_profile_pic_buyer);
            senderName = itemView.findViewById(R.id.tv_sender_name_buyer);
            lastMessage = itemView.findViewById(R.id.tv_last_message_buyer);
            timestamp = itemView.findViewById(R.id.tv_timestamp_buyer);
            unreadCount = itemView.findViewById(R.id.tv_unread_count_buyer);
            readTick = itemView.findViewById(R.id.iv_read_tick_buyer);
        }
    }

    // Helper method to update data (optional)
    public void updateChatItems(List<ChatItem> newChatItems) {
        this.chatItems.clear();
        this.chatItems.addAll(newChatItems);
        notifyDataSetChanged();
    }
}
