package com.example.androidclient;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.example.androidclient.login.LoginDto;

public class MyApp extends Application {

    public static Application application;
    public static LoginDto userInfo = null;
//    public static boolean isnaver = false;
    public static SharedPreferences sp;
    private static String TAG = "내앱정보";


    public void onCreate() {
        super.onCreate();
        MyApp.application = this;
    }

    public static SharedPreferences getDefaultSp() {
        return PreferenceManager.getDefaultSharedPreferences(application.getApplicationContext());
    }

    public static Application getApplication() {
        return application;
    }

    public static SharedPreferences getSp() {
        sp = MyApp.application.getSharedPreferences("emailcode", MODE_PRIVATE);
        return sp;
    }

    public static void setSp(SharedPreferences sp) {
        MyApp.sp = sp;
    }

    public static LoginDto getUserInfo() {
        return userInfo;
    }

    public static void setUserInfo(LoginDto userInfo) {
        MyApp.userInfo = userInfo;
    }
}
