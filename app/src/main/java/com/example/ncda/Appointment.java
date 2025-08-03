package com.example.ncda;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.Objects;
import java.io.Serializable;

public class Appointment implements SubmissionItem, Serializable {
    private String id;
    private String userId;

    // New fields to handle the old Firestore document structure
    private String firstName;
    private String middleName;
    private String lastName;
    private String appointmentType;
    private String note; // <-- Added this field

    // Fields for the new Firestore document structure
    private String fullName;
    private String purpose;

    private String contactNumber;
    private String emailAddress;
    private String pwdId;
    private String preferredDate;
    private String preferredTime;
    private String alternateDateTime;
    private String mode;
    private String region;
    private String office;
    private boolean interpreterNeeded;
    private boolean wheelchairAccessNeeded;
    private String specialRequests;
    private boolean agreement;
    private String status;
    @ServerTimestamp
    private Date timestamp;

    public Appointment() {}

    // --- Getters and Setters for the new fields ---
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAppointmentType() { return appointmentType; }
    public void setAppointmentType(String appointmentType) { this.appointmentType = appointmentType; }

    // Getter and Setter for the new 'note' field
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    // --- Existing Getters and Setters ---
    @Override
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getContactNumber() { return contactNumber; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getPwdId() { return pwdId; }
    public void setPwdId(String pwdId) { this.pwdId = pwdId; }

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

    public String getSpecialRequests() {
        // Check for the new field first
        if (this.specialRequests != null && !this.specialRequests.isEmpty()) {
            return this.specialRequests;
        }
        // If the new field is null, check for the old 'note' field
        else if (this.note != null && !this.note.isEmpty()) {
            return this.note;
        }
        return null;
    }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public boolean isAgreement() { return agreement; }
    public void setAgreement(boolean agreement) { this.agreement = agreement; }

    @Override
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Appointment that = (Appointment) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(userId, that.userId) &&
                Objects.equals(fullName, that.fullName) &&
                Objects.equals(contactNumber, that.contactNumber) &&
                Objects.equals(emailAddress, that.emailAddress) &&
                Objects.equals(pwdId, that.pwdId) &&
                Objects.equals(purpose, that.purpose) &&
                Objects.equals(preferredDate, that.preferredDate) &&
                Objects.equals(preferredTime, that.preferredTime) &&
                Objects.equals(alternateDateTime, that.alternateDateTime) &&
                Objects.equals(mode, that.mode) &&
                Objects.equals(region, that.region) &&
                Objects.equals(office, that.office) &&
                interpreterNeeded == that.interpreterNeeded &&
                wheelchairAccessNeeded == that.wheelchairAccessNeeded &&
                Objects.equals(specialRequests, that.specialRequests) &&
                agreement == that.agreement &&
                Objects.equals(status, that.status) &&
                Objects.equals(timestamp, that.timestamp);
    }
}