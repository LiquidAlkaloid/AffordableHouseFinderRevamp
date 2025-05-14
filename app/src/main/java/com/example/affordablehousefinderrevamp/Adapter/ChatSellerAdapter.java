package com.example.affordablehousefinderrevamp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// import com.bumptech.glide.Glide; // Glide not used if profileImageUrl is removed from ChatItem
// import com.bumptech.glide.request.RequestOptions; // Not used
import com.example.affordablehousefinderrevamp.ChatItem;
import com.example.affordablehousefinderrevamp.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;

public class ChatSellerAdapter extends RecyclerView.Adapter<ChatSellerAdapter.ChatViewHolder> {

    private Context context;
    private List<ChatItem> chatItems;
    private OnChatItemClickListener listener;

    public interface OnChatItemClickListener {
        void onItemClick(ChatItem chatItem);
    }

    public ChatSellerAdapter(Context context, List<ChatItem> chatItems, OnChatItemClickListener listener) {
        this.context = context;
        this.chatItems = chatItems;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_seller, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatItem currentItem = chatItems.get(position);

        // senderName in ChatItem for the seller's list is the Buyer's name
        if (currentItem.getSenderName() != null) {
            holder.senderName.setText(currentItem.getSenderName());
        } else {
            holder.senderName.setText("Buyer"); // Fallback name
        }
        holder.lastMessage.setText(currentItem.getLastMessage());

        // Format and display the timestamp from the Date object
        if (currentItem.getTimestampDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.timestamp.setText(sdf.format(currentItem.getTimestampDate()));
        } else {
            holder.timestamp.setText("");
        }

        // Profile Image for the Buyer is no longer loaded from ChatItem's profileImageUrl
        // Set a default placeholder for the profile picture
        holder.profilePic.setImageResource(R.drawable.ic_person_placeholder);


        // Handle Unread Count display
        if (currentItem.getUnreadCount() > 0) {
            holder.unreadCount.setVisibility(View.VISIBLE);
            holder.unreadCount.setText(String.valueOf(currentItem.getUnreadCount()));
            if (holder.readTick != null) holder.readTick.setVisibility(View.GONE);
        } else {
            holder.unreadCount.setVisibility(View.GONE);
            if (holder.readTick != null) holder.readTick.setVisibility(View.GONE);
        }

        // Set click listener for the item view
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return chatItems != null ? chatItems.size() : 0;
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        ImageView profilePic;
        TextView senderName;
        TextView lastMessage;
        TextView timestamp;
        TextView unreadCount;
        ImageView readTick;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            profilePic = itemView.findViewById(R.id.iv_profile_pic); // Ensure this ID is in item_chat_seller.xml
            senderName = itemView.findViewById(R.id.tv_sender_name);
            lastMessage = itemView.findViewById(R.id.tv_last_message);
            timestamp = itemView.findViewById(R.id.tv_timestamp);
            unreadCount = itemView.findViewById(R.id.tv_unread_count);
            readTick = itemView.findViewById(R.id.iv_read_tick);
        }
    }
}
