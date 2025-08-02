// ChatbotActivity.java
package com.example.ncda; // IMPORTANT: Ensure this matches your actual package name

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log; // Keep for logging
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ncda.adapter.ChatAdapter; // Assume adapter is in 'adapter' package
import com.example.ncda.chatbot.NCDABot; // Assume NCDABot is in 'chatbot' package
import com.example.ncda.model.ChatMessage; // Assume ChatMessage is in 'model' package

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatbotActivity extends AppCompatActivity {

    private static final String TAG = "ChatbotActivity";

    private RecyclerView chatRecyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ImageButton micButton;

    private NCDABot ncdaBot;
    private ExecutorService executorService;

    private final int SPEECH_REQUEST_CODE = 100;
    private final int RECORD_AUDIO_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);


        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        micButton = findViewById(R.id.micButton);


        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);


        ncdaBot = new NCDABot();
        executorService = Executors.newSingleThreadExecutor();


        executorService.execute(new Runnable() {
            @Override
            public void run() {
                ncdaBot.loadFAQs(new NCDABot.OnFAQsLoadedListener() {
                    @Override
                    public void onFAQsLoaded() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addBotMessage("Hello! I'm your NCDA information assistant. How can I help you today?");
                            }
                        });
                    }

                    @Override
                    public void onFAQsLoadFailed(Exception e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ChatbotActivity.this, "Failed to load FAQs. Please check internet.", Toast.LENGTH_LONG).show();
                                addBotMessage("I'm having trouble accessing my knowledge base right now. Please check your internet connection.");
                            }
                        });
                        Log.e(TAG, "Error loading FAQs: " + e.getMessage(), e);
                    }
                });
            }
        });


        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageEditText.setText("");
            }
        });


        micButton.setOnClickListener(v -> {
            checkAudioPermissionAndStartSpeechRecognition();
        });
    }

    /**
     * Handles adding the user's message to the chat display (RecyclerView)
     * and then initiates getting a response from the NCDABot.
     * @param text The message content submitted by the user.
     */
    private void sendMessage(String text) {

        addUserMessage(text);


        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final String botResponse = ncdaBot.getAnswer(text);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        addBotMessage(botResponse);
                        chatRecyclerView.scrollToPosition(messageList.size() - 1);
                    }
                });
            }
        });
    }

    private void addUserMessage(String text) {
        chatAdapter.addMessage(new ChatMessage(text, true));
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void addBotMessage(String text) {
        chatAdapter.addMessage(new ChatMessage(text, false));
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }



    private void checkAudioPermissionAndStartSpeechRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    RECORD_AUDIO_PERMISSION_CODE
            );
        } else {
            startSpeechToText();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechToText();
            } else {
                Toast.makeText(this, "Audio recording permission denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your NCDA question...");
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().getLanguage());

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not available on this device.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            ArrayList<String> results = data != null ? data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) : null;
            String spokenText = (results != null && !results.isEmpty()) ? results.get(0) : null;

            if (spokenText != null) {
                messageEditText.setText(spokenText);

                sendMessage(spokenText);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}