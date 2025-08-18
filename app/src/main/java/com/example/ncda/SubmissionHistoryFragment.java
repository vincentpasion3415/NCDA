package com.example.ncda;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.ncda.model.Referral;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SubmissionHistoryFragment extends Fragment {

    private RecyclerView recyclerViewSubmissions;
    private SubmissionHistoryAdapter submissionHistoryAdapter;
    private ProgressBar progressBarLoading;
    private TextView tvEmptyState;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private List<SubmissionItem> submissionList = new ArrayList<>();

    public SubmissionHistoryFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_submission_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        recyclerViewSubmissions = view.findViewById(R.id.recycler_view_submissions);
        progressBarLoading = view.findViewById(R.id.progress_bar_loading);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);

        submissionHistoryAdapter = new SubmissionHistoryAdapter();
        recyclerViewSubmissions.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSubmissions.setAdapter(submissionHistoryAdapter);

        submissionHistoryAdapter.setOnItemClickListener(item -> {
            if (item instanceof Appointment) {
                Intent intent = new Intent(getContext(), AppointmentDetailActivity.class);
                intent.putExtra(AppointmentDetailActivity.EXTRA_APPOINTMENT, (Appointment) item);
                startActivity(intent);
            } else if (item instanceof PWDApplication) {
                Intent intent = new Intent(getContext(), PWDApplicationDetailsActivity.class);
                intent.putExtra("pwdApplication", (PWDApplication) item);
                startActivity(intent);
            } else if (item instanceof Complaint) {
                Intent intent = new Intent(getContext(), ComplaintDetailsActivity.class);
                intent.putExtra("complaint", (Complaint) item);
                startActivity(intent);
            } else if (item instanceof Referral) {
                // This is the updated code to start the ReferralDetailsActivity
                Intent intent = new Intent(getContext(), ReferralDetailsActivity.class);
                intent.putExtra("referral", (Referral) item);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchSubmissions();
    }

    private void fetchSubmissions() {
        String userId = mAuth.getCurrentUser().getUid();
        showLoading(true);
        submissionList.clear();

        final AtomicInteger pendingQueries = new AtomicInteger(4);

        // Fetch Appointments
        db.collection("appointments").whereEqualTo("userId", userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            submissionList.add(doc.toObject(Appointment.class));
                        }
                    } else {
                        Toast.makeText(getContext(), "Error fetching appointments.", Toast.LENGTH_SHORT).show();
                    }
                    if (pendingQueries.decrementAndGet() == 0) {
                        onAllQueriesComplete();
                    }
                });

        // Fetch PWD Applications
        db.collection("pwdApplications").whereEqualTo("userId", userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            submissionList.add(doc.toObject(PWDApplication.class));
                        }
                    } else {
                        Toast.makeText(getContext(), "Error fetching PWD applications.", Toast.LENGTH_SHORT).show();
                    }
                    if (pendingQueries.decrementAndGet() == 0) {
                        onAllQueriesComplete();
                    }
                });

        // Fetch Complaints
        db.collection("complaints").whereEqualTo("userId", userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Complaint complaint = doc.toObject(Complaint.class);
                            if (complaint.getStatus() == null || complaint.getStatus().isEmpty()) {
                                complaint.setStatus("Pending");
                            }
                            submissionList.add(complaint);
                        }
                    } else {
                        Toast.makeText(getContext(), "Error fetching complaints.", Toast.LENGTH_SHORT).show();
                    }
                    if (pendingQueries.decrementAndGet() == 0) {
                        onAllQueriesComplete();
                    }
                });

        // Fetch Referrals
        db.collection("referrals").whereEqualTo("userId", userId).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            submissionList.add(doc.toObject(Referral.class));
                        }
                    } else {
                        Toast.makeText(getContext(), "Error fetching referrals.", Toast.LENGTH_SHORT).show();
                    }
                    if (pendingQueries.decrementAndGet() == 0) {
                        onAllQueriesComplete();
                    }
                });
    }

    private void onAllQueriesComplete() {
        showLoading(false);
        Collections.sort(submissionList, (o1, o2) -> {
            if (o1.getTimestamp() == null || o2.getTimestamp() == null) {
                return 0;
            }
            return o2.getTimestamp().compareTo(o1.getTimestamp());
        });
        submissionHistoryAdapter.submitList(new ArrayList<>(submissionList));

        if (submissionList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
        }
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBarLoading.setVisibility(View.VISIBLE);
            recyclerViewSubmissions.setVisibility(View.GONE);
            tvEmptyState.setVisibility(View.GONE);
        } else {
            progressBarLoading.setVisibility(View.GONE);
            recyclerViewSubmissions.setVisibility(View.VISIBLE);
        }
    }
}