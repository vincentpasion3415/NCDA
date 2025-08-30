package com.example.ncda;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.Editable;
import android.text.Html;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.View;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final String PHILIPPINES_COUNTRY_CODE = "+63";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Date formatter
    private SimpleDateFormat dateFormatter;

    // --- UI elements for Account Credentials ---
    private TextInputEditText emailEditText, passwordEditText, repeatPasswordEditText;

    // --- UI elements for Personal Information ---
    private TextInputEditText firstNameEditText, middleNameEditText, lastNameEditText, placeOfBirthEditText;
    private AutoCompleteTextView suffixDropdown, civilStatusDropdown;
    private TextInputEditText dateOfBirthEditText;
    private RadioGroup genderRadioGroup;
    private RadioButton radioMale, radioFemale, radioOther;
    private TextInputEditText nationalityEditText;

    // --- UI elements for Contact Information ---
    private TextInputEditText fullAddressEditText, mobileNumberEditText;

    // --- UI elements for Disability Information ---
    private CheckBox cbVisual, cbHearing, cbSpeech, cbPhysical, cbIntellectual, cbPsychosocial, cbLearning, cbDevelopmental, cbMultiple;
    private TextInputEditText disabilityOtherEditText, pwdIdNumberEditText;
    private AutoCompleteTextView causeOfDisabilityDropdown;

    // --- UI elements for Emergency Contact ---
    private TextInputEditText emergencyContactPersonEditText, emergencyContactNumberEditText, emergencyContactRelationshipEditText;

    // --- UI elements for Consent ---
    private CheckBox privacyConsentCheckbox, truthfulnessDeclarationCheckbox;

    // --- General UI Elements ---
    private MaterialButton buttonRegister;
    private ProgressBar progressBar;
    private TextView loginButtonTextView, speechStatusTextView;

    // --- Speech-to-Text buttons and functionality ---
    private SpeechRecognizer speechRecognizer;
    private final Handler handler = new Handler();
    private TextInputEditText currentSpeechInputEditText;
    private String currentSpeechInputPrompt;
    private ImageButton firstNameSpeechButton, middleNameSpeechButton, lastNameSpeechButton,
            emailSpeechButton, passwordSpeechButton, repeatPasswordSpeechButton,
            placeOfBirthSpeechButton, nationalitySpeechButton, fullAddressSpeechButton,
            mobileNumberSpeechButton, disabilityOtherSpeechButton, pwdIdNumberSpeechButton,
            emergencyContactPersonSpeechButton, emergencyContactNumberSpeechButton,
            emergencyContactRelationshipSpeechButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        dateFormatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

        initializeViews();

        setupDropdown(suffixDropdown, new String[]{"", "Jr.", "Sr.", "III", "IV"});
        setupDropdown(civilStatusDropdown, new String[]{"Single", "Married", "Widowed", "Separated/Annulled"});
        setupDropdown(causeOfDisabilityDropdown, new String[]{"Congenital / Born with", "Acquired (Illness)", "Acquired (Accident)", "Others"});

        dateOfBirthEditText.setOnClickListener(v -> showDatePickerDialog(dateOfBirthEditText));
        dateOfBirthEditText.setInputType(InputType.TYPE_NULL);

        setupCountryCodeAutoFill(mobileNumberEditText);
        setupCountryCodeAutoFill(emergencyContactNumberEditText);

        buttonRegister.setOnClickListener(v -> validateAndRegisterUser());
        loginButtonTextView.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        String htmlText = getString(R.string.privacy_consent_text);
        SpannableString spannableString = new SpannableString(Html.fromHtml(htmlText));
        int linkColor = ContextCompat.getColor(this, R.color.your_link_color);

        String linkText = "Privacy Policy";
        int start = spannableString.toString().indexOf(linkText);
        int end = start + linkText.length();

        if (start != -1) {
            spannableString.setSpan(new ForegroundColorSpan(linkColor), start, end, 0);
        }

        privacyConsentCheckbox.setText(spannableString);
        privacyConsentCheckbox.setMovementMethod(LinkMovementMethod.getInstance());

        // --- BLUR ONLY THE BACKGROUND IMAGE ---
        ImageView backgroundImage = findViewById(R.id.backgroundImage);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            backgroundImage.setRenderEffect(
                    RenderEffect.createBlurEffect(
                            20f, // blur X
                            20f, // blur Y
                            Shader.TileMode.CLAMP
                    )
            );
        }
        // --------------------------------------

        // Speech recognition setup
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(speechRecognitionListener);
        setupSpeechButtons();
    }




    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("email", Objects.requireNonNull(emailEditText.getText()).toString());
        outState.putString("password", Objects.requireNonNull(passwordEditText.getText()).toString());
        outState.putString("repeatPassword", Objects.requireNonNull(repeatPasswordEditText.getText()).toString());
        outState.putString("firstName", Objects.requireNonNull(firstNameEditText.getText()).toString());
        outState.putString("middleName", Objects.requireNonNull(middleNameEditText.getText()).toString());
        outState.putString("lastName", Objects.requireNonNull(lastNameEditText.getText()).toString());
        outState.putString("suffix", suffixDropdown.getText().toString());
        outState.putString("dateOfBirth", Objects.requireNonNull(dateOfBirthEditText.getText()).toString());
        outState.putString("placeOfBirth", Objects.requireNonNull(placeOfBirthEditText.getText()).toString());
        outState.putInt("gender", genderRadioGroup.getCheckedRadioButtonId());
        outState.putString("civilStatus", civilStatusDropdown.getText().toString());
        outState.putString("nationality", Objects.requireNonNull(nationalityEditText.getText()).toString());
        outState.putString("fullAddress", Objects.requireNonNull(fullAddressEditText.getText()).toString());
        outState.putString("mobileNumber", Objects.requireNonNull(mobileNumberEditText.getText()).toString());
        outState.putBoolean("cb_visual", cbVisual.isChecked());
        outState.putBoolean("cb_hearing", cbHearing.isChecked());
        outState.putBoolean("cb_speech", cbSpeech.isChecked());
        outState.putBoolean("cb_physical", cbPhysical.isChecked());
        outState.putBoolean("cb_intellectual", cbIntellectual.isChecked());
        outState.putBoolean("cb_psychosocial", cbPsychosocial.isChecked());
        outState.putBoolean("cb_learning", cbLearning.isChecked());
        outState.putBoolean("cb_developmental", cbDevelopmental.isChecked());
        outState.putBoolean("cb_multiple", cbMultiple.isChecked());
        outState.putString("disabilityOther", Objects.requireNonNull(disabilityOtherEditText.getText()).toString());
        outState.putString("causeOfDisability", causeOfDisabilityDropdown.getText().toString());
        outState.putString("pwdIdNumber", Objects.requireNonNull(pwdIdNumberEditText.getText()).toString());
        outState.putString("emergencyContactPerson", Objects.requireNonNull(emergencyContactPersonEditText.getText()).toString());
        outState.putString("emergencyContactNumber", Objects.requireNonNull(emergencyContactNumberEditText.getText()).toString());
        outState.putString("emergencyContactRelationship", Objects.requireNonNull(emergencyContactRelationshipEditText.getText()).toString());
        outState.putBoolean("privacyConsent", privacyConsentCheckbox.isChecked());
        outState.putBoolean("truthfulnessDeclaration", truthfulnessDeclarationCheckbox.isChecked());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        emailEditText.setText(savedInstanceState.getString("email"));
        passwordEditText.setText(savedInstanceState.getString("password"));
        repeatPasswordEditText.setText(savedInstanceState.getString("repeatPassword"));
        firstNameEditText.setText(savedInstanceState.getString("firstName"));
        middleNameEditText.setText(savedInstanceState.getString("middleName"));
        lastNameEditText.setText(savedInstanceState.getString("lastName"));
        suffixDropdown.setText(savedInstanceState.getString("suffix"), false);
        dateOfBirthEditText.setText(savedInstanceState.getString("dateOfBirth"));
        placeOfBirthEditText.setText(savedInstanceState.getString("placeOfBirth"));
        genderRadioGroup.check(savedInstanceState.getInt("gender"));
        civilStatusDropdown.setText(savedInstanceState.getString("civilStatus"), false);
        nationalityEditText.setText(savedInstanceState.getString("nationality"));
        fullAddressEditText.setText(savedInstanceState.getString("fullAddress"));
        mobileNumberEditText.setText(savedInstanceState.getString("mobileNumber"));

        cbVisual.setChecked(savedInstanceState.getBoolean("cb_visual"));
        cbHearing.setChecked(savedInstanceState.getBoolean("cb_hearing"));
        cbSpeech.setChecked(savedInstanceState.getBoolean("cb_speech"));
        cbPhysical.setChecked(savedInstanceState.getBoolean("cb_physical"));
        cbIntellectual.setChecked(savedInstanceState.getBoolean("cb_intellectual"));
        cbPsychosocial.setChecked(savedInstanceState.getBoolean("cb_psychosocial"));
        cbLearning.setChecked(savedInstanceState.getBoolean("cb_learning"));
        cbDevelopmental.setChecked(savedInstanceState.getBoolean("cb_developmental"));
        cbMultiple.setChecked(savedInstanceState.getBoolean("cb_multiple"));
        disabilityOtherEditText.setText(savedInstanceState.getString("disabilityOther"));
        causeOfDisabilityDropdown.setText(savedInstanceState.getString("causeOfDisability"), false);
        pwdIdNumberEditText.setText(savedInstanceState.getString("pwdIdNumber"));

        emergencyContactPersonEditText.setText(savedInstanceState.getString("emergencyContactPerson"));
        emergencyContactNumberEditText.setText(savedInstanceState.getString("emergencyContactNumber"));
        emergencyContactRelationshipEditText.setText(savedInstanceState.getString("emergencyContactRelationship"));

        privacyConsentCheckbox.setChecked(savedInstanceState.getBoolean("privacyConsent"));
        truthfulnessDeclarationCheckbox.setChecked(savedInstanceState.getBoolean("truthfulnessDeclaration"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (progressBar.getVisibility() == View.VISIBLE) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        repeatPasswordEditText = findViewById(R.id.repeatPassword);
        firstNameEditText = findViewById(R.id.firstName);
        middleNameEditText = findViewById(R.id.middleName);
        lastNameEditText = findViewById(R.id.lastName);
        suffixDropdown = findViewById(R.id.suffix);
        dateOfBirthEditText = findViewById(R.id.dateOfBirth);
        placeOfBirthEditText = findViewById(R.id.placeOfBirth);
        genderRadioGroup = findViewById(R.id.genderRadioGroup);
        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);
        radioOther = findViewById(R.id.radioOther);
        civilStatusDropdown = findViewById(R.id.civilStatus);
        nationalityEditText = findViewById(R.id.nationality);
        fullAddressEditText = findViewById(R.id.fullAddress);
        mobileNumberEditText = findViewById(R.id.mobileNumber);
        cbVisual = findViewById(R.id.cb_visual);
        cbHearing = findViewById(R.id.cb_hearing);
        cbSpeech = findViewById(R.id.cb_speech);
        cbPhysical = findViewById(R.id.cb_physical);
        cbIntellectual = findViewById(R.id.cb_intellectual);
        cbPsychosocial = findViewById(R.id.cb_psychosocial);
        cbLearning = findViewById(R.id.cb_learning);
        cbDevelopmental = findViewById(R.id.cb_developmental);
        cbMultiple = findViewById(R.id.cb_multiple);
        disabilityOtherEditText = findViewById(R.id.disabilityOther);
        causeOfDisabilityDropdown = findViewById(R.id.causeOfDisability);
        pwdIdNumberEditText = findViewById(R.id.pwdIdNumber);
        emergencyContactPersonEditText = findViewById(R.id.emergencyContactPerson);
        emergencyContactNumberEditText = findViewById(R.id.emergencyContactNumber);
        emergencyContactRelationshipEditText = findViewById(R.id.emergencyContactRelationship);
        privacyConsentCheckbox = findViewById(R.id.privacyConsentCheckbox);
        truthfulnessDeclarationCheckbox = findViewById(R.id.truthfulnessDeclarationCheckbox);
        buttonRegister = findViewById(R.id.buttonRegister);
        progressBar = findViewById(R.id.progressBarRegister);
        loginButtonTextView = findViewById(R.id.loginButton);
        progressBar.setVisibility(View.GONE);

        // Initialize Speech-to-Text buttons and status TextView
        speechStatusTextView = findViewById(R.id.speech_status_text_view);
        firstNameSpeechButton = findViewById(R.id.firstNameSpeechButton);
        middleNameSpeechButton = findViewById(R.id.middleNameSpeechButton);
        lastNameSpeechButton = findViewById(R.id.lastNameSpeechButton);
        emailSpeechButton = findViewById(R.id.emailSpeechButton);
        passwordSpeechButton = findViewById(R.id.passwordSpeechButton);
        repeatPasswordSpeechButton = findViewById(R.id.repeatPasswordSpeechButton);
        placeOfBirthSpeechButton = findViewById(R.id.placeOfBirthSpeechButton);
        nationalitySpeechButton = findViewById(R.id.nationalitySpeechButton);
        fullAddressSpeechButton = findViewById(R.id.fullAddressSpeechButton);
        mobileNumberSpeechButton = findViewById(R.id.mobileNumberSpeechButton);
        disabilityOtherSpeechButton = findViewById(R.id.disabilityOtherSpeechButton);
        pwdIdNumberSpeechButton = findViewById(R.id.pwdIdNumberSpeechButton);
        emergencyContactPersonSpeechButton = findViewById(R.id.emergencyContactPersonSpeechButton);
        emergencyContactNumberSpeechButton = findViewById(R.id.emergencyContactNumberSpeechButton);
        emergencyContactRelationshipSpeechButton = findViewById(R.id.emergencyContactRelationshipSpeechButton);

        speechStatusTextView.setVisibility(View.GONE);
    }

    private void setupSpeechButtons() {
        setupSpeechButton(firstNameSpeechButton, firstNameEditText, "first name");
        setupSpeechButton(middleNameSpeechButton, middleNameEditText, "middle name");
        setupSpeechButton(lastNameSpeechButton, lastNameEditText, "last name");
        setupSpeechButton(emailSpeechButton, emailEditText, "email address");
        setupSpeechButton(passwordSpeechButton, passwordEditText, "password");
        setupSpeechButton(repeatPasswordSpeechButton, repeatPasswordEditText, "password again");
        setupSpeechButton(placeOfBirthSpeechButton, placeOfBirthEditText, "place of birth");
        setupSpeechButton(nationalitySpeechButton, nationalityEditText, "nationality");
        setupSpeechButton(fullAddressSpeechButton, fullAddressEditText, "your full address");
        setupSpeechButton(mobileNumberSpeechButton, mobileNumberEditText, "mobile number");
        setupSpeechButton(disabilityOtherSpeechButton, disabilityOtherEditText, "other disability");
        setupSpeechButton(pwdIdNumberSpeechButton, pwdIdNumberEditText, "PWD ID number");
        setupSpeechButton(emergencyContactPersonSpeechButton, emergencyContactPersonEditText, "emergency contact person's name");
        setupSpeechButton(emergencyContactNumberSpeechButton, emergencyContactNumberEditText, "emergency contact number");
        setupSpeechButton(emergencyContactRelationshipSpeechButton, emergencyContactRelationshipEditText, "relationship to emergency contact");
    }

    private void setupSpeechButton(ImageButton button, TextInputEditText editText, String prompt) {
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
                    Objects.requireNonNull(currentSpeechInputEditText.getText()).append(" ");
                    currentSpeechInputEditText.setSelection(currentSpeechInputEditText.getText().length());
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
            speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
            speechIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L);
            speechRecognizer.startListening(speechIntent);
        } else {
            Toast.makeText(this, "Speech recognition service is not available.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (currentSpeechInputEditText != null) {
                    startFieldSpecificSpeechRecognition();
                }
            } else {
                Toast.makeText(this, "Audio recording permission is required for voice input.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupDropdown(AutoCompleteTextView dropdown, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
        dropdown.setAdapter(adapter);
    }

    private void showDatePickerDialog(final TextInputEditText dateField) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Get the custom theme ID you defined in styles.xml
        int themeResId = R.style.MyBlueDatePickerTheme;

        // Use the constructor that accepts a theme ID
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                themeResId,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDayOfMonth);
                    dateField.setText(dateFormatter.format(selectedDate.getTime()));
                },
                year,
                month,
                day
        );

        // Add the explicit buttons to ensure they exist
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "CANCEL", (dialog, which) -> dialog.dismiss());
        datePickerDialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", datePickerDialog);

        // Apply the workaround to ensure the button colors are set manually
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

    private void setupCountryCodeAutoFill(final TextInputEditText editText) {
        editText.setText(PHILIPPINES_COUNTRY_CODE);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().startsWith(PHILIPPINES_COUNTRY_CODE)) {
                    editText.setText(PHILIPPINES_COUNTRY_CODE);
                    Objects.requireNonNull(editText.getText()).append("");
                    editText.setSelection(PHILIPPINES_COUNTRY_CODE.length());
                } else if (s.length() == PHILIPPINES_COUNTRY_CODE.length() && !s.toString().equals(PHILIPPINES_COUNTRY_CODE)) {
                    editText.setText(PHILIPPINES_COUNTRY_CODE);
                    Objects.requireNonNull(editText.getText()).append("");
                    editText.setSelection(PHILIPPINES_COUNTRY_CODE.length());
                }
            }
        });

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && editText.getText().toString().equals(PHILIPPINES_COUNTRY_CODE)) {
                editText.setSelection(PHILIPPINES_COUNTRY_CODE.length());
            }
        });
    }

    private void validateAndRegisterUser() {
        String userEmail = Objects.requireNonNull(emailEditText.getText()).toString().trim();
        String userPassword = Objects.requireNonNull(passwordEditText.getText()).toString();
        String userRepeatPassword = Objects.requireNonNull(repeatPasswordEditText.getText()).toString();
        String firstName = Objects.requireNonNull(firstNameEditText.getText()).toString().trim();
        String middleName = Objects.requireNonNull(middleNameEditText.getText()).toString().trim();
        String lastName = Objects.requireNonNull(lastNameEditText.getText()).toString().trim();
        String suffix = suffixDropdown.getText().toString().trim();
        String dateOfBirth = Objects.requireNonNull(dateOfBirthEditText.getText()).toString().trim();
        String placeOfBirth = Objects.requireNonNull(placeOfBirthEditText.getText()).toString().trim();
        final String gender;
        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedGenderId != -1) {
            RadioButton selectedGenderButton = findViewById(selectedGenderId);
            gender = selectedGenderButton.getText().toString();
        } else {
            gender = "";
        }
        String civilStatus = civilStatusDropdown.getText().toString().trim();
        String nationality = Objects.requireNonNull(nationalityEditText.getText()).toString().trim();
        String fullAddress = Objects.requireNonNull(fullAddressEditText.getText()).toString().trim();
        String mobileNumber = Objects.requireNonNull(mobileNumberEditText.getText()).toString().trim();
        List<String> disabilityTypes = new ArrayList<>();
        if (cbVisual.isChecked()) disabilityTypes.add("Visual Impairment");
        if (cbHearing.isChecked()) disabilityTypes.add("Hearing Impairment");
        if (cbSpeech.isChecked()) disabilityTypes.add("Speech Impairment");
        if (cbPhysical.isChecked()) disabilityTypes.add("Physical Disability");
        if (cbIntellectual.isChecked()) disabilityTypes.add("Intellectual Disability");
        if (cbPsychosocial.isChecked()) disabilityTypes.add("Psychosocial Disability");
        if (cbLearning.isChecked()) disabilityTypes.add("Learning Disability");
        if (cbDevelopmental.isChecked()) disabilityTypes.add("Developmental Disability");
        if (cbMultiple.isChecked()) disabilityTypes.add("Multiple Disabilities");
        String disabilityOther = Objects.requireNonNull(disabilityOtherEditText.getText()).toString().trim();
        String causeOfDisability = causeOfDisabilityDropdown.getText().toString().trim();
        String pwdIdNumber = Objects.requireNonNull(pwdIdNumberEditText.getText()).toString().trim();
        String emergencyContactPerson = Objects.requireNonNull(emergencyContactPersonEditText.getText()).toString().trim();
        String emergencyContactNumber = Objects.requireNonNull(emergencyContactNumberEditText.getText()).toString().trim();
        String emergencyContactRelationship = Objects.requireNonNull(emergencyContactRelationshipEditText.getText()).toString().trim();
        boolean privacyConsent = privacyConsentCheckbox.isChecked();
        boolean truthfulnessDeclaration = truthfulnessDeclarationCheckbox.isChecked();
        if (userEmail.isEmpty() || userPassword.isEmpty() || userRepeatPassword.isEmpty()) {
            Toast.makeText(this, "Email and password fields are required.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
            emailEditText.setError("Please enter a valid email address.");
            emailEditText.requestFocus();
            return;
        }
        if (userPassword.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters long.");
            passwordEditText.requestFocus();
            return;
        }
        if (!userPassword.equals(userRepeatPassword)) {
            repeatPasswordEditText.setError("Passwords do not match.");
            repeatPasswordEditText.requestFocus();
            return;
        }
        if (firstName.isEmpty()) { firstNameEditText.setError("First name is required."); firstNameEditText.requestFocus(); return; }
        if (lastName.isEmpty()) { lastNameEditText.setError("Last name is required."); lastNameEditText.requestFocus(); return; }
        if (dateOfBirth.isEmpty()) { dateOfBirthEditText.setError("Date of birth is required."); dateOfBirthEditText.requestFocus(); return; }
        if (placeOfBirth.isEmpty()) { placeOfBirthEditText.setError("Place of birth is required."); placeOfBirthEditText.requestFocus(); return; }
        if (gender.isEmpty()) { Toast.makeText(this, "Please select your gender.", Toast.LENGTH_SHORT).show(); return; }
        if (civilStatus.isEmpty()) { civilStatusDropdown.setError("Civil status is required."); civilStatusDropdown.requestFocus(); return; }
        if (nationality.isEmpty()) { nationalityEditText.setError("Nationality is required."); nationalityEditText.requestFocus(); return; }
        if (fullAddress.isEmpty()) { fullAddressEditText.setError("Full address is required."); fullAddressEditText.requestFocus(); return; }
        if (mobileNumber.isEmpty() || mobileNumber.equals(PHILIPPINES_COUNTRY_CODE)) {
            mobileNumberEditText.setError("Mobile number is required."); mobileNumberEditText.requestFocus(); return;
        }
        if (!mobileNumber.startsWith(PHILIPPINES_COUNTRY_CODE) || mobileNumber.length() != 13) {
            mobileNumberEditText.setError("Mobile number must be 11 digits (e.g., +63917xxxxxxx).");
            mobileNumberEditText.requestFocus();
            return;
        }
        if (disabilityTypes.isEmpty() && disabilityOther.isEmpty()) {
            Toast.makeText(this, "Please select at least one type of disability or specify 'Other'.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (causeOfDisability.isEmpty()) { causeOfDisabilityDropdown.setError("Cause of disability is required."); causeOfDisabilityDropdown.requestFocus(); return; }
        if (pwdIdNumber.isEmpty()) {
            pwdIdNumberEditText.setError("PWD ID Number is required.");
            pwdIdNumberEditText.requestFocus();
            return;
        }
        if (emergencyContactPerson.isEmpty()) { emergencyContactPersonEditText.setError("Emergency contact person is required."); emergencyContactPersonEditText.requestFocus(); return; }
        if (emergencyContactNumber.isEmpty() || emergencyContactNumber.equals(PHILIPPINES_COUNTRY_CODE)) {
            emergencyContactNumberEditText.setError("Emergency contact number is required."); emergencyContactNumberEditText.requestFocus(); return;
        }
        if (!emergencyContactNumber.startsWith(PHILIPPINES_COUNTRY_CODE) || emergencyContactNumber.length() != 13) {
            emergencyContactNumberEditText.setError("Emergency contact number must be 11 digits (e.g., +63917xxxxxxx).");
            emergencyContactNumberEditText.requestFocus();
            return;
        }
        if (emergencyContactRelationship.isEmpty()) { emergencyContactRelationshipEditText.setError("Relationship to emergency contact is required."); emergencyContactRelationshipEditText.requestFocus(); return; }
        if (!privacyConsent) {
            Toast.makeText(this, "Please agree to the Privacy Policy consent.", Toast.LENGTH_SHORT).show();
            privacyConsentCheckbox.requestFocus();
            return;
        }
        if (!truthfulnessDeclaration) {
            Toast.makeText(this, "Please declare that the information is true and correct.", Toast.LENGTH_SHORT).show();
            truthfulnessDeclarationCheckbox.requestFocus();
            return;
        }
        progressBar.setVisibility(View.VISIBLE);
        disableAllInputFieldsAndButtons();

        mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "createUserWithEmail:success UID: " + user.getUid());
                            Toast.makeText(RegisterActivity.this, "Registration successful! Saving additional data...", Toast.LENGTH_SHORT).show();
                            saveUserDataToFirestore(user,
                                    firstName, middleName, lastName, suffix, dateOfBirth, placeOfBirth, gender, civilStatus, nationality,
                                    fullAddress, mobileNumber,
                                    disabilityTypes, disabilityOther, causeOfDisability, pwdIdNumber,
                                    emergencyContactPerson, emergencyContactNumber, emergencyContactRelationship,
                                    privacyConsent, truthfulnessDeclaration);
                        } else {
                            Log.e(TAG, "createUserWithEmail:success but user is null.");
                            Toast.makeText(RegisterActivity.this, "Registration successful, but failed to get user data.", Toast.LENGTH_LONG).show();
                            enableAllInputFieldsAndButtons();
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        enableAllInputFieldsAndButtons();
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            emailEditText.setError("This email address is already registered. Please login or use a different email.");
                            emailEditText.requestFocus();
                            Toast.makeText(RegisterActivity.this, "Email already registered.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserDataToFirestore(FirebaseUser user,
                                         String firstName, String middleName, String lastName, String suffix, String dateOfBirth, String placeOfBirth, String gender, String civilStatus, String nationality,
                                         String fullAddress, String mobileNumber,
                                         List<String> disabilityTypes, String disabilityOther, String causeOfDisability, String pwdIdNumber,
                                         String emergencyContactPerson, String emergencyContactNumber, String emergencyContactRelationship,
                                         boolean privacyConsent, boolean truthfulnessDeclaration) {

        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", user.getUid());
        userData.put("email", user.getEmail());
        userData.put("firstName", firstName);
        userData.put("middleName", middleName);
        userData.put("lastName", lastName);
        userData.put("suffix", suffix);
        userData.put("dateOfBirth", dateOfBirth);
        userData.put("placeOfBirth", placeOfBirth);
        userData.put("gender", gender);
        userData.put("civilStatus", civilStatus);
        userData.put("nationality", nationality);
        userData.put("fullAddress", fullAddress);
        userData.put("mobileNumber", mobileNumber);
        userData.put("disabilityTypes", disabilityTypes);
        userData.put("disabilityOther", disabilityOther);
        userData.put("causeOfDisability", causeOfDisability);
        userData.put("pwdIdNumber", pwdIdNumber);
        userData.put("emergencyContactPerson", emergencyContactPerson);
        userData.put("emergencyContactNumber", emergencyContactNumber);
        userData.put("emergencyContactRelationship", emergencyContactRelationship);
        userData.put("privacyConsent", privacyConsent);
        userData.put("truthfulnessDeclaration", truthfulnessDeclaration);
        userData.put("applicationStatus", "Pending Review");
        userData.put("registrationDate", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("registrationApplications").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "PWD Application data saved to Firestore for UID: " + user.getUid());
                    Toast.makeText(RegisterActivity.this, "Registration complete! Your application is under review.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, PendingApprovalActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving PWD application data to Firestore: " + e.getMessage(), e);
                    Toast.makeText(RegisterActivity.this, "Failed to save application data. Please try again.", Toast.LENGTH_LONG).show();
                    user.delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Log.d(TAG, "Firebase user deleted due to Firestore save failure.");
                        } else {
                            Log.e(TAG, "Failed to delete Firebase user: " + deleteTask.getException());
                        }
                    });
                    enableAllInputFieldsAndButtons();
                });
    }

    private void disableAllInputFieldsAndButtons() {
        emailEditText.setEnabled(false);
        passwordEditText.setEnabled(false);
        repeatPasswordEditText.setEnabled(false);
        buttonRegister.setEnabled(false);
        loginButtonTextView.setEnabled(false);
        firstNameEditText.setEnabled(false);
        middleNameEditText.setEnabled(false);
        lastNameEditText.setEnabled(false);
        suffixDropdown.setEnabled(false);
        dateOfBirthEditText.setEnabled(false);
        placeOfBirthEditText.setEnabled(false);
        for (int i = 0; i < genderRadioGroup.getChildCount(); i++) {
            genderRadioGroup.getChildAt(i).setEnabled(false);
        }
        civilStatusDropdown.setEnabled(false);
        nationalityEditText.setEnabled(false);
        fullAddressEditText.setEnabled(false);
        mobileNumberEditText.setEnabled(false);
        cbVisual.setEnabled(false);
        cbHearing.setEnabled(false);
        cbSpeech.setEnabled(false);
        cbPhysical.setEnabled(false);
        cbIntellectual.setEnabled(false);
        cbPsychosocial.setEnabled(false);
        cbLearning.setEnabled(false);
        cbDevelopmental.setEnabled(false);
        cbMultiple.setEnabled(false);
        disabilityOtherEditText.setEnabled(false);
        causeOfDisabilityDropdown.setEnabled(false);
        pwdIdNumberEditText.setEnabled(false);
        emergencyContactPersonEditText.setEnabled(false);
        emergencyContactNumberEditText.setEnabled(false);
        emergencyContactRelationshipEditText.setEnabled(false);
        privacyConsentCheckbox.setEnabled(false);
        truthfulnessDeclarationCheckbox.setEnabled(false);
        setSpeechButtonsEnabled(false);
    }

    private void enableAllInputFieldsAndButtons() {
        emailEditText.setEnabled(true);
        passwordEditText.setEnabled(true);
        repeatPasswordEditText.setEnabled(true);
        buttonRegister.setEnabled(true);
        loginButtonTextView.setEnabled(true);
        firstNameEditText.setEnabled(true);
        middleNameEditText.setEnabled(true);
        lastNameEditText.setEnabled(true);
        suffixDropdown.setEnabled(true);
        dateOfBirthEditText.setEnabled(true);
        placeOfBirthEditText.setEnabled(true);
        for (int i = 0; i < genderRadioGroup.getChildCount(); i++) {
            genderRadioGroup.getChildAt(i).setEnabled(true);
        }
        civilStatusDropdown.setEnabled(true);
        nationalityEditText.setEnabled(true);
        fullAddressEditText.setEnabled(true);
        mobileNumberEditText.setEnabled(true);
        cbVisual.setEnabled(true);
        cbHearing.setEnabled(true);
        cbSpeech.setEnabled(true);
        cbPhysical.setEnabled(true);
        cbIntellectual.setEnabled(true);
        cbPsychosocial.setEnabled(true);
        cbLearning.setEnabled(true);
        cbDevelopmental.setEnabled(true);
        cbMultiple.setEnabled(true);
        disabilityOtherEditText.setEnabled(true);
        causeOfDisabilityDropdown.setEnabled(true);
        pwdIdNumberEditText.setEnabled(true);
        emergencyContactPersonEditText.setEnabled(true);
        emergencyContactNumberEditText.setEnabled(true);
        emergencyContactRelationshipEditText.setEnabled(true);
        privacyConsentCheckbox.setEnabled(true);
        truthfulnessDeclarationCheckbox.setEnabled(true);
        setSpeechButtonsEnabled(true);
    }

    private void setSpeechButtonsEnabled(boolean enabled) {
        ImageButton[] buttons = {
                firstNameSpeechButton, middleNameSpeechButton, lastNameSpeechButton,
                emailSpeechButton, passwordSpeechButton, repeatPasswordSpeechButton,
                placeOfBirthSpeechButton, nationalitySpeechButton, fullAddressSpeechButton,
                mobileNumberSpeechButton, disabilityOtherSpeechButton, pwdIdNumberSpeechButton,
                emergencyContactPersonSpeechButton, emergencyContactNumberSpeechButton,
                emergencyContactRelationshipSpeechButton
        };
        for (ImageButton button : buttons) {
            if (button != null) {
                button.setEnabled(enabled);
                button.setVisibility(enabled ? View.VISIBLE : View.GONE);
            }
        }
    }

}