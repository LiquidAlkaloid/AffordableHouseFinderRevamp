package com.example.affordablehousefinderrevamp.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.affordablehousefinderrevamp.Model.Offer;
import com.example.affordablehousefinderrevamp.R;
import java.util.List;

public class OfferAdapter extends RecyclerView.Adapter<OfferAdapter.OfferViewHolder> {

    private Context context;
    private List<Offer> offerList;
    private OnOfferActionListener listener;

    public interface OnOfferActionListener {
        void onAcceptOffer(Offer offer, int position);
        void onDeclineOffer(Offer offer, int position);
    }

    public OfferAdapter(Context context, List<Offer> offerList, OnOfferActionListener listener) {
        this.context = context;
        this.offerList = offerList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OfferViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_offer_seller, parent, false);
        return new OfferViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfferViewHolder holder, int position) {
        Offer offer = offerList.get(position);

        holder.tvPropertyTitle.setText(offer.getPropertyTitle());
        holder.tvOfferAmount.setText("Offer: " + offer.getOfferAmount());
        holder.tvBuyerName.setText("From: " + offer.getBuyerName());
        holder.tvOfferStatus.setText("Status: " + offer.getStatus().substring(0, 1).toUpperCase() + offer.getStatus().substring(1));


        if (offer.getPropertyImageUrl() != null && !offer.getPropertyImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(offer.getPropertyImageUrl())
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.ic_launcher_background)
                    .into(holder.imgProperty);
        } else {
            holder.imgProperty.setImageResource(R.drawable.placeholder_image);
        }

        if ("pending".equalsIgnoreCase(offer.getStatus())) {
            holder.btnAccept.setVisibility(View.VISIBLE);
            holder.btnDecline.setVisibility(View.VISIBLE);
            holder.tvOfferStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending_color)); // Define this color
        } else if ("accepted".equalsIgnoreCase(offer.getStatus())) {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnDecline.setVisibility(View.GONE);
            holder.tvOfferStatus.setTextColor(ContextCompat.getColor(context, R.color.status_available_color));
        } else if ("declined".equalsIgnoreCase(offer.getStatus())) {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnDecline.setVisibility(View.GONE);
            holder.tvOfferStatus.setTextColor(ContextCompat.getColor(context, R.color.status_taken_color));
        } else {
            holder.btnAccept.setVisibility(View.GONE);
            holder.btnDecline.setVisibility(View.GONE);
            holder.tvOfferStatus.setTextColor(ContextCompat.getColor(context, R.color.grey_text));
        }


        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAcceptOffer(offer, position);
            }
        });

        holder.btnDecline.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeclineOffer(offer, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return offerList.size();
    }

    static class OfferViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProperty;
        TextView tvPropertyTitle, tvOfferAmount, tvBuyerName, tvOfferStatus;
        Button btnAccept, btnDecline;
        // Button btnViewDetails; // Removed as it was in the example but not used for offers directly

        public OfferViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProperty = itemView.findViewById(R.id.img_property_offer);
            tvPropertyTitle = itemView.findViewById(R.id.tv_property_title_offer);
            tvOfferAmount = itemView.findViewById(R.id.tv_offer_amount);
            tvBuyerName = itemView.findViewById(R.id.tv_buyer_name_offer);
            tvOfferStatus = itemView.findViewById(R.id.tv_offer_status);
            btnAccept = itemView.findViewById(R.id.btn_accept_offer);
            btnDecline = itemView.findViewById(R.id.btn_decline_offer);
            // btnViewDetails = itemView.findViewById(R.id.btn_view); // ID from example
        }
    }
}