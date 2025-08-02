package com.example.ncda;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.analytics.FirebaseAnalytics;
import android.app.AlertDialog;
import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri; // Added for opening app settings
import android.os.Bundle;
import android.os.Handler; // Added for delayed UI updates
import android.provider.Settings; // Added for opening app settings for permissions
import android.speech.RecognitionListener; // Added for custom speech listener
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer; // Explicitly used for custom speech input
import android.util.Log;
import android.util.TypedValue; // Added for applyFontSize with SP
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Activity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue; // Added for server timestamp

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects; // Added for Objects.requireNonNull

public class AppointmentSchedulingActivity extends AppCompatActivity {


    private TextInputEditText editTextFullName, editTextContactNumber, editTextEmailAddress, editTextPwdId, editTextSpecialRequests;
    private Spinner spinnerPurpose, spinnerPreferredTime, spinnerRegion, spinnerOffice;
    private EditText editTextPreferredDate, editTextAlternateDateTime;
    private RadioGroup radioGroupMode;
    private CheckBox checkBoxInterpreter, checkBoxWheelchairAccess, checkBoxAgreement;
    private Button buttonUploadDocuments, buttonSubmit;
    private ImageButton voiceCommandButton;
    private ImageButton fullNameSpeechButton, contactNumberSpeechButton, emailAddressSpeechButton, pwdIdSpeechButton, specialRequestsSpeechButton;
    private TextView speechStatusTextView; // ADDED: for custom speech status feedback

    private FirebaseAnalytics mFirebaseAnalytics;
    private Calendar preferredDateCalendar = Calendar.getInstance();
    private Calendar alternateDateTimeCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

    private String selectedRegion;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    // Renamed for clarity and consistency with PwdApplicationActivity
    private static final int REQUEST_CODE_VOICE_COMMAND_GENERAL = 103; // For general commands via RecognizerIntent
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private EditText currentSpeechInputEditText; // Renamed to clearly indicate its use for speech input
    private String currentSpeechInputPrompt; // Renamed for clarity
    private int activeFieldIdForNavigation; // Tracks the currently active field for voice navigation

    private SpeechRecognizer speechRecognizer; // Declared SpeechRecognizer instance
    private final Handler handler = new Handler(); // For delayed UI updates

