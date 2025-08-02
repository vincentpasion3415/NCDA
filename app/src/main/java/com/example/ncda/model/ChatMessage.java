
package com.example.ncda.model;

import java.util.Date;

public class ChatMessage {
    private String text;
    private boolean isUser;
    private Date timestamp;

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


}