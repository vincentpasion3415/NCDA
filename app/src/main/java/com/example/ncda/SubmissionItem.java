package com.example.ncda;

import java.util.Date;


public interface SubmissionItem {

    Date getTimestamp();


    String getId();


    String getFullName();
    String getStatus();


    @Override
    boolean equals(Object obj);
    @Override
    int hashCode();
}