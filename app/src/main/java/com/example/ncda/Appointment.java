// app/src/main/java/com/example/ncda/data/Appointment.java
package com.example.ncda;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date; // Use java.util.Date for Firestore Timestamp field

public class Appointment implements SubmissionItem {
    private String id; // Document ID from Firestore
    private String userId;
    private String fullName;
    private String contactNumber;
    private String emailAddress;
    private String pwdId;
    private String purpose;
    private String preferredDate; // Keep as String as per your form
    private String preferredTime; // Keep as String as per your form
    private String alternateDateTime; // Keep as String as per your form
    private String mode;
    private String region;
    private String office;
    private boolean interpreterNeeded;
    private boolean wheelchairAccessNeeded;
    private String specialRequests;
    private boolean agreement;
    private String status;
    @ServerTimestamp // Firestore annotation for server timestamp
    private Date timestamp; // Use Date to store Firestore Timestamp

    // No-argument constructor required for Firestore
    public Appointment() {}

    // Constructor with all fields for creating objects (optional, but good for testing)
    public Appointment(String id, String userId, String fullName, String contactNumber, String emailAddress, String pwdId, String purpose, String preferredDate, String preferredTime, String alternateDateTime, String mode, String region, String office, boolean interpreterNeeded, boolean wheelchairAccessNeeded, String specialRequests, boolean agreement, String status, Date timestamp) {
        this.id = id;
        this.userId = userId;
        this.fullName = fullName;
        this.contactNumber = contactNumber;
        this.emailAddress = emailAddress;
        this.pwdId = pwdId;
        this.purpose = purpose;
        this.preferredDate = preferredDate;
        this.preferredTime = preferredTime;
        this.alternateDateTime = alternateDateTime;
        this.mode = mode;
        this.region = region;
        this.office = office;
        this.interpreterNeeded = interpreterNeeded;
        this.wheelchairAccessNeeded = wheelchairAccessNeeded;
        this.specialRequests = specialRequests;
        this.agreement = agreement;
        this.status = status;
        this.timestamp = timestamp;
    }

    // --- Getters and Setters ---
    // You MUST have public getters and setters for all fields for Firestore to work
    // Make sure to generate them in your IDE (Alt+Insert or Cmd+N on a Mac, then "Getter and Setter").

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getPwdId() { return pwdId; }
    public void setPwdId(String pwdId) { this.pwdId = pwdId; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getPreferredDate() { return preferredDate; }
    public void setPreferredDate(String preferredDate) { this.preferredDate = preferredDate; }

    public String getPreferredTime() { return preferredTime; }
    public void setPreferredTime(String preferredTime) { this.preferredTime = preferredTime; }

    public String getAlternateDateTime() { return alternateDateTime; }
    public void setAlternateDateTime(String alternateDateTime) { this.alternateDateTime = alternateDateTime; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getOffice() { return office; }
    public void setOffice(String office) { this.office = office; }

    public boolean isInterpreterNeeded() { return interpreterNeeded; }
    public void setInterpreterNeeded(boolean interpreterNeeded) { this.interpreterNeeded = interpreterNeeded; }

    public boolean isWheelchairAccessNeeded() { return wheelchairAccessNeeded; }
    public void setWheelchairAccessNeeded(boolean wheelchairAccessNeeded) { this.wheelchairAccessNeeded = wheelchairAccessNeeded; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public boolean isAgreement() { return agreement; }
    public void setAgreement(boolean agreement) { this.agreement = agreement; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}