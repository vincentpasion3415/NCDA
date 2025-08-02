// Path: app/src/main/java/com.example.ncda/model/NewsArticle.java
package com.example.ncda.model;

import com.google.firebase.Timestamp;

public class NewsArticle {
    private String id;
    private String title;
    private String description;
    private String imageUrl;
    private Timestamp publishedOn;

    public NewsArticle() {
        // Required no-argument constructor for Firebase Firestore deserialization
    }

    public NewsArticle(String id, String title, String description, String imageUrl, Timestamp publishedOn) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.imageUrl = imageUrl;
        this.publishedOn = publishedOn;
    }

    // Getters (required for Firestore)
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public Timestamp getPublishedOn() { return publishedOn; }

    // Setters (useful for setting ID after fetching, or for building objects)
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setPublishedOn(Timestamp publishedOn) { this.publishedOn = publishedOn; }
}