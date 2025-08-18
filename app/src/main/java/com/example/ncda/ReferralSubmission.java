package com.example.ncda;
public class ReferralSubmission {

    private String id;
    private String personalName;
    private String pwdId;
    private String disability;
    private String serviceNeeded;
    private String remarks;
    private String status;
    private String adminRemark;

    public ReferralSubmission(String id, String personalName, String pwdId, String disability, String serviceNeeded, String remarks, String status, String adminRemark) {
        this.id = id;
        this.personalName = personalName;
        this.pwdId = pwdId;
        this.disability = disability;
        this.serviceNeeded = serviceNeeded;
        this.remarks = remarks;
        this.status = status;
        this.adminRemark = adminRemark;
    }

    // Getters and Setters for all fields
    public String getId() { return id; }
    public String getPersonalName() { return personalName; }
    public String getPwdId() { return pwdId; }
    public String getDisability() { return disability; }
    public String getServiceNeeded() { return serviceNeeded; }
    public String getRemarks() { return remarks; }
    public String getStatus() { return status; }
    public String getAdminRemark() { return adminRemark; }
}