package com.example.ncda;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import com.example.ncda.model.Referral;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class ReferralDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_referral_details);

        // Find the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add a back button to the toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        Referral referral = (Referral) getIntent().getSerializableExtra("referral");

        if (referral != null) {
            // Get all the TextViews from your layout
            TextView tvPersonalName = findViewById(R.id.tv_personal_name);
            TextView tvPwdId = findViewById(R.id.tv_pwd_id);
            TextView tvDisability = findViewById(R.id.tv_disability);
            TextView tvServiceNeeded = findViewById(R.id.tv_service_needed);
            TextView tvRemarks = findViewById(R.id.tv_remarks);
            TextView tvAdminRemark = findViewById(R.id.tv_admin_remark);
            TextView tvStatus = findViewById(R.id.tv_status);
            TextView tvTimestamp = findViewById(R.id.tv_timestamp);

            // Populate all the TextViews with data
            tvPersonalName.setText("Name: " + referral.getPersonalName());
            tvPwdId.setText("PWD ID: " + referral.getPwdId());
            tvDisability.setText("Disability: " + referral.getDisability());
            tvServiceNeeded.setText("Service Needed: " + referral.getServiceNeeded());
            tvRemarks.setText("Remarks: " + referral.getRemarks());
            tvAdminRemark.setText(referral.getAdminRemark());
            tvStatus.setText("Status: " + referral.getStatus());

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            String formattedDate = sdf.format(referral.getTimestamp());
            tvTimestamp.setText("Submitted: " + formattedDate);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle back button click
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
