package com.example.litera.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class Book {
    private String id;
    private String title;
    private String authorId;  // ID của tác giả trong database
    private String description;
    private String imageUrl;
    private String contentUrl;
    private String price;
    private String pricePhysic; // Thêm trường mới này
    private String rating;

    // Đối tượng Author đầy đủ - được đánh dấu @Exclude để Firestore bỏ qua khi serialize/deserialize
    @Exclude
    private Author author;

    public Book() {
        // Constructor rỗng cần thiết cho Firestore
    }

    public Book(String id, String title, String authorId, String description, String imageUrl,
                String price, String pricePhysic, String rating) {
        this.id = id;
        this.title = title;
        this.authorId = authorId;
        this.description = description;
        this.imageUrl = imageUrl;
        this.price = price;
        this.pricePhysic = pricePhysic;
        this.rating = rating;
    }

    // Getters và setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // Đổi tên phương thức getter để phù hợp với trường trong Firestore
    @PropertyName("author")
    public String getAuthorId() {
        return authorId;
    }

    // Đổi tên phương thức setter để phù hợp với trường trong Firestore
    @PropertyName("author")
    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Đổi tên getter/setter cho imageUrl để phù hợp với trường 'cover' trong Firestore
    @PropertyName("cover")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("cover")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    // Đánh dấu rằng getter này map với trường "content" trong Firebase
    @PropertyName("content")
    public String getContentUrl() {
        return contentUrl;
    }

    // Đánh dấu rằng setter này nhận giá trị từ trường "content" trong Firebase
    @PropertyName("content")
    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    // Thêm getter và setter cho pricePhysic
    public String getPricePhysic() {
        return pricePhysic;
    }

    public void setPricePhysic(String pricePhysic) {
        this.pricePhysic = pricePhysic;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    // Getter và setter cho đối tượng Author
    @Exclude
    public Author getAuthor() {
        return author;
    }

    @Exclude
    public void setAuthor(Author author) {
        this.author = author;
    }

    // Thêm getter/setter cho trường trending nếu cần
    private boolean trending;

    @PropertyName("trending")
    public boolean isTrending() {
        return trending;
    }

    @PropertyName("trending")
    public void setTrending(boolean trending) {
        this.trending = trending;
    }
}