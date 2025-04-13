package com.example.litera.models;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.PropertyName;

public class Author {
    private String id;       // ID của tác giả
    private String name;     // Tên tác giả
    private String imageUrl; // URL ảnh đại diện của tác giả (được ánh xạ với trường "pfp" trong Firestore)

    // Constructor mặc định cần thiết cho Firestore
    public Author() {
    }

    // Constructor để khởi tạo đối tượng Author
    public Author(String id, String name, String imageUrl) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
    }

    // Getter và Setter cho id
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // Getter và Setter cho name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getter và Setter cho imageUrl, ánh xạ với trường "pfp" trong Firestore
    @PropertyName("pfp")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("pfp")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Phương thức để lấy thông tin Author dưới dạng chuỗi (tùy chọn)
    @NonNull
    @Override
    public String toString() {
        return "Author{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
