package com.example.ncda.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import com.example.ncda.R;
import com.example.ncda.ReferralSubmission;

public class ReferralHistoryAdapter extends RecyclerView.Adapter<ReferralHistoryAdapter.ReferralHistoryViewHolder> {

    private List<ReferralSubmission> referralList;

    public ReferralHistoryAdapter(List<ReferralSubmission> referralList) {
        this.referralList = referralList;
    }

    @NonNull
    @Override
    public ReferralHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_referral_history, parent, false); // Create a new XML layout file named this
        return new ReferralHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReferralHistoryViewHolder holder, int position) {
        ReferralSubmission referral = referralList.get(position);
        holder.tvStatus.setText("Status: " + referral.getStatus());
        holder.tvRemarks.setText("Admin Remarks: " + referral.getAdminRemark());
        // You can add more fields to display here
    }

    @Override
    public int getItemCount() {
        return referralList.size();
    }

    static class ReferralHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvStatus, tvRemarks;

        ReferralHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStatus = itemView.findViewById(R.id.tv_referral_status);
            tvRemarks = itemView.findViewById(R.id.tv_referral_remarks);
        }
    }
}