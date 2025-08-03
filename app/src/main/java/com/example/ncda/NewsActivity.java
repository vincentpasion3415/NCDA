package com.example.ncda;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.text.method.LinkMovementMethod; // Import the LinkMovementMethod class

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.ncda.model.NewsArticle;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class NewsActivity extends AppCompatActivity {

    private static final String TAG = "NewsActivity";

    private FirebaseFirestore db;
    private TextView newsTitle, newsDescription, newsDate;
    private ImageView newsImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        // Find the Toolbar and set it as the activity's action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add the back button to the Toolbar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide the default title
        }

        db = FirebaseFirestore.getInstance();

        // Initialize UI elements (IDs must match activity_news.xml)
        newsTitle = findViewById(R.id.detailed_news_title);
        newsDescription = findViewById(R.id.detailed_news_description);
        newsImage = findViewById(R.id.detailed_news_image);
        newsDate = findViewById(R.id.detailed_news_date);

        // Get the announcement ID from the intent that launched this activity
        String announcementId = getIntent().getStringExtra("announcement_id");

        if (announcementId != null && !announcementId.isEmpty()) {
            loadAnnouncementDetails(announcementId);
        } else {
            Toast.makeText(this, "No announcement ID provided.", Toast.LENGTH_SHORT).show();
            finish(); // Close activity if no ID
        }
    }

    // Override this method to handle the back button click
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadAnnouncementDetails(String announcementId) {
        db.collection("announcements").document(announcementId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        NewsArticle announcement = documentSnapshot.toObject(NewsArticle.class);
                        if (announcement != null) {
                            newsTitle.setText(announcement.getTitle());
                            newsDescription.setText(announcement.getDescription());

                            // This is the key line to make links clickable
                            newsDescription.setMovementMethod(LinkMovementMethod.getInstance());

                            // Load image with Glide
                            if (announcement.getImageUrl() != null && !announcement.getImageUrl().isEmpty()) {
                                newsImage.setVisibility(View.VISIBLE);
                                Glide.with(NewsActivity.this)
                                        .load(announcement.getImageUrl())
                                        .placeholder(R.drawable.placeholder_image)
                                        .error(R.drawable.error_image)
                                        .into(newsImage);
                            } else {
                                newsImage.setVisibility(View.GONE);
                                Glide.with(NewsActivity.this).clear(newsImage);
                            }

                            // Format and set date
                            if (announcement.getPublishedOn() != null) {
                                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                                newsDate.setText("Published: " + dateFormat.format(announcement.getPublishedOn().toDate()));
                            } else {
                                newsDate.setText("Published: N/A");
                            }
                        } else {
                            Log.e(TAG, "Announcement data is null for ID: " + announcementId);
                            Toast.makeText(NewsActivity.this, "Announcement data is null.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Log.d(TAG, "No such document with ID: " + announcementId);
                        Toast.makeText(NewsActivity.this, "Announcement not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching announcement details", e);
                    Toast.makeText(NewsActivity.this, "Error loading details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}