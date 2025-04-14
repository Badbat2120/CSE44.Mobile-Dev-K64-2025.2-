package com.example.litera.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.litera.models.User;
import com.example.litera.repositories.UserRepository;

public class ProfileUserViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ProfileUserViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void login(String email, String password) {
        isLoading.setValue(true);
        userRepository.loginWithEmail(email, password, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                userLiveData.postValue(user);
                isLoading.postValue(false);
            }

            @Override
            public void onFailure(String error) {
                ProfileUserViewModel.this.errorMessage.postValue(error);
                isLoading.postValue(false);
            }
        });
    }

    public void register(String email, String password, String name) {
        isLoading.setValue(true);
        userRepository.registerWithEmail(email, password, name, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                userLiveData.postValue(user);
                isLoading.postValue(false);
            }

            @Override
            public void onFailure(String errorMessage) {
                ProfileUserViewModel.this.errorMessage.postValue(errorMessage);
                isLoading.postValue(false);
            }
        });
    }

    public void fetchCurrentUser() {
        isLoading.setValue(true);
        userRepository.getCurrentUser(new UserRepository.OnUserFetchListener() {
            @Override
            public void onSuccess(User user) {
                userLiveData.postValue(user);
                isLoading.postValue(false);
            }

            @Override
            public void onFailure(String error) {
                userLiveData.postValue(null);
                errorMessage.postValue(error);
                isLoading.postValue(false);
            }
        });
    }

    public void logout() {
        userRepository.signOut();
        userLiveData.postValue(null);
    }

    public boolean isUserLoggedIn() {
        return userRepository.getCurrentUser() != null;
    }
}