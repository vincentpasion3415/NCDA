package com.example.ncda;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.example.ncda.chatbot.adapter.NewsAdapter;
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

    // CHANGE THESE FROM CARDVIEW TO BUTTON
    private Button referralButton;
    private Button ncdareferralButton;
    private Button complaintButton; // 1. Add this new button variable

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        announcementsRecyclerView = view.findViewById(R.id.announcementsRecyclerView);
        announcementsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        announcementsList = new ArrayList<>();
        newsAdapter = new NewsAdapter(announcementsList);
        announcementsRecyclerView.setAdapter(newsAdapter);

        noAnnouncementsMessage = view.findViewById(R.id.no_announcements_message);
        if (noAnnouncementsMessage != null) {
            noAnnouncementsMessage.setVisibility(View.GONE);
        }

        // FIND THE BUTTON FOR "SUBMIT COMPLAINT" AND SET ITS LISTENER
        complaintButton = view.findViewById(R.id.btn_submit_complaint); // 2. Find the new button by its ID
        if (complaintButton != null) {
            complaintButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 3. Start the ComplaintActivity when the button is clicked
                    Intent intent = new Intent(getActivity(), ComplaintActivity.class);
                    startActivity(intent);
                }
            });
        }

        // FIND THE BUTTON FOR "START REFERRAL" AND SET ITS LISTENER
        ncdareferralButton = view.findViewById(R.id.btn_start_referral);
        if (ncdareferralButton != null) {
            ncdareferralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null) {
                        String userId = user.getUid();
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String name = documentSnapshot.getString("name");
                                        String pwdId = documentSnapshot.getString("pwdId");
                                        Intent intent = new Intent(getActivity(), NCDAReferralFormActivity.class);
                                        intent.putExtra("personalName", name);
                                        intent.putExtra("pwdId", pwdId);
                                        startActivity(intent);
                                    } else {
                                        startActivity(new Intent(getActivity(), NCDAReferralFormActivity.class));
                                        Toast.makeText(getContext(), "User data not found. Please fill in manually.", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    startActivity(new Intent(getActivity(), NCDAReferralFormActivity.class));
                                    Toast.makeText(getContext(), "Error retrieving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    Log.e(TAG, "Error retrieving user data", e);
                                });
                    } else {
                        startActivity(new Intent(getActivity(), NCDAReferralFormActivity.class));
                        Toast.makeText(getContext(), "User not logged in. Please sign in to pre-fill the form.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // FIND THE BUTTON FOR "EXPLORE SERVICES" AND SET ITS LISTENER
        referralButton = view.findViewById(R.id.referral_button);
        if (referralButton != null) {
            referralButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), ReferralActivity.class);
                    startActivity(intent);
                }
            });
        }

        newsAdapter.setOnItemClickListener(new NewsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(NewsArticle newsArticle) {
                Toast.makeText(getContext(), "Opening: " + newsArticle.getTitle(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getContext(), NewsActivity.class);
                intent.putExtra("announcement_id", newsArticle.getId());
                startActivity(intent);
            }
        });

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