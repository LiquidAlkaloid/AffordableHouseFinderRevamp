package com.example.affordablehousefinderrevamp.Seller;
 // Replace com.example.yourappname with your actual package name

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

// Import your ChatItem model
// import com.bumptech.glide.Glide; // Example for image loading, add Glide dependency if you use it

public class ChatSellerAdapter extends RecyclerView.Adapter<ChatSellerAdapter.ChatViewHolder> {

    private Context context;
    private List<ChatItem> chatItems;
    private OnChatItemClickListener listener;

    // Interface for click events
    public interface OnChatItemClickListener {
        void onItemClick(ChatItem chatItem);
    }

    // Constructor
    public ChatSellerAdapter(Context context, List<ChatItem> chatItems, OnChatItemClickListener listener) {
        this.context = context;
        this.chatItems = chatItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the item layout
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_seller, parent, false);
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
        // You would typically use a library like Glide or Picasso here
        // Glide.with(context).load(currentItem.getProfileImageUrl()).placeholder(R.drawable.ic_profile_placeholder).into(holder.profilePic);
        holder.profilePic.setImageResource(R.drawable.ic_person_placeholder); // Placeholder

        // Handle unread count and read tick visibility
        if (currentItem.getUnreadCount() > 0) {
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText(String.valueOf(currentItem.getUnreadCount()));
            holder.readTick.setVisibility(View.GONE);
        } else if (currentItem.isRead()) {
            holder.unreadCount.setVisibility(View.GONE);
            holder.readTick.setVisibility(View.VISIBLE);
            // You might want different tick icons for sent, delivered, read
            // For simplicity, using one tick for "read"
        } else { // Message sent but not read, no unread count
            holder.unreadCount.setVisibility(View.GONE);
            holder.readTick.setVisibility(View.GONE); // Or a "sent" tick if you have one
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
            profilePic = itemView.findViewById(R.id.iv_profile_pic);
            senderName = itemView.findViewById(R.id.tv_sender_name);
            lastMessage = itemView.findViewById(R.id.tv_last_message);
            timestamp = itemView.findViewById(R.id.tv_timestamp);
            unreadCount = itemView.findViewById(R.id.tv_unread_count);
            readTick = itemView.findViewById(R.id.iv_read_tick);
        }
    }

    // Helper method to update data (optional)
    public void updateChatItems(List<ChatItem> newChatItems) {
        this.chatItems.clear();
        this.chatItems.addAll(newChatItems);
        notifyDataSetChanged();
    }
}
