package com.example.ncda;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ComplaintDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint_details);

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

            // Populate the views with the Complaint data
            tvDetails.setText("Details: " + complaint.getDetails());
            tvStatus.setText("Status: " + complaint.getStatus());
            tvFullName.setText("Complainant: " + complaint.getFullName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            tvTimestamp.setText("Submitted: " + sdf.format(complaint.getTimestamp()));

        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}