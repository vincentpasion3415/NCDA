package com.example.ncda;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AppointmentSchedulingActivity extends BaseActivity { // Changed to extend BaseActivity

    private TextInputEditText editTextFirstName, editTextMiddleName, editTextLastName, editTextContactNumber, editTextPwdId, editTextNote;
    private Spinner spinnerAppointmentType, spinnerPreferredTime;
    private EditText editTextPreferredDate;
    private Button buttonSubmit;
    private CheckBox checkBoxConfirmation;
    private ImageButton voiceCommandButton;
    private ImageButton firstNameSpeechButton, middleNameSpeechButton, lastNameSpeechButton, contactNumberSpeechButton, pwdIdSpeechButton, noteSpeechButton;
    private TextView speechStatusTextView;

    private FirebaseAnalytics mFirebaseAnalytics;
    private Calendar preferredDateCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private static final int REQUEST_CODE_VOICE_COMMAND_GENERAL = 103;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private EditText currentSpeechInputEditText;
    private String currentSpeechInputPrompt;
    private int activeFieldIdForNavigation;

    private SpeechRecognizer speechRecognizer;
    private final Handler handler = new Handler();

    private final RecognitionListener speechRecognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            speechStatusTextView.setText(currentSpeechInputPrompt + " (Listening...)");
            speechStatusTextView.setVisibility(View.VISIBLE);
        }
        @Override public void onBeginningOfSpeech() { }
        @Override public void onRmsChanged(float rmsdB) { }
        @Override public void onBufferReceived(byte[] buffer) { }
        @Override
        public void onEndOfSpeech() {
            speechStatusTextView.setText("Processing...");
        }
        @Override
        public void onError(int error) {
            String errorMessage;
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO: errorMessage = "Audio recording error."; break;
                case SpeechRecognizer.ERROR_CLIENT: errorMessage = "Client side error."; break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    errorMessage = "Insufficient permissions. Please enable in settings.";
                    new AlertDialog.Builder(AppointmentSchedulingActivity.this)
                            .setTitle("Permission Needed")
                            .setMessage("Microphone permission is required for voice input. Please enable it in app settings.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).create().show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK: errorMessage = "Network error. Check internet connection."; break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: errorMessage = "Network timeout. Try again."; break;
                case SpeechRecognizer.ERROR_NO_MATCH: errorMessage = "No speech recognized. Please try again."; break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: errorMessage = "Recognition service is busy. Try again soon."; break;
                case SpeechRecognizer.ERROR_SERVER: errorMessage = "Server error. Try again."; break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: errorMessage = "No speech input detected."; break;
                default: errorMessage = "Unknown error occurred: " + error; break;
            }
            speechStatusTextView.setText("Error: " + errorMessage);
            Log.e("SpeechRecognition", "Error: " + error + " - " + errorMessage);
            handler.postDelayed(() -> speechStatusTextView.setVisibility(View.GONE), 3000);
        }
        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String recognizedText = matches.get(0);
                if (currentSpeechInputEditText != null) {
                    currentSpeechInputEditText.setText(recognizedText);
                    currentSpeechInputEditText.setSelection(recognizedText.length());
                    speechStatusTextView.setText("Recognized: " + recognizedText);
                } else {
                    Log.w("SpeechRecognition", "currentSpeechInputEditText is null. Recognized text: " + recognizedText);
                    speechStatusTextView.setText("Recognized but no active field: " + recognizedText);
                }
            } else {
                speechStatusTextView.setText("No speech recognized.");
            }
            handler.postDelayed(() -> speechStatusTextView.setVisibility(View.GONE), 2000);
            currentSpeechInputEditText = null;
            currentSpeechInputPrompt = null;
        }
        @Override public void onPartialResults(Bundle partialResults) { }
        @Override public void onEvent(int eventType, Bundle params) { }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        setContentView(R.layout.activity_appointment_scheduling);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Appointment");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews();
        initializeSpinners();
        setupListeners();
        setupSpeechButtons();

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(speechRecognitionListener);

        // ✅ Apply accessibility settings for this screen
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        applyAccessibilitySettings(sharedPreferences);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Bundle screenViewBundle = new Bundle();
        screenViewBundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "AppointmentScheduling");
        screenViewBundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "AppointmentSchedulingActivity");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewBundle);
    }

    // ✅ Not overriding, just a helper
    protected void applyAccessibilitySettings(SharedPreferences sharedPreferences) {
        float fontSize = sharedPreferences.getInt("fontSize", 16);
        applyFontSizesToAllTextViews(fontSize);

        boolean isSimplifiedMode = sharedPreferences.getBoolean("simplifiedMode", false);
        if (isSimplifiedMode) {
            // TODO: Apply simplified mode adjustments for this activity
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    // This method handles the back button click
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void initializeViews() {
        editTextFirstName = findViewById(R.id.editTextFirstName);
        editTextMiddleName = findViewById(R.id.editTextMiddleName);
        editTextLastName = findViewById(R.id.editTextLastName);
        editTextContactNumber = findViewById(R.id.editTextContactNumber);
        editTextPwdId = findViewById(R.id.editTextPwdId);
        editTextNote = findViewById(R.id.editTextNote);
        spinnerAppointmentType = findViewById(R.id.spinnerAppointmentType);
        editTextPreferredDate = findViewById(R.id.editTextPreferredDate);
        spinnerPreferredTime = findViewById(R.id.spinnerPreferredTime);
        checkBoxConfirmation = findViewById(R.id.checkBoxConfirmation);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        voiceCommandButton = findViewById(R.id.voiceCommandButton);
        firstNameSpeechButton = findViewById(R.id.firstNameSpeechButton);
        middleNameSpeechButton = findViewById(R.id.middleNameSpeechButton);
        lastNameSpeechButton = findViewById(R.id.lastNameSpeechButton);
        contactNumberSpeechButton = findViewById(R.id.contactNumberSpeechButton);
        pwdIdSpeechButton = findViewById(R.id.pwdIdSpeechButton);
        noteSpeechButton = findViewById(R.id.noteSpeechButton);
        speechStatusTextView = findViewById(R.id.speechStatusTextView);
        speechStatusTextView.setVisibility(View.GONE);

        buttonSubmit.setEnabled(false);
    }

    private void initializeSpinners() {
        ArrayAdapter<String> appointmentTypeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{
                        "Choose Appointment Type",
                        "Free Assistive Devices",
                        "Financial Assistance",
                        "Educational Support",
                        "Job Assistance / Employment",
                        "Medical Certificate Assistance"
                });
        appointmentTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAppointmentType.setAdapter(appointmentTypeAdapter);

        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(this,
                R.array.preferred_time_slots, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPreferredTime.setAdapter(timeAdapter);
    }


    private void setupSpeechButtons() {
        setupSpeechButton(firstNameSpeechButton, editTextFirstName, "your first name", 1);
        setupSpeechButton(middleNameSpeechButton, editTextMiddleName, "your middle name", 2);
        setupSpeechButton(lastNameSpeechButton, editTextLastName, "your last name", 3);
        setupSpeechButton(contactNumberSpeechButton, editTextContactNumber, "your contact number", 4);
        setupSpeechButton(pwdIdSpeechButton, editTextPwdId, "your PWD ID number", 5);
        setupSpeechButton(noteSpeechButton, editTextNote, "your request note", 6);
    }

    private void setupListeners() {
        editTextPreferredDate.setOnClickListener(v -> showCustomDatePickerDialog(editTextPreferredDate, preferredDateCalendar));
        checkBoxConfirmation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonSubmit.setEnabled(isChecked);
        });
        buttonSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                Bundle bundle = new Bundle();
                bundle.putString("appointment_status", "submitted");
                mFirebaseAnalytics.logEvent("appointment_submission", bundle);
                submitAppointmentToFirebase();
            }
        });
        voiceCommandButton.setOnClickListener(v -> startGeneralVoiceCommandRecognition());
    }

    private void applyFontSizesToAllTextViews(float fontSize) {
        applyFontSize(editTextFirstName, fontSize);
        applyFontSize(editTextMiddleName, fontSize);
        applyFontSize(editTextLastName, fontSize);
        applyFontSize(editTextContactNumber, fontSize);
        applyFontSize(editTextPwdId, fontSize);
        applyFontSize(editTextNote, fontSize);
        applyFontSize(editTextPreferredDate, fontSize);
        applyFontSize(checkBoxConfirmation, fontSize);
        applyFontSize(buttonSubmit, fontSize);
        applyFontSize(speechStatusTextView, fontSize);
    }

    private void startGeneralVoiceCommandRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a general command...");
        try {
            startActivityForResult(intent, REQUEST_CODE_VOICE_COMMAND_GENERAL);
        } catch (Exception e) {
            Toast.makeText(this, "Error starting general voice recognition: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("AppointmentSchedulingActivity", "Error starting general voice recognition: " + e.getMessage());
        }
    }

    private void setupSpeechButton(ImageButton button, EditText editText, String prompt, int fieldId) {
        if (button != null) {
            button.setOnClickListener(v -> {
                activeFieldIdForNavigation = fieldId;
                currentSpeechInputEditText = editText;
                currentSpeechInputPrompt = prompt;

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startFieldSpecificSpeechRecognition();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                }
            });
        } else {
            Log.e("AppointmentSchedulingActivity", "Speech button not found for field ID: " + fieldId);
        }
    }

    private void startFieldSpecificSpeechRecognition() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.cancel();
            Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());


            speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
            speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);

            speechStatusTextView.setText("Ready for " + currentSpeechInputPrompt.toLowerCase() + "...");
            speechStatusTextView.setVisibility(View.VISIBLE);
            speechRecognizer.startListening(speechIntent);
        } else {
            Toast.makeText(this, "Speech recognition service is not available.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showCustomDatePickerDialog(EditText targetEditText, Calendar calendar) {
        // Start from today
        Calendar today = Calendar.getInstance();

        // Limit to 60 days in advance
        Calendar maxDate = Calendar.getInstance();
        maxDate.add(Calendar.DAY_OF_YEAR, 60);

        // Use a custom theme for the DatePickerDialog
        // You must define this theme in your res/values/styles.xml or res/values/themes.xml
        int themeResId = R.style.MyBlueDatePickerTheme;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                themeResId, // <-- Apply the custom theme here
                (view, year, monthOfYear, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year, monthOfYear, dayOfMonth);

                    int dayOfWeek = selectedDate.get(Calendar.DAY_OF_WEEK);
                    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                        Toast.makeText(this, "Please select a weekday (Mon–Fri).", Toast.LENGTH_LONG).show();
                    } else {
                        calendar.set(year, monthOfYear, dayOfMonth);
                        // User-friendly date format for display
                        SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                        targetEditText.setText(displayFormat.format(calendar.getTime()));
                    }
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );

        // Set limits
        datePickerDialog.getDatePicker().setMinDate(today.getTimeInMillis());
        datePickerDialog.getDatePicker().setMaxDate(maxDate.getTimeInMillis());

        // This is a common fix for buttons not showing on smaller screens.
        // It ensures the buttons are always visible, even if the content is long.
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", (dialog, which) -> dialog.dismiss());
        datePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", datePickerDialog);

        // 🎨 Apply a workaround to ensure the button colors are set manually.
        datePickerDialog.setOnShowListener(dialog -> {
            Button positiveButton = datePickerDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            Button negativeButton = datePickerDialog.getButton(DialogInterface.BUTTON_NEGATIVE);

            if (positiveButton != null) {
                positiveButton.setTextColor(getResources().getColor(R.color.colorPrimary));
            }

            if (negativeButton != null) {
                negativeButton.setTextColor(getResources().getColor(R.color.colorPrimary));
            }
        });

        datePickerDialog.show();
    }


    public void applyFontSize(TextView textView, float fontSize) {
        if (textView != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_VOICE_COMMAND_GENERAL) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String spokenText = result.get(0);
                    processSpokenCommand(spokenText.toLowerCase());
                } else {
                    Toast.makeText(this, "No speech input recognized for command.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Operation cancelled.", Toast.LENGTH_SHORT).show();
            speechStatusTextView.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, "Operation failed.", Toast.LENGTH_SHORT).show();
            speechStatusTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            handleRecordAudioPermissionResult(grantResults);
        }
    }

    private void handleRecordAudioPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (currentSpeechInputEditText != null && currentSpeechInputPrompt != null) {
                startFieldSpecificSpeechRecognition();
            } else {
                startGeneralVoiceCommandRecognition();
            }
        } else {
            Toast.makeText(this, "Record audio permission denied. Voice input will not work.", Toast.LENGTH_SHORT).show();
            speechStatusTextView.setVisibility(View.GONE);
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Needed")
                        .setMessage("Microphone permission is required for voice input.")
                        .setPositiveButton("OK", (dialog, which) ->
                                ActivityCompat.requestPermissions(AppointmentSchedulingActivity.this,
                                        new String[]{Manifest.permission.RECORD_AUDIO},
                                        REQUEST_RECORD_AUDIO_PERMISSION))
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Denied Permanently")
                        .setMessage("Microphone permission is required for voice features. Please enable it in app settings.")
                        .setPositiveButton("Open Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .create()
                        .show();
            }
        }
        currentSpeechInputEditText = null;
        currentSpeechInputPrompt = null;
    }

    public static int calculateLevenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase(Locale.getDefault());
        s2 = s2.toLowerCase(Locale.getDefault());
        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) costs[j] = j;
                else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1))
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (i > 0) costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    private boolean validateInputs() {
        boolean isValid = true;

        if (editTextFirstName.getText().toString().trim().isEmpty()) {
            editTextFirstName.setError("First Name is required.");
            isValid = false;
        }
        if (editTextLastName.getText().toString().trim().isEmpty()) {
            editTextLastName.setError("Last Name is required.");
            isValid = false;
        }

        String contactNum = editTextContactNumber.getText().toString().trim();
        if (contactNum.isEmpty()) {
            editTextContactNumber.setError("Contact Number is required.");
            isValid = false;
        } else if (contactNum.length() < 7 || contactNum.length() > 15) {
            editTextContactNumber.setError("Invalid Contact Number length.");
            isValid = false;
        }

        String pwdId = editTextPwdId.getText().toString().trim();
        if (pwdId.isEmpty()) {
            editTextPwdId.setError("PWD ID Number is required.");
            isValid = false;
        }

        if (spinnerAppointmentType.getSelectedItemPosition() == 0) {
            ((TextView) spinnerAppointmentType.getSelectedView()).setError("Appointment Type is required.");
            isValid = false;
        }

        String preferredDateStr = editTextPreferredDate.getText().toString().trim();
        if (preferredDateStr.isEmpty()) {
            editTextPreferredDate.setError("Preferred Date is required.");
            isValid = false;
        } else {
            try {
                // Use the same date format as the display format (MMM dd, yyyy)
                SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date preferredDate = displayFormat.parse(preferredDateStr);
                if (preferredDate.before(new Date())) {
                    // This check is good for ensuring a future date is selected
                    editTextPreferredDate.setError("Please select a future date.");
                    isValid = false;
                }
            } catch (ParseException e) {
                // This error will no longer occur with the correct date format
                editTextPreferredDate.setError("Invalid date format.");
                isValid = false;
            }
        }

        if (spinnerPreferredTime.getSelectedItemPosition() == 0) {
            ((TextView) spinnerPreferredTime.getSelectedView()).setError("Preferred Time is required.");
            isValid = false;
        }

        if (!checkBoxConfirmation.isChecked()) {
            checkBoxConfirmation.setError("You must confirm the information is correct.");
            Toast.makeText(this, "Please confirm the information is correct.", Toast.LENGTH_LONG).show();
            isValid = false;
        } else {
            checkBoxConfirmation.setError(null);
        }

        if (!isValid) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
        }

        return isValid;
    }

    private void submitAppointmentToFirebase() {
        String userId = null;
        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
        }

        if (userId == null) {
            Toast.makeText(this, "User not logged in. Cannot submit appointment.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> appointment = new HashMap<>();
        appointment.put("userId", userId);
        appointment.put("firstName", editTextFirstName.getText().toString().trim());
        appointment.put("middleName", editTextMiddleName.getText().toString().trim());
        appointment.put("lastName", editTextLastName.getText().toString().trim());
        appointment.put("contactNumber", editTextContactNumber.getText().toString().trim());
        appointment.put("pwdId", editTextPwdId.getText().toString().trim());
        appointment.put("appointmentType", spinnerAppointmentType.getSelectedItem().toString());
        appointment.put("preferredDate", editTextPreferredDate.getText().toString().trim());
        appointment.put("preferredTime", spinnerPreferredTime.getSelectedItem().toString());
        appointment.put("note", editTextNote.getText().toString().trim());
        appointment.put("timestamp", FieldValue.serverTimestamp());
        appointment.put("status", "Pending");
        appointment.put("agreementConfirmed", checkBoxConfirmation.isChecked());

        db.collection("appointments")
                .add(appointment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AppointmentSchedulingActivity.this, "Appointment submitted successfully!", Toast.LENGTH_LONG).show();
                    Log.d("AppointmentScheduling", "DocumentSnapshot added with ID: " + documentReference.getId());
                    clearForm();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AppointmentSchedulingActivity.this, "Error submitting appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.w("AppointmentScheduling", "Error adding document", e);
                });
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(AppointmentSchedulingActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
    }

    private void clearForm() {
        editTextFirstName.setText("");
        editTextMiddleName.setText("");
        editTextLastName.setText("");
        editTextContactNumber.setText("");
        editTextPwdId.setText("");
        editTextNote.setText("");
        spinnerAppointmentType.setSelection(0);
        editTextPreferredDate.setText("");
        spinnerPreferredTime.setSelection(0);
        checkBoxConfirmation.setChecked(false);
        buttonSubmit.setEnabled(false);
        Toast.makeText(this, "Form cleared.", Toast.LENGTH_SHORT).show();
    }

    private void processSpokenCommand(String spokenText) {
        Map<String, Runnable> commands = new HashMap<>();
        commands.put("profile", () -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(AppointmentSchedulingActivity.this, ProfileActivity.class)
                        .putExtra("userId", FirebaseAuth.getInstance().getCurrentUser().getUid()));
            } else {
                Toast.makeText(this, "You need to be logged in to view your profile.", Toast.LENGTH_SHORT).show();
            }
        });
        commands.put("about", () -> startActivity(new Intent(AppointmentSchedulingActivity.this, AboutActivity.class)));
        commands.put("home", () -> {
            startActivity(new Intent(AppointmentSchedulingActivity.this, HomeActivity.class));
            finish();
        });
        commands.put("back", this::onBackPressed);
        commands.put("logout", this::logoutUser);
        commands.put("sign out", this::logoutUser);
        commands.put("clear form", this::clearForm);
        commands.put("submit appointment", () -> {
            if (validateInputs()) submitAppointmentToFirebase();
        });
        commands.put("i confirm", () -> checkBoxConfirmation.setChecked(true));
        commands.put("unconfirm", () -> checkBoxConfirmation.setChecked(false));
        commands.put("open preferred date", () -> showCustomDatePickerDialog(editTextPreferredDate, preferredDateCalendar));
        commands.put("select preferred date", () -> showCustomDatePickerDialog(editTextPreferredDate, preferredDateCalendar));

        int bestDistance = Integer.MAX_VALUE;
        Runnable bestAction = null;
        String matchedCommand = "";
        int MATCH_THRESHOLD = 2;

        for (Map.Entry<String, Runnable> entry : commands.entrySet()) {
            int distance = calculateLevenshteinDistance(spokenText, entry.getKey());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestAction = entry.getValue();
                matchedCommand = entry.getKey();
            }
        }

        if (bestAction != null && bestDistance <= MATCH_THRESHOLD) {
            Toast.makeText(this, "Command: '" + matchedCommand + "' executed.", Toast.LENGTH_SHORT).show();
            bestAction.run();
        } else {
            Toast.makeText(this, "Command not recognized. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}