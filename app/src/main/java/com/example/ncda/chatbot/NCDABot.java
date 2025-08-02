package com.example.ncda.chatbot;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.ncda.model.NCDAFaq;

import android.util.Log; // Import Log for debugging

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NCDABot {

    private static final String TAG = "NCDABot"; // Tag for Logcat messages

    private FirebaseFirestore db;
    private List<NCDAFaq> faqs;
    private final String FAQS_COLLECTION = "ncda_knowledge_base";


    public interface OnFAQsLoadedListener {
        void onFAQsLoaded();
        void onFAQsLoadFailed(Exception e);
    }

    public NCDABot() {
        db = FirebaseFirestore.getInstance();
        faqs = new ArrayList<>();
    }


    public void loadFAQs(final OnFAQsLoadedListener listener) {
        Log.d(TAG, "Attempting to load FAQs from collection: " + FAQS_COLLECTION); // Debug log
        db.collection(FAQS_COLLECTION)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        faqs.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            NCDAFaq faq = document.toObject(NCDAFaq.class);
                            faqs.add(faq);
                            Log.d(TAG, "Loaded FAQ: Question='" + faq.getQuestion() + "', Answer='" + faq.getAnswer() + "', Keywords=" + faq.getKeywords()); // Debug log for each loaded FAQ
                        }
                        Log.i(TAG, "FAQs loaded successfully: " + faqs.size() + " entries"); // Changed to Log.i for info
                        if (listener != null) {
                            listener.onFAQsLoaded();
                        }
                    } else {
                        Log.e(TAG, "Error loading FAQs: " + task.getException().getMessage(), task.getException()); // Changed to Log.e for error
                        if (listener != null) {
                            listener.onFAQsLoadFailed(task.getException());
                        }
                    }
                });
    }

    public String getAnswer(String userQuery) {
        String lowerCaseQuery = userQuery.toLowerCase(Locale.ROOT);
        Log.d(TAG, "User query received: '" + userQuery + "', Lowercase query: '" + lowerCaseQuery + "'"); // Debug log

        if (faqs.isEmpty()) {
            Log.w(TAG, "FAQs list is empty when getAnswer is called."); // Warning log
            return "My knowledge base is not loaded yet. Please try again in a moment or check your internet connection.";
        }

        for (NCDAFaq faq : faqs) {
            // Log the FAQ being checked and its keywords
            Log.d(TAG, "Checking FAQ: Question='" + faq.getQuestion() + "'");
            if (faq.getKeywords() != null) {
                for (String keyword : faq.getKeywords()) {
                    String lowerCaseKeyword = keyword.toLowerCase(Locale.ROOT);
                    Log.d(TAG, "  Checking keyword: '" + keyword + "' (lowercase: '" + lowerCaseKeyword + "') against query: '" + lowerCaseQuery + "'"); // Debug log

                    if (lowerCaseQuery.contains(lowerCaseKeyword)) {
                        Log.i(TAG, "  Match found! Keyword: '" + keyword + "' in query: '" + userQuery + "'"); // Info log
                        return faq.getAnswer();
                    }
                }
            } else {
                Log.w(TAG, "  FAQ has null keywords list: Question='" + faq.getQuestion() + "'"); // Warning log if keywords are null
            }
        }

        Log.i(TAG, "No direct answer found for query: '" + userQuery + "'"); // Info log
        return "Sorry, I couldn't find a direct answer to your question about '" + userQuery + "'. Please rephrase your question, try different keywords, or contact NCDA directly for further assistance.";
    }
}
