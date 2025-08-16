package com.example.ncda;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class PWDApplicationDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pwd_application_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("PWD Application Details");
        }

        // Get the PWDApplication object passed from the previous activity
        PWDApplication pwdApplication = (PWDApplication) getIntent().getSerializableExtra("pwdApplication");

        if (pwdApplication != null) {
            // Find the TextViews in your layout
            TextView tvApplicationType = findViewById(R.id.tv_application_type);
            TextView tvDisabilityType = findViewById(R.id.tv_disability_type);
            TextView tvStatus = findViewById(R.id.tv_pwd_application_status);
            TextView tvFullName = findViewById(R.id.tv_pwd_application_full_name);
            TextView tvTimestamp = findViewById(R.id.tv_pwd_application_timestamp);

            // Populate the views with the PWDApplication data
            tvApplicationType.setText("Application Type: " + pwdApplication.getApplicationType());
            tvDisabilityType.setText("Disability: " + pwdApplication.getDisabilityType());
            tvStatus.setText("Status: " + pwdApplication.getStatus());
            tvFullName.setText("Applicant: " + pwdApplication.getFullName());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            tvTimestamp.setText("Submitted: " + sdf.format(pwdApplication.getTimestamp()));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}