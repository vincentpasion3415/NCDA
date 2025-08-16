package com.example.ncda;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.view.MenuItem;
import androidx.annotation.NonNull;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.navigation.NavigationBarView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class HomeActivity extends AppCompatActivity {
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db;

    private BottomNavigationView bottomNavigationView;
    private Toolbar toolbar;
    private static final int REQUEST_CODE_SPEECH_INPUT = 100;

    private ImageButton settingsButton;
    // New button variable
    private ImageButton complainButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        FirebaseApp.initializeApp(this);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            redirectToLogin();
            return;
        }

        db = FirebaseFirestore.getInstance();
        checkUserApprovalStatus(currentUser);

        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean darkMode = sharedPreferences.getBoolean("darkMode", false);
        setAppTheme(darkMode);

        setContentView(R.layout.activity_home);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    replaceFragment(new HomeFragment());
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.valueOf(itemId));
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, item.getTitle().toString());
                    mFirebaseAnalytics.logEvent("bottom_nav_selection", bundle);
                    return true;
                } else if (itemId == R.id.nav_profile) {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        startActivity(new Intent(HomeActivity.this, ProfileActivity.class)
                                .putExtra("userId", currentUser.getUid()));
                    } else {
                        Toast.makeText(HomeActivity.this, "User not logged in.", Toast.LENGTH_SHORT).show();
                        redirectToLogin();
                    }
                    return true;
                } else if (itemId == R.id.nav_appointment) {
                    startActivity(new Intent(HomeActivity.this, AppointmentSchedulingActivity.class));
                    return true;
                } else if (itemId == R.id.nav_submission_history) {
                    replaceFragment(new SubmissionHistoryFragment());
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.valueOf(itemId));
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, item.getTitle().toString());
                    mFirebaseAnalytics.logEvent("bottom_nav_selection", bundle);
                    return true;
                } else if (itemId == R.id.nav_settings) {
                    startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                    return true;
                } else if (itemId == R.id.openChatbotButton) {
                    startActivity(new Intent(HomeActivity.this, ChatbotActivity.class));
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, String.valueOf(itemId));
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, item.getTitle().toString());
                    mFirebaseAnalytics.logEvent("bottom_nav_selection", bundle);
                    return true;
                }
                return false;
            }
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        ImageButton voiceCommandButton = findViewById(R.id.voiceCommandButton);
        if (voiceCommandButton != null) {
            voiceCommandButton.setOnClickListener(v -> startVoiceCommand());
        } else {
            Log.e("HomeActivity", "Voice Command Button (R.id.voiceCommandButton) not found in layout!");
        }

        settingsButton = findViewById(R.id.settingsButton);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                Log.d("HomeActivity", "Settings button in toolbar clicked. Launching SettingsActivity.");
                mFirebaseAnalytics.logEvent("settings_button_clicked_toolbar", null);
            });
        } else {
            Log.e("HomeActivity", "Settings Button (R.id.settingsButton) not found in layout!");
        }

        // New: Handle the click for the complain button
        complainButton = findViewById(R.id.complainButton);
        if (complainButton != null) {
            complainButton.setOnClickListener(v -> {
                startActivity(new Intent(HomeActivity.this, ComplaintActivity.class));
                Log.d("HomeActivity", "Complaint button clicked. Launching ComplaintActivity.");
                mFirebaseAnalytics.logEvent("complaint_button_clicked", null);
            });
        } else {
            Log.e("HomeActivity", "Complaint Button (R.id.complainButton) not found in layout!");
        }
    }

    private void checkUserApprovalStatus(FirebaseUser user) {
        db.collection("registrationApplications").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        String status = task.getResult().getString("applicationStatus");
                        if (!"Approved".equalsIgnoreCase(status)) {
                            Intent intent = new Intent(HomeActivity.this, PendingApprovalActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Log.e("HomeActivity", "User document not found. Redirecting to pending approval.");
                        Intent intent = new Intent(HomeActivity.this, PendingApprovalActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.commit();
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

    private void startVoiceCommand() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak a command...");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                if (result != null && !result.isEmpty()) {
                    String spokenText = result.get(0).toLowerCase();
                    Log.d("VoiceCommand", "Recognized text: " + spokenText);

                    Bundle voiceBundle = new Bundle();
                    voiceBundle.putString("recognized_command", spokenText);
                    mFirebaseAnalytics.logEvent("voice_command_recognized", voiceBundle);

                    if (spokenText.contains("home") || spokenText.contains("main page") ||
                            spokenText.contains("news")) {
                        replaceFragment(new HomeFragment());
                        bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    } else if (spokenText.contains("profile") || spokenText.contains("my profile") ||
                            spokenText.contains("user profile") || spokenText.contains("user") ||
                            spokenText.contains("account") || spokenText.contains("my account")) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            startActivity(new Intent(HomeActivity.this, ProfileActivity.class)
                                    .putExtra("userId", currentUser.getUid()));
                        } else {
                            Toast.makeText(HomeActivity.this, "User not logged in.", Toast.LENGTH_SHORT).show();
                            redirectToLogin();
                        }
                    } else if (spokenText.contains("appointment") || spokenText.contains("schedule") ||
                            spokenText.contains("book")) {
                        startActivity(new Intent(HomeActivity.this, AppointmentSchedulingActivity.class));
                    } else if (spokenText.contains("submissions") || spokenText.contains("history") ||
                            spokenText.contains("my applications") || spokenText.contains("my appointments")) {
                        replaceFragment(new SubmissionHistoryFragment());
                        bottomNavigationView.setSelectedItemId(R.id.nav_submission_history);
                    } else if (spokenText.contains("settings") || spokenText.contains("options") ||
                            spokenText.contains("configure")) {
                        startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                    } else if (spokenText.contains("chatbot") || spokenText.contains("chat with bot") ||
                            spokenText.contains("ask bot") || spokenText.contains("talk to bot")) {
                        startActivity(new Intent(HomeActivity.this, ChatbotActivity.class));
                    } else if (spokenText.contains("referral") || spokenText.contains("government services") ||
                            spokenText.contains("sss") || spokenText.contains("dswd") ||
                            spokenText.contains("philhealth") || spokenText.contains("pag-ibig")) {
                        startActivity(new Intent(HomeActivity.this, ReferralActivity.class));
                    } else if (spokenText.contains("complaint") || spokenText.contains("report") ||
                            spokenText.contains("feedback")) {
                        // New: Voice command for complaint
                        startActivity(new Intent(HomeActivity.this, ComplaintActivity.class));
                    } else {
                        Toast.makeText(this, "Command not recognized.", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(this, "No speech recognized.", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "Speech recognition failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isUserLoggedInWithFirebase() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    private void redirectToLogin()  {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        redirectToLogin();
    }

    public static void applyFontSize(TextView textView, float fontSize) {
        if (textView != null) {
            textView.setTextSize(fontSize);
        }
    }
}