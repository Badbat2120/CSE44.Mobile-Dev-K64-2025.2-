package com.example.litera.views.activities.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.views.activities.model.User;

public class ProfileUserViewModel extends ViewModel {

    private final MutableLiveData<User> user = new MutableLiveData<>();

    public LiveData<User> getUser() {
        return user;
    }

    public void loadUser() {
        // Mock user data
        user.setValue(new User("John Doe", "johndoe@example.com"));
    }
}