package com.example.ncda;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Apply Dark Mode
        boolean darkMode = sharedPreferences.getBoolean("darkMode", false);
        setAppTheme(darkMode);

        // Apply High Contrast if enabled
        boolean highContrast = sharedPreferences.getBoolean("highContrast", false);
        if (highContrast) {
            setTheme(R.style.Theme_NCDA_HighContrast);
            // ⚠️ You’ll need to define Theme_NCDA_HighContrast in styles.xml
        }

        setContentView(R.layout.activity_about);

        // Get references
        TextView missionTextView = findViewById(R.id.missionTextView);
        TextView visionTextView = findViewById(R.id.visionTextView);
        TextView coreValuesTextView = findViewById(R.id.coreValuesTextView);
        TextView servicesTextView = findViewById(R.id.servicesTextView);

        // Apply font size
        int fontSize = sharedPreferences.getInt("fontSize", 14);
        applyFontSize(missionTextView, fontSize);
        applyFontSize(visionTextView, fontSize);
        applyFontSize(coreValuesTextView, fontSize);
        applyFontSize(servicesTextView, fontSize);

        // Apply Screen Reader Hints (adds content descriptions)
        boolean screenReaderHints = sharedPreferences.getBoolean("screenReaderHints", false);
        if (screenReaderHints) {
            missionTextView.setContentDescription(getString(R.string.ncda_mission_text));
            visionTextView.setContentDescription(getString(R.string.ncda_vision_text));
            coreValuesTextView.setContentDescription(getString(R.string.ncda_core_values_text));
            servicesTextView.setContentDescription(getString(R.string.ncda_services_text));
        }

        // Apply Simplified Mode (e.g. hide sections not essential)
        boolean simplifiedMode = sharedPreferences.getBoolean("simplifiedMode", false);
        if (simplifiedMode) {
            coreValuesTextView.setVisibility(View.GONE); // Example: hide “Core Values” in simplified mode
        }

        // Set the actual text
        missionTextView.setText(getString(R.string.ncda_mission_text));
        visionTextView.setText(getString(R.string.ncda_vision_text));
        coreValuesTextView.setText(getString(R.string.ncda_core_values_text));
        servicesTextView.setText(getString(R.string.ncda_services_text));
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

    public static void applyFontSize(TextView textView, float fontSize) {
        if (textView != null) {
            textView.setTextSize(fontSize);
        }
    }
}
