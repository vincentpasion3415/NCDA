package com.example.ncda;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

public class Complaint implements SubmissionItem, Serializable {
    private String complaintId;
    private String userId;
    private String name;
    private String details;
    private String status;
    private Date timestamp;

    public Complaint() {
        // Required for Firestore
    }

    // Constructor with all fields
    public Complaint(String complaintId, String userId, String name, String details, String status, Date timestamp) {
        this.complaintId = complaintId;
        this.userId = userId;
        this.name = name;
        this.details = details;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters
    @Override
    public String getId() {
        return complaintId;
    }

    @Override // This override is now correct
    public String getUserId() {
        return userId;
    }

    @Override
    public String getFullName() {
        return name;
    }

    public String getDetails() {
        return details;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    // Setters
    @Override // ADD THIS
    public void setId(String id) {
        this.complaintId = id;
    }

    @Override // ADD THIS
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override // ADD THIS
    public void setStatus(String status) {
        this.status = status;
    }

    @Override // ADD THIS
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    // ... (equals and hashCode methods remain the same) ...
}