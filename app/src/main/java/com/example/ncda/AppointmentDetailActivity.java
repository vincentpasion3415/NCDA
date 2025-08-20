package com.example.ncda;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            fetchAdminComments(appointment.getId());
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

        // Status
        tvStatus.setText(appointment.getStatus() != null ? appointment.getStatus() : "N/A");

        // Applicant Name
        String fullName = appointment.getFullName();
        if (fullName == null || fullName.isEmpty()) {
            fullName = String.format(Locale.getDefault(), "%s %s %s",
                    appointment.getFirstName() != null ? appointment.getFirstName() : "",
                    appointment.getMiddleName() != null ? appointment.getMiddleName() : "",
                    appointment.getLastName() != null ? appointment.getLastName() : "").trim();
        }
        tvApplicantName.setText(!fullName.isEmpty() ? fullName : "N/A");

        // Contact
        tvContact.setText(appointment.getContactNumber() != null ? appointment.getContactNumber() : "N/A");

        // PWD ID
        tvPwdId.setText(appointment.getPwdId() != null ? appointment.getPwdId() : "N/A");

        // Purpose / appointment type
        String purpose = appointment.getPurpose();
        if (purpose == null || purpose.isEmpty()) {
            purpose = appointment.getAppointmentType();
        }
        tvPurpose.setText(purpose != null ? purpose : "N/A");

        // Date / time / special request
        tvDate.setText(appointment.getPreferredDate() != null ? appointment.getPreferredDate() : "N/A");
        tvTime.setText(appointment.getPreferredTime() != null ? appointment.getPreferredTime() : "N/A");
        tvSpecialRequests.setText(appointment.getSpecialRequests() != null ? appointment.getSpecialRequests() : "N/A");

        // Submitted timestamp
        if (appointment.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
            tvTimestamp.setText(sdf.format(appointment.getTimestamp()));
        } else {
            tvTimestamp.setText("N/A");
        }
    }

    // 🔹 Fetch admin comments from Firestore and display each with text + timestamp
    private void fetchAdminComments(String appointmentId) {
        TextView tvAdminComments = findViewById(R.id.tv_admin_comments);

        FirebaseFirestore.getInstance()
                .collection("appointments")
                .document(appointmentId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        List<Map<String, Object>> comments = (List<Map<String, Object>>) snapshot.get("comments");
                        if (comments != null && !comments.isEmpty()) {
                            StringBuilder sb = new StringBuilder();
                            SimpleDateFormat sdf2 = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());

                            for (Map<String, Object> c : comments) {
                                String text = (String) c.get("text");
                                Timestamp ts = (Timestamp) c.get("timestamp");
                                String dateStr = ts != null ? sdf2.format(ts.toDate()) : "";
                                sb.append(text).append(" – ").append(dateStr).append("\n");
                            }

                            tvAdminComments.setText(sb.toString().trim());
                        } else {
                            tvAdminComments.setText("No admin comments yet.");
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("AppointmentDetails", "Failed to fetch comments", e));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
