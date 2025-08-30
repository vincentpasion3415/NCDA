package com.example.ncda;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity to handle both language and high contrast theme settings for the entire application.
 * All other activities should extend this class to automatically apply these preferences.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final String PREF_HIGH_CONTRAST = "highContrast";

    @Override
    protected void attachBaseContext(Context newBase) {
        // Step 1: Handle language setting first.
        // This must be done before the activity's onCreate method is called.
        LanguageManager languageManager = new LanguageManager(newBase);
        String savedLanguage = languageManager.getAppLanguage();
        Context updatedContext = languageManager.setAppLanguage(savedLanguage);
        super.attachBaseContext(updatedContext);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Step 2: Handle the theme setting after the context has been attached.
        // This is the correct place to set the theme.
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isHighContrast = sharedPreferences.getBoolean(PREF_HIGH_CONTRAST, false);

        if (isHighContrast) {
            setTheme(R.style.HighContrastTheme);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
    }
}