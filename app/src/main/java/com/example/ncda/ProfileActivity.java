package com.example.ncda;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {
    private EditText firstNameEditText, lastNameEditText, usernameEditText, emailEditText, phoneNumberEditText, birthEditText;
    private TextView firstNameTextView, lastNameTextView, usernameTextView, emailTextView, phoneNumberTextView, birthTextView, genderTextView;
    private Button editButton, saveButton;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageReference;
    private boolean isEditMode = false;
    private String userId;
    private TextView speechStatusTextView;
    private ImageView profileImageView;

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int GALLERY_PERMISSION_REQUEST_CODE = 101;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int REQUEST_CODE_SPEECH_INPUT_COMMANDS = 201;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    private Uri imageUri;
    private Spinner genderSpinner;
    private SpeechRecognizer speechRecognizer;
    private EditText currentEditText;
    private String currentPrompt; // Stores the prompt for field-specific speech input
    private final Handler handler = new Handler(); // For delayed UI updates

    // --- Speech Recognition Listener with enhanced feedback ---
    private final RecognitionListener speechRecognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle params) {
            speechStatusTextView.setText(currentPrompt + " (Listening...)"); // Show specific prompt
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
            handler.postDelayed(() -> speechStatusTextView.setText(""), 3000);
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String recognizedText = matches.get(0);
                if (currentEditText != null) {
                    currentEditText.setText(recognizedText);
                    speechStatusTextView.setText("Recognized: " + recognizedText);
                } else {
                    Log.w("SpeechRecognition", "currentEditText is null. Recognized text: " + recognizedText);
                    speechStatusTextView.setText("Recognized but no active field: " + recognizedText);
                }
            } else {
                speechStatusTextView.setText("No speech recognized.");
            }
            // Clear status after a short delay
            handler.postDelayed(() -> speechStatusTextView.setText(""), 2000);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    };
    private int activeFieldId;

    private List<String> labels;
    private static final int IMAGE_SIZE_X = 224;
    private static final int IMAGE_SIZE_Y = 224;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean darkMode = sharedPreferences.getBoolean("darkMode", false);
        setAppTheme(darkMode);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        profileImageView = findViewById(R.id.profileImageView);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        birthEditText = findViewById(R.id.birthEditText);
        genderSpinner = findViewById(R.id.genderSpinner);

        phoneNumberEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
        phoneNumberEditText.setHint("11 digits max");
        phoneNumberEditText.setMaxLines(1);
        phoneNumberEditText.setMaxEms(11);

        birthEditText.setInputType(InputType.TYPE_NULL);
        birthEditText.setOnClickListener(v -> showDatePickerDialog());
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.genders_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        firstNameTextView = findViewById(R.id.firstNameTextView);
        lastNameTextView = findViewById(R.id.lastNameTextView);
        usernameTextView = findViewById(R.id.usernameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        phoneNumberTextView = findViewById(R.id.phoneNumberTextView);
        birthTextView = findViewById(R.id.birthTextView);
        genderTextView = findViewById(R.id.genderTextView);
        speechStatusTextView = findViewById(R.id.speech_status_text_view);

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(speechRecognitionListener);

        initializeSpeechButtons(); // Call this to set up listeners for individual field speech buttons
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);
        FloatingActionButton editProfileImageButton = findViewById(R.id.editProfileImageButton);

        ImageButton voiceCommandButton = findViewById(R.id.voiceCommandButton);
        voiceCommandButton.setOnClickListener(v -> startGeneralVoiceRecognition());

        int fontSize = sharedPreferences.getInt("fontSize", 14);
        applyFontSize(firstNameTextView, fontSize);
        applyFontSize(lastNameTextView, fontSize);
        applyFontSize(usernameTextView, fontSize);
        applyFontSize(emailTextView, fontSize);
        applyFontSize(phoneNumberTextView, fontSize);
        applyFontSize(birthTextView, fontSize);
        applyFontSize(genderTextView, fontSize);
        applyFontSize(speechStatusTextView, fontSize);

        userId = getIntent().getStringExtra("userId");
        if (userId != null) {
            fetchProfileData(userId);
        } else {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            Log.e("ProfileActivity", "User ID is null in onCreate");
        }

        editButton.setOnClickListener(v -> toggleEditMode());
        saveButton.setOnClickListener(v -> saveChanges());
        editProfileImageButton.setOnClickListener(v -> editProfileImage());

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (userId != null) {
            fetchProfileData(userId);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }

    }

    private void setAppTheme(boolean darkMode) {
        if (darkMode) {
            setTheme(R.style.Theme_NCDA_Dark);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            setTheme(R.style.Theme_NCDA);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void applyFontSize(TextView textView, int fontSize) {
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize);
    }





    private void startGeneralVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString()); // Use Locale.getDefault() or specific like "en-PH"
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a command...");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT_COMMANDS);
        } catch (Exception e) {
            Toast.makeText(this, "Voice recognition not available: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("ProfileActivity", "Error starting general voice recognition: " + e.getMessage());
        }
    }


    private void speakForField(int fieldId) {
        activeFieldId = fieldId;
        EditText targetEditText = null;
        String prompt = "";
        if (fieldId == R.id.firstNameTextView) {
            targetEditText = firstNameEditText;
            prompt = "Speak the first name";
        } else if (fieldId == R.id.lastNameTextView) {
            targetEditText = lastNameEditText;
            prompt = "Speak the last name";
        } else if (fieldId == R.id.usernameTextView) {
            targetEditText = usernameEditText;
            prompt = "Speak the username";
        } else if (fieldId == R.id.emailTextView) {
            targetEditText = emailEditText;
            prompt = "Speak the email address";
        } else if (fieldId == R.id.phoneNumberTextView) {
            targetEditText = phoneNumberEditText;
            prompt = "Speak the phone number";
        } else if (fieldId == R.id.birthTextView) {
            // For birth date, we will typically open the date picker
            showDatePickerDialog();
            return; // Exit as we handled it by showing date picker
        }

        if (targetEditText != null) {
            setPromptAndStart(targetEditText, prompt);
        } else {
            Toast.makeText(this, "Cannot speak for this field.", Toast.LENGTH_SHORT).show();
        }
    }


    private void setPromptAndStart(EditText targetEditText, String prompt) {
        currentEditText = targetEditText;
        currentPrompt = prompt;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            startSpeechRecognitionWithPrompt(currentEditText, currentPrompt);
        }
    }

    private void startSpeechRecognitionWithPrompt(EditText targetEditText, String prompt) {

        speechRecognizer.stopListening();
        speechRecognizer.cancel();

        Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());
        speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, prompt);


        speechStatusTextView.setText("Ready for " + prompt.toLowerCase() + "...");
        speechRecognizer.startListening(speechIntent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT_COMMANDS) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String spokenText = result.get(0).toLowerCase(Locale.getDefault());
                    Log.d("VoiceCommand", "Recognized text: " + spokenText);

                    handleVoiceCommand(spokenText);

                } else {
                    Log.e("VoiceCommand", "No speech recognition results.");
                    Toast.makeText(this, "No speech recognized for command.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("VoiceCommand", "Speech recognition failed: resultCode=" + resultCode);
                Toast.makeText(this, "Speech recognition failed for command.", Toast.LENGTH_SHORT).show();
            }
        }
        // Handle Image Picker results (Gallery or Camera)
        else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Bitmap bitmap = null;
            Uri selectedImageUri = null;

            if (data != null) {
                if (data.getData() != null) { // From Gallery
                    selectedImageUri = data.getData();
                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error loading image from gallery.", Toast.LENGTH_SHORT).show();
                    }
                } else if (data.getExtras() != null && data.getExtras().containsKey("data")) { // From Camera
                    bitmap = (Bitmap) data.getExtras().get("data");
                    if (bitmap != null) {
                        selectedImageUri = getImageUri(bitmap); // Convert bitmap to Uri for consistent handling
                    }
                }
            }

            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap);


                imageUri = selectedImageUri; // Update the global imageUri
                uploadProfileImage(imageUri); // Trigger image upload here
            } else {
                Toast.makeText(this, "Failed to load image from gallery/camera.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- Fuzzy Matching Utility Function ---
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


    private void handleVoiceCommand(String spokenText) {
        Map<String, Runnable> commands = new HashMap<>();


        commands.put("home", () -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        commands.put("return home", () -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        commands.put("main menu", () -> {
            startActivity(new Intent(this, HomeActivity.class));
            finish();
        });
        commands.put("back", this::onBackPressed);
        commands.put("go back", this::onBackPressed);
        commands.put("previous page", this::onBackPressed);
        commands.put("log out", () -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
        commands.put("sign out", () -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });


        commands.put("edit profile", this::toggleEditMode);
        commands.put("modify profile", this::toggleEditMode);
        commands.put("change profile", this::toggleEditMode);
        commands.put("update profile", this::toggleEditMode);

        commands.put("save changes", this::saveChanges);
        commands.put("confirm changes", this::saveChanges);

        commands.put("next field", this::navigateNextField);
        commands.put("move to next", this::navigateNextField);
        commands.put("previous field", this::navigatePreviousField);
        commands.put("move to previous", this::navigatePreviousField);


        commands.put("enable edit mode", () -> setEditModeVisibility(true));
        commands.put("turn on edit mode", () -> setEditModeVisibility(true));
        commands.put("disable edit mode", () -> setEditModeVisibility(false));
        commands.put("turn off edit mode", () -> setEditModeVisibility(false));
        commands.put("exit edit mode", () -> setEditModeVisibility(false));


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

            if ((matchedCommand.contains("save") || matchedCommand.contains("confirm")) && !isEditMode) {
                Toast.makeText(this, "Please enter edit mode first to save changes.", Toast.LENGTH_SHORT).show();
            } else if ((matchedCommand.contains("next field") || matchedCommand.contains("previous field")) && !isEditMode) {
                Toast.makeText(this, "Please enter edit mode first to navigate fields.", Toast.LENGTH_SHORT).show();
            } else if (matchedCommand.contains("open profile") || matchedCommand.contains("view profile") || matchedCommand.contains("my account")) {
                Toast.makeText(this, "You are already on the profile page.", Toast.LENGTH_SHORT).show();
            } else {
                bestAction.run();
                Toast.makeText(this, "Command: '" + matchedCommand + "' executed.", Toast.LENGTH_SHORT).show();
            }
        } else {

            if (currentEditText != null && isEditMode) {
                currentEditText.setText(spokenText);
                Toast.makeText(this, "Inputted to field: " + spokenText, Toast.LENGTH_SHORT).show();

            } else {
                Log.e("VoiceCommand", "No command recognized and no active field for input.");
                Toast.makeText(this, "Command not recognized. Say 'help' for commands, or enter edit mode to input text.", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == GALLERY_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseImageFromGallery();
            } else {
                Toast.makeText(this, "Gallery permission denied. Cannot select photos.", Toast.LENGTH_LONG).show();
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Needed")
                            .setMessage("This app needs gallery access to let you select a profile picture.")
                            .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_PERMISSION_REQUEST_CODE))
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Denied Permanently")
                            .setMessage("Gallery permission is required for this feature. Please enable it in app settings.")
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
        } else if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhotoFromCamera();
            } else {
                Toast.makeText(this, "Camera permission denied. Cannot take photos.", Toast.LENGTH_SHORT).show();
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Needed")
                            .setMessage("This app needs camera access to let you take a profile picture.")
                            .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(ProfileActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE))
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .create()
                            .show();
                } else {
                    new AlertDialog.Builder(this)
                            .setTitle("Permission Denied Permanently")
                            .setMessage("Camera permission is required for this feature. Please enable it in app settings.")
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
        } else if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (currentEditText != null && currentPrompt != null) {
                    startSpeechRecognitionWithPrompt(currentEditText, currentPrompt);
                } else {
                    startGeneralVoiceRecognition(); // Fallback if no specific field was targeted
                }
            } else {
                Toast.makeText(this, "Audio recording permission denied. Voice input will not work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void navigateNextField() {
        if (!isEditMode) {
            Toast.makeText(this, "Please enter edit mode to navigate fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        // Define the order of fields for navigation
        int[] fieldOrder = {
                R.id.firstNameTextView,
                R.id.lastNameTextView,
                R.id.usernameTextView,
                R.id.emailTextView,
                R.id.phoneNumberTextView,
                R.id.birthTextView // Birth date will trigger date picker
        };

        int currentIndex = -1;
        for (int i = 0; i < fieldOrder.length; i++) {
            if (activeFieldId == fieldOrder[i]) {
                currentIndex = i;
                break;
            }
        }

        int nextIndex = (currentIndex + 1) % fieldOrder.length;
        speakForField(fieldOrder[nextIndex]);
    }

    private void navigatePreviousField() {
        if (!isEditMode) {
            Toast.makeText(this, "Please enter edit mode to navigate fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int[] fieldOrder = {
                R.id.firstNameTextView,
                R.id.lastNameTextView,
                R.id.usernameTextView,
                R.id.emailTextView,
                R.id.phoneNumberTextView,
                R.id.birthTextView
        };

        int currentIndex = -1;
        for (int i = 0; i < fieldOrder.length; i++) {
            if (activeFieldId == fieldOrder[i]) {
                currentIndex = i;
                break;
            }
        }

        int previousIndex = (currentIndex - 1 + fieldOrder.length) % fieldOrder.length;
        speakForField(fieldOrder[previousIndex]);
    }

    private void setEditModeVisibility(boolean enableEdit) {
        if (enableEdit == isEditMode) {
            Toast.makeText(this, enableEdit ? "Already in edit mode." : "Already in view mode.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enableEdit) {
            firstNameEditText.setVisibility(View.VISIBLE);
            lastNameEditText.setVisibility(View.VISIBLE);
            usernameEditText.setVisibility(View.VISIBLE);
            emailEditText.setVisibility(View.VISIBLE);
            phoneNumberEditText.setVisibility(View.VISIBLE);
            birthEditText.setVisibility(View.VISIBLE);
            genderSpinner.setVisibility(View.VISIBLE);

            findViewById(R.id.firstNameSpeechButton).setVisibility(View.VISIBLE);
            findViewById(R.id.lastNameSpeechButton).setVisibility(View.VISIBLE);
            findViewById(R.id.usernameSpeechButton).setVisibility(View.VISIBLE);
            findViewById(R.id.emailSpeechButton).setVisibility(View.VISIBLE);
            findViewById(R.id.phoneNumberSpeechButton).setVisibility(View.VISIBLE);
            findViewById(R.id.birthSpeechButton).setVisibility(View.VISIBLE);

            firstNameTextView.setVisibility(View.GONE);
            lastNameTextView.setVisibility(View.GONE);
            usernameTextView.setVisibility(View.GONE);
            emailTextView.setVisibility(View.GONE);
            phoneNumberTextView.setVisibility(View.GONE);
            birthTextView.setVisibility(View.GONE);
            genderTextView.setVisibility(View.GONE);

            saveButton.setVisibility(View.VISIBLE);
            editButton.setText("Cancel");


            firstNameEditText.setText(firstNameTextView.getText().toString());
            lastNameEditText.setText(lastNameTextView.getText().toString());
            usernameEditText.setText(usernameTextView.getText().toString());
            emailEditText.setText(emailTextView.getText().toString());
            phoneNumberEditText.setText(phoneNumberTextView.getText().toString().replace("Not Available", ""));
            birthEditText.setText(birthTextView.getText().toString());

            String currentGender = genderTextView.getText().toString();
            String[] gendersArray = getResources().getStringArray(R.array.genders_array);
            int spinnerPosition = -1;
            for (int i = 0; i < gendersArray.length; i++) {
                if (currentGender.equalsIgnoreCase(gendersArray[i])) {
                    spinnerPosition = i;
                    break;
                }
            }
            if (spinnerPosition != -1) {
                genderSpinner.setSelection(spinnerPosition);
            } else {
                genderSpinner.setSelection(genderSpinner.getAdapter().getCount() - 1); // Default to last option if no match
            }

            isEditMode = true;
            Toast.makeText(this, "Edit mode enabled.", Toast.LENGTH_SHORT).show();

        } else {
            firstNameEditText.setVisibility(View.GONE);
            lastNameEditText.setVisibility(View.GONE);
            usernameEditText.setVisibility(View.GONE);
            emailEditText.setVisibility(View.GONE);
            phoneNumberEditText.setVisibility(View.GONE);
            birthEditText.setVisibility(View.GONE);
            genderSpinner.setVisibility(View.GONE);

            findViewById(R.id.firstNameSpeechButton).setVisibility(View.GONE);
            findViewById(R.id.lastNameSpeechButton).setVisibility(View.GONE);
            findViewById(R.id.usernameSpeechButton).setVisibility(View.GONE);
            findViewById(R.id.emailSpeechButton).setVisibility(View.GONE);
            findViewById(R.id.phoneNumberSpeechButton).setVisibility(View.GONE);
            findViewById(R.id.birthSpeechButton).setVisibility(View.GONE);

            firstNameTextView.setVisibility(View.VISIBLE);
            lastNameTextView.setVisibility(View.VISIBLE);
            usernameTextView.setVisibility(View.VISIBLE);
            emailTextView.setVisibility(View.VISIBLE);
            phoneNumberTextView.setVisibility(View.VISIBLE);
            birthTextView.setVisibility(View.VISIBLE);
            genderTextView.setVisibility(View.VISIBLE);
            saveButton.setVisibility(View.GONE);
            editButton.setText("Edit");

            isEditMode = false;
            Toast.makeText(this, "Edit mode disabled.", Toast.LENGTH_SHORT).show();


            if (userId != null) {
                fetchProfileData(userId);
            }
        }
    }

    private void toggleEditMode() {
        setEditModeVisibility(!isEditMode);
    }

    private void saveChanges() {
        if (userId == null) {
            Toast.makeText(this, "Error: User ID is null. Cannot save changes.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isEditMode) {
            Toast.makeText(this, "Not in edit mode. Nothing to save.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Basic validation
        if (firstNameEditText.getText().toString().trim().isEmpty() ||
                lastNameEditText.getText().toString().trim().isEmpty() ||
                usernameEditText.getText().toString().trim().isEmpty() ||
                emailEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (phoneNumberEditText.getText().toString().length() > 0 && phoneNumberEditText.getText().toString().length() != 11) {
            Toast.makeText(this, "Phone number must be 11 digits.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailEditText.getText().toString()).matches()) {
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("firstName", firstNameEditText.getText().toString());
        userUpdates.put("lastName", lastNameEditText.getText().toString());
        userUpdates.put("username", usernameEditText.getText().toString());
        userUpdates.put("email", emailEditText.getText().toString());
        userUpdates.put("contact Number", phoneNumberEditText.getText().toString());
        userUpdates.put("birth", birthEditText.getText().toString());
        userUpdates.put("gender", genderSpinner.getSelectedItem().toString());

        db.collection("users").document(userId)
                .set(userUpdates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    setEditModeVisibility(false); // Exit edit mode after saving
                    fetchProfileData(userId); // Refresh displayed data
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ProfileActivity", "Error updating document", e);
                });
    }

    private void fetchProfileData(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e("ProfileActivity", "User ID is null or empty in fetchProfileData");
            Toast.makeText(this, "User ID is invalid. Cannot fetch profile.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        firstNameTextView.setText(documentSnapshot.getString("firstName"));
                        lastNameTextView.setText(documentSnapshot.getString("lastName"));
                        usernameTextView.setText(documentSnapshot.getString("username"));
                        emailTextView.setText(documentSnapshot.getString("email"));
                        phoneNumberTextView.setText(documentSnapshot.getString("contact Number"));
                        birthTextView.setText(documentSnapshot.getString("birth"));
                        genderTextView.setText(documentSnapshot.getString("gender"));

                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.default_profile_image) // Placeholder
                                    .error(R.drawable.error_profile_image)     // Error image
                                    .into(profileImageView);
                        } else {
                            profileImageView.setImageResource(R.drawable.default_profile_image);
                        }

                    } else {
                        Toast.makeText(ProfileActivity.this, "Profile data not found.", Toast.LENGTH_SHORT).show();
                        Log.d("ProfileActivity", "No such document for userId: " + userId);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error fetching profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("ProfileActivity", "Error fetching document", e);
                });
    }

    private void editProfileImage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Image Source");
        builder.setItems(new CharSequence[]{"Camera", "Gallery"}, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermission();
            } else {
                checkGalleryPermission();
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            takePhotoFromCamera();
        }
    }

    private void checkGalleryPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, GALLERY_PERMISSION_REQUEST_CODE);
        } else {
            chooseImageFromGallery();
        }
    }

    private void chooseImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void takePhotoFromCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, PICK_IMAGE_REQUEST);
        } else {
            Toast.makeText(this, "No camera app found.", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to get Uri from Bitmap (needed for Camera images)
    private Uri getImageUri(Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void uploadProfileImage(Uri imageUri) {
        if (imageUri != null && userId != null) {

            StorageReference profileImageRef = storageReference.child("profile_images/" + userId + ".jpg");

            profileImageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();

                        Map<String, Object> userUpdates = new HashMap<>();
                        userUpdates.put("profileImageUrl", downloadUrl);

                        db.collection("users").document(userId)
                                .set(userUpdates, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Profile image uploaded and link saved!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to save image link: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }))
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No image selected or user not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDatePickerDialog() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(year1, monthOfYear, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
                    birthEditText.setText(sdf.format(selectedDate.getTime()));
                }, year, month, day);
        datePickerDialog.show();
    }


    private void initializeSpeechButtons() {
        findViewById(R.id.firstNameSpeechButton).setOnClickListener(v -> speakForField(R.id.firstNameTextView));
        findViewById(R.id.lastNameSpeechButton).setOnClickListener(v -> speakForField(R.id.lastNameTextView));
        findViewById(R.id.usernameSpeechButton).setOnClickListener(v -> speakForField(R.id.usernameTextView));
        findViewById(R.id.emailSpeechButton).setOnClickListener(v -> speakForField(R.id.emailTextView));
        findViewById(R.id.phoneNumberSpeechButton).setOnClickListener(v -> speakForField(R.id.phoneNumberTextView));
        findViewById(R.id.birthSpeechButton).setOnClickListener(v -> speakForField(R.id.birthTextView)); // This will trigger showDatePickerDialog
    }


}