package com.example.ncda;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // UI elements
    private ImageView ncdaLogo;
    private LinearLayout initialButtonsContainer;
    private LinearLayout loginFormContainer;
    private TextInputEditText emailEditText, passwordEditText;
    private MaterialButton loginButton, registerButton;
    private MaterialButton loginSubmitButton;
    private ProgressBar loginProgressBar;
    private TextView backToInitialButtonsTextView;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // ADDED: Firestore instance
    private FirebaseAnalytics mFirebaseAnalytics;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // ADDED: Firestore initialization
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // --- Initialize UI elements ---
        ncdaLogo = findViewById(R.id.ncdaLogo);
        initialButtonsContainer = findViewById(R.id.initialButtonsContainer);
        loginFormContainer = findViewById(R.id.loginFormContainer);
        emailEditText = findViewById(R.id.email);
        passwordEditText = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);
        loginSubmitButton = findViewById(R.id.loginSubmitButton);
        loginProgressBar = findViewById(R.id.loginProgressBar);
        backToInitialButtonsTextView = findViewById(R.id.backToInitialButtons);

        // --- Initial Visibility Setup ---
        initialButtonsContainer.setVisibility(View.GONE);
        loginFormContainer.setVisibility(View.GONE);
        loginProgressBar.setVisibility(View.GONE);

        // --- Splash Screen Animation ---
        ncdaLogo.animate().alpha(1.0f).setDuration(1000).withEndAction(() -> {
            handler.postDelayed(() -> {
                ncdaLogo.animate().alpha(0.0f).setDuration(1000).withEndAction(() -> {
                    ncdaLogo.setVisibility(View.GONE);
                    initialButtonsContainer.setAlpha(0.0f);
                    initialButtonsContainer.setVisibility(View.VISIBLE);
                    initialButtonsContainer.animate().alpha(1.0f).setDuration(500).start();
                }).start();
            }, 1000);
        }).start();

        // --- Button Listeners ---
        loginButton.setOnClickListener(v -> {
            initialButtonsContainer.animate().alpha(0.0f).setDuration(300).withEndAction(() -> {
                initialButtonsContainer.setVisibility(View.GONE);
                loginFormContainer.setAlpha(0.0f);
                loginFormContainer.setVisibility(View.VISIBLE);
                loginFormContainer.animate().alpha(1.0f).setDuration(300).start();
                logAnalyticsEvent("login_form_displayed", "LoginActivity", "Login form fields shown");
            }).start();
        });

        registerButton.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            logAnalyticsEvent("register_button_clicked", "LoginActivity", "Navigated to RegisterActivity");
        });

        loginSubmitButton.setOnClickListener(v -> loginUser());

        backToInitialButtonsTextView.setOnClickListener(v -> {
            loginFormContainer.animate().alpha(0.0f).setDuration(300).withEndAction(() -> {
                loginFormContainer.setVisibility(View.GONE);
                initialButtonsContainer.setAlpha(0.0f);
                initialButtonsContainer.setVisibility(View.VISIBLE);
                initialButtonsContainer.animate().alpha(1.0f).setDuration(300).start();
            }).start();
        });

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.METHOD, "initial_view");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle);
    }

    // UPDATED: This method now checks Firestore for the user's application status
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            checkApplicationStatusAndRedirect(currentUser);
        }
    }

    // UPDATED: This method now calls the new status check after successful login
    private void loginUser() {
        String userEmail = emailEditText.getText().toString().trim();
        String userPassword = passwordEditText.getText().toString().trim();

        if (userEmail.isEmpty() || userPassword.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        loginProgressBar.setVisibility(View.VISIBLE);
        loginSubmitButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(this, task -> {
                    loginProgressBar.setVisibility(View.GONE);
                    loginSubmitButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            logAnalyticsEvent("login_success", "LoginActivity", "Email/Password login successful for " + userEmail);
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();

                            checkApplicationStatusAndRedirect(user);
                        }
                    } else {
                        String errorMessage;
                        try {
                            throw task.getException();
                        } catch (FirebaseAuthInvalidUserException e) {
                            errorMessage = "User not found or is disabled.";
                            emailEditText.setError("User not found.");
                            emailEditText.requestFocus();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            errorMessage = "Invalid password or email.";
                            passwordEditText.setError("Invalid password.");
                            passwordEditText.requestFocus();
                        } catch (Exception e) {
                            errorMessage = "Login failed: " + e.getMessage();
                            Log.e(TAG, "Login failed: " + e.getMessage(), e);
                        }

                        logAnalyticsEvent("login_failed", "LoginActivity", errorMessage);
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // NEW METHOD: This will handle the navigation based on the user's status
    // NEW METHOD: This will handle the navigation based on the user's status
    private void checkApplicationStatusAndRedirect(FirebaseUser user) {
        db.collection("registrationApplications").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String status = task.getResult().getString("applicationStatus");
                        if ("Approved".equalsIgnoreCase(status)) {
                            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else if ("Rejected".equalsIgnoreCase(status)) {
                            Toast.makeText(this, "Your registration was rejected. Please contact support.", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        } else {
                            // CHANGE THIS LINE
                            Intent intent = new Intent(LoginActivity.this, RegistrationPendingActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Log.e(TAG, "User document not found in registrationApplications collection.");
                        // CHANGE THIS LINE AS WELL
                        Intent intent = new Intent(LoginActivity.this, RegistrationPendingActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void logAnalyticsEvent(String eventName, String screenName, String description) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        bundle.putString(FirebaseAnalytics.Param.VALUE, description);
        mFirebaseAnalytics.logEvent(eventName, bundle);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}