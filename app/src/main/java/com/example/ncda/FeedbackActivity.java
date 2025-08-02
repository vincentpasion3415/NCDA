package com.example.ncda;

import android.content.Context;
import android.content.DialogInterface; // Added for AlertDialog
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private EditText feedbackEditText;
    private RatingBar feedbackRatingBar;
    private Button submitFeedbackButton;
    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseFirestore db; // Declare Firestore instance
    private FirebaseAuth mAuth; // Declare FirebaseAuth instance for user ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // Initialize Firebase instances
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        db = FirebaseFirestore.getInstance(); // Initialize Firestore
        mAuth = FirebaseAuth.getInstance(); // Initialize FirebaseAuth

        // Initialize UI elements from layout
        feedbackEditText = findViewById(R.id.feedbackEditText);
        feedbackRatingBar = findViewById(R.id.feedbackRatingBar);
        submitFeedbackButton = findViewById(R.id.submitFeedbackButton);

        // Set up click listener for the submit button
        submitFeedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSubmitFeedback();
            }
        });
    }

    private void handleSubmitFeedback() {
        String feedbackText = feedbackEditText.getText().toString().trim();
        float rating = feedbackRatingBar.getRating();

        // Basic validation
        if (feedbackText.isEmpty()) {
            Toast.makeText(this, "Please enter your feedback.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rating == 0.0f) {
            Toast.makeText(this, "Please provide a star rating.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Log feedback to Logcat
        Log.d("FeedbackActivity", "Feedback Received:");
        Log.d("FeedbackActivity", "Text: " + feedbackText);
        Log.d("FeedbackActivity", "Rating: " + rating + " stars");

        // Prepare data for Firestore
        FirebaseUser currentUser = mAuth.getCurrentUser();
        String userId = "anonymous"; // Default to anonymous
        if (currentUser != null) {
            userId = currentUser.getUid();
        }

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("userId", userId);
        feedback.put("feedbackText", feedbackText);
        feedback.put("rating", rating);
        feedback.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp()); // Use server timestamp

        // Add the feedback to a Firestore collection
        // We'll use a collection named 'feedback'
        // For multi-user apps, you might store it in /artifacts/{appId}/public/data/feedback
        // or /artifacts/{appId}/users/{userId}/feedback for private user feedback.
        // For simplicity, let's assume a public collection here, but remember the security rules.
        db.collection("feedback")
                .add(feedback)
                .addOnSuccessListener(documentReference -> {
                    Log.d("FeedbackActivity", "Feedback document added with ID: " + documentReference.getId());
                    // Log analytics event for successful submission
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "feedback_submission_success");
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "user_feedback_sent");
                    bundle.putString("feedback_text_length", String.valueOf(feedbackText.length()));
                    bundle.putLong(FirebaseAnalytics.Param.VALUE, (long) rating);
                    mFirebaseAnalytics.logEvent("feedback_submitted_to_firestore", bundle);

                    showFeedbackConfirmationDialog(); // Show success message
                    feedbackEditText.setText(""); // Clear form
                    feedbackRatingBar.setRating(0.0f);
                })
                .addOnFailureListener(e -> {
                    Log.w("FeedbackActivity", "Error adding feedback document", e);
                    Toast.makeText(FeedbackActivity.this, "Failed to submit feedback: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Log analytics event for failed submission
                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "feedback_submission_failed");
                    bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "user_feedback_error");
                    bundle.putString("error_message", e.getMessage());
                    mFirebaseAnalytics.logEvent("feedback_submission_failed", bundle);
                });
    }

    /**
     * Shows a custom AlertDialog to confirm feedback submission.
     */
    private void showFeedbackConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dialogView = inflater.inflate(R.layout.dialog_feedback_confirmation, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button dialogOkButton = dialogView.findViewById(R.id.dialogOkButton);

        dialogTitle.setText("Thank You!");
        dialogMessage.setText("Your feedback has been successfully submitted. We appreciate your input!");

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false); // Make dialog non-cancelable by outside touch

        dialogOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                finish(); // Close FeedbackActivity after user acknowledges
            }
        });

        dialog.show();
    }
}