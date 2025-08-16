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

    public String getUserId() {
        return userId;
    }

    @Override
    public String getFullName() {
        return name; // This method should return the 'name' field
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
    public void setComplaintId(String complaintId) {
        this.complaintId = complaintId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Complaint complaint = (Complaint) o;
        return Objects.equals(complaintId, complaint.complaintId) &&
                Objects.equals(userId, complaint.userId) &&
                Objects.equals(name, complaint.name) &&
                Objects.equals(details, complaint.details) &&
                Objects.equals(status, complaint.status) &&
                Objects.equals(timestamp, complaint.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(complaintId, userId, name, details, status, timestamp);
    }
}