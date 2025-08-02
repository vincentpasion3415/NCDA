// NCDAFaq.java
package com.example.ncda.model; // IMPORTANT: Make sure this package matches your setup

import java.util.List;

public class NCDAFaq {
    private String question;
    private String answer;
    private List<String> keywords;
    private String category; // Optional: for categorizing FAQs

    // No-argument constructor required for Firestore DataSnapshot.toObject()
    public NCDAFaq() {
    }

    public NCDAFaq(String question, String answer, List<String> keywords, String category) {
        this.question = question;
        this.answer = answer;
        this.keywords = keywords;
        this.category = category;
    }

    // Getters
    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getCategory() {
        return category;
    }


    public void setQuestion(String question) {
        this.question = question;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}