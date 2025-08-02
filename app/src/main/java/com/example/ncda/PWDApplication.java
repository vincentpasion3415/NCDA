// app/src/main/java/com/example/ncda/data/PWDApplication.java
package com.example.ncda;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date; // Use java.util.Date for Firestore Timestamp field

public class PWDApplication implements SubmissionItem {
    private String id; // Document ID from Firestore
    private String address;
    private String applicationType; // "New Application" or "Renewal"
    private String birthDate; // Keep as String as per your form
    private String contactNumber;
    private String disabilityDescription;
    private String disabilityType;
    private String documentUrl; // The URL to the uploaded document, if any
    private String fullName;
    private String gender;
    private String guardianContact;
    private String guardianName;
    private String previousPWDId;
    private String userId; // Assuming you save userId with PWD applications as well
    private String status; // We'll add a default "Pending" or similar if not present
    @ServerTimestamp
    private Date timestamp; // To sort by submission time

    // No-argument constructor required for Firestore
    public PWDApplication() {}

    // Constructor with all fields (optional)
    public PWDApplication(String id, String address, String applicationType, String birthDate, String contactNumber, String disabilityDescription, String disabilityType, String documentUrl, String fullName, String gender, String guardianContact, String guardianName, String previousPWDId, String userId, String status, Date timestamp) {
        this.id = id;
        this.address = address;
        this.applicationType = applicationType;
        this.birthDate = birthDate;
        this.contactNumber = contactNumber;
        this.disabilityDescription = disabilityDescription;
        this.disabilityType = disabilityType;
        this.documentUrl = documentUrl;
        this.fullName = fullName;
        this.gender = gender;
        this.guardianContact = guardianContact;
        this.guardianName = guardianName;
        this.previousPWDId = previousPWDId;
        this.userId = userId;
        this.status = status;
        this.timestamp = timestamp;
    }

    // --- Getters and Setters ---
    // You MUST have public getters and setters for all fields for Firestore
    // Generate all of them here similar to the Appointment class.

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getApplicationType() { return applicationType; }
    public void setApplicationType(String applicationType) { this.applicationType = applicationType; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getDisabilityDescription() { return disabilityDescription; }
    public void setDisabilityDescription(String disabilityDescription) { this.disabilityDescription = disabilityDescription; }

    public String getDisabilityType() { return disabilityType; }
    public void setDisabilityType(String disabilityType) { this.disabilityType = disabilityType; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getGuardianContact() { return guardianContact; }
    public void setGuardianContact(String guardianContact) { this.guardianContact = guardianContact; }

    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }

    public String getPreviousPWDId() { return previousPWDId; }
    public void setPreviousPWDId(String previousPWDId) { this.previousPWDId = previousPWDId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}