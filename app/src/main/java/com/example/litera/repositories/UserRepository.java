package com.example.litera.repositories;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.litera.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public UserRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    // Tạo tài liệu người dùng mới trong Firestore
    public void createUser(String name, String email, OnUserCreationListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not authenticated");
            return;
        }

        // Tạo một đối tượng User mới
        User newUser = new User(name, email);
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("favourite", new ArrayList<String>());
        user.put("continue", new ArrayList<String>()); // Trong Firestore là 'continue' chứ không phải 'continueReading'
        user.put("role", "user");
        user.put("value", "0");

        // Thêm người dùng vào collection "users"
        db.collection("users")
                .document(currentUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created with ID: " + currentUser.getUid());
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user document", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // Lấy thông tin người dùng từ Firestore
    public void getCurrentUser(OnUserFetchListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not authenticated");
            return;
        }

        db.collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            try {
                                User user = document.toObject(User.class);
                                if (user != null) {
                                    user.setId(document.getId());
                                    listener.onSuccess(user);
                                } else {
                                    listener.onFailure("Failed to parse user data");
                                }
                            } catch (Exception e) {
                                listener.onFailure("Error parsing user data: " + e.getMessage());
                            }
                        } else {
                            listener.onFailure("User document does not exist");
                        }
                    } else {
                        listener.onFailure("Error getting user: " + task.getException().getMessage());
                    }
                });
    }

    // Tìm người dùng bằng email
    public void getUserByEmail(String email, OnUserFetchListener listener) {
        db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                User user = document.toObject(User.class);
                                user.setId(document.getId());
                                listener.onSuccess(user);
                                return;
                            }
                        } else {
                            listener.onFailure("User not found with email: " + email);
                        }
                    } else {
                        listener.onFailure("Error getting user: " + task.getException().getMessage());
                    }
                });
    }

    // Cập nhật thông tin người dùng
    public void updateUser(User user, OnUserUpdateListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not authenticated");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        if (user.getName() != null) updates.put("name", user.getName());
        if (user.getFavourite() != null) updates.put("favourite", user.getFavourite());
        if (user.getContinueReading() != null) updates.put("continue", user.getContinueReading());
        if (user.getValue() != null) updates.put("value", user.getValue());
        if (user.getAvatar() != null) updates.put("avatar", user.getAvatar());

        db.collection("users")
                .document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User updated successfully");
                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error updating user", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // Đổi mật khẩu người dùng
    public void changePassword(String currentPassword, String newPassword, OnPasswordChangeListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onFailure("User not authenticated");
            return;
        }

        // Xác thực lại với mật khẩu hiện tại
        auth.signInWithEmailAndPassword(user.getEmail(), currentPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Đổi mật khẩu
                        user.updatePassword(newPassword)
                                .addOnCompleteListener(passwordTask -> {
                                    if (passwordTask.isSuccessful()) {
                                        listener.onSuccess();
                                    } else {
                                        listener.onFailure(passwordTask.getException().getMessage());
                                    }
                                });
                    } else {
                        listener.onFailure("Current password is incorrect");
                    }
                });
    }

    // Interface cho callbacks
    public interface OnUserCreationListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnUserFetchListener {
        void onSuccess(User user);
        void onFailure(String error);
    }

    public interface OnUserUpdateListener {
        void onSuccess();
        void onFailure(String error);
    }

    public interface OnPasswordChangeListener {
        void onSuccess();
        void onFailure(String error);
    }

    public void rateBook(String bookId, int rating, OnUserUpdateListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onFailure("User not authenticated");
            return;
        }

        // Lấy document user hiện tại
        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        // Kiểm tra xem có trường ratings chưa
                        Map<String, Object> ratings = (Map<String, Object>) document.get("ratings");
                        if (ratings == null) {
                            ratings = new HashMap<>();
                        }

                        // Cập nhật rating cho sách này
                        ratings.put(bookId, rating);

                        // Cập nhật vào Firestore
                        db.collection("users").document(currentUser.getUid())
                                .update("ratings", ratings)
                                .addOnSuccessListener(aVoid -> {
                                    listener.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    listener.onFailure("Failed to update rating: " + e.getMessage());
                                });
                    } else {
                        listener.onFailure("Failed to get user document");
                    }
                });
    }

    // Phương thức để kiểm tra xem người dùng đã đọc sách chưa
    public void checkUserHasReadBook(String bookId, OnBookReadCheckListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onResult(false);
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        // Lấy danh sách sách đang đọc
                        List<String> continueReading = (List<String>) document.get("continue");

                        // Kiểm tra xem bookId có trong danh sách không
                        boolean hasRead = continueReading != null && continueReading.contains(bookId);
                        listener.onResult(hasRead);
                    } else {
                        listener.onResult(false);
                    }
                });
    }

    // Phương thức để kiểm tra xem người dùng đã đánh giá sách chưa
    // Thêm annotation để loại bỏ cảnh báo unchecked
    @SuppressWarnings("unchecked")
    public void checkUserHasRatedBook(String bookId, OnBookRatingCheckListener listener) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            listener.onResult(false, 0);
            return;
        }

        db.collection("users").document(currentUser.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();

                        // Lấy map ratings
                        Map<String, Object> ratings = (Map<String, Object>) document.get("ratings");

                        // Kiểm tra xem bookId có trong ratings không
                        if (ratings != null && ratings.containsKey(bookId)) {
                            // Lấy giá trị rating
                            int rating = 0;
                            Object ratingObj = ratings.get(bookId);
                            if (ratingObj instanceof Long) {
                                rating = ((Long) ratingObj).intValue();
                            } else if (ratingObj instanceof Integer) {
                                rating = (Integer) ratingObj;
                            } else if (ratingObj instanceof Double) {
                                rating = ((Double) ratingObj).intValue();
                            }

                            listener.onResult(true, rating);
                        } else {
                            listener.onResult(false, 0);
                        }
                    } else {
                        listener.onResult(false, 0);
                    }
                });
    }

    // Interface cho việc kiểm tra sách đã đọc
    public interface OnBookReadCheckListener {
        void onResult(boolean hasRead);
    }

    // Interface cho việc kiểm tra đánh giá sách
    public interface OnBookRatingCheckListener {
        void onResult(boolean hasRated, int rating);
    }
}