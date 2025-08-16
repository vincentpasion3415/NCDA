package com.example.ncda;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ncda.R;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects; // Import the Objects class

public class SubmissionHistoryAdapter extends ListAdapter<SubmissionItem, SubmissionHistoryAdapter.SubmissionViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(SubmissionItem item);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    private static final int VIEW_TYPE_APPOINTMENT = 1;
    private static final int VIEW_TYPE_PWD_APPLICATION = 2;
    private static final int VIEW_TYPE_COMPLAINT = 3; // New View Type

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
        } else if (item instanceof Complaint) { // New condition
            return VIEW_TYPE_COMPLAINT;
        }
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public SubmissionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // All view types can use the same layout for simplicity
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_submission, parent, false);
        return new SubmissionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SubmissionViewHolder holder, int position) {
        SubmissionItem currentItem = getItem(position);
        holder.bind(currentItem);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentItem);
            }
        });
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
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            tvSubmissionTimestamp.setText("Submitted: " + sdf.format(item.getTimestamp()));

            // Set the full name for all submission types using the common getFullName() method
            if (item.getFullName() != null) {
                if (item instanceof Complaint) {
                    tvFullName.setText("Complainant: " + item.getFullName());
                } else {
                    tvFullName.setText("Applicant: " + item.getFullName());
                }
            }

            // Handle display based on the type of submission
            if (item instanceof Appointment) {
                Appointment appointment = (Appointment) item;
                tvSubmissionType.setText("Appointment Request");
                tvMainDetail.setText("Purpose: " + (appointment.getPurpose() != null ? appointment.getPurpose() : appointment.getAppointmentType()));
                tvSecondaryDetail.setText(String.format(Locale.getDefault(), "Date: %s | Time: %s", appointment.getPreferredDate(), appointment.getPreferredTime()));

            } else if (item instanceof PWDApplication) {
                PWDApplication pwdApplication = (PWDApplication) item;
                tvSubmissionType.setText("PWD Application");
                tvMainDetail.setText("Type: " + pwdApplication.getApplicationType());
                tvSecondaryDetail.setText("Disability: " + pwdApplication.getDisabilityType());

            } else if (item instanceof Complaint) { // New logic for complaints
                Complaint complaint = (Complaint) item;
                tvSubmissionType.setText("Complaint");
                tvMainDetail.setText("Details: " + complaint.getDetails());
                tvSecondaryDetail.setVisibility(View.GONE); // Complaints don't have a secondary detail
            }

            // Set the status text color
            if (item.getStatus() != null) {
                switch (item.getStatus().toLowerCase(Locale.ROOT)) {
                    case "pending":
                        tvSubmissionStatus.setTextColor(itemView.getContext().getColor(R.color.color_status_pending));
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
            tvSubmissionStatus.setText("Status: " + item.getStatus());
        }
    }

    private static final DiffUtil.ItemCallback<SubmissionItem> DIFF_CALLBACK = new DiffUtil.ItemCallback<SubmissionItem>() {
        @Override
        public boolean areItemsTheSame(@NonNull SubmissionItem oldItem, @NonNull SubmissionItem newItem) {
            // FIX: Use Objects.equals() to prevent NullPointerException
            return Objects.equals(oldItem.getId(), newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull SubmissionItem oldItem, @NonNull SubmissionItem newItem) {
            return oldItem.equals(newItem);
        }
    };
}