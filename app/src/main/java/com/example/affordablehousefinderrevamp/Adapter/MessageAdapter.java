package com.example.affordablehousefinderrevamp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.affordablehousefinderrevamp.Model.Message;
import com.example.affordablehousefinderrevamp.R; // Ensure R is imported for layout resources
// import com.google.firebase.auth.FirebaseAuth; // Not strictly needed if mCurrentUserId is passed
// import com.google.firebase.auth.FirebaseUser; // Not strictly needed

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    public static final int MSG_TYPE_LEFT = 0;  // Receiver's message (shows on the left)
    public static final int MSG_TYPE_RIGHT = 1; // Sender's message (current user, shows on the right)

    private Context mContext;
    private List<Message> mMessageList;
    private String mCurrentUserId; // ID of the currently logged-in user

    // Constructor updated to accept currentUserId
    public MessageAdapter(Context context, List<Message> messageList, String currentUserId) {
        this.mContext = context;
        this.mMessageList = messageList;
        this.mCurrentUserId = currentUserId;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == MSG_TYPE_RIGHT) {
            // Inflate layout for messages sent by the current user (right side)
            view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_message_right, parent, false);
        } else {
            // Inflate layout for messages received by the current user (left side)
            view = LayoutInflater.from(mContext).inflate(R.layout.item_chat_message_left, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = mMessageList.get(position);

        holder.messageTextView.setText(message.getText());

        // Optionally display timestamp
        if (holder.timestampTextView != null) { // Check if the TextView exists in the layout
            if (message.getTimestamp() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                holder.timestampTextView.setText(sdf.format(message.getTimestamp()));
                holder.timestampTextView.setVisibility(View.VISIBLE);
            } else {
                holder.timestampTextView.setVisibility(View.GONE);
            }
        }


        // Optionally, display read status for messages sent by the current user
        if (holder.readStatusTextView != null) { // Check if the TextView exists
            if (getItemViewType(position) == MSG_TYPE_RIGHT) {
                if (message.isRead()) {
                    holder.readStatusTextView.setText("Read");
                    holder.readStatusTextView.setVisibility(View.VISIBLE);
                } else {
                    holder.readStatusTextView.setText("Sent");
                    holder.readStatusTextView.setVisibility(View.VISIBLE);
                }
            } else {
                // For received messages, hide the read status indicator
                holder.readStatusTextView.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mMessageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView messageTextView;
        public TextView timestampTextView; // Make sure this ID (timestamp_text) exists in both layouts
        public TextView readStatusTextView; // Optional: for read receipts (ID: read_status_text)

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.message_text);
            timestampTextView = itemView.findViewById(R.id.timestamp_text); // Ensure this ID is in both XMLs
            // It's safer to check for readStatusTextView's existence here or handle potential null in onBindViewHolder
            // For now, we assume it might be null and check in onBindViewHolder.
            // If R.id.read_status_text is guaranteed in item_chat_message_right.xml, this is fine.
            // If not, it could be null for item_chat_message_left.xml.
            try {
                readStatusTextView = itemView.findViewById(R.id.message_text);
            } catch (Exception e) {
                // read_status_text might not exist in item_chat_message_left.xml, which is fine.
                readStatusTextView = null;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mMessageList.get(position).getSenderId().equals(mCurrentUserId)) {
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }
}
