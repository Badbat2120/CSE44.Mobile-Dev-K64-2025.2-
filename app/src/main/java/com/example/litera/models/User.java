package com.example.litera.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String name;
    private String email;
    private List<String> favourite;
    private List<String> continueReading;
    private String role;
    private String value;
    private String avatar;

    // No-argument constructor cho Firestore
    public User() {
        // Required empty constructor for Firestore
    }

    // Constructor cho đăng ký người dùng mới
    public User(String name, String email) {
        this.name = name;
        this.email = email;
        this.favourite = new ArrayList<>();
        this.continueReading = new ArrayList<>();
        this.role = "user";
        this.value = "0";
    }

    // Constructor đầy đủ
    public User(String id, String name, String email, List<String> favourite,
                List<String> continueReading, String role, String value) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.favourite = favourite;
        this.continueReading = continueReading;
        this.role = role;
        this.value = value;
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getFavourite() {
        return favourite;
    }

    public void setFavourite(List<String> favourite) {
        this.favourite = favourite;
    }

    public List<String> getContinueReading() {
        return continueReading;
    }

    public void setContinueReading(List<String> continueReading) {
        this.continueReading = continueReading;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}