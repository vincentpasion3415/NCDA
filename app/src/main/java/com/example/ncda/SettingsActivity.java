package com.example.ncda;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
// import android.widget.Switch; // Removed, replaced by SwitchCompat
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SwitchCompat; // Added for Switch compatibility
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private static final int PICK_DOCUMENT_REQUEST = 1002;

    private SeekBar fontSizeSeekBar;
    private FirebaseAnalytics mFirebaseAnalytics;
    private SwitchCompat switchDarkMode, switchNotifications; // Changed to SwitchCompat
    private TextView fontSizePreview;
    // Removed LinearLayout fields as they will be local variables
    // private LinearLayout txtLanguage, txtHelp, txtAbout, txtClearCache, txtFeedback;
    private Button btnLogout;
    private Button btnUploadDocument;

    private SharedPreferences sharedPreferences;
    private static final String CHANNEL_ID = "news_channel";

    private FirebaseStorage storage;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        String savedLanguage = sharedPreferences.getString("language", "en");
        setAppLanguage(savedLanguage);

        boolean darkMode = sharedPreferences.getBoolean("darkMode", false);
        setAppTheme(darkMode);

        setContentView(R.layout.activity_settings);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Initialize Firebase Storage and Auth
        storage = FirebaseStorage.getInstance();
        mAuth = FirebaseAuth.getInstance();


        // Initialize UI elements
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        if (fontSizeSeekBar == null) Log.e(TAG, "fontSizeSeekBar is null!");

        switchDarkMode = findViewById(R.id.switchDarkMode);
        if (switchDarkMode == null) Log.e(TAG, "switchDarkMode is null!");

        switchNotifications = findViewById(R.id.switchNotifications);
        if (switchNotifications == null) Log.e(TAG, "switchNotifications is null!");

        fontSizePreview = findViewById(R.id.fontSizePreview);
        if (fontSizePreview == null) Log.e(TAG, "fontSizePreview is null!");

        // Declaring LinearLayouts as local variables
        LinearLayout txtLanguage = findViewById(R.id.txtLanguage);
        if (txtLanguage == null) Log.e(TAG, "txtLanguage (LinearLayout) is null!");

        // onlineStorageLayout LinearLayout was removed from XML, so no need to find it here

        LinearLayout txtHelp = findViewById(R.id.txtHelp);
        if (txtHelp == null) Log.e(TAG, "txtHelp (LinearLayout) is null!");

        LinearLayout txtAbout = findViewById(R.id.txtAbout);
        if (txtAbout == null) Log.e(TAG, "txtAbout (LinearLayout) is null!");

        LinearLayout txtClearCache = findViewById(R.id.txtClearCache);
        if (txtClearCache == null) Log.e(TAG, "txtClearCache (LinearLayout) is null!");

        LinearLayout txtFeedback = findViewById(R.id.txtFeedback);
        if (txtFeedback == null) Log.e(TAG, "txtFeedback (LinearLayout) is null!");

        btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout == null) Log.e(TAG, "btnLogout is null!");

        btnUploadDocument = findViewById(R.id.btnUploadDocument);
        if (btnUploadDocument == null) Log.e(TAG, "btnUploadDocument is null!");


        // Load and apply font size settings
        int fontSize = sharedPreferences.getInt("fontSize", 14);
        if (fontSizeSeekBar != null) {
            fontSizeSeekBar.setProgress(fontSize);
        }
        applyFontSize(fontSizePreview, fontSize);
        applyFontSizeToAllTextViews(fontSize);

        // Set initial state for Dark Mode switch
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(darkMode);
        }

        updateNotificationSwitchState();


        // Set up Listeners
        if (fontSizeSeekBar != null) {
            fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    sharedPreferences.edit().putInt("fontSize", progress).apply();
                    applyFontSize(fontSizePreview, progress);
                    applyFontSizeToAllTextViews(progress);
                    logAnalyticsEvent("font_size_changed", "SettingsActivity", "Font size set to " + progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    Toast.makeText(SettingsActivity.this, "Font size saved", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("darkMode", isChecked);
                editor.apply();
                setAppTheme(isChecked);
                recreate();
                logAnalyticsEvent("dark_mode_toggled", "SettingsActivity", isChecked ? "Enabled" : "Disabled");
            });
        }

        if (switchNotifications != null) {
            switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(SettingsActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(SettingsActivity.this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
                        } else {
                            sharedPreferences.edit().putBoolean("notificationsEnabled", true).apply();
                            logAnalyticsEvent("notifications_toggled", "SettingsActivity", "Enabled (Permission already granted)");
                            Toast.makeText(SettingsActivity.this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        sharedPreferences.edit().putBoolean("notificationsEnabled", true).apply();
                        logAnalyticsEvent("notifications_toggled", "SettingsActivity", "Enabled (Older Android)");
                        Toast.makeText(SettingsActivity.this, "Notifications enabled", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    sharedPreferences.edit().putBoolean("notificationsEnabled", false).apply();
                    logAnalyticsEvent("notifications_toggled", "SettingsActivity", "Disabled");
                    Toast.makeText(SettingsActivity.this, "Notifications disabled", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (txtLanguage != null) {
            txtLanguage.setOnClickListener(v -> showLanguageSelectionDialog());
        } else {
            Log.e(TAG, "txtLanguage is null, cannot set OnClickListener!");
        }

        if (btnUploadDocument != null) {
            btnUploadDocument.setOnClickListener(v -> pickDocumentForUpload());
        } else {
            Log.e(TAG, "btnUploadDocument is null, cannot set OnClickListener!");
        }

        if (txtHelp != null) {
            txtHelp.setOnClickListener(v -> {
                startActivity(new Intent(this, HelpActivity.class));
            });
        } else {
            Log.e(TAG, "txtHelp is null, cannot set OnClickListener!");
        }

        if (txtAbout != null) {
            txtAbout.setOnClickListener(v -> {
                startActivity(new Intent(this, AboutActivity.class));
            });
        } else {
            Log.e(TAG, "txtAbout is null, cannot set OnClickListener!");
        }

        if (txtFeedback != null) {
            txtFeedback.setOnClickListener(v -> {
                startActivity(new Intent(this, FeedbackActivity.class));
                logAnalyticsEvent("feedback_opened", "SettingsActivity", "Feedback activity launched");
            });
        } else {
            Log.e(TAG, "txtFeedback is null, cannot set OnClickListener!");
        }

        if (txtClearCache != null) {
            txtClearCache.setOnClickListener(v -> {
                clearCache();
            });
        } else {
            Log.e(TAG, "txtClearCache is null, cannot set OnClickListener!");
        }

        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth != null) {
                    auth.signOut();
                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    logAnalyticsEvent("user_logged_out", "SettingsActivity", "FirebaseAuth");
                } else {
                    Toast.makeText(SettingsActivity.this, "Logout failed", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Log.e(TAG, "btnLogout is null, cannot set OnClickListener!");
        }

        createNotificationChannel();
    }

    @Override
    protected void onResume() {
        super.onResume();
        logAnalyticsEvent("user_engagement", "SettingsActivity", "User actively engaging in settings");
        updateNotificationSwitchState();
    }

    private void pickDocumentForUpload() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please log in to use online storage.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "image/*", "text/plain", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a Document to Upload"), PICK_DOCUMENT_REQUEST);
            logAnalyticsEvent("document_upload_initiated", TAG, "User opened file picker for upload.");
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No file manager app found to pick documents.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "No activity found to handle document picking: " + ex.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_DOCUMENT_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            Log.d(TAG, "Selected file URI: " + fileUri.toString());
            uploadFileToFirebaseStorage(fileUri);
        } else if (requestCode == PICK_DOCUMENT_REQUEST && resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Document selection cancelled.", Toast.LENGTH_SHORT).show();
            logAnalyticsEvent("document_upload_cancelled", TAG, "User cancelled document selection.");
        }
    }

    private void uploadFileToFirebaseStorage(Uri fileUri) {
        final FirebaseUser currentUser = mAuth.getCurrentUser(); // Made effectively final
        if (currentUser == null) {
            Toast.makeText(this, "Error: User not logged in. Cannot upload document.", Toast.LENGTH_SHORT).show();
            logAnalyticsEvent("document_upload_failed", TAG, "User not logged in.");
            return;
        }

        final String userId = currentUser.getUid(); // Made final
        String baseFileName = getFileName(fileUri);
        final String fileNameToUse; // New effectively final variable for filename

        if (baseFileName == null || baseFileName.isEmpty()) {
            fileNameToUse = "uploaded_document_" + System.currentTimeMillis();
        } else {
            fileNameToUse = baseFileName;
        }

        StorageReference documentRef = storage.getReference()
                .child("users")
                .child(userId)
                .child("documents")
                .child(fileNameToUse); // Use fileNameToUse here

        Toast.makeText(this, "Uploading " + fileNameToUse + "...", Toast.LENGTH_LONG).show();
        logAnalyticsEvent("document_upload_started", TAG, "File: " + fileNameToUse);

        UploadTask uploadTask = documentRef.putFile(fileUri);

        uploadTask.addOnProgressListener(snapshot -> {
            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
            Log.d(TAG, "Upload is " + progress + "% done");
        }).addOnPausedListener(snapshot -> {
            Log.d(TAG, "Upload is paused");
            Toast.makeText(this, "Upload paused for " + fileNameToUse, Toast.LENGTH_SHORT).show(); // Use fileNameToUse
        }).addOnFailureListener(exception -> {
            Log.e(TAG, "Upload failed for " + fileNameToUse + ": " + exception.getMessage(), exception); // Use fileNameToUse
            Toast.makeText(this, "Upload failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            logAnalyticsEvent("document_upload_failed", TAG, "File: " + fileNameToUse + ", Error: " + exception.getMessage()); // Use fileNameToUse
        }).addOnSuccessListener(taskSnapshot -> {
            documentRef.getDownloadUrl().addOnSuccessListener(uri -> {
                Log.d(TAG, "Download URL: " + uri.toString());
                Toast.makeText(this, "Document uploaded successfully!", Toast.LENGTH_LONG).show();
                logAnalyticsEvent("document_uploaded_success", TAG, "File: " + fileNameToUse + ", URL: " + uri.toString()); // Use fileNameToUse
                saveDocumentMetadataToFirestore(userId, fileNameToUse, uri.toString()); // Use fileNameToUse
            }).addOnFailureListener(exception -> {
                Log.e(TAG, "Failed to get download URL for " + fileNameToUse + ": " + exception.getMessage()); // Use fileNameToUse
                Toast.makeText(this, "Upload success, but failed to get download URL.", Toast.LENGTH_LONG).show();
            });
        });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting file name from content URI", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void saveDocumentMetadataToFirestore(String userId, String fileName, String downloadUrl) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> documentData = new HashMap<>();
        documentData.put("fileName", fileName);
        documentData.put("downloadUrl", downloadUrl);
        documentData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        documentData.put("uploadedBy", userId);

        db.collection("users").document(userId)
                .collection("uploaded_documents")
                .add(documentData)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Document metadata saved to Firestore with ID: " + documentReference.getId());
                    Toast.makeText(this, "Document metadata saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving document metadata to Firestore: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to save document metadata.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
                sharedPreferences.edit().putBoolean("notificationsEnabled", true).apply();
                if (switchNotifications != null) {
                    switchNotifications.setChecked(true);
                }
                logAnalyticsEvent("notification_permission", "SettingsActivity", "Granted");
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_LONG).show();
                sharedPreferences.edit().putBoolean("notificationsEnabled", false).apply();
                if (switchNotifications != null) {
                    switchNotifications.setChecked(false);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Permission Needed")
                            .setMessage("Notifications permission is required for this feature. Please enable it in app settings.")
                            .setPositiveButton("Go to Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                }
                logAnalyticsEvent("notification_permission", "SettingsActivity", "Denied");
            }
        }
    }

    private boolean areNotificationsGloballyEnabled() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return notificationManager.areNotificationsEnabled();
        } else {
            return true;
        }
    }

    private void updateNotificationSwitchState() {
        boolean userPreferenceEnabled = sharedPreferences.getBoolean("notificationsEnabled", false);
        boolean systemPermissionGranted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            systemPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            systemPermissionGranted = areNotificationsGloballyEnabled();
        }

        if (switchNotifications != null) {
            switchNotifications.setChecked(userPreferenceEnabled && systemPermissionGranted);
            if (!switchNotifications.isChecked() && userPreferenceEnabled) {
                sharedPreferences.edit().putBoolean("notificationsEnabled", false).apply();
            }
        }
    }

    private void showLanguageSelectionDialog() {
        String[] languages = {"English", "Tagalog"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setItems(languages, (dialog, which) -> {
                    String selectedLanguage = (which == 0) ? "en" : "tl";
                    sharedPreferences.edit().putString("language", selectedLanguage).apply();
                    setAppLanguage(selectedLanguage);
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                    Toast.makeText(this, "Language changed to " + languages[which], Toast.LENGTH_SHORT).show();
                    logAnalyticsEvent("language_changed", "SettingsActivity", "Language set to " + languages[which]);
                })
                .show();
    }

    private void setAppLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        DisplayMetrics dm = resources.getDisplayMetrics();
        config.setLocale(locale);
        resources.updateConfiguration(config, dm);
    }

    private void logAnalyticsEvent(String eventName, String screenName, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName);
        bundle.putString(FirebaseAnalytics.Param.VALUE, value);
        mFirebaseAnalytics.logEvent(eventName, bundle);
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "News Notifications";
            String description = "Channel for news notifications";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void clearCache() {
        try {
            File cacheDir = getCacheDir();
            deleteDir(cacheDir);
            Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
            logAnalyticsEvent("cache_cleared", "SettingsActivity", "Application cache was cleared");
        } catch (Exception e) {
            Toast.makeText(this, "Failed to clear cache", Toast.LENGTH_SHORT).show();
            logAnalyticsEvent("cache_clear_failed", "SettingsActivity", "Error: " + e.getMessage());
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

    public static void applyFontSize(TextView textView, float fontSize) {
        if (textView != null) {
            textView.setTextSize(fontSize);
        }
    }

    private void applyFontSizeToAllTextViews(float fontSize) {
        if (fontSizePreview != null) {
            applyFontSize(fontSizePreview, fontSize);
        }
    }

    public void showNewsNotification(String title, String message) {
        boolean userPreferenceEnabled = sharedPreferences.getBoolean("notificationsEnabled", false);
        boolean systemPermissionGranted;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            systemPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            systemPermissionGranted = areNotificationsGloballyEnabled();
        }

        if (userPreferenceEnabled && systemPermissionGranted) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                return;
            }
            notificationManager.notify(1, builder.build());
        } else {
            logAnalyticsEvent("notification_failed", "NewsNotification", "User preference: " + userPreferenceEnabled + ", System permission: " + systemPermissionGranted);
        }
    }
}