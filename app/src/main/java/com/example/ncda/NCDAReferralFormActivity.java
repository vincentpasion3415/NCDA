package com.example.ncda;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class NCDAReferralFormActivity extends AppCompatActivity {

    private EditText etPersonalName, etPwdId, etDisability, etRemarks;
    private Button btnSubmit;
    private ImageButton btnMicName, btnMicPwd, btnMicDisability, btnMicRemarks;
    private Spinner spinnerService;
    private String selectedService;
    private FirebaseFirestore db;

    // Speech Recognition
    private SpeechRecognizer speechRecognizer;
    private Handler handler;
    private EditText currentSpeechInputEditText;
    private String currentSpeechInputPrompt;
    private TextView speechStatusTextView;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

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

        btnMicName = findViewById(R.id.btn_mic_name);
        btnMicPwd = findViewById(R.id.btn_mic_pwd);
        btnMicDisability = findViewById(R.id.btn_mic_disability);
        btnMicRemarks = findViewById(R.id.btn_mic_remarks);

        speechStatusTextView = findViewById(R.id.speech_status_text_view);
        handler = new Handler();

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(speechRecognitionListener);
        } else {
            Toast.makeText(this, "Speech recognition not supported.", Toast.LENGTH_LONG).show();
            btnMicName.setVisibility(View.GONE);
            btnMicPwd.setVisibility(View.GONE);
            btnMicDisability.setVisibility(View.GONE);
            btnMicRemarks.setVisibility(View.GONE);
        }

        // Link mic buttons to their EditTexts
        setupSpeechButton(btnMicName, etPersonalName, "Say personal name...");
        setupSpeechButton(btnMicPwd, etPwdId, "Say PWD ID...");
        setupSpeechButton(btnMicDisability, etDisability, "Say disability...");
        setupSpeechButton(btnMicRemarks, etRemarks, "Say remarks...");

        // Retrieve pre-filled intent data
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
        btnSubmit.setOnClickListener(v -> submitReferral());
    }

    private void setupSpeechButton(ImageButton button, EditText editText, String prompt) {
        if (button != null) {
            button.setOnClickListener(v -> {
                currentSpeechInputEditText = editText;
                currentSpeechInputPrompt = prompt;

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    startFieldSpecificSpeechRecognition();
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_RECORD_AUDIO_PERMISSION);
                }
            });
        }
    }

    private void startFieldSpecificSpeechRecognition() {
        if (speechRecognizer != null) {
            Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, currentSpeechInputPrompt);

            speechRecognizer.startListening(speechIntent);
        }
    }

    private final RecognitionListener speechRecognitionListener = new RecognitionListener() {
        @Override public void onReadyForSpeech(Bundle params) {
            if (speechStatusTextView != null) {
                speechStatusTextView.setText(currentSpeechInputPrompt + " (Listening...)");
                speechStatusTextView.setVisibility(View.VISIBLE);
            }
        }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rmsdB) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override public void onEndOfSpeech() {
            if (speechStatusTextView != null) {
                speechStatusTextView.setText("Processing...");
            }
        }
        @Override public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty() && currentSpeechInputEditText != null) {
                String recognizedText = matches.get(0);
                String existingText = currentSpeechInputEditText.getText().toString();
                currentSpeechInputEditText.setText(existingText.isEmpty() ? recognizedText : existingText + " " + recognizedText);
            }
            if (speechStatusTextView != null) speechStatusTextView.setVisibility(View.GONE);
            currentSpeechInputEditText = null;
            currentSpeechInputPrompt = null;
        }
        @Override public void onError(int error) {
            if (speechStatusTextView != null) {
                speechStatusTextView.setText("Error. Try again.");
                handler.postDelayed(() -> speechStatusTextView.setVisibility(View.GONE), 2000);
            }
            currentSpeechInputEditText = null;
            currentSpeechInputPrompt = null;
        }
        @Override public void onPartialResults(Bundle partialResults) { }
        @Override public void onEvent(int eventType, Bundle params) { }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startFieldSpecificSpeechRecognition();
            } else {
                showPermissionDialog();
            }
        }
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs microphone access to use speech-to-text.")
                .setPositiveButton("Grant", (dialog, which) ->
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.RECORD_AUDIO},
                                REQUEST_RECORD_AUDIO_PERMISSION))
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create().show();
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
        referralData.put("userId", user.getUid());

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
