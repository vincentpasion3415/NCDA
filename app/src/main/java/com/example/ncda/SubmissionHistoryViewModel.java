package com.example.ncda;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.ncda.model.Referral; // ADD THIS IMPORT
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class SubmissionHistoryViewModel extends ViewModel {

    private final MutableLiveData<List<SubmissionItem>> _submissionHistory = new MutableLiveData<>();
    public LiveData<List<SubmissionItem>> getSubmissionHistory() {
        return _submissionHistory;
    }

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>();
    public LiveData<Boolean> isLoading() {
        return _isLoading;
    }

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> getErrorMessage() {
        return _errorMessage;
    }

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private String currentUserId;

    public SubmissionHistoryViewModel() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (currentUserId == null) {
            _errorMessage.setValue("User not logged in.");
            _isLoading.setValue(false);
        } else {
            fetchSubmissions();
        }
    }

    public void fetchSubmissions() {
        if (currentUserId == null) {
            _errorMessage.setValue("Cannot fetch submissions: User ID is null.");
            _isLoading.setValue(false);
            return;
        }

        _isLoading.setValue(true);
        _errorMessage.setValue(null);

        final List<SubmissionItem> combinedList = new ArrayList<>();
        final AtomicInteger pendingFetches = new AtomicInteger(3); // CHANGE THE COUNT TO 3

        db.collection("appointments")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Appointment appointment = document.toObject(Appointment.class);
                        appointment.setId(document.getId());
                        combinedList.add(appointment);
                    }
                    if (pendingFetches.decrementAndGet() == 0) {
                        sortAndPostSubmissions(combinedList);
                    }
                })
                .addOnFailureListener(e -> {
                    _errorMessage.setValue("Error fetching appointments: " + e.getMessage());
                    _isLoading.setValue(false);
                });


        db.collection("pwdApplications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        PWDApplication pwdApplication = document.toObject(PWDApplication.class);
                        pwdApplication.setId(document.getId());
                        combinedList.add(pwdApplication);
                    }
                    if (pendingFetches.decrementAndGet() == 0) {
                        sortAndPostSubmissions(combinedList);
                    }
                })
                .addOnFailureListener(e -> {
                    _errorMessage.setValue("Error fetching PWD applications: " + e.getMessage());
                    _isLoading.setValue(false);
                });

        db.collection("referrals") // ADD THIS NEW QUERY
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Referral referral = document.toObject(Referral.class);
                        referral.setId(document.getId());
                        combinedList.add(referral);
                    }
                    if (pendingFetches.decrementAndGet() == 0) {
                        sortAndPostSubmissions(combinedList);
                    }
                })
                .addOnFailureListener(e -> {
                    _errorMessage.setValue("Error fetching referrals: " + e.getMessage());
                    _isLoading.setValue(false);
                });
    }

    private void sortAndPostSubmissions(List<SubmissionItem> list) {
        Collections.sort(list, (o1, o2) -> {
            if (o1.getTimestamp() == null && o2.getTimestamp() == null) return 0;
            if (o1.getTimestamp() == null) return 1;
            if (o2.getTimestamp() == null) return -1;
            return o2.getTimestamp().compareTo(o1.getTimestamp());
        });
        _submissionHistory.setValue(list);
        _isLoading.setValue(false);
    }

    public void retryFetch() {
        if (currentUserId != null) {
            fetchSubmissions();
        }
    }
}