package com.example.affordablehousefinderrevamp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.affordablehousefinderrevamp.Model.Message;
import com.example.affordablehousefinderrevamp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    public static final int MSG_TYPE_LEFT = 0; // Receiver's message
    public static final int MSG_TYPE_RIGHT = 1; // Sender's message (current user)

    private Context context;
    private List<Message> messageList;
    private String currentUserId;

    public MessageAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser != null) {
            this.currentUserId = firebaseUser.getUid();
        } else {
            // Handle case where user is not logged in, though this adapter
            // should ideally only be used when a user is logged in.
            this.currentUserId = "";
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == MSG_TYPE_RIGHT) {
            // Inflate layout for messages sent by the current user
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_message_right, parent, false);
        } else {
            // Inflate layout for messages received by the current user
            view = LayoutInflater.from(context).inflate(R.layout.item_chat_message_left, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.messageText.setText(message.getText());

        if (message.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.timestampText.setText(sdf.format(message.getTimestamp()));
        } else {
            holder.timestampText.setText("");
        }

        // Optional: Handle read receipts or profile pictures if your layout supports them
        // if (position == messageList.size() - 1 && message.getSenderId().equals(currentUserId)) {
        //     if (message.isRead()) {
        //         holder.readStatusText.setText("Read");
        //     } else {
        //         holder.readStatusText.setText("Sent");
        //     }
        // } else {
        //     if (holder.readStatusText != null) holder.readStatusText.setVisibility(View.GONE);
        // }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;
        public TextView timestampText;
        // public TextView readStatusText; // Optional for read receipts
        // public ImageView profileImage; // Optional if you show profile images next to messages

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.message_text);
            timestampText = itemView.findViewById(R.id.timestamp_text);
            // readStatusText = itemView.findViewById(R.id.read_status_text); // If you have this ID
            // profileImage = itemView.findViewById(R.id.profile_image_message); // If you have this ID
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    public void updateMessages(List<Message> newMessages) {
        this.messageList.clear();
        this.messageList.addAll(newMessages);
        notifyDataSetChanged();
    }
}
