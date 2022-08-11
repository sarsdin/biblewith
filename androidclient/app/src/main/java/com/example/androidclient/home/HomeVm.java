package com.example.androidclient.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeVm extends ViewModel {

    public MutableLiveData<String> mText = new MutableLiveData<>();

    public HomeVm() {
//        mText = new MutableLiveData<>();
        mText.setValue("home");
    }

    public LiveData<String> getText() {
        return mText;
    }
}