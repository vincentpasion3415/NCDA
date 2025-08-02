package com.example.ncda;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; // Import ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;



public class SubmissionHistoryFragment extends Fragment {

    private RecyclerView recyclerViewSubmissions;
    private SubmissionHistoryAdapter submissionHistoryAdapter;
    private ProgressBar progressBarLoading;
    private TextView tvEmptyState;
    private SubmissionHistoryViewModel submissionHistoryViewModel;

    public SubmissionHistoryFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_submission_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        recyclerViewSubmissions = view.findViewById(R.id.recycler_view_submissions);
        progressBarLoading = view.findViewById(R.id.progress_bar_loading);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);


        submissionHistoryAdapter = new SubmissionHistoryAdapter();
        recyclerViewSubmissions.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewSubmissions.setAdapter(submissionHistoryAdapter);


        submissionHistoryViewModel = new ViewModelProvider(this).get(SubmissionHistoryViewModel.class);


        submissionHistoryViewModel.getSubmissionHistory().observe(getViewLifecycleOwner(), submissions -> {
            submissionHistoryAdapter.submitList(submissions);

            if (submissions.isEmpty()) {
                tvEmptyState.setVisibility(View.VISIBLE);
                recyclerViewSubmissions.setVisibility(View.GONE);
            } else {
                tvEmptyState.setVisibility(View.GONE);
                recyclerViewSubmissions.setVisibility(View.VISIBLE);
            }
        });

        submissionHistoryViewModel.isLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            if (isLoading) {
                recyclerViewSubmissions.setVisibility(View.GONE);
                tvEmptyState.setVisibility(View.GONE);
            }
        });

        submissionHistoryViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_LONG).show();

                tvEmptyState.setText("Error loading submissions: " + errorMessage);
                tvEmptyState.setVisibility(View.VISIBLE);
                recyclerViewSubmissions.setVisibility(View.GONE);
            }
        });

    }
}