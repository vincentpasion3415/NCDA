
package com.example.ncda.chatbot;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.ncda.model.NCDAFaq;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NCDABot {

    private FirebaseFirestore db;
    private List<NCDAFaq> faqs;
    private final String FAQS_COLLECTION = "ncd_faqs";


    public interface OnFAQsLoadedListener {
        void onFAQsLoaded();
        void onFAQsLoadFailed(Exception e);
    }

    public NCDABot() {
        db = FirebaseFirestore.getInstance();
        faqs = new ArrayList<>();
    }


    public void loadFAQs(final OnFAQsLoadedListener listener) {
        db.collection(FAQS_COLLECTION)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        faqs.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            NCDAFaq faq = document.toObject(NCDAFaq.class);
                            faqs.add(faq);
                        }
                        System.out.println("NCDABot: FAQs loaded successfully: " + faqs.size() + " entries");
                        if (listener != null) {
                            listener.onFAQsLoaded();
                        }
                    } else {
                        System.err.println("NCDABot: Error loading FAQs: " + task.getException());
                        if (listener != null) {
                            listener.onFAQsLoadFailed(task.getException());
                        }
                    }
                });
    }

    public String getAnswer(String userQuery) {
        String lowerCaseQuery = userQuery.toLowerCase(Locale.ROOT);

        if (faqs.isEmpty()) {
            return "My knowledge base is not loaded yet. Please try again in a moment or check your internet connection.";
        }


        for (NCDAFaq faq : faqs) {
            if (faq.getKeywords() != null) {
                for (String keyword : faq.getKeywords()) {

                    if (lowerCaseQuery.contains(keyword.toLowerCase(Locale.ROOT))) {
                        return faq.getAnswer();
                    }
                }
            }
        }


        return "Sorry, I couldn't find a direct answer to your question about '" + userQuery + "'. Please rephrase your question, try different keywords, or contact NCDA directly for further assistance.";
    }
}