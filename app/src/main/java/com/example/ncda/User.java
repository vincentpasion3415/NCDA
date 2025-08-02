package com.example.ncda;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class User {
    public String name;
    public String email;
    public String password;
    public String mobileNumber;

    @PrimaryKey(autoGenerate = true)
    public int id;

    public User(String name, String email, String password, String mobileNumber) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.mobileNumber = mobileNumber;
    }
}