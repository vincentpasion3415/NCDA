package com.example.ncda;

import java.io.Serializable;
import java.util.Date;

public interface SubmissionItem extends Serializable {

    String getId();
    void setId(String id);

    String getStatus();
    void setStatus(String status);

    String getUserId();
    void setUserId(String userId);

    Date getTimestamp();
    void setTimestamp(Date timestamp);

    String getFullName();
}