package com.example.litera.models;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

public class Book {
    private String id;
    private String title;
    private String authorId;
    private String description;
    private String imageUrl;
    private String contentUrl;
    private String price;
    private String pricePhysic;
    private String rating;

    // Thay đổi kiểu dữ liệu để khớp với Firestore
    private String ratingCount;    // String trong Firestore (thay đổi từ Long)
    private String ratingAverage;  // String trong Firestore

    @Exclude
    private Author author;
    private boolean trending;

    // Constructor rỗng cần thiết cho Firestore
    public Book() {
        // Default constructor required for Firestore
        this.ratingCount = "0";    // Giá trị mặc định là "0" cho kiểu String
        this.rating = "0.0";
        this.ratingAverage = "0.0";
    }

    // Constructor với các tham số cơ bản
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
        this.ratingCount = "0";    // Thay đổi từ 0L sang "0"
        this.ratingAverage = "0.0";
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

    @PropertyName("author")
    public String getAuthorId() {
        return authorId;
    }

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

    @PropertyName("cover")
    public String getImageUrl() {
        return imageUrl;
    }

    @PropertyName("cover")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @PropertyName("content")
    public String getContentUrl() {
        return contentUrl;
    }

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

    // Sửa đổi getter và setter cho ratingCount với kiểu String
    public String getRatingCount() {
        return ratingCount != null ? ratingCount : "0";
    }

    public void setRatingCount(String ratingCount) {
        this.ratingCount = ratingCount != null ? ratingCount : "0";
    }

    // Getter và setter cho ratingAverage với kiểu String
    public String getRatingAverage() {
        return ratingAverage != null ? ratingAverage : "0.0";
    }

    public void setRatingAverage(String ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    @PropertyName("trending")
    public boolean isTrending() {
        return trending;
    }

    @PropertyName("trending")
    public void setTrending(boolean trending) {
        this.trending = trending;
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

    // Phương thức tiện ích để lấy rating dưới dạng float
    @Exclude
    public float getRatingAsFloat() {
        if (rating == null || rating.isEmpty()) {
            return 0.0f;
        }
        try {
            return Float.parseFloat(rating);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }

    // Phương thức tiện ích để lấy ratingCount dưới dạng int
    @Exclude
    public int getRatingCountAsInt() {
        if (ratingCount == null || ratingCount.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(ratingCount);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Phương thức tiện ích để lấy rating trung bình dưới dạng double
    @Exclude
    public double getRatingAverageAsDouble() {
        if (ratingAverage == null || ratingAverage.isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(ratingAverage);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}