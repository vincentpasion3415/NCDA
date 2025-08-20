package com.example.ncda;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.ncda.model.Referral;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

public class ReferralDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView tvAdminRemark;
    private TextView tvRemarks;
    private TextView tvStatus;

    private Referral referral;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_referral_details);

        db   = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        referral = (Referral) getIntent().getSerializableExtra("referral");

        if (referral != null) {
            TextView tvPersonalName   = findViewById(R.id.tv_personal_name);
            TextView tvPwdId          = findViewById(R.id.tv_pwd_id);
            TextView tvDisability     = findViewById(R.id.tv_disability);
            TextView tvServiceNeeded  = findViewById(R.id.tv_service_needed);
            tvRemarks                 = findViewById(R.id.tv_remarks);
            tvAdminRemark             = findViewById(R.id.tv_admin_remark);
            tvStatus                  = findViewById(R.id.tv_status);
            TextView tvTimestamp      = findViewById(R.id.tv_timestamp);

            // Set Personal and Referral information
            tvPersonalName.setText(referral.getPersonalName());
            tvPwdId.setText(referral.getPwdId());
            tvDisability.setText(referral.getDisability());
            tvServiceNeeded.setText(referral.getServiceNeeded());

            // Static status (initial display)
            if (referral.getStatus() != null && !referral.getStatus().isEmpty()) {
                tvStatus.setText(referral.getStatus());
            } else {
                tvStatus.setText("N/A");
            }

            // Timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            String formattedDate = sdf.format(referral.getTimestamp());
            tvTimestamp.setText(formattedDate);

            // Retrieve remarks and status from Firestore
            if (referral.getId() != null) {
                displayRemarks(referral.getId());
            } else {
                tvRemarks.setText("No remarks submitted yet.");
                tvAdminRemark.setText("No admin remarks available.");
            }
        }
    }

    private void displayRemarks(String referralId) {
        db.collection("referrals")
                .document(referralId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    // Update status (in case admin changed it)
                    String latestStatus = documentSnapshot.getString("status");
                    if (latestStatus != null && !latestStatus.isEmpty()) {
                        tvStatus.setText(latestStatus);
                    }

                    // Show user remarks
                    String userRemarks = documentSnapshot.getString("remarks");
                    if (userRemarks != null && !userRemarks.isEmpty()) {
                        tvRemarks.setText(userRemarks);
                    } else {
                        tvRemarks.setText("No remarks submitted yet.");
                    }

                    // Check adminRemark
                    String adminRemarks = documentSnapshot.getString("adminRemark");
                    if (adminRemarks != null && !adminRemarks.isEmpty()) {
                        tvAdminRemark.setText(adminRemarks);
                        return;
                    }

                    // Fall back to comments array
                    Object commentsObject = documentSnapshot.get("comments");
                    if (commentsObject instanceof java.util.List<?>) {
                        java.util.List<?> commentsList = (java.util.List<?>) commentsObject;
                        if (!commentsList.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            for (Object item : commentsList) {
                                if (item instanceof Map) {
                                    Map<String, Object> commentMap = (Map<String, Object>) item;
                                    String text = (String) commentMap.get("text");
                                    com.google.firebase.Timestamp timestamp =
                                            (com.google.firebase.Timestamp) commentMap.get("timestamp");

                                    if (text != null && !text.isEmpty()) {
                                        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                                        String ts = (timestamp != null) ? sdf.format(timestamp.toDate()) : "";
                                        sb.append(text)
                                                .append("\nTimestamp: ")
                                                .append(ts)
                                                .append("\n\n");
                                    }
                                }
                            }

                            if (sb.length() > 0) {
                                tvAdminRemark.setText(sb.toString().trim());
                            } else {
                                tvAdminRemark.setText("No admin remarks available.");
                            }
                        } else {
                            tvAdminRemark.setText("No admin remarks available.");
                        }
                    } else {
                        tvAdminRemark.setText("No admin remarks available.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ReferralDetailsActivity", "Error getting remarks", e);
                    tvRemarks.setText("Error loading remarks.");
                    tvAdminRemark.setText("Error loading admin remarks.");
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
