package com.example.ncda;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Base activity to handle high contrast theme setting for the entire application.
 * All other activities should extend this class to automatically apply the theme preference.
 */
public class BaseActivity extends AppCompatActivity {

    private static final String PREF_HIGH_CONTRAST = "highContrast";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Get shared preferences to check the high contrast setting.
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isHighContrast = sharedPreferences.getBoolean(PREF_HIGH_CONTRAST, false);

        // Set the appropriate theme before calling super.onCreate().
        // This is crucial for the theme to be applied correctly.
        if (isHighContrast) {
            setTheme(R.style.HighContrastTheme);
        } else {
            setTheme(R.style.AppTheme);
        }

        super.onCreate(savedInstanceState);
    }
}
