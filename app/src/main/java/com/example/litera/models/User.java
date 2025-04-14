package com.example.litera.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User {
    private String id;
    private String name;
    private String email;
    private List<String> favourite;
    private List<String> continueReading;
    private String role;
    private String value;
    private String avatar;
    private Map<String, Integer> ratings;

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
        this.ratings = new HashMap<>();
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
        this.ratings = new HashMap<>();
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

    public Map<String, Integer> getRatings() {
        if (ratings == null) {
            ratings = new HashMap<>();
        }
        return ratings;
    }

    public void setRatings(Map<String, Integer> ratings) {
        this.ratings = ratings;
    }

    // Phương thức tiện ích để kiểm tra người dùng đã đọc sách hay chưa
    public boolean hasReadBook(String bookId) {
        return continueReading != null && continueReading.contains(bookId);
    }

    // Phương thức tiện ích để kiểm tra người dùng đã đánh giá sách hay chưa
    public boolean hasRatedBook(String bookId) {
        return ratings != null && ratings.containsKey(bookId);
    }

    // Phương thức để lấy đánh giá của người dùng cho một cuốn sách cụ thể
    public int getRatingForBook(String bookId) {
        if (ratings != null && ratings.containsKey(bookId)) {
            return ratings.get(bookId);
        }
        return 0; // 0 nghĩa là chưa đánh giá
    }

    // Phương thức để đặt đánh giá cho một cuốn sách
    public void setRatingForBook(String bookId, int rating) {
        if (ratings == null) {
            ratings = new HashMap<>();
        }
        ratings.put(bookId, rating);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", favourite=" + favourite +
                ", continueReading=" + continueReading +
                ", role='" + role + '\'' +
                ", value='" + value + '\'' +
                ", avatar='" + avatar + '\'' +
                ", ratings=" + ratings +
                '}';
    }
}