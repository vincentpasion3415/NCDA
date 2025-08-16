package com.example.ncda.model;

public class GovernmentService {
    private String name;
    private String hotline;
    private String email;
    private String website;

    public GovernmentService(String name, String hotline, String email, String website) {
        this.name = name;
        this.hotline = hotline;
        this.email = email;
        this.website = website;
    }

    public String getName() {
        return name;
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