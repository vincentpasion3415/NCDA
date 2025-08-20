package com.example.ncda;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ComplaintDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView tvAdminRemark;

    private TextView tvDetails;
    private TextView tvStatus;
    private TextView tvTimestamp;
    private TextView tvFullName;

    private DocumentReference complaintRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint_details);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Complaint Details");
        }

        // Get the Complaint object passed from the previous activity
        Complaint complaint = (Complaint) getIntent().getSerializableExtra("complaint");

        // Init views
        tvDetails      = findViewById(R.id.tv_complaint_details);
        tvStatus       = findViewById(R.id.tv_complaint_status);
        tvTimestamp    = findViewById(R.id.tv_complaint_timestamp);
        tvFullName     = findViewById(R.id.tv_complaint_name);
        tvAdminRemark  = findViewById(R.id.tv_admin_remark);

        if (complaint != null) {
            // Always get the latest fields from Firestore using the id
            complaintRef = db.collection("complaints").document(complaint.getId());

            complaintRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name     = documentSnapshot.getString("name");
                    String details  = documentSnapshot.getString("details");
                    String status   = documentSnapshot.getString("status");
                    Date   ts       = documentSnapshot.getDate("timestamp");

                    if (name != null)    tvFullName.setText(name);
                    if (details != null) tvDetails.setText(details);
                    if (status != null)  tvStatus.setText(status);

                    if (ts != null) {
                        SimpleDateFormat sdf =
                                new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                        tvTimestamp.setText(sdf.format(ts));
                    }
                }
                // after we load the main document -> load admin remarks
                displayAdminRemarks(complaint.getId());
            }).addOnFailureListener(e -> {
                Log.e("ComplaintDetailsActivity", "Error loading complaint", e);
                displayAdminRemarks(complaint.getId());
            });
        }
    }

    private void displayAdminRemarks(String complaintId) {
        db.collection("complaints")
                .document(complaintId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder remarksText = new StringBuilder();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault());

                    if (queryDocumentSnapshots.isEmpty()) {
                        remarksText.append("No remarks found.");
                    } else {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                            // main text
                            String text = doc.getString("text");
                            Date   date = doc.getDate("timestamp");

                            // OPTIONAL: pull commenter name as well (if it exists in the comment)
                            String commenter = doc.getString("name"); // <-- if your comment has a "name" field

                            if (text != null) {
                                if (commenter != null) {
                                    remarksText.append(commenter).append(": ");
                                }
                                remarksText.append(text).append("\n");
                            }

                            if (date != null) {
                                remarksText.append("Posted on: ")
                                        .append(sdf.format(date))
                                        .append("\n\n");
                            }
                        }
                    }
                    tvAdminRemark.setText(remarksText.toString().trim());
                })
                .addOnFailureListener(e -> {
                    Log.e("ComplaintDetailsActivity", "Error getting remarks", e);
                    tvAdminRemark.setText("Error loading remarks.");
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
