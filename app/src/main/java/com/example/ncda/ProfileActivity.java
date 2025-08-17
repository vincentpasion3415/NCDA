package com.example.ncda;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
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
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private static final int GALLERY_PERMISSION_REQUEST_CODE = 101;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageReference;

    // UI Elements
    private ShapeableImageView profileImageView;

    private Button editButton, saveButton;
    private ImageButton voiceCommandButton;

    // Speech-to-Text buttons
    private ImageButton firstNameSpeechButton, lastNameSpeechButton, middleNameSpeechButton,
            emailSpeechButton, phoneNumberSpeechButton, birthSpeechButton,
            addressSpeechButton, genderSpeechButton;

    // TextViews for displaying profile data
    private TextView firstNameTextView, emailTextView, phoneNumberTextView, birthTextView, genderTextView, addressTextView, pwdIdNumberTextView, speechStatusTextView;

    // EditTexts and Spinner for editing profile data
    private EditText firstNameEditText, lastNameEditText, middleNameEditText, emailEditText, phoneNumberEditText, birthEditText, addressEditText, pwdIdNumberEditText;
    private Spinner genderSpinner;

    private boolean isEditMode = false;
    private String userId;

    private SpeechRecognizer speechRecognizer;
    private final Handler handler = new Handler();
    private EditText currentSpeechInputEditText;
    private String currentSpeechInputPrompt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Get userId from intent or current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
        } else {
            Toast.makeText(this, "User not logged in.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            // This is the key change: we no longer set the title here.
            // The title is now a custom TextView in the layout.
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialize ALL Views
        initializeViews();

        // Setup Gender Spinner
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(this,
                R.array.genders_array, android.R.layout.simple_spinner_item);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(genderAdapter);

        // Set Click Listeners

        editButton.setOnClickListener(v -> toggleEditMode());
        saveButton.setOnClickListener(v -> saveChanges());
        voiceCommandButton.setOnClickListener(v -> startVoiceCommand());

        // Speech recognition setup
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(speechRecognitionListener);
        setupSpeechButtons();

        // Initial state
        fetchProfileData(userId);
        setEditModeVisibility(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private void initializeViews() {
        profileImageView = findViewById(R.id.profileImageView);
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);

        firstNameTextView = findViewById(R.id.firstNameTextView);
        emailTextView = findViewById(R.id.emailTextView);
        phoneNumberTextView = findViewById(R.id.phoneNumberTextView);
        birthTextView = findViewById(R.id.birthTextView);
        genderTextView = findViewById(R.id.genderTextView);
        addressTextView = findViewById(R.id.addressTextView);
        pwdIdNumberTextView = findViewById(R.id.pwdIdNumberTextView);
        speechStatusTextView = findViewById(R.id.speech_status_text_view);

        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        middleNameEditText = findViewById(R.id.middleNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        phoneNumberEditText = findViewById(R.id.phoneNumberEditText);
        birthEditText = findViewById(R.id.birthEditText);
        genderSpinner = findViewById(R.id.genderSpinner);
        addressEditText = findViewById(R.id.addressEditText);
        pwdIdNumberEditText = findViewById(R.id.pwdIdNumberEditText);

        // Set PWD ID, Email and Birth date to be non-editable.
        pwdIdNumberEditText.setEnabled(false);
        pwdIdNumberEditText.setFocusable(false);
        emailEditText.setEnabled(false);
        emailEditText.setFocusable(false);
        birthEditText.setEnabled(false);
        birthEditText.setFocusable(false);

        voiceCommandButton = findViewById(R.id.voiceCommandButton);
        firstNameSpeechButton = findViewById(R.id.firstNameSpeechButton);
        lastNameSpeechButton = findViewById(R.id.lastNameSpeechButton);
        middleNameSpeechButton = findViewById(R.id.middleNameSpeechButton);
        emailSpeechButton = findViewById(R.id.emailSpeechButton);
        phoneNumberSpeechButton = findViewById(R.id.phoneNumberSpeechButton);
        birthSpeechButton = findViewById(R.id.birthSpeechButton);
        addressSpeechButton = findViewById(R.id.addressSpeechButton);
        genderSpeechButton = findViewById(R.id.genderSpeechButton);
    }

    private void setupSpeechButtons() {
        setupSpeechButton(firstNameSpeechButton, firstNameEditText, "first name");
        setupSpeechButton(lastNameSpeechButton, lastNameEditText, "last name");
        setupSpeechButton(middleNameSpeechButton, middleNameEditText, "middle name");
        setupSpeechButton(phoneNumberSpeechButton, phoneNumberEditText, "phone number");
        setupSpeechButton(addressSpeechButton, addressEditText, "your address");
        // Speech buttons for Email, Birth date, and Gender are removed as per user request.
        // There is no need for a speech button for PWD ID as it is not editable.
    }

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
            speechStatusTextView.setText("Error: " + errorMessage);
            Log.e(TAG, "Speech Error: " + errorMessage);
            handler.postDelayed(() -> speechStatusTextView.setVisibility(View.GONE), 3000);
            currentSpeechInputEditText = null;
            currentSpeechInputPrompt = null;
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

    private void startFieldSpecificSpeechRecognition() {
        if (speechRecognizer != null) {
            Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());
            speechIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, currentSpeechInputPrompt);

            // These extras extend the silence timeout
            speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
            speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);

            speechRecognizer.startListening(speechIntent);
        } else {
            Toast.makeText(this, "Speech recognition service is not available.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Needed")
                .setMessage("Microphone permission is required for voice input. Please enable it in app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).create().show();
    }

    private void setEditModeVisibility(boolean isEnabled) {
        if (isEnabled) {
            // Populate EditTexts from TextViews before showing them
            String fullName = firstNameTextView.getText().toString();
            String[] names = fullName.split("\\s+");
            if (names.length > 0) firstNameEditText.setText(names[0]);
            if (names.length > 2) {
                middleNameEditText.setText(names[1]);
                lastNameEditText.setText(names[2]);
            } else if (names.length > 1) {
                lastNameEditText.setText(names[1]);
            }

            emailEditText.setText(emailTextView.getText().toString());
            phoneNumberEditText.setText(phoneNumberTextView.getText().toString());
            birthEditText.setText(birthTextView.getText().toString());
            addressEditText.setText(addressTextView.getText().toString());
            pwdIdNumberEditText.setText(pwdIdNumberTextView.getText().toString());

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
                genderSpinner.setSelection(0);
            }

            // Show editable EditTexts and hide TextViews
            firstNameEditText.setVisibility(View.VISIBLE);
            lastNameEditText.setVisibility(View.VISIBLE);
            middleNameEditText.setVisibility(View.VISIBLE);
            emailEditText.setVisibility(View.VISIBLE);
            phoneNumberEditText.setVisibility(View.VISIBLE);
            addressEditText.setVisibility(View.VISIBLE);
            genderSpinner.setVisibility(View.VISIBLE);
            birthEditText.setVisibility(View.VISIBLE);

            // PWD ID and Email are read-only
            pwdIdNumberEditText.setVisibility(View.VISIBLE);
            pwdIdNumberTextView.setVisibility(View.GONE);
            emailEditText.setVisibility(View.VISIBLE);
            emailTextView.setVisibility(View.GONE);
            birthEditText.setVisibility(View.VISIBLE);
            birthTextView.setVisibility(View.GONE);


            firstNameTextView.setVisibility(View.GONE);
            phoneNumberTextView.setVisibility(View.GONE);
            genderTextView.setVisibility(View.GONE);
            addressTextView.setVisibility(View.GONE);


            saveButton.setVisibility(View.VISIBLE);
            editButton.setText("Cancel");

            // Show speech buttons for editable fields
            firstNameSpeechButton.setVisibility(View.VISIBLE);
            lastNameSpeechButton.setVisibility(View.VISIBLE);
            middleNameSpeechButton.setVisibility(View.VISIBLE);
            phoneNumberSpeechButton.setVisibility(View.VISIBLE);
            addressSpeechButton.setVisibility(View.VISIBLE);

            // Hide speech buttons for non-editable fields (Email, Birth date, Gender)
            emailSpeechButton.setVisibility(View.GONE);
            birthSpeechButton.setVisibility(View.GONE);
            genderSpeechButton.setVisibility(View.GONE);
            voiceCommandButton.setVisibility(View.VISIBLE);

            isEditMode = true;
            Toast.makeText(this, "Edit mode enabled.", Toast.LENGTH_SHORT).show();

        } else {
            // Hide all EditTexts and show TextViews
            firstNameEditText.setVisibility(View.GONE);
            lastNameEditText.setVisibility(View.GONE);
            middleNameEditText.setVisibility(View.GONE);
            emailEditText.setVisibility(View.GONE);
            phoneNumberEditText.setVisibility(View.GONE);
            birthEditText.setVisibility(View.GONE);
            addressEditText.setVisibility(View.GONE);
            genderSpinner.setVisibility(View.GONE);
            pwdIdNumberEditText.setVisibility(View.GONE);


            // Hide speech buttons
            firstNameSpeechButton.setVisibility(View.GONE);
            lastNameSpeechButton.setVisibility(View.GONE);
            middleNameSpeechButton.setVisibility(View.GONE);
            emailSpeechButton.setVisibility(View.GONE);
            phoneNumberSpeechButton.setVisibility(View.GONE);
            birthSpeechButton.setVisibility(View.GONE);
            addressSpeechButton.setVisibility(View.GONE);
            genderSpeechButton.setVisibility(View.GONE);
            speechStatusTextView.setVisibility(View.GONE);

            // Show TextViews
            firstNameTextView.setVisibility(View.VISIBLE);
            emailTextView.setVisibility(View.VISIBLE);
            phoneNumberTextView.setVisibility(View.VISIBLE);
            birthTextView.setVisibility(View.VISIBLE);
            genderTextView.setVisibility(View.VISIBLE);
            addressTextView.setVisibility(View.VISIBLE);
            pwdIdNumberTextView.setVisibility(View.VISIBLE);


            saveButton.setVisibility(View.GONE);
            editButton.setText("Edit");

            // Stop any ongoing speech recognition when exiting edit mode
            if (speechRecognizer != null) {
                speechRecognizer.cancel();
            }

            isEditMode = false;
            Toast.makeText(this, "Edit mode disabled.", Toast.LENGTH_SHORT).show();
            if (userId != null) {
                fetchProfileData(userId);
            }
        }
    }

    private void toggleEditMode() {
        if (isEditMode) {
            setEditModeVisibility(false);
        } else {
            setEditModeVisibility(true);
        }
    }

    private void saveChanges() {
        if (userId == null || !isEditMode) {
            Toast.makeText(this, "Error: Cannot save changes.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (firstNameEditText.getText().toString().trim().isEmpty() ||
                lastNameEditText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "First Name and Last Name are required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("firstName", firstNameEditText.getText().toString());
        userUpdates.put("lastName", lastNameEditText.getText().toString());
        userUpdates.put("middleName", middleNameEditText.getText().toString());
        userUpdates.put("mobileNumber", phoneNumberEditText.getText().toString());
        userUpdates.put("fullAddress", addressEditText.getText().toString());
        userUpdates.put("gender", genderSpinner.getSelectedItem().toString());
        // Do not update non-editable fields (PWD ID, Email, Birth Date)
        // userUpdates.put("pwdIdNumber", pwdIdNumberEditText.getText().toString());
        // userUpdates.put("email", emailEditText.getText().toString());
        // userUpdates.put("dateOfBirth", birthEditText.getText().toString());

        db.collection("registrationApplications").document(userId)
                .set(userUpdates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    setEditModeVisibility(false);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating profile", e);
                    Toast.makeText(ProfileActivity.this, "Error updating profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchProfileData(String userId) {
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty in fetchProfileData");
            return;
        }

        db.collection("registrationApplications").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String middleName = documentSnapshot.getString("middleName");
                        String lastName = documentSnapshot.getString("lastName");
                        String fullName = "";
                        if (firstName != null) fullName += firstName + " ";
                        if (middleName != null && !middleName.isEmpty()) fullName += middleName + " ";
                        if (lastName != null) fullName += lastName;
                        firstNameTextView.setText(fullName.trim());

                        emailTextView.setText(documentSnapshot.getString("email"));
                        phoneNumberTextView.setText(documentSnapshot.getString("mobileNumber"));
                        birthTextView.setText(documentSnapshot.getString("dateOfBirth"));
                        genderTextView.setText(documentSnapshot.getString("gender"));
                        addressTextView.setText(documentSnapshot.getString("fullAddress"));
                        pwdIdNumberTextView.setText(documentSnapshot.getString("pwdIdNumber"));

                        String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                        Glide.with(this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.man)
                                .error(R.drawable.man)
                                .into(profileImageView);


                    } else {
                        Toast.makeText(ProfileActivity.this, "Profile data not found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ProfileActivity.this, "Error fetching profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching document", e);
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

                        db.collection("registrationApplications").document(userId)
                                .set(userUpdates, SetOptions.merge())
                                .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Profile image uploaded and link saved!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to save image link: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }))
                    .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No image selected or user not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startVoiceCommand() {
        if (isEditMode) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command, e.g., 'save' or 'cancel'.");
                try {
                    startActivityForResult(intent, 201); // Using a new request code for voice navigation
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, "Speech recognition not supported.", Toast.LENGTH_SHORT).show();
                }
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 201);
            }
        } else {
            Toast.makeText(this, "Voice commands are only available in edit mode.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhotoFromCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == GALLERY_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                chooseImageFromGallery();
            } else {
                Toast.makeText(this, "Gallery permission is required to select images.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (currentSpeechInputEditText != null) {
                    startFieldSpecificSpeechRecognition();
                } else if (isEditMode) {
                    startVoiceCommand();
                }
            } else {
                Toast.makeText(this, "Audio recording permission is required for voice features.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                Uri imageUri = null;
                if (data != null && data.getData() != null) {
                    imageUri = data.getData();
                } else if (data != null && data.getExtras() != null) {
                    Bitmap photo = (Bitmap) data.getExtras().get("data");
                    if (photo != null) {
                        imageUri = getImageUri(photo);
                    }
                }
                if (imageUri != null) {
                    Glide.with(this).load(imageUri).into(profileImageView);
                    uploadProfileImage(imageUri);
                }
            } else if (requestCode == 201 && data != null) { // Voice navigation
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    String recognizedText = results.get(0);
                    handleVoiceCommandResult(recognizedText);
                }
            }
        }
    }

    private void handleVoiceCommandResult(String recognizedText) {
        String lowerCaseCommand = recognizedText.toLowerCase(Locale.ROOT);
        if (lowerCaseCommand.contains("save")) {
            if (isEditMode) {
                saveChanges();
            }
        } else if (lowerCaseCommand.contains("cancel")) {
            if (isEditMode) {
                toggleEditMode();
            }
        } else if (lowerCaseCommand.contains("go back") || lowerCaseCommand.contains("back")) {
            finish();
        } else {
            Toast.makeText(this, "Voice command not recognized. Try 'save', or 'cancel'.", Toast.LENGTH_SHORT).show();
        }
    }
}