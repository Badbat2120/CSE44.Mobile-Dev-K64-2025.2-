package com.example.litera.viewmodels;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.example.litera.repositories.UserRepository;

public class ViewModelFactory implements ViewModelProvider.Factory {
    private final UserRepository userRepository;

    public ViewModelFactory(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(ProfileUserViewModel.class)) {
            ProfileUserViewModel viewModel = new ProfileUserViewModel(userRepository);
            viewModel.fetchCurrentUser(); // Automatically fetch user when ViewModel is created
            return (T) viewModel;
        }
        throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass);
    }
}