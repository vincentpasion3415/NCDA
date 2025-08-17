package com.example.ncda.model;

public class GovernmentService {
    private String name;
    private String acronym; // Added new field
    private String hotline;
    private String email;
    private String website;

    // Updated constructor to accept the acronym
    public GovernmentService(String name, String acronym, String hotline, String email, String website) {
        this.name = name;
        this.acronym = acronym;
        this.hotline = hotline;
        this.email = email;
        this.website = website;
    }

    public String getName() {
        return name;
    }

    // New getter for the acronym
    public String getAcronym() {
        return acronym;
    }

    public String getHotline() {
        return hotline;
    }

    public String getEmail() {
        return email;
    }

    public String getWebsite() {
        return website;
    }
}