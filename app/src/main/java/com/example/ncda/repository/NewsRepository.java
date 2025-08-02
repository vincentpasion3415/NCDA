package com.example.ncda.repository;

import android.util.Log;
import com.example.ncda.model.NewsArticle;
import com.google.firebase.Timestamp; // <--- Make sure to import Timestamp
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class NewsRepository {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface NewsCallback {
        void onNewsLoaded(List<NewsArticle> newsArticles);
        void onError(Exception e);
    }

    public void getNewsArticles(String userId, NewsCallback callback) {
        Log.d("NewsRepository", "Fetching preferences for userId: " + userId);

        db.collection("userPreferences").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    Query query = db.collection("announcements"); // <--- Changed from "newsArticles" to "announcements" based on Firestore usage in HomeFragment

                    List<String> preferredCategories = documentSnapshot.get("preferredCategories", List.class);
                    if (preferredCategories != null && !preferredCategories.isEmpty()) {
                        Log.d("NewsRepository", "User's preferred categories: " + preferredCategories);
                        query = query.whereIn("category", preferredCategories);
                    }
                    // Order by publishedOn for consistent results, similar to HomeFragment's direct fetch
                    query = query.orderBy("publishedOn", Query.Direction.DESCENDING); // <--- Added ordering

                    query.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            List<NewsArticle> newsArticles = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                // Extract all necessary fields with correct types
                                String id = document.getId(); // Get the Firestore document ID
                                String title = document.getString("title");
                                String description = document.getString("description");
                                String imageUrl = document.getString("imageUrl"); // <--- Ensure this is the correct field name for image URL
                                Timestamp publishedOn = document.getTimestamp("publishedOn"); // <--- Get as Timestamp

                                // You had 'articleUrl' and 'category' here.
                                // If 'articleUrl' is part of your NewsArticle, you need to add it to the model.
                                // If 'category' is used for filtering but not displayed, it doesn't belong in NewsArticle.

                                // Basic validation for required fields
                                if (id != null && title != null && description != null && imageUrl != null && publishedOn != null) {
                                    // Instantiate NewsArticle with the correct 5 arguments and types
                                    newsArticles.add(new NewsArticle(id, title, description, imageUrl, publishedOn));
                                } else {
                                    // Log missing fields for debugging
                                    Log.w("NewsRepository", "Skipping article " + id + " due to missing data: " +
                                            "title=" + (title == null) +
                                            ", description=" + (description == null) +
                                            ", imageUrl=" + (imageUrl == null) +
                                            ", publishedOn=" + (publishedOn == null));
                                }
                            }
                            callback.onNewsLoaded(newsArticles);
                        } else {
                            Log.w("NewsRepository", "Failed to fetch news articles", task.getException());
                            callback.onError(task.getException());
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("NewsRepository", "Error fetching user preferences", e);
                    callback.onError(e);
                });
    }
}