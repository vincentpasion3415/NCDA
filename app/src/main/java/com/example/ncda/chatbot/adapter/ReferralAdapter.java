package com.example.ncda.chatbot.adapter;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ncda.R;
import com.example.ncda.model.GovernmentService;
import java.util.List;

public class ReferralAdapter extends RecyclerView.Adapter<ReferralAdapter.ServiceViewHolder> {

    private List<GovernmentService> serviceList;

    public ReferralAdapter(List<GovernmentService> serviceList) {
        this.serviceList = serviceList;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_referral_service, parent, false);
        return new ServiceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ServiceViewHolder holder, int position) {
        GovernmentService service = serviceList.get(position);

        holder.nameTextView.setText(service.getName());

        String hotline = service.getHotline(); // Corrected method call
        if (hotline != null && !hotline.isEmpty()) {
            holder.hotlineButton.setVisibility(View.VISIBLE);
            holder.hotlineButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + hotline.replace(" ", "").replace("(", "").replace(")", "")));
                v.getContext().startActivity(intent);
            });
        } else {
            holder.hotlineButton.setVisibility(View.GONE);
        }

        String email = service.getEmail();
        if (email != null && !email.isEmpty()) {
            holder.emailButton.setVisibility(View.VISIBLE);
            holder.emailButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:"));
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                try {
                    v.getContext().startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(v.getContext(), "No email app found.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.emailButton.setVisibility(View.GONE);
        }

        String website = service.getWebsite();
        if (website != null && !website.isEmpty()) {
            holder.websiteButton.setVisibility(View.VISIBLE);
            holder.websiteButton.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(website));
                v.getContext().startActivity(intent);
            });
        } else {
            holder.websiteButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return serviceList.size();
    }

    static class ServiceViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView;
        Button hotlineButton;
        Button emailButton;
        Button websiteButton;

        ServiceViewHolder(@NonNull View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.service_name);
            hotlineButton = itemView.findViewById(R.id.service_hotline_button);
            emailButton = itemView.findViewById(R.id.service_email_button);
            websiteButton = itemView.findViewById(R.id.service_website_button);
        }
    }
}