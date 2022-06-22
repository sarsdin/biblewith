package com.example.androidclient.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeVm extends ViewModel {

    private final MutableLiveData<String> mText;

    public HomeVm() {
        mText = new MutableLiveData<>();
        mText.setValue("호옴");
    }

    public LiveData<String> getText() {
        return mText;
    }
}