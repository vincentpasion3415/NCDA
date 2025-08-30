package com.example.ncda;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import java.util.Locale;

public class LanguageManager {
    private static final String PREF_NAME = "AppSettings";
    private static final String KEY_LANGUAGE = "language_code";
    private Context context;
    private SharedPreferences sharedPreferences;

    public LanguageManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public Context setAppLanguage(String languageCode) {
        persistLanguage(languageCode);
        return updateResources(context, languageCode);
    }

    public String getAppLanguage() {
        return sharedPreferences.getString(KEY_LANGUAGE, "en"); // "en" is default
    }

    private void persistLanguage(String languageCode) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LANGUAGE, languageCode);
        editor.apply();
    }

    @SuppressWarnings("deprecation")
    private Context updateResources(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
        } else {
            configuration.locale = locale;
        }

        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }
}