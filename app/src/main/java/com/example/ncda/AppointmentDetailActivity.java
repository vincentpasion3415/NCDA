package com.example.ncda;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;

public class AppointmentDetailActivity extends AppCompatActivity {

    public static final String EXTRA_APPOINTMENT = "extra_appointment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_detail);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Appointment Information");

        Appointment appointment = (Appointment) getIntent().getSerializableExtra(EXTRA_APPOINTMENT);

        if (appointment != null) {
            populateViews(appointment);
        }
    }

    private void populateViews(Appointment appointment) {
        TextView tvStatus = findViewById(R.id.tv_detail_status);
        TextView tvApplicantName = findViewById(R.id.tv_detail_applicant_name);
        TextView tvContact = findViewById(R.id.tv_detail_contact);
        TextView tvPwdId = findViewById(R.id.tv_detail_pwd_id);
        TextView tvPurpose = findViewById(R.id.tv_detail_purpose);
        TextView tvDate = findViewById(R.id.tv_detail_date);
        TextView tvTime = findViewById(R.id.tv_detail_time);
        TextView tvSpecialRequests = findViewById(R.id.tv_detail_special_requests);
        TextView tvTimestamp = findViewById(R.id.tv_detail_timestamp);

        tvStatus.setText("Status: " + (appointment.getStatus() != null ? appointment.getStatus() : "N/A"));

        String fullName = appointment.getFullName();
        if (fullName == null || fullName.isEmpty()) {
            fullName = String.format(Locale.getDefault(), "%s %s %s",
                    appointment.getFirstName() != null ? appointment.getFirstName() : "",
                    appointment.getMiddleName() != null ? appointment.getMiddleName() : "",
                    appointment.getLastName() != null ? appointment.getLastName() : "").trim();
        }
        tvApplicantName.setText("Applicant Name: " + (!fullName.isEmpty() ? fullName : "N/A"));

        tvContact.setText("Contact Number: " + (appointment.getContactNumber() != null ? appointment.getContactNumber() : "N/A"));
        tvPwdId.setText("PWD ID: " + (appointment.getPwdId() != null ? appointment.getPwdId() : "N/A"));

        String purpose = appointment.getPurpose();
        if (purpose == null || purpose.isEmpty()) {
            purpose = appointment.getAppointmentType();
        }
        tvPurpose.setText("Purpose: " + (purpose != null ? purpose : "N/A"));

        tvDate.setText("Preferred Date: " + (appointment.getPreferredDate() != null ? appointment.getPreferredDate() : "N/A"));
        tvTime.setText("Preferred Time: " + (appointment.getPreferredTime() != null ? appointment.getPreferredTime() : "N/A"));

        tvSpecialRequests.setText("Note for your request: " + (appointment.getSpecialRequests() != null ? appointment.getSpecialRequests() : "N/A"));

        if (appointment.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            tvTimestamp.setText("Submitted On: " + sdf.format(appointment.getTimestamp()));
        } else {
            tvTimestamp.setText("Submitted On: N/A");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}