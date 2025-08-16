package com.example.ncda;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button; // Add this import

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
    private TextView noAnnouncementsMessage;
    private Button referralButton; // Declared the button here

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize RecyclerView and Adapter from the inflated view
        announcementsRecyclerView = view.findViewById(R.id.announcementsRecyclerView);
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        announcementsList = new ArrayList<>();
        newsAdapter = new NewsAdapter(announcementsList);
        announcementsRecyclerView.setAdapter(newsAdapter);

        // Initialize "No Announcements" message
        noAnnouncementsMessage = view.findViewById(R.id.no_announcements_message);
        if (noAnnouncementsMessage != null) {
            noAnnouncementsMessage.setVisibility(View.GONE);
        }

        // ------------------ NEW CODE STARTS HERE ------------------
        // Initialize the referral button from the layout file
        referralButton = view.findViewById(R.id.referral_button);

        // Set an OnClickListener on the button
        if (referralButton != null) {
            referralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Create an Intent to start the ReferralActivity
                    Intent intent = new Intent(getActivity(), ReferralActivity.class);
                    startActivity(intent);
                }
            });
        }
        // ------------------ NEW CODE ENDS HERE ------------------

        // Set up item click listener for the news articles
        newsAdapter.setOnItemClickListener(new NewsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(NewsArticle newsArticle) {
                Toast.makeText(getContext(), "Opening: " + newsArticle.getTitle(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getContext(), NewsActivity.class);
                intent.putExtra("announcement_id", newsArticle.getId());
                startActivity(intent);
            }
        });

        // Load announcements from Firestore when the fragment view is created
        loadAnnouncements();

        return view;
    }

    private void loadAnnouncements() {
        db.collection("announcements")
                .orderBy("publishedOn", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
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
                                NewsArticle announcement = doc.toObject(NewsArticle.class);
                                announcement.setId(doc.getId());
                                newAnnouncements.add(announcement);
                            }
                            newsAdapter.updateNewsList(newAnnouncements);
                            Log.d(TAG, "Announcements updated: " + newAnnouncements.size() + " items.");

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