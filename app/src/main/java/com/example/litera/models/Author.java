package com.example.litera.models;

import com.google.firebase.firestore.PropertyName;

public class Author {
    private String id;
    private String name;
    private String imageUrl; // Sẽ được ánh xạ với trường "pfp" trong Firestore

    // Constructor rỗng cần thiết cho Firestore
    public Author() {
    }

    public Author(String name, String imageUrl) {
        this.name = name;
        this.imageUrl = imageUrl;
    }

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

    // Map imageUrl với trường pfp trong Firestore
    @PropertyName("pfp")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("pfp")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}