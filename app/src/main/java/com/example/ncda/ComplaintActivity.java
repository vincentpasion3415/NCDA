package com.example.ncda;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ComplaintActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String TAG = "ComplaintActivity";

    private EditText complaintDetailsEditText;
    private Button submitComplaintButton;
    private ImageButton voiceInputButton;
    private Toolbar toolbar;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SpeechRecognizer speechRecognizer;
    private Handler handler;

    // UI elements to show speech status (you'll need to add this to your XML)
    private TextView speechStatusTextView;

    // Variables to manage which EditText is currently active for speech input
    private EditText currentSpeechInputEditText;
    private String currentSpeechInputPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaint);

        // Make sure you have a TextView with this ID in your activity_complaint.xml
        speechStatusTextView = findViewById(R.id.speech_status_text_view);
        speechStatusTextView.setVisibility(View.GONE);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        toolbar = findViewById(R.id.complaint_toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        complaintDetailsEditText = findViewById(R.id.complaint_details_edit_text);
        submitComplaintButton = findViewById(R.id.submit_complaint_button);
        voiceInputButton = findViewById(R.id.voice_input_button);

        handler = new Handler();

        // Initialize SpeechRecognizer
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(speechRecognitionListener);
        } else {
            Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_LONG).show();
            voiceInputButton.setVisibility(View.GONE); // Hide the button if not supported
        }

        // Setup the voice input button with the new method
        setupSpeechButton(voiceInputButton, complaintDetailsEditText, "Speak your complaint now...");

        submitComplaintButton.setOnClickListener(v -> {
            String complaint = complaintDetailsEditText.getText().toString().trim();
            if (complaint.isEmpty()) {
                Toast.makeText(this, "Please type your complaint.", Toast.LENGTH_SHORT).show();
            } else {
                fetchAndSubmitComplaint(complaint);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start recognition
                startFieldSpecificSpeechRecognition();
            } else {
                Toast.makeText(this, "Audio permission is required for voice input.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // New method to set up the speech button
    private void setupSpeechButton(ImageButton button, EditText editText, String prompt) {
        if (button != null) {
            button.setOnClickListener(v -> {
                currentSpeechInputEditText = editText;
                currentSpeechInputPrompt = prompt;
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startFieldSpecificSpeechRecognition();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                }
            });
        }
    }

    // New method to start speech recognition
    private void startFieldSpecificSpeechRecognition() {
        if (speechRecognizer != null) {
            Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, currentSpeechInputPrompt);

            // This is the key change: start listening with the `SpeechRecognizer` object
            speechRecognizer.startListening(speechIntent);
        }
    }

    // The RecognitionListener as you provided
    private final RecognitionListener speechRecognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            // Check for null to avoid crash if the user backs out of the activity
            if (speechStatusTextView != null) {
                speechStatusTextView.setText(currentSpeechInputPrompt + " (Listening...)");
                speechStatusTextView.setVisibility(View.VISIBLE);
            }
        }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rmsdB) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override
        public void onEndOfSpeech() {
            if (speechStatusTextView != null) {
                speechStatusTextView.setText("Processing...");
            }
        }
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String recognizedText = matches.get(0);
                if (currentSpeechInputEditText != null) {
                    String existingText = currentSpeechInputEditText.getText().toString();
                    if (!existingText.isEmpty()) {
                        currentSpeechInputEditText.setText(existingText + " " + recognizedText);
                    } else {
                        currentSpeechInputEditText.setText(recognizedText);
                    }
                }
            }
            if (speechStatusTextView != null) {
                speechStatusTextView.setVisibility(View.GONE);
            }
            currentSpeechInputEditText = null;
            currentSpeechInputPrompt = null;
        }

        @Override
        public void onError(int error) {
            String errorMessage;
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO: errorMessage = "Audio recording error."; break;
                case SpeechRecognizer.ERROR_CLIENT: errorMessage = "Client side error."; break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    errorMessage = "Insufficient permissions.";
                    showPermissionDialog();
                    break;
                case SpeechRecognizer.ERROR_NETWORK: errorMessage = "Network error."; break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: errorMessage = "Network timeout."; break;
                case SpeechRecognizer.ERROR_NO_MATCH: errorMessage = "No speech recognized. Please try again."; break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: errorMessage = "Recognition service is busy. Try again soon."; break;
                case SpeechRecognizer.ERROR_SERVER: errorMessage = "Server error."; break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: errorMessage = "No speech input detected."; break;
                default: errorMessage = "Unknown error occurred."; break;
            }
            if (speechStatusTextView != null) {
                speechStatusTextView.setText("Error: " + errorMessage);
                Log.e(TAG, "Speech Error: " + errorMessage);
                handler.postDelayed(() -> speechStatusTextView.setVisibility(View.GONE), 3000);
            }
            currentSpeechInputEditText = null;
            currentSpeechInputPrompt = null;
        }

        @Override public void onPartialResults(Bundle partialResults) { }
        @Override public void onEvent(int eventType, Bundle params) { }
    };

    // Helper method to show permission dialog
    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs the microphone permission to enable voice input.")
                .setPositiveButton("Grant", (dialog, which) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void fetchAndSubmitComplaint(String complaint) {
        // Your existing method, no changes needed here.
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        String userEmail = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : null;

        if (userId == null) {
            Toast.makeText(this, "User not authenticated. Cannot submit complaint.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("registrationApplications").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String name = "Anonymous";
                    if (documentSnapshot.exists()) {
                        String fullName = documentSnapshot.getString("fullName");
                        if (fullName != null && !fullName.isEmpty()) {
                            name = fullName;
                        } else {
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
        complaintData.put("status", "Pending");

        db.collection("complaints")
                .add(complaintData)
                .addOnSuccessListener(documentReference -> {
                    // Get the auto-generated ID from the DocumentReference
                    String complaintId = documentReference.getId();

                    // Now, create the Complaint object and set its ID
                    Complaint newComplaint = new Complaint(complaintId, userId, name, complaint, "Pending", new Date());

                    // You can now pass this full object to ComplaintDetailsActivity
                    // and the ID will be available, preventing the crash.
                    // For example, if you wanted to view the complaint right after submitting:
                    // Intent intent = new Intent(ComplaintActivity.this, ComplaintDetailsActivity.class);
                    // intent.putExtra("complaint", newComplaint);
                    // startActivity(intent);

                    Toast.makeText(ComplaintActivity.this, "Your complaint is for reviewing. We will inform you if it's done processed.", Toast.LENGTH_LONG).show();
                    complaintDetailsEditText.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ComplaintActivity.this, "Error submitting complaint. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }
}