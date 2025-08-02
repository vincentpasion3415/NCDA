package com.example.ncda;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ncda.Appointment; // Your Appointment model
import com.example.ncda.PWDApplication; // Your PWDApplication model
import com.example.ncda.R; // R file for your project
import com.example.ncda.SubmissionItem; // Your SubmissionItem interface

import java.text.SimpleDateFormat;
import java.util.Locale;

public class SubmissionHistoryAdapter extends ListAdapter<SubmissionItem, SubmissionHistoryAdapter.SubmissionViewHolder> {


    private static final int VIEW_TYPE_APPOINTMENT = 1;
    private static final int VIEW_TYPE_PWD_APPLICATION = 2;

    public SubmissionHistoryAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public int getItemViewType(int position) {
        SubmissionItem item = getItem(position);
        if (item instanceof Appointment) {
            return VIEW_TYPE_APPOINTMENT;
        } else if (item instanceof PWDApplication) {
            return VIEW_TYPE_PWD_APPLICATION;
        }
        return super.getItemViewType(position); // Should not happen
    }

    @NonNull
    @Override
    public SubmissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_submission, parent, false);
        return new SubmissionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubmissionViewHolder holder, int position) {
        SubmissionItem currentItem = getItem(position);
        holder.bind(currentItem);
    }

    public static class SubmissionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvSubmissionType;
        private final TextView tvSubmissionStatus;
        private final TextView tvFullName;
        private final TextView tvMainDetail;
        private final TextView tvSecondaryDetail;
        private final TextView tvSubmissionTimestamp;

        public SubmissionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubmissionType = itemView.findViewById(R.id.tv_submission_type);
            tvSubmissionStatus = itemView.findViewById(R.id.tv_submission_status);
            tvFullName = itemView.findViewById(R.id.tv_full_name);
            tvMainDetail = itemView.findViewById(R.id.tv_main_detail);
            tvSecondaryDetail = itemView.findViewById(R.id.tv_secondary_detail);
            tvSubmissionTimestamp = itemView.findViewById(R.id.tv_submission_timestamp);
        }

        public void bind(SubmissionItem item) {
            tvFullName.setText("Applicant: " + item.getFullName());
            tvSubmissionStatus.setText("Status: " + item.getStatus());
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            tvSubmissionTimestamp.setText("Submitted: " + sdf.format(item.getTimestamp()));


            if (item instanceof Appointment) {
                Appointment appointment = (Appointment) item;
                tvSubmissionType.setText("Appointment Request");
                tvMainDetail.setText("Purpose: " + appointment.getPurpose());
                tvSecondaryDetail.setText(String.format(Locale.getDefault(), "Date: %s | Time: %s",
                        appointment.getPreferredDate(), appointment.getPreferredTime()));

            } else if (item instanceof PWDApplication) {
                PWDApplication pwdApplication = (PWDApplication) item;
                tvSubmissionType.setText("PWD Application");
                tvMainDetail.setText("Type: " + pwdApplication.getApplicationType());
                tvSecondaryDetail.setText("Disability: " + pwdApplication.getDisabilityType());
            }


            if (item.getStatus() != null) {
                switch (item.getStatus().toLowerCase(Locale.ROOT)) {
                    case "pending":
                        tvSubmissionStatus.setTextColor(itemView.getContext().getColor(R.color.color_status_pending)); // Define these colors in colors.xml
                        break;
                    case "approved":
                        tvSubmissionStatus.setTextColor(itemView.getContext().getColor(R.color.color_status_approved));
                        break;
                    case "declined":
                        tvSubmissionStatus.setTextColor(itemView.getContext().getColor(R.color.color_status_declined));
                        break;
                    default:
                        tvSubmissionStatus.setTextColor(itemView.getContext().getColor(android.R.color.darker_gray));
                        break;
                }
            }
        }
    }


    private static final DiffUtil.ItemCallback<SubmissionItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<SubmissionItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull SubmissionItem oldItem, @NonNull SubmissionItem newItem) {

            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SubmissionItem oldItem, @NonNull SubmissionItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}