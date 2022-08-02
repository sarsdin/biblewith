package com.example.androidclient;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.androidclient.login.LoginDto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

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

    //객체를 깊은 복사하는 메소드 - 데이터주소까지 완전히 다른 객체가 된다
    public static Object deepCopy(Object o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(o);

        byte[] buff = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(buff);
        ObjectInputStream os = new ObjectInputStream(bais);
        Object copy = os.readObject();
        return copy;
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

    public static String getTime(String ui표시orData, String datetime){
        if (ui표시orData.equals("ui")) {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss");
            DateTimeFormatter out_format = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm");
            DateTimeFormatter out_format2 = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");
            DateTimeFormatter out_format3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime ldt = LocalDateTime.parse(datetime, format); //포멧에 맞는 형태의 문자열 날짜시간값을 받아와서 파싱함
//            LocalDateTime.now().toInstant();
//            ldt.toEpochSecond(ZoneOffset.UTC);
            String res_st = ldt.format(out_format);
            String res_st2 = ldt.format(out_format2);
            String res_st3 = ldt.format(out_format3);
            long currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)/*atZone(ZoneId.systemDefault()).toEpochSecond()*/;
            //zoneoffset의 구분은 중요하다. systemdefault zone으로 정하면 서울 시간을 기준으로 계산되고 utc기준이랑은 시차가 생기게 되니 주의해야한다.
            long mNow = System.currentTimeMillis();
            Date mDate = new Date(mNow);//1644034298
            long mDate_muter = mDate.toInstant().getEpochSecond();

            long second = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC)+2); //시간이 -가 되는 증세가 있음. 기기마다 시간계산이 미묘하게 달라서..+2초해줌
            long minute = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC))/60L;
            long hour = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC))/60/60;
            long day = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC))/60/60/24;
            long year = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC))/60/60/24/365;
//            Log.e("MyApp", "res_st: "+res_st );
//            Log.e("MyApp", "res_st2: "+res_st2 );
//            Log.e("MyApp", "res_st3: "+res_st3 );
//            Log.e("MyApp", "currentTime: "+currentTime );
//            Log.e("MyApp", "ldt.toEpochSecond(ZoneOffset.UTC): "+ldt.toEpochSecond(ZoneOffset.UTC) );
//            Log.e("MyApp", "현재시간 - 저장된 시간 (초): "+ second );
//            Log.e("MyApp", "현재시간 - 저장된 시간 (분): "+ minute);
//            Log.e("MyApp", "현재시간 - 저장된 시간 (시간): "+ hour);
//            Log.e("MyApp", "현재시간 - 저장된 시간 (일): "+ day );
//            Log.e("MyApp", "현재시간 - 저장된 시간 (년): "+ year );
            String res = "";
            //각 시간 기준을 못넘기면 그 이전 기준으로 계산된 시간을 리턴해줌
            if (minute < 1 ) {
                return second+"초전";
            } else if (hour < 1) {
                return minute+"분전";
            } else if (day < 1) {
                return hour+"시간전";
            } else if (year < 1) {
                return day+"일전";
            }
            return  res_st;

        } else if (ui표시orData.equals("data")) { //현재 시간을 "yyyy-MM-dd hh:mm:ss" 형태의 포맷으로 리턴해줌
            long mNow = System.currentTimeMillis();
            Date mDate = new Date(mNow);
//            mDate.toInstant().getEpochSecond();
            SimpleDateFormat mtime = new SimpleDateFormat();

            return new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(mDate);

        } else if (ui표시orData.equals(".")) { //현재 시간을 "yyyy-MM-dd hh:mm:ss" 형태의 포맷으로 리턴해줌
            long mNow = System.currentTimeMillis();
            Date mDate = new Date(mNow);
//            mDate.toInstant().getEpochSecond();
            SimpleDateFormat mtime = new SimpleDateFormat();

            return new SimpleDateFormat("yyyy.MM.dd").format(mDate);

        } else if (ui표시orData.equals(".ui")) {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss");
            DateTimeFormatter out_format = DateTimeFormatter.ofPattern("yyyy.MM.dd");
            LocalDateTime ldt = LocalDateTime.parse(datetime, format); //포멧에 맞는 형태의 문자열 날짜시간값을 받아와서 파싱함
            return  ldt.format(out_format);

        } else if(ui표시orData.equals("ui2")) {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss");
            DateTimeFormatter out_format = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm");
            DateTimeFormatter out_format2 = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm");
            DateTimeFormatter out_format3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime ldt = LocalDateTime.parse(datetime, format); //포멧에 맞는 형태의 문자열 날짜시간값을 받아와서 파싱함
//            LocalDateTime.now().toInstant();
//            ldt.toEpochSecond(ZoneOffset.UTC);
            String res_st = ldt.format(out_format);
            String res_st2 = ldt.format(out_format2);
            String res_st3 = ldt.format(out_format3);
            long currentTime = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)/*atZone(ZoneId.systemDefault()).toEpochSecond()*/;
            //zoneoffset의 구분은 중요하다. systemdefault zone으로 정하면 서울 시간을 기준으로 계산되고 utc기준이랑은 시차가 생기게 되니 주의해야한다.
            long mNow = System.currentTimeMillis();
            Date mDate = new Date(mNow);//1644034298
            long mDate_muter = mDate.toInstant().getEpochSecond();

            long second = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC)+2); //시간이 -가 되는 증세가 있음. 기기마다 시간계산이 미묘하게 달라서..+2초해줌
            long minute = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC))/60L;
            long hour = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC))/60/60;
            long day = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC))/60/60/24;
            long year = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC))/60/60/24/365;
//            Log.e("MyApp", "res_st: "+res_st );
//            Log.e("MyApp", "res_st2: "+res_st2 );
//            Log.e("MyApp", "res_st3: "+res_st3 );
//            Log.e("MyApp", "currentTime: "+currentTime );
//            Log.e("MyApp", "ldt.toEpochSecond(ZoneOffset.UTC): "+ldt.toEpochSecond(ZoneOffset.UTC) );
//            Log.e("MyApp", "현재시간 - 저장된 시간 (초): "+ second );
//            Log.e("MyApp", "현재시간 - 저장된 시간 (분): "+ minute);
//            Log.e("MyApp", "현재시간 - 저장된 시간 (시간): "+ hour);
//            Log.e("MyApp", "현재시간 - 저장된 시간 (일): "+ day );
//            Log.e("MyApp", "현재시간 - 저장된 시간 (년): "+ year );
            String res = "";
            //각 시간 기준을 못넘기면 그 이전 기준으로 계산된 시간을 리턴해줌
            if (minute < 1 ) {
                return second+"초 경과";
            } else if (hour < 1) {
                return minute+"분 경과";
            } else if (day < 1) {
                return hour+"시간 경과";
            } else if (year < 1) {
                return day+"일 경과";
            }
            return  res_st;

        }
        return "";
    }
}
