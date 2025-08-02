package com.example.ncda.model; // IMPORTANT: Ensure this matches your project's package structure

import java.util.Date;

public class ChatMessage {
    private String text;
    private boolean isUser; // true if from user, false if from bot
    private Date timestamp;

    public ChatMessage() {
        // Required for Firebase deserialization
        // If you intend to use this with Firestore, a no-argument constructor is essential.
    }

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.timestamp = new Date();
    }

    // Getters
    public String getText() {
        return text;
    }

    public boolean isUser() {
        return isUser;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    // Setters (re-adding for Firebase deserialization, as Firestore needs them to set private fields)
    public void setText(String text) {
        this.text = text;
    }

    public void setUser(boolean user) {
        isUser = user;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
