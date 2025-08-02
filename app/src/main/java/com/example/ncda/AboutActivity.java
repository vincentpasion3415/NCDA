package com.example.ncda;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve Dark Mode Setting and Apply Theme
        SharedPreferences sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean darkMode = sharedPreferences.getBoolean("darkMode", false);
        setAppTheme(darkMode); // Apply theme based on saved setting

        setContentView(R.layout.activity_about);

        // Set NCDA information from strings.xml
        TextView missionTextView = findViewById(R.id.missionTextView);
        TextView visionTextView = findViewById(R.id.visionTextView);
        TextView coreValuesTextView = findViewById(R.id.coreValuesTextView);
        TextView servicesTextView = findViewById(R.id.servicesTextView);

        int fontSize = sharedPreferences.getInt("fontSize", 14); // Default to 14 if not found

        // Apply font size to all TextViews
        applyFontSize(missionTextView, fontSize);
        applyFontSize(visionTextView, fontSize);
        applyFontSize(coreValuesTextView, fontSize);
        applyFontSize(servicesTextView, fontSize);

        // Set the text after applying font size
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