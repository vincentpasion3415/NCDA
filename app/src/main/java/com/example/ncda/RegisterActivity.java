package com.example.ncda; // IMPORTANT: Ensure this matches your package name

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher; // Import TextWatcher
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions; // For merging data

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final String PHILIPPINES_COUNTRY_CODE = "+63"; // Define the country code

    // --- UI elements for Account Credentials ---
    private TextInputEditText emailEditText, passwordEditText, repeatPasswordEditText;

    // --- UI elements for Personal Information ---
    private TextInputEditText firstNameEditText, middleNameEditText, lastNameEditText, placeOfBirthEditText;
    private AutoCompleteTextView suffixDropdown, civilStatusDropdown;
    private TextInputEditText dateOfBirthEditText; // For Date Picker
    private RadioGroup genderRadioGroup;
    private RadioButton radioMale, radioFemale, radioOther;
    private TextInputEditText nationalityEditText; // Still keeping nationality as it's typically important

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
    private TextView loginButtonTextView;

    // Firebase instances
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Date formatter
    private SimpleDateFormat dateFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        dateFormatter = new SimpleDateFormat("MM/dd/yyyy", Locale.US); // For DatePicker

        // --- Initialize UI elements - Account Credentials ---
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        repeatPasswordEditText = findViewById(R.id.repeatPassword);

        // --- Initialize UI elements - Personal Information ---
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

        // --- Initialize UI elements - Contact Information ---
        fullAddressEditText = findViewById(R.id.fullAddress); // New combined address field
        mobileNumberEditText = findViewById(R.id.mobileNumber);

        // --- Initialize UI elements - Disability Information ---
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

        // --- Initialize UI elements - Emergency Contact ---
        emergencyContactPersonEditText = findViewById(R.id.emergencyContactPerson);
        emergencyContactNumberEditText = findViewById(R.id.emergencyContactNumber);
        emergencyContactRelationshipEditText = findViewById(R.id.emergencyContactRelationship);

        // --- Initialize UI elements - Consent ---
        privacyConsentCheckbox = findViewById(R.id.privacyConsentCheckbox);
        truthfulnessDeclarationCheckbox = findViewById(R.id.truthfulnessDeclarationCheckbox);

        // --- General UI Elements ---
        buttonRegister = findViewById(R.id.buttonRegister); // The main register button
        progressBar = findViewById(R.id.progressBarRegister);
        loginButtonTextView = findViewById(R.id.loginButton);

        // Initially hide progress bar
        progressBar.setVisibility(View.GONE);

        // --- Setup Dropdowns (AutoCompleteTextView) ---
        setupDropdown(suffixDropdown, new String[]{"", "Jr.", "Sr.", "III", "IV"}); // Empty string for no selection
        setupDropdown(civilStatusDropdown, new String[]{"Single", "Married", "Widowed", "Separated/Annulled"});
        setupDropdown(causeOfDisabilityDropdown, new String[]{"Congenital / Born with", "Acquired (Illness)", "Acquired (Accident)", "Others"});

        // --- Setup Date Pickers ---
        dateOfBirthEditText.setOnClickListener(v -> showDatePickerDialog(dateOfBirthEditText));
        // Prevent keyboard from showing for date inputs
        dateOfBirthEditText.setInputType(InputType.TYPE_NULL);

        // --- Auto-fill +63 for Mobile Numbers ---
        setupCountryCodeAutoFill(mobileNumberEditText);
        setupCountryCodeAutoFill(emergencyContactNumberEditText);

        // --- Button Listeners ---
        buttonRegister.setOnClickListener(v -> validateAndRegisterUser());
        loginButtonTextView.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // Helper method to set up AutoCompleteTextView
    private void setupDropdown(AutoCompleteTextView dropdown, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, items);
        dropdown.setAdapter(adapter);
    }

    // Helper method to show DatePickerDialog
    private void showDatePickerDialog(final TextInputEditText dateField) {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDayOfMonth);
                    dateField.setText(dateFormatter.format(selectedDate.getTime()));
                }, year, month, day);

        datePickerDialog.show();
    }

    // NEW HELPER METHOD: Automatically fills +63 prefix
    private void setupCountryCodeAutoFill(final TextInputEditText editText) {
        editText.setText(PHILIPPINES_COUNTRY_CODE); // Set initial text

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed for this functionality
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not needed for this functionality
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().startsWith(PHILIPPINES_COUNTRY_CODE)) {
                    // If the user somehow deletes '+63' or types something else first
                    // Re-insert '+63' at the beginning and set cursor
                    editText.setText(PHILIPPINES_COUNTRY_CODE);
                    editText.setSelection(PHILIPPINES_COUNTRY_CODE.length());
                } else if (s.length() == PHILIPPINES_COUNTRY_CODE.length() && !s.toString().equals(PHILIPPINES_COUNTRY_CODE)) {
                    // If they type exactly '+63' but it's not actually the prefix (e.g. they type something else and delete back)
                    editText.setText(PHILIPPINES_COUNTRY_CODE);
                    editText.setSelection(PHILIPPINES_COUNTRY_CODE.length());
                }
            }
        });

        // Set cursor position to after +63 when the field gets focus
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && editText.getText().toString().equals(PHILIPPINES_COUNTRY_CODE)) {
                editText.setSelection(PHILIPPINES_COUNTRY_CODE.length());
            }
        });
    }


    private void validateAndRegisterUser() {
        // --- Account Credentials ---
        String userEmail = emailEditText.getText().toString().trim();
        String userPassword = passwordEditText.getText().toString();
        String userRepeatPassword = repeatPasswordEditText.getText().toString();

        // --- Personal Information ---
        String firstName = firstNameEditText.getText().toString().trim();
        String middleName = middleNameEditText.getText().toString().trim();
        String lastName = lastNameEditText.getText().toString().trim();
        String suffix = suffixDropdown.getText().toString().trim();
        String dateOfBirth = dateOfBirthEditText.getText().toString().trim();
        String placeOfBirth = placeOfBirthEditText.getText().toString().trim();
        final String gender; // Declare as final without immediate initialization
        int selectedGenderId = genderRadioGroup.getCheckedRadioButtonId();
        if (selectedGenderId != -1) {
            RadioButton selectedGenderButton = findViewById(selectedGenderId);
            gender = selectedGenderButton.getText().toString(); // Assigned here
        } else {
            gender = ""; // Assigned here (guarantees initialization in all paths)
        }
        String civilStatus = civilStatusDropdown.getText().toString().trim();
        String nationality = nationalityEditText.getText().toString().trim();


        String fullAddress = fullAddressEditText.getText().toString().trim(); // Combined address
        String mobileNumber = mobileNumberEditText.getText().toString().trim();


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
        String disabilityOther = disabilityOtherEditText.getText().toString().trim();
        String causeOfDisability = causeOfDisabilityDropdown.getText().toString().trim();
        String pwdIdNumber = pwdIdNumberEditText.getText().toString().trim();

        // --- Emergency Contact ---
        String emergencyContactPerson = emergencyContactPersonEditText.getText().toString().trim();
        String emergencyContactNumber = emergencyContactNumberEditText.getText().toString().trim();
        String emergencyContactRelationship = emergencyContactRelationshipEditText.getText().toString().trim();

        // --- Consent ---
        boolean privacyConsent = privacyConsentCheckbox.isChecked();
        boolean truthfulnessDeclaration = truthfulnessDeclarationCheckbox.isChecked();


        // --- Input Validation ---
        // Basic Account Validation
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

        // Personal Information Validation
        if (firstName.isEmpty()) { firstNameEditText.setError("First name is required."); firstNameEditText.requestFocus(); return; }
        if (lastName.isEmpty()) { lastNameEditText.setError("Last name is required."); lastNameEditText.requestFocus(); return; }
        if (dateOfBirth.isEmpty()) { dateOfBirthEditText.setError("Date of birth is required."); dateOfBirthEditText.requestFocus(); return; }
        if (placeOfBirth.isEmpty()) { placeOfBirthEditText.setError("Place of birth is required."); placeOfBirthEditText.requestFocus(); return; }
        if (gender.isEmpty()) { Toast.makeText(this, "Please select your gender.", Toast.LENGTH_SHORT).show(); return; }
        if (civilStatus.isEmpty()) { civilStatusDropdown.setError("Civil status is required."); civilStatusDropdown.requestFocus(); return; }
        if (nationality.isEmpty()) { nationalityEditText.setError("Nationality is required."); nationalityEditText.requestFocus(); return; }

        // Contact Information Validation
        if (fullAddress.isEmpty()) { fullAddressEditText.setError("Full address is required."); fullAddressEditText.requestFocus(); return; }
        if (mobileNumber.isEmpty() || mobileNumber.equals(PHILIPPINES_COUNTRY_CODE)) { // Check if only +63 is present
            mobileNumberEditText.setError("Mobile number is required."); mobileNumberEditText.requestFocus(); return;
        }
        // Simplified check: only allows numbers after +63 and ensures length
        // You can use a more robust regex for phone numbers if needed
        if (!mobileNumber.startsWith(PHILIPPINES_COUNTRY_CODE) || mobileNumber.length() != 13) { // +63 and 10 digits = 13 chars
            mobileNumberEditText.setError("Mobile number must be 11 digits (e.g., +63917xxxxxxx).");
            mobileNumberEditText.requestFocus();
            return;
        }


        // Disability Information Validation
        if (disabilityTypes.isEmpty() && disabilityOther.isEmpty()) {
            Toast.makeText(this, "Please select at least one type of disability or specify 'Other'.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (causeOfDisability.isEmpty()) { causeOfDisabilityDropdown.setError("Cause of disability is required."); causeOfDisabilityDropdown.requestFocus(); return; }
        // --- NEW PWD ID NUMBER VALIDATION ---
        if (pwdIdNumber.isEmpty()) {
            pwdIdNumberEditText.setError("PWD ID Number is required.");
            pwdIdNumberEditText.requestFocus();
            return;
        }


        // Emergency Contact Validation
        if (emergencyContactPerson.isEmpty()) { emergencyContactPersonEditText.setError("Emergency contact person is required."); emergencyContactPersonEditText.requestFocus(); return; }
        if (emergencyContactNumber.isEmpty() || emergencyContactNumber.equals(PHILIPPINES_COUNTRY_CODE)) { // Check if only +63 is present
            emergencyContactNumberEditText.setError("Emergency contact number is required."); emergencyContactNumberEditText.requestFocus(); return;
        }
        // Simplified check: only allows numbers after +63 and ensures length
        if (!emergencyContactNumber.startsWith(PHILIPPINES_COUNTRY_CODE) || emergencyContactNumber.length() != 13) { // +63 and 10 digits = 13 chars
            emergencyContactNumberEditText.setError("Emergency contact number must be 11 digits (e.g., +63917xxxxxxx).");
            emergencyContactNumberEditText.requestFocus();
            return;
        }
        if (emergencyContactRelationship.isEmpty()) { emergencyContactRelationshipEditText.setError("Relationship to emergency contact is required."); emergencyContactRelationshipEditText.requestFocus(); return; }


        // Consent Validation
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
        disableAllInputFieldsAndButtons(); // Disable UI during registration

        // --- Perform Firebase Email/Password Registration ---
        mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE); // Hide progress bar regardless of success/failure

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "createUserWithEmail:success UID: " + user.getUid());
                            Toast.makeText(RegisterActivity.this, "Registration successful! Saving additional data...", Toast.LENGTH_SHORT).show();
                            saveUserDataToFirestore(user,
                                    firstName, middleName, lastName, suffix, dateOfBirth, placeOfBirth, gender, civilStatus, nationality,
                                    fullAddress, mobileNumber,
                                    disabilityTypes, disabilityOther, causeOfDisability, pwdIdNumber, // pwdIdNumber is now passed
                                    emergencyContactPerson, emergencyContactNumber, emergencyContactRelationship,
                                    privacyConsent, truthfulnessDeclaration);
                        } else {
                            Log.e(TAG, "createUserWithEmail:success but user is null.");
                            Toast.makeText(RegisterActivity.this, "Registration successful, but failed to get user data.", Toast.LENGTH_LONG).show();
                            enableAllInputFieldsAndButtons();
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        enableAllInputFieldsAndButtons(); // Re-enable fields on failure

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
        // Account Credentials (UID, email from auth)
        userData.put("uid", user.getUid());
        userData.put("email", user.getEmail()); // Email comes from Firebase Auth directly

        // Personal Information
        userData.put("firstName", firstName);
        userData.put("middleName", middleName);
        userData.put("lastName", lastName);
        userData.put("suffix", suffix);
        userData.put("dateOfBirth", dateOfBirth);
        userData.put("placeOfBirth", placeOfBirth);
        userData.put("gender", gender);
        userData.put("civilStatus", civilStatus);
        userData.put("nationality", nationality);

        // Contact Information
        userData.put("fullAddress", fullAddress); // Combined address
        userData.put("mobileNumber", mobileNumber);

        // Disability Information
        userData.put("disabilityTypes", disabilityTypes); // List of strings
        userData.put("disabilityOther", disabilityOther);
        userData.put("causeOfDisability", causeOfDisability);
        userData.put("pwdIdNumber", pwdIdNumber);

        // Emergency Contact
        userData.put("emergencyContactPerson", emergencyContactPerson);
        userData.put("emergencyContactNumber", emergencyContactNumber);
        userData.put("emergencyContactRelationship", emergencyContactRelationship);

        // Consent
        userData.put("privacyConsent", privacyConsent);
        userData.put("truthfulnessDeclaration", truthfulnessDeclaration);

        // Add a field to mark this as a PWD application, status, etc.
        userData.put("applicationStatus", "Pending Review"); // Initial status
        userData.put("registrationDate", com.google.firebase.firestore.FieldValue.serverTimestamp());


        // Save to Firestore in a "registrationApplications" collection
        db.collection("registrationApplications").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "PWD Application data saved to Firestore for UID: " + user.getUid());
                    Toast.makeText(RegisterActivity.this, "Registration complete! Your application is under review.", Toast.LENGTH_SHORT).show();

                    // >>>>>>> CHANGE STARTS HERE <<<<<<<
                    // Navigate to a confirmation page
                    Intent intent = new Intent(RegisterActivity.this, PendingApprovalActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    // >>>>>>> CHANGE ENDS HERE <<<<<<<
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving PWD application data to Firestore: " + e.getMessage(), e);
                    Toast.makeText(RegisterActivity.this, "Failed to save application data. Please try again.", Toast.LENGTH_LONG).show();
                    // If Firestore save fails, consider deleting the Firebase Auth user to prevent orphaned accounts
                    user.delete().addOnCompleteListener(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            Log.d(TAG, "Firebase user deleted due to Firestore save failure.");
                        } else {
                            Log.e(TAG, "Failed to delete Firebase user: " + deleteTask.getException());
                        }
                    });
                    enableAllInputFieldsAndButtons(); // Re-enable fields for retry
                });
    }

    // Helper to disable UI elements
    private void disableAllInputFieldsAndButtons() {
        emailEditText.setEnabled(false);
        passwordEditText.setEnabled(false);
        repeatPasswordEditText.setEnabled(false);
        buttonRegister.setEnabled(false);
        loginButtonTextView.setEnabled(false); // Also disable login link

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
    }

    // Helper to enable UI elements
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
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Add specific fields you want to save if an activity restart occurs
        outState.putString("email", emailEditText.getText().toString());
        outState.putString("firstName", firstNameEditText.getText().toString());
        outState.putString("middleName", middleNameEditText.getText().toString());
        outState.putString("lastName", lastNameEditText.getText().toString());
        outState.putString("suffix", suffixDropdown.getText().toString());
        outState.putString("dateOfBirth", dateOfBirthEditText.getText().toString());
        outState.putString("placeOfBirth", placeOfBirthEditText.getText().toString());
        // For RadioGroup, save the checked ID
        outState.putInt("gender", genderRadioGroup.getCheckedRadioButtonId());
        outState.putString("civilStatus", civilStatusDropdown.getText().toString());
        outState.putString("nationality", nationalityEditText.getText().toString());
        outState.putString("fullAddress", fullAddressEditText.getText().toString());
        outState.putString("mobileNumber", mobileNumberEditText.getText().toString());

        // For Checkboxes, save their checked state
        outState.putBoolean("cb_visual", cbVisual.isChecked());
        outState.putBoolean("cb_hearing", cbHearing.isChecked());
        outState.putBoolean("cb_speech", cbSpeech.isChecked());
        outState.putBoolean("cb_physical", cbPhysical.isChecked());
        outState.putBoolean("cb_intellectual", cbIntellectual.isChecked());
        outState.putBoolean("cb_psychosocial", cbPsychosocial.isChecked());
        outState.putBoolean("cb_learning", cbLearning.isChecked());
        outState.putBoolean("cb_developmental", cbDevelopmental.isChecked());
        outState.putBoolean("cb_multiple", cbMultiple.isChecked());
        outState.putString("disabilityOther", disabilityOtherEditText.getText().toString());
        outState.putString("causeOfDisability", causeOfDisabilityDropdown.getText().toString());
        outState.putString("pwdIdNumber", pwdIdNumberEditText.getText().toString());

        outState.putString("emergencyContactPerson", emergencyContactPersonEditText.getText().toString());
        outState.putString("emergencyContactNumber", emergencyContactNumberEditText.getText().toString());
        outState.putString("emergencyContactRelationship", emergencyContactRelationshipEditText.getText().toString());

        outState.putBoolean("privacyConsent", privacyConsentCheckbox.isChecked());
        outState.putBoolean("truthfulnessDeclaration", truthfulnessDeclarationCheckbox.isChecked());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore values
        emailEditText.setText(savedInstanceState.getString("email"));
        firstNameEditText.setText(savedInstanceState.getString("firstName"));
        middleNameEditText.setText(savedInstanceState.getString("middleName"));
        lastNameEditText.setText(savedInstanceState.getString("lastName"));
        suffixDropdown.setText(savedInstanceState.getString("suffix"), false); // 'false' to not show dropdown on restore
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
        if (progressBar.getVisibility() == View.VISIBLE) {
            progressBar.setVisibility(View.GONE);
        }
    }
}