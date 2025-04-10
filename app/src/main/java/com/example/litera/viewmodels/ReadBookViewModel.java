package com.example.litera.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ReadBookViewModel extends ViewModel {

    private final MutableLiveData<String> bookContent = new MutableLiveData<>();

    public LiveData<String> getBookContent() {
        return bookContent;
    }

    public void loadBookContent(String content) {
        // Load book content (mock data or from repository)
        bookContent.setValue(content);
    }
}