    // --- Speech Recognition Listener with enhanced feedback (from ProfileActivity & PwdApplicationActivity) ---
    private final RecognitionListener speechRecognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            speechStatusTextView.setText(currentSpeechInputPrompt + " (Listening...)"); // Show specific prompt
            speechStatusTextView.setVisibility(View.VISIBLE); // Make status visible
        }

        @Override
        public void onBeginningOfSpeech() {
            // Optional: Provide a subtle visual cue or sound
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            // Optional: Provide a visual indicator of voice volume
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            speechStatusTextView.setText("Processing...");
        }

        @Override
        public void onError(int error) {
            String errorMessage;
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    errorMessage = "Audio recording error.";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    errorMessage = "Client side error.";
                    break;
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
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    errorMessage = "Network error. Check internet connection.";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    errorMessage = "Network timeout. Try again.";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errorMessage = "No speech recognized. Please try again.";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    errorMessage = "Recognition service is busy. Try again soon.";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    errorMessage = "Server error. Try again.";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errorMessage = "No speech input detected.";
                    break;
                default:
                    errorMessage = "Unknown error occurred: " + error;
                    break;
            }
            speechStatusTextView.setText("Error: " + errorMessage);
            Log.e("SpeechRecognition", "Error: " + error + " - " + errorMessage);
            // Clear status after a delay
            handler.postDelayed(() -> speechStatusTextView.setVisibility(View.GONE), 3000); // Hide after error
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String recognizedText = matches.get(0);
                if (currentSpeechInputEditText != null) {
                    currentSpeechInputEditText.setText(recognizedText);
                    currentSpeechInputEditText.setSelection(recognizedText.length()); // Set cursor to end
                    speechStatusTextView.setText("Recognized: " + recognizedText);
                } else {
                    Log.w("SpeechRecognition", "currentSpeechInputEditText is null. Recognized text: " + recognizedText);
                    speechStatusTextView.setText("Recognized but no active field: " + recognizedText);
                }
            } else {
                speechStatusTextView.setText("No speech recognized.");
            }
            // Clear status after a short delay
            handler.postDelayed(() -> speechStatusTextView.setVisibility(View.GONE), 2000); // Hide after success
            currentSpeechInputEditText = null; // Clear the reference after processing
            currentSpeechInputPrompt = null;
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean darkMode = sharedPreferences.getBoolean("darkMode", false);
        setAppTheme(darkMode);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appointment_scheduling);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("");
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initializeViews(); // Call the new initializeViews method
        initializeSpinners(); // Call the new initializeSpinners method
        setupListeners(); // Call the new setupListeners method
        setupSpeechButtons(); // Setup field-specific speech buttons

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(speechRecognitionListener);

        float fontSize = sharedPreferences.getInt("fontSize", 16);
        applyFontSizesToAllTextViews(fontSize); // Apply font size to all relevant views
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy(); // Release SpeechRecognizer resources
        }
    }

    private void initializeViews() {
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextContactNumber = findViewById(R.id.editTextContactNumber);
        editTextEmailAddress = findViewById(R.id.editTextEmailAddress);
        editTextPwdId = findViewById(R.id.editTextPwdId);
        spinnerPurpose = findViewById(R.id.spinnerPurpose);
        editTextPreferredDate = findViewById(R.id.editTextPreferredDate);
        spinnerPreferredTime = findViewById(R.id.spinnerPreferredTime);
        editTextAlternateDateTime = findViewById(R.id.editTextAlternateDateTime);
        radioGroupMode = findViewById(R.id.radioGroupMode);
        spinnerRegion = findViewById(R.id.spinnerRegion);
        spinnerOffice = findViewById(R.id.spinnerOffice);
        editTextSpecialRequests = findViewById(R.id.editTextSpecialRequests);
        checkBoxInterpreter = findViewById(R.id.checkBoxInterpreter);
        checkBoxWheelchairAccess = findViewById(R.id.checkBoxWheelchairAccess);
        buttonUploadDocuments = findViewById(R.id.buttonUploadDocuments);
        checkBoxAgreement = findViewById(R.id.checkBoxAgreement);
        buttonSubmit = findViewById(R.id.buttonSubmit);

        voiceCommandButton = findViewById(R.id.voiceCommandButton);
        fullNameSpeechButton = findViewById(R.id.fullNameSpeechButton);
        contactNumberSpeechButton = findViewById(R.id.contactNumberSpeechButton);
        emailAddressSpeechButton = findViewById(R.id.emailAddressSpeechButton);
        pwdIdSpeechButton = findViewById(R.id.pwdIdSpeechButton);
        specialRequestsSpeechButton = findViewById(R.id.specialRequestsSpeechButton);
        speechStatusTextView = findViewById(R.id.speechStatusTextView); // INITIALIZE THE NEW TEXTVIEW
        speechStatusTextView.setVisibility(View.GONE); // Start as GONE
    }

    private void initializeSpinners() {
        // Purpose Spinner
        ArrayAdapter<CharSequence> purposeAdapter = ArrayAdapter.createFromResource(this,
                R.array.appointment_purposes, android.R.layout.simple_spinner_item);
        purposeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPurpose.setAdapter(purposeAdapter);

        // Preferred Time Spinner
        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(this,
                R.array.preferred_time_slots, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPreferredTime.setAdapter(timeAdapter);

        // Region Spinner - FIX: Changed R.array.philippine_regions to R.array.ncda_regions
        ArrayAdapter<CharSequence> regionAdapter = ArrayAdapter.createFromResource(this,
                R.array.ncda_regions, android.R.layout.simple_spinner_item);
        regionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRegion.setAdapter(regionAdapter);
    }

    private void setupSpeechButtons() {
        // Field IDs can be arbitrary unique integers, or use a specific order
        setupSpeechButton(fullNameSpeechButton, editTextFullName, "your full name", 1);
        setupSpeechButton(contactNumberSpeechButton, editTextContactNumber, "your contact number", 2);
        setupSpeechButton(emailAddressSpeechButton, editTextEmailAddress, "your email address", 3);
        setupSpeechButton(pwdIdSpeechButton, editTextPwdId, "your PWD ID number", 4);
        setupSpeechButton(specialRequestsSpeechButton, editTextSpecialRequests, "your special requests", 5);
    }

    private void setupListeners() {
        editTextPreferredDate.setOnClickListener(v -> showDatePickerDialog(editTextPreferredDate, preferredDateCalendar));
        editTextAlternateDateTime.setOnClickListener(v -> showDateTimePickerDialog(editTextAlternateDateTime, alternateDateTimeCalendar));

        spinnerRegion.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRegion = parent.getItemAtPosition(position).toString();
                populateOfficesSpinner(selectedRegion);
                Bundle bundle = new Bundle();
                bundle.putString("selected_region", selectedRegion);
                mFirebaseAnalytics.logEvent("region_selection", bundle);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Bundle bundle = new Bundle();
                bundle.putString("selected_region", "None");
                mFirebaseAnalytics.logEvent("region_not_selected", bundle);
            }
        });


        buttonUploadDocuments.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("upload_status", "clicked");
            mFirebaseAnalytics.logEvent("document_upload_attempt", bundle);
            Toast.makeText(AppointmentSchedulingActivity.this, "Document Upload Clicked", Toast.LENGTH_SHORT).show();
        });

        buttonSubmit.setOnClickListener(v -> {
            if (validateInputs()) {
                Bundle bundle = new Bundle();
                bundle.putString("appointment_status", "submitted");
                mFirebaseAnalytics.logEvent("appointment_submission", bundle);
                submitAppointmentToFirebase();
            }
        });

        voiceCommandButton.setOnClickListener(v -> startGeneralVoiceCommandRecognition()); // Changed to call the new method
    }

    // NEW METHOD: setAppTheme
    private void setAppTheme(boolean isDarkMode) {
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void applyFontSizesToAllTextViews(float fontSize) {
        applyFontSize(editTextFullName, fontSize);
        applyFontSize(editTextContactNumber, fontSize);
        applyFontSize(editTextEmailAddress, fontSize);
        applyFontSize(editTextPwdId, fontSize);
        applyFontSize(editTextSpecialRequests, fontSize);
        applyFontSize(editTextPreferredDate, fontSize);
        applyFontSize(editTextAlternateDateTime, fontSize);
        applyFontSize(checkBoxInterpreter, fontSize);
        applyFontSize(checkBoxWheelchairAccess, fontSize);
        applyFontSize(checkBoxAgreement, fontSize);
        applyFontSize(buttonUploadDocuments, fontSize);
        applyFontSize(buttonSubmit, fontSize);
        applyFontSize(speechStatusTextView, fontSize); // Apply font size to the new TextView

        // Apply to radio buttons in group (if needed, otherwise apply to individual radios if they are accessed)
        // If radio buttons are inside radioGroupMode, you might need to iterate or get references directly
        // FIX: Changed R.id.radioButtonOnline to R.id.radioButtonVirtual
        RadioButton onlineRb = findViewById(R.id.radioButtonVirtual);
        RadioButton inPersonRb = findViewById(R.id.radioButtonInPerson);
        if (onlineRb != null) applyFontSize(onlineRb, fontSize);
        if (inPersonRb != null) applyFontSize(inPersonRb, fontSize);

        // Spinner TextViews - often need custom adapter for font size, but setting on Spinner itself might help
        // Or you'd need to extend ArrayAdapter and override getView/getDropDownView
        // For simplicity, we'll skip direct spinner item text sizing here unless a custom adapter is implemented.
    }


    @Override
    protected void onResume() {
        super.onResume();
        Bundle screenViewBundle = new Bundle();
        screenViewBundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, "AppointmentScheduling");
        screenViewBundle.putString(FirebaseAnalytics.Param.SCREEN_CLASS, "AppointmentSchedulingActivity");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, screenViewBundle);
    }

    // Renamed from startVoiceCommandRecognition() for clarity, now uses RecognizerIntent
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

    // New method for setting up speech input for EditTexts using SpeechRecognizer instance
    private void setupSpeechButton(ImageButton button, EditText editText, String prompt, int fieldId) {
        if (button != null) {
            button.setOnClickListener(v -> {
                activeFieldIdForNavigation = fieldId; // Set active field for potential navigation
                currentSpeechInputEditText = editText; // Set the target EditText
                currentSpeechInputPrompt = prompt; // Set the prompt for the listener

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    startFieldSpecificSpeechRecognition();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
                    // The speech will start after permission is granted in onRequestPermissionsResult
                }
            });
        } else {
            Log.e("AppointmentSchedulingActivity", "Speech button not found for field ID: " + fieldId);
        }
    }

    // New method to start SpeechRecognizer for specific fields
    private void startFieldSpecificSpeechRecognition() {
        speechRecognizer.stopListening(); // Stop any previous listening
        speechRecognizer.cancel();        // Cancel any pending recognition

        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString()); // Use device locale
        // EXTRA_PROMPT is handled by RecognitionListener in this approach
        // speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, currentSpeechInputPrompt); // No need here, listener handles it

        speechStatusTextView.setText("Ready for " + currentSpeechInputPrompt.toLowerCase() + "...");
        speechStatusTextView.setVisibility(View.VISIBLE); // Make status visible
        speechRecognizer.startListening(speechIntent);
    }

    // NEW METHOD: showDatePickerDialog
    private void showDatePickerDialog(EditText targetEditText, Calendar calendar) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, monthOfYear);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    targetEditText.setText(dateFormatter.format(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    // NEW METHOD: showDateTimePickerDialog
    private void showDateTimePickerDialog(EditText targetEditText, Calendar calendar) {
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, monthOfYear);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);
                                targetEditText.setText(dateTimeFormatter.format(calendar.getTime()));
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            false); // `false` for 12-hour format, `true` for 24-hour format
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    // NEW METHOD: populateOfficesSpinner
    private void populateOfficesSpinner(String region) {
        int officesArrayResId;
        switch (region) {
            case "National Capital Region (NCR)":
                officesArrayResId = R.array.ncr_offices;
                break;
            case "Region I (Ilocos Region)":
                officesArrayResId = R.array.ilocos_offices;
                break;
            case "Region II (Cagayan Valley)":
                officesArrayResId = R.array.cagayan_valley_offices;
                break;
            case "Region III (Central Luzon)":
                officesArrayResId = R.array.central_luzon_offices;
                break;
            case "Region IV-A (CALABARZON)":
                officesArrayResId = R.array.calabarzon_offices;
                break;
            case "Region IV-B (MIMAROPA)":
                officesArrayResId = R.array.mimaropa_offices;
                break;
            case "Region V (Bicol Region)":
                officesArrayResId = R.array.bicol_offices;
                break;
            case "Region VI (Western Visayas)":
                officesArrayResId = R.array.western_visayas_offices;
                break;
            case "Region VII (Central Visayas)":
                officesArrayResId = R.array.central_visayas_offices;
                break;
            case "Region VIII (Eastern Visayas)":
                officesArrayResId = R.array.eastern_visayas_offices;
                break;
            case "Region IX (Zamboanga Peninsula)":
                officesArrayResId = R.array.zamboanga_peninsula_offices;
                break;
            case "Region X (Northern Mindanao)":
                officesArrayResId = R.array.northern_mindanao_offices;
                break;
            case "Region XI (Davao Region)":
                officesArrayResId = R.array.davao_region_offices;
                break;
            case "Region XII (SOCCSKSARGEN)":
                officesArrayResId = R.array.soccsksargen_offices;
                break;
            case "Region XIII (Caraga)":
                officesArrayResId = R.array.caraga_offices;
                break;
            case "Cordillera Administrative Region (CAR)":
                officesArrayResId = R.array.car_offices;
                break;
            case "BARMM (Bangsamoro Autonomous Region in Muslim Mindanao)":
                officesArrayResId = R.array.barmm_offices;
                break;
            default:
                officesArrayResId = R.array.default_offices; // A fallback or empty array
                break;
        }

        ArrayAdapter<CharSequence> officeAdapter = ArrayAdapter.createFromResource(this,
                officesArrayResId, android.R.layout.simple_spinner_item);
        officeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerOffice.setAdapter(officeAdapter);
    }

    public void applyFontSize(TextView textView, float fontSize) {
        if (textView != null) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize); // Use TypedValue for SP
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == REQUEST_CODE_VOICE_COMMAND_GENERAL) { // Only handle general commands here
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String spokenText = result.get(0);
                    processSpokenCommand(spokenText.toLowerCase());
                } else {
                    Toast.makeText(this, "No speech input recognized for command.", Toast.LENGTH_SHORT).show();
                }
            }
            // Add other onActivityResult handlers here if you have any (e.g., file pickers)
        } else if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this, "Operation cancelled.", Toast.LENGTH_SHORT).show();
            speechStatusTextView.setVisibility(View.GONE); // Hide status on cancel
        } else {
            Toast.makeText(this, "Operation failed.", Toast.LENGTH_SHORT).show();
            speechStatusTextView.setVisibility(View.GONE); // Hide status on fail
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            handleRecordAudioPermissionResult(grantResults);
        }
        // Handle other permission results like file storage if you have them
    }

    private void handleRecordAudioPermissionResult(int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // If a specific EditText was targeted before permission request
            if (currentSpeechInputEditText != null && currentSpeechInputPrompt != null) {
                startFieldSpecificSpeechRecognition();
            } else {
                // Otherwise, assume it's for general voice command (e.g., if user clicked the main voice button)
                startGeneralVoiceCommandRecognition();
            }
        } else {
            Toast.makeText(this, "Record audio permission denied. Voice input will not work.", Toast.LENGTH_SHORT).show();
            speechStatusTextView.setVisibility(View.GONE); // Hide status if permission denied
            // Optionally, show a dialog explaining why the permission is needed and how to enable it
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
        // Clear references regardless of permission outcome to avoid stale state
        currentSpeechInputEditText = null;
        currentSpeechInputPrompt = null;
    }

    // Fuzzy Matching Utility Function (from ProfileActivity and PwdApplicationActivity)
    public static int calculateLevenshteinDistance(String s1, String s2) {
        s1 = s1.toLowerCase(Locale.getDefault());
        s2 = s2.toLowerCase(Locale.getDefault());

        int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0)
                    costs[j] = j;
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
            if (i > 0)
                costs[s2.length()] = lastValue;
        }
        return costs[s2.length()];
    }

    // NEW METHOD: validateInputs
    private boolean validateInputs() {
        boolean isValid = true;

        // Full Name
        if (editTextFullName.getText().toString().trim().isEmpty()) {
            editTextFullName.setError("Full Name is required.");
            isValid = false;
        }

        // Contact Number (basic validation)
        String contactNum = editTextContactNumber.getText().toString().trim();
        if (contactNum.isEmpty()) {
            editTextContactNumber.setError("Contact Number is required.");
            isValid = false;
        } else if (contactNum.length() < 7 || contactNum.length() > 15) { // Example length check
            editTextContactNumber.setError("Invalid Contact Number length.");
            isValid = false;
        }

        // Email Address (basic validation)
        String email = editTextEmailAddress.getText().toString().trim();
        if (email.isEmpty()) {
            editTextEmailAddress.setError("Email Address is required.");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmailAddress.setError("Enter a valid email address.");
            isValid = false;
        }

        // Purpose Spinner
        if (spinnerPurpose.getSelectedItemPosition() == 0) { // Assuming "Select Purpose" is at position 0
            ((TextView) spinnerPurpose.getSelectedView()).setError("Purpose is required.");
            isValid = false;
        }

        // Preferred Date
        if (editTextPreferredDate.getText().toString().trim().isEmpty()) {
            editTextPreferredDate.setError("Preferred Date is required.");
            isValid = false;
        }

        // Preferred Time Spinner
        if (spinnerPreferredTime.getSelectedItemPosition() == 0) { // Assuming "Select Time Slot" is at position 0
            ((TextView) spinnerPreferredTime.getSelectedView()).setError("Preferred Time is required.");
            isValid = false;
        }

        // Mode of Appointment RadioGroup
        if (radioGroupMode.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Please select a Mode of Appointment.", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        // Region Spinner (for in-person)
        RadioButton inPersonRadioButton = findViewById(R.id.radioButtonInPerson);
        if (inPersonRadioButton != null && inPersonRadioButton.isChecked()) {
            if (spinnerRegion.getSelectedItemPosition() == 0) { // Assuming "Select Region" is at position 0
                ((TextView) spinnerRegion.getSelectedView()).setError("Region is required for In-Person appointments.");
                isValid = false;
            }
            // Check Office Spinner if region is selected and it's not "Select Office"
            if (spinnerOffice.getAdapter() != null && spinnerOffice.getSelectedItemPosition() == 0 && spinnerRegion.getSelectedItemPosition() != 0) {
                ((TextView) spinnerOffice.getSelectedView()).setError("Specific Office is required for In-Person appointments.");
                isValid = false;
            }
        }


        // Agreement CheckBox
        if (!checkBoxAgreement.isChecked()) {
            checkBoxAgreement.setError("You must agree to the terms.");
            isValid = false;
            Toast.makeText(this, "Please confirm that the information provided is accurate.", Toast.LENGTH_LONG).show();
        } else {
            checkBoxAgreement.setError(null); // Clear error if checked
        }

        // If validation fails, scroll to the first invalid field or show a general toast
        if (!isValid) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
        }

        return isValid;
    }

    // NEW METHOD: submitAppointmentToFirebase
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
        appointment.put("fullName", editTextFullName.getText().toString().trim());
        appointment.put("contactNumber", editTextContactNumber.getText().toString().trim());
        appointment.put("emailAddress", editTextEmailAddress.getText().toString().trim());
        appointment.put("pwdId", editTextPwdId.getText().toString().trim());
        appointment.put("purpose", spinnerPurpose.getSelectedItem().toString());
        appointment.put("preferredDate", editTextPreferredDate.getText().toString().trim());
        appointment.put("preferredTime", spinnerPreferredTime.getSelectedItem().toString());
        appointment.put("alternateDateTime", editTextAlternateDateTime.getText().toString().trim());

        RadioButton selectedModeRadioButton = findViewById(radioGroupMode.getCheckedRadioButtonId());
        if (selectedModeRadioButton != null) {
            appointment.put("mode", selectedModeRadioButton.getText().toString());
        } else {
            appointment.put("mode", "Not specified");
        }

        // Only add region/office if in-person mode is selected
        if (selectedModeRadioButton != null && selectedModeRadioButton.getId() == R.id.radioButtonInPerson) {
            appointment.put("region", spinnerRegion.getSelectedItem().toString());
            appointment.put("office", spinnerOffice.getSelectedItem().toString());
        } else {
            appointment.put("region", "N/A");
            appointment.put("office", "N/A");
        }

        appointment.put("interpreterNeeded", checkBoxInterpreter.isChecked());
        appointment.put("wheelchairAccessRequired", checkBoxWheelchairAccess.isChecked());
        appointment.put("specialRequests", editTextSpecialRequests.getText().toString().trim());
        appointment.put("agreementConfirmed", checkBoxAgreement.isChecked());
        appointment.put("timestamp", FieldValue.serverTimestamp()); // Use server timestamp for consistency

        // --- ADD THIS LINE TO SET THE INITIAL STATUS ---
        appointment.put("status", "Pending"); // Default status for new appointments
        // -------------------------------------------------

        db.collection("appointments")
                .add(appointment)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(AppointmentSchedulingActivity.this, "Appointment submitted successfully!", Toast.LENGTH_LONG).show();
                        Log.d("AppointmentScheduling", "DocumentSnapshot added with ID: " + documentReference.getId());
                        clearForm(); // Clear the form after successful submission
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(AppointmentSchedulingActivity.this, "Error submitting appointment: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        Log.w("AppointmentScheduling", "Error adding document", e);
                    }
                });
    }

    // NEW METHOD: logoutUser
    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(AppointmentSchedulingActivity.this, LoginActivity.class); // Assuming LoginActivity
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear back stack
        startActivity(intent);
        finish();
        Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show();
    }

    // NEW METHOD: clearForm
    private void clearForm() {
        editTextFullName.setText("");
        editTextContactNumber.setText("");
        editTextEmailAddress.setText("");
        editTextPwdId.setText("");
        spinnerPurpose.setSelection(0); // Select the first item ("Select Purpose")
        editTextPreferredDate.setText("");
        spinnerPreferredTime.setSelection(0); // Select the first item ("Select Preferred Time Slot")
        editTextAlternateDateTime.setText("");
        radioGroupMode.clearCheck(); // Clear radio button selection
        spinnerRegion.setSelection(0); // Select the first item ("Select Region")
        spinnerOffice.setSelection(0); // Reset office spinner
        editTextSpecialRequests.setText("");
        checkBoxInterpreter.setChecked(false);
        checkBoxWheelchairAccess.setChecked(false);
        checkBoxAgreement.setChecked(false);
        Toast.makeText(this, "Form cleared.", Toast.LENGTH_SHORT).show();
    }

    private void processSpokenCommand(String spokenText) {
        Map<String, Runnable> commands = new HashMap<>();

        // Navigation Commands (similar to PwdApplicationActivity)
        commands.put("profile", () -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(AppointmentSchedulingActivity.this, ProfileActivity.class)
                        .putExtra("userId", FirebaseAuth.getInstance().getCurrentUser().getUid()));
            } else {
                Toast.makeText(this, "You need to be logged in to view your profile.", Toast.LENGTH_SHORT).show();
            }
        });
        commands.put("my profile", () -> {
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
        commands.put("go home", () -> {
            startActivity(new Intent(AppointmentSchedulingActivity.this, HomeActivity.class));
            finish();
        });
        commands.put("main menu", () -> {
            startActivity(new Intent(AppointmentSchedulingActivity.this, HomeActivity.class));
            finish();
        });
        commands.put("back", this::onBackPressed);
        commands.put("go back", this::onBackPressed);
        commands.put("previous page", this::onBackPressed);
        commands.put("logout", this::logoutUser);
        commands.put("sign out", this::logoutUser);
        commands.put("appointments", () -> Toast.makeText(this, "You are already on the appointment scheduling page.", Toast.LENGTH_SHORT).show());


        // Form-specific commands (action-oriented)
        commands.put("clear form", this::clearForm);
        commands.put("reset form", this::clearForm);
        commands.put("submit appointment", this::submitAppointmentToFirebase); // Assuming this validates and submits
        commands.put("upload documents", () -> Toast.makeText(this, "Please use the 'Upload Documents' button to manually select files.", Toast.LENGTH_SHORT).show()); // Voice command to remind of manual upload
        commands.put("open preferred date", () -> showDatePickerDialog(editTextPreferredDate, preferredDateCalendar));
        commands.put("select preferred date", () -> showDatePickerDialog(editTextPreferredDate, preferredDateCalendar));
        commands.put("open alternate date time", () -> showDateTimePickerDialog(editTextAlternateDateTime, alternateDateTimeCalendar));
        commands.put("select alternate date time", () -> showDateTimePickerDialog(editTextAlternateDateTime, alternateDateTimeCalendar));
        commands.put("check interpreter", () -> checkBoxInterpreter.setChecked(!checkBoxInterpreter.isChecked()));
        commands.put("toggle interpreter", () -> checkBoxInterpreter.setChecked(!checkBoxInterpreter.isChecked()));
        commands.put("check wheelchair access", () -> checkBoxWheelchairAccess.setChecked(!checkBoxWheelchairAccess.isChecked()));
        commands.put("toggle wheelchair access", () -> checkBoxWheelchairAccess.setChecked(!checkBoxWheelchairAccess.isChecked()));
        commands.put("agree to terms", () -> checkBoxAgreement.setChecked(true));
        commands.put("disagree to terms", () -> checkBoxAgreement.setChecked(false)); // Allow unchecking

        // Radio group commands - FIX: Changed R.id.radioButtonOnline to R.id.radioButtonVirtual
        commands.put("select online mode", () -> selectRadioButton(R.id.radioButtonVirtual));
        commands.put("select in-person mode", () -> selectRadioButton(R.id.radioButtonInPerson));

        // Field navigation commands
        commands.put("next field", this::navigateNextField);
        commands.put("previous field", this::navigatePreviousField);


        int bestDistance = Integer.MAX_VALUE;
        Runnable bestAction = null;
        String matchedCommand = "";
        int MATCH_THRESHOLD = 2; // Adjust as needed for command matching

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
            // FIX: Completed the Toast.makeText string literal
            Toast.makeText(this, "Command not recognized: " + spokenText + ". Try 'help' for available commands.", Toast.LENGTH_SHORT).show();
        }
    }

    // NEW METHOD: selectRadioButton
    private void selectRadioButton(int radioButtonId) {
        radioGroupMode.check(radioButtonId);
        RadioButton selectedRb = findViewById(radioButtonId);
        if (selectedRb != null) {
            Toast.makeText(this, "Selected " + selectedRb.getText().toString() + " mode.", Toast.LENGTH_SHORT).show();
        }
    }

    // NEW METHOD: navigateNextField
    private void navigateNextField() {
        // Define an ordered list of editable fields by their IDs
        // This is a simplified example; a more robust solution might use a List<EditText>
        // that you populate in initializeViews() in the desired order.
        int[] fieldOrder = {
                R.id.editTextFullName,
                R.id.editTextContactNumber,
                R.id.editTextEmailAddress,
                R.id.editTextPwdId,
                R.id.editTextSpecialRequests
        };

        int currentFieldIndex = -1;
        if (activeFieldIdForNavigation != 0) {
            for (int i = 0; i < fieldOrder.length; i++) {
                if (fieldOrder[i] == activeFieldIdForNavigation) {
                    currentFieldIndex = i;
                    break;
                }
            }
        }

        int nextFieldIndex = currentFieldIndex + 1;
        if (nextFieldIndex < fieldOrder.length) {
            EditText nextField = findViewById(fieldOrder[nextFieldIndex]);
            if (nextField != null) {
                nextField.requestFocus();
                nextField.setSelection(nextField.getText().length()); // Place cursor at end
                Toast.makeText(this, "Navigating to next field.", Toast.LENGTH_SHORT).show();
                activeFieldIdForNavigation = fieldOrder[nextFieldIndex]; // Update active field for next cycle
            }
        } else {
            Toast.makeText(this, "End of fields.", Toast.LENGTH_SHORT).show();
            activeFieldIdForNavigation = 0; // Reset or loop to first field if desired
        }
    }

    // NEW METHOD: navigatePreviousField
    private void navigatePreviousField() {
        int[] fieldOrder = {
                R.id.editTextFullName,
                R.id.editTextContactNumber,
                R.id.editTextEmailAddress,
                R.id.editTextPwdId,
                R.id.editTextSpecialRequests
        };

        int currentFieldIndex = -1;
        if (activeFieldIdForNavigation != 0) {
            for (int i = 0; i < fieldOrder.length; i++) {
                if (fieldOrder[i] == activeFieldIdForNavigation) {
                    currentFieldIndex = i;
                    break;
                }
            }
        }

        int previousFieldIndex = currentFieldIndex - 1;
        if (previousFieldIndex >= 0) {
            EditText previousField = findViewById(fieldOrder[previousFieldIndex]);
            if (previousField != null) {
                previousField.requestFocus();
                previousField.setSelection(previousField.getText().length());
                Toast.makeText(this, "Navigating to previous field.", Toast.LENGTH_SHORT).show();
                activeFieldIdForNavigation = fieldOrder[previousFieldIndex]; // Update active field for next cycle
            }
        } else {
            Toast.makeText(this, "Beginning of fields.", Toast.LENGTH_SHORT).show();
            activeFieldIdForNavigation = 0; // Reset or loop to last field if desired
        }
    }
}