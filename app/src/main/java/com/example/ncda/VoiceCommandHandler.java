package com.example.ncda;

import android.content.Context;
import android.content.Intent;
import android.speech.SpeechRecognizer;
import android.widget.TextView;
import android.widget.Toast;

public class VoiceCommandHandler {

    private Context context;
    private SpeechRecognizer speechRecognizer;
    private TextView speechTextView;

    public VoiceCommandHandler(Context context, SpeechRecognizer speechRecognizer, TextView speechTextView) {
        this.context = context;
        this.speechRecognizer = speechRecognizer;
        this.speechTextView = speechTextView;
    }

    public void handleCommand(String command) {
        switch (command) {
            case "go to home":
                startActivity(HomeActivity.class);
                break;
            case "go to profile":
                startActivity(ProfileActivity.class);
                break;
            case "go to news":
                startActivity(NewsActivity.class);
                break;
            case "go to register":
                startActivity(RegisterActivity.class);
                break;
            case "go to login":
                startActivity(LoginActivity.class);
                break;
            case "go to about":
                startActivity(AboutActivity.class);
                break;


            default:
                Toast.makeText(context, "Command not recognized", Toast.LENGTH_SHORT).show();
        }
    }

    private void startActivity(Class<?> cls) {
        Intent intent = new Intent(context, cls);
        context.startActivity(intent);
    }
}