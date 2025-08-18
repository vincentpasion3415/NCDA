package com.example.ncda;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.content.Intent;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import java.util.Map;
import java.util.HashMap;

public class NCDAReferralFormActivity extends AppCompatActivity {

    private EditText etPersonalName, etPwdId, etDisability, etRemarks;
    private Button btnSubmit;
    private Spinner spinnerService;
    private String selectedService;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_referral_form);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("NCDA Referral Form");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etPersonalName = findViewById(R.id.et_personal_name);
        etPwdId = findViewById(R.id.et_pwd_id);
        etDisability = findViewById(R.id.et_disability);
        etRemarks = findViewById(R.id.et_remarks);

        // Retrieve and pre-fill data from the Intent
        Intent intent = getIntent();
        if (intent != null) {
            String personalName = intent.getStringExtra("personalName");
            String pwdId = intent.getStringExtra("pwdId");

            if (personalName != null && !personalName.isEmpty()) {
                etPersonalName.setText(personalName);
            }
            if (pwdId != null && !pwdId.isEmpty()) {
                etPwdId.setText(pwdId);
            }
        }

        spinnerService = findViewById(R.id.spinner_service);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.service_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerService.setAdapter(adapter);

        spinnerService.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedService = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedService = null;
            }
        });

        btnSubmit = findViewById(R.id.btn_submit_referral);
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitReferral();
            }
        });
    }

    private void submitReferral() {
        String personalName = etPersonalName.getText().toString().trim();
        String pwdId = etPwdId.getText().toString().trim();
        String disability = etDisability.getText().toString().trim();
        String remarks = etRemarks.getText().toString().trim();

        if (personalName.isEmpty() || pwdId.isEmpty() || disability.isEmpty() || selectedService == null || selectedService.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> referralData = new HashMap<>();
        referralData.put("personalName", personalName);
        referralData.put("pwdId", pwdId);
        referralData.put("disability", disability);
        referralData.put("serviceNeeded", selectedService);
        referralData.put("remarks", remarks);
        referralData.put("status", "Pending");
        referralData.put("adminRemark", null);
        referralData.put("timestamp", FieldValue.serverTimestamp());
        referralData.put("userId", user.getUid()); // ADD THIS LINE

        db.collection("referrals")
                .add(referralData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(NCDAReferralFormActivity.this, "Referral submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(NCDAReferralFormActivity.this, "Error submitting referral: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}