package com.example.ncda;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.util.Locale;

/**
 * Handles the display and logic for the Settings screen.
 * It sets up the toolbar and makes the back button functional.
 */
public class SettingsActivity extends BaseActivity { // This line has been changed to extend BaseActivity

    private static final String TAG = "SettingsActivity";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;
    private static final String PREF_HIGH_CONTRAST = "highContrast";

    private SeekBar fontSizeSeekBar;
    private FirebaseAnalytics mFirebaseAnalytics;
    private SwitchCompat switchNotifications;
    private SwitchCompat switchHighContrast;
    private TextView fontSizePreview;
    private Button btnLogout;

    private SharedPreferences sharedPreferences;
    private static final String CHANNEL_ID = "news_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The theme setting logic is now handled in BaseActivity, so we remove it from here.
        super.onCreate(savedInstanceState);
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        String savedLanguage = sharedPreferences.getString("language", "en");
        setAppLanguage(savedLanguage);

        setContentView(R.layout.activity_settings);

        // Initialize Toolbar and set up the back button logic
        Toolbar toolbar = findViewById(R.id.settingsToolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            // Hides the default title from the toolbar
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            // This line enables the back arrow (or 'Up') button in the toolbar
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Sets a click listener on the back button.
        // The `finish()` method will close this activity and go back to the previous one.
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize UI elements
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        if (fontSizeSeekBar == null) Log.e(TAG, "fontSizeSeekBar is null!");

        switchNotifications = findViewById(R.id.switchNotifications);
        if (switchNotifications == null) Log.e(TAG, "switchNotifications is null!");

        switchHighContrast = findViewById(R.id.switchHighContrast); // Initialize new switch
        if (switchHighContrast == null) Log.e(TAG, "switchHighContrast is null!");

        fontSizePreview = findViewById(R.id.fontSizePreview);
        if (fontSizePreview == null) Log.e(TAG, "fontSizePreview is null!");

        // Declaring LinearLayouts as local variables
        LinearLayout txtLanguage = findViewById(R.id.txtLanguage);
        if (txtLanguage == null) Log.e(TAG, "txtLanguage (LinearLayout) is null!");

        LinearLayout txtAbout = findViewById(R.id.txtAbout);
        if (txtAbout == null) Log.e(TAG, "txtAbout (LinearLayout) is null!");

        LinearLayout txtClearCache = findViewById(R.id.txtClearCache);
        if (txtClearCache == null) Log.e(TAG, "txtClearCache (LinearLayout) is null!");

        LinearLayout txtFeedback = findViewById(R.id.txtFeedback);
        if (txtFeedback == null) Log.e(TAG, "txtFeedback (LinearLayout) is null!");

        btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout == null) Log.e(TAG, "btnLogout is null!");


        // Load and apply font size settings
        int fontSize = sharedPreferences.getInt("fontSize", 14);
        if (fontSizeSeekBar != null) {
            fontSizeSeekBar.setProgress(fontSize);
        }
        applyFontSize(fontSizePreview, fontSize);
        applyFontSizeToAllTextViews(fontSize);

        updateNotificationSwitchState();
        boolean isHighContrast = sharedPreferences.getBoolean(PREF_HIGH_CONTRAST, false);
        switchHighContrast.setChecked(isHighContrast);


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

        // Set up listener for the new high contrast switch
        if (switchHighContrast != null) {
            switchHighContrast.setOnCheckedChangeListener((buttonView, isChecked) -> {
                sharedPreferences.edit().putBoolean(PREF_HIGH_CONTRAST, isChecked).apply();
                logAnalyticsEvent("high_contrast_toggled", "SettingsActivity", "High contrast mode set to " + isChecked);
                recreate(); // Recreate the activity to apply the new theme
            });
        }


        if (txtLanguage != null) {
            txtLanguage.setOnClickListener(v -> showLanguageSelectionDialog());
        } else {
            Log.e(TAG, "txtLanguage is null, cannot set OnClickListener!");
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

    // You can override onOptionsItemSelected to handle clicks if you prefer
    // @Override
    // public boolean onOptionsItemSelected(MenuItem item) {
    //     if (item.getItemId() == android.R.id.home) {
    //         onBackPressed();
    //         return true;
    //     }
    //     return super.onOptionsItemSelected(item);
    // }

    @Override
    protected void onResume() {
        super.onResume();
        logAnalyticsEvent("user_engagement", "SettingsActivity", "User actively engaging in settings");
        updateNotificationSwitchState();
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
