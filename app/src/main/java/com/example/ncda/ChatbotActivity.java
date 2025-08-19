package com.example.ncda; // IMPORTANT: Ensure this matches your actual package name

import android.os.Bundle;
import android.util.Log; // Keep for logging
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
    // Removed: private ImageButton micButton; // No longer needed

    private NCDABot ncdaBot;
    private ExecutorService executorService;

    // Removed: private final int SPEECH_REQUEST_CODE = 100; // No longer needed
    // Removed: private final int RECORD_AUDIO_PERMISSION_CODE = 101; // No longer needed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Initialize Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // Enable back button
            getSupportActionBar().setTitle("FAQS Chat");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed()); // Handle back button click

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);
        // Removed: micButton = findViewById(R.id.micButton); // No longer needed

        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Keep messages at the bottom
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setAdapter(chatAdapter);

        ncdaBot = new NCDABot();
        executorService = Executors.newSingleThreadExecutor();

        // Load FAQs in a background thread
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

        // Set up send button click listener
        sendButton.setOnClickListener(v -> {
            String message = messageEditText.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageEditText.setText("");
            }
        });

        // Removed: micButton.setOnClickListener(v -> { // No longer needed
        // Removed:     checkAudioPermissionAndStartSpeechRecognition(); // No longer needed
        // Removed: }); // No longer needed
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

    // Removed: All speech recognition and permission methods:
    // Removed: private void checkAudioPermissionAndStartSpeechRecognition() { ... }
    // Removed: @Override public void onRequestPermissionsResult(...) { ... }
    // Removed: private void startSpeechToText() { ... }
    // Removed: @Override protected void onActivityResult(...) { ... }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure the executor service is shut down to prevent memory leaks
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
