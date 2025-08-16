package com.example.ncda;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

public class ComplaintActivity extends AppCompatActivity {

    // The name field is now removed from the UI
    private EditText complaintDetailsEditText;
    private Button submitComplaintButton;
    private Toolbar toolbar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.complaint_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Removed complainantNameEditText initialization
        complaintDetailsEditText = findViewById(R.id.complaint_details_edit_text);
        submitComplaintButton = findViewById(R.id.submit_complaint_button);

        submitComplaintButton.setOnClickListener(v -> {
            String complaint = complaintDetailsEditText.getText().toString().trim();

            if (complaint.isEmpty()) {
                Toast.makeText(this, "Please type your complaint.", Toast.LENGTH_SHORT).show();
            } else {
                fetchAndSubmitComplaint(complaint);
            }
        });
    }

    private void fetchAndSubmitComplaint(String complaint) {
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        String userEmail = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : null;

        if (userId == null) {
            Toast.makeText(this, "User not authenticated. Cannot submit complaint.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Fetch the user's name from their registration document
        db.collection("registrationApplications").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = "Anonymous";
                    if (documentSnapshot.exists()) {
                        // Assuming you store the full name in a 'fullName' field
                        String fullName = documentSnapshot.getString("fullName");
                        if (fullName != null && !fullName.isEmpty()) {
                            name = fullName;
                        } else {
                            // If fullName doesn't exist, try to build it from first, middle, last names
                            String firstName = documentSnapshot.getString("firstName");
                            String middleName = documentSnapshot.getString("middleName");
                            String lastName = documentSnapshot.getString("lastName");
                            if (firstName != null && lastName != null) {
                                name = (firstName + " " + (middleName != null ? middleName + " " : "") + lastName).trim();
                            }
                        }
                    }
                    submitComplaintToFirestore(name, complaint, userId, userEmail);
                })
                .addOnFailureListener(e -> {
                    // If fetching the name fails, submit the complaint anonymously
                    submitComplaintToFirestore("Anonymous", complaint, userId, userEmail);
                    Toast.makeText(this, "Could not fetch user name. Submitting anonymously.", Toast.LENGTH_SHORT).show();
                });
    }

    private void submitComplaintToFirestore(String name, String complaint, String userId, String userEmail) {
        Map<String, Object> complaintData = new HashMap<>();
        complaintData.put("name", name);
        complaintData.put("details", complaint);
        complaintData.put("timestamp", new Date());
        complaintData.put("userId", userId);
        complaintData.put("userEmail", userEmail);

        db.collection("complaints")
                .add(complaintData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(ComplaintActivity.this, "Your complaint is for reviewing. We will inform you if its done processed.", Toast.LENGTH_LONG).show();
                    complaintDetailsEditText.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ComplaintActivity.this, "Error submitting complaint. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }
}