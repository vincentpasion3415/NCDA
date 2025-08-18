package com.example.ncda.model;

import com.example.ncda.SubmissionItem;
import com.google.firebase.firestore.DocumentId;
import java.util.Date;
import java.util.Objects;

public class Referral implements SubmissionItem  {
    @DocumentId
    private String id;
    private String personalName;
    private String pwdId;
    private String disability;
    private String serviceNeeded;
    private String remarks;
    private String status;
    private String userId;
    private Date timestamp;
    private String adminRemark;

    public Referral() {
        // Required empty public constructor for Firestore
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        this.personalName = personalName;
    }

    public String getPwdId() {
        return pwdId;
    }

    public void setPwdId(String pwdId) {
        this.pwdId = pwdId;
    }

    public String getDisability() {
        return disability;
    }

    public void setDisability(String disability) {
        this.disability = disability;
    }

    public String getServiceNeeded() {
        return serviceNeeded;
    }

    public void setServiceNeeded(String serviceNeeded) {
        this.serviceNeeded = serviceNeeded;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getAdminRemark() {
        return adminRemark;
    }

    public void setAdminRemark(String adminRemark) {
        this.adminRemark = adminRemark;
    }

    @Override
    public String getFullName() {
        return this.personalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Referral that = (Referral) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}