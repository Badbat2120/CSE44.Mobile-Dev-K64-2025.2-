package com.example.litera.models;

public class Author {
    private String name;
    private String imageUrl;

    // Default constructor for Firebase
    public Author() {
        // Required empty constructor for Firebase
    }

    public Author(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}