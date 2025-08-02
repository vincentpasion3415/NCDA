// Path: app/src/main/java/com.example.ncda/HomeFragment.java
package com.example.ncda; // Your main package

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.example.ncda.adapter.NewsAdapter;
import com.example.ncda.model.NewsArticle;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private FirebaseFirestore db;
    private RecyclerView announcementsRecyclerView;
    private NewsAdapter newsAdapter;
    private List<NewsArticle> announcementsList;
    private TextView noAnnouncementsMessage; // For the "No announcements found" text

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false); // This links to your fragment_home.xml

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerView and Adapter from the inflated view
        announcementsRecyclerView = view.findViewById(R.id.announcementsRecyclerView);
        // Set up the layout manager for the RecyclerView (vertical list)
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        announcementsList = new ArrayList<>(); // Initialize the list to hold news articles
        newsAdapter = new NewsAdapter(announcementsList); // Create the adapter with the list
        announcementsRecyclerView.setAdapter(newsAdapter); // Set the adapter to the RecyclerView

        // Initialize "No Announcements" message (optional, but good for user feedback)
        noAnnouncementsMessage = view.findViewById(R.id.no_announcements_message);
        if (noAnnouncementsMessage != null) {
            noAnnouncementsMessage.setVisibility(View.GONE); // Hidden by default
        }

        // Set up item click listener for the news articles
        newsAdapter.setOnItemClickListener(new NewsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(NewsArticle newsArticle) {
                Toast.makeText(getContext(), "Opening: " + newsArticle.getTitle(), Toast.LENGTH_SHORT).show();

                // Start NewsActivity to show detailed view of the clicked announcement
                Intent intent = new Intent(getContext(), NewsActivity.class);
                intent.putExtra("announcement_id", newsArticle.getId()); // Pass the document ID to NewsActivity
                startActivity(intent);
            }
        });

        // Load announcements from Firestore when the fragment view is created
        loadAnnouncements();

        // You can also include other UI elements and logic specific to your HomeFragment here
        // For example, if you have a welcome message or other dashboard components, add them to fragment_home.xml
        // and find them here using 'view.findViewById()':
        // TextView welcomeMessage = view.findViewById(R.id.welcome_message);
        // if (welcomeMessage != null) {
        //     welcomeMessage.setText("Welcome from HomeFragment!");
        // }

        return view; // Return the inflated view
    }

    // Method to fetch announcements from Firestore
    private void loadAnnouncements() {
        db.collection("announcements") // Access the "announcements" collection
                .orderBy("publishedOn", Query.Direction.DESCENDING) // Order by 'publishedOn' field, newest first
                .addSnapshotListener(new EventListener<QuerySnapshot>() { // Real-time listener
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed for announcements.", e);
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Error loading announcements.", Toast.LENGTH_SHORT).show();
                            }
                            return;
                        }

                        if (snapshots != null) {
                            List<NewsArticle> newAnnouncements = new ArrayList<>();
                            for (QueryDocumentSnapshot doc : snapshots) {
                                NewsArticle announcement = doc.toObject(NewsArticle.class); // Convert document to NewsArticle object
                                announcement.setId(doc.getId()); // Set the Firestore document ID to the NewsArticle object
                                newAnnouncements.add(announcement);
                            }
                            newsAdapter.updateNewsList(newAnnouncements); // Update the adapter with the new data
                            Log.d(TAG, "Announcements updated: " + newAnnouncements.size() + " items.");

                            // Logic to show/hide the "No announcements" message
                            if (noAnnouncementsMessage != null) {
                                if (newAnnouncements.isEmpty()) {
                                    noAnnouncementsMessage.setVisibility(View.VISIBLE);
                                } else {
                                    noAnnouncementsMessage.setVisibility(View.GONE);
                                }
                            }
                        }
                    }
                });
    }
}