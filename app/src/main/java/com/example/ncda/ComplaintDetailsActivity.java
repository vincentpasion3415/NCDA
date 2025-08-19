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
import com.google.firebase.firestore.ServerTimestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ComplaintDetailsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView tvAdminRemark;
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

        if (complaint != null) {
            // Find the TextViews in your layout
            TextView tvDetails = findViewById(R.id.tv_complaint_details);
            TextView tvStatus = findViewById(R.id.tv_complaint_status);
            TextView tvTimestamp = findViewById(R.id.tv_complaint_timestamp);
            TextView tvFullName = findViewById(R.id.tv_complaint_full_name);
            tvAdminRemark = findViewById(R.id.tv_admin_remark);

            // Get a reference to the Firestore document for the complaint
            complaintRef = db.collection("complaints").document(complaint.getId());

            // Populate the views with the Complaint data
            tvDetails.setText("Details: " + complaint.getDetails());
            tvStatus.setText("Status: " + complaint.getStatus());
            tvFullName.setText("Complainant: " + complaint.getFullName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            tvTimestamp.setText("Submitted: " + sdf.format(complaint.getTimestamp()));

            // Call the function to display the admin remarks
            displayAdminRemarks(complaint.getId());
        }
    }

    private void displayAdminRemarks(String complaintId) {
        // Query the 'comments' sub-collection instead of the parent document
        db.collection("complaints")
                .document(complaintId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING) // Order by timestamp
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    StringBuilder remarksText = new StringBuilder();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy, hh:mm a", Locale.getDefault());

                    if (queryDocumentSnapshots.isEmpty()) {
                        remarksText.append("No remarks found.");
                    } else {
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            // The 'text' and 'timestamp' fields are directly on the comment document
                            String text = doc.getString("text");
                            Date date = doc.getDate("timestamp");

                            remarksText.append("Admin Remark: \n");
                            if (text != null) {
                                remarksText.append(text).append("\n");
                            }
                            if (date != null) {
                                remarksText.append("Posted on: ").append(sdf.format(date)).append("\n\n");
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