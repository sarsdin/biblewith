package com.example.androidclient.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData

class HomeVm : ViewModel() {

    var mText = MutableLiveData<String>()
    val text: LiveData<String>
        get() = mText

    init {
//        mText = new MutableLiveData<>();
        mText.value = "home"
    }
}