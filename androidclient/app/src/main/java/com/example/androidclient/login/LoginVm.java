package com.example.androidclient.login;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.androidclient.util.Http;
import com.google.gson.JsonObject;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Retrofit;

public class LoginVm extends ViewModel {
    private LoginRepository loginRepository; //뷰모델팩토리에서 만들때 레포지토리 객체를 이미 만들어서 넣어서 옴.
    private String host = "15.165.174.226";

//    private MutableLiveData<LoginFormState> loginFormState = new MutableLiveData<>();//로그인 상태를 저장하는 Livedata(옵저버가 사용하는 단한개의 key만있는자료구조?orMap 이라고 보면 될듯)
//    private MutableLiveData<LoginResult> loginResult = new MutableLiveData<>();      //로그인 결과를 저장하는 Livedata
//    private MutableLiveData<JoinFormState> joinFormState = new MutableLiveData<>(); //회원가입 상태를 저장하는 LIvedata

    public LoginVm() {}

    //생성자
    public LoginVm(LoginRepository loginRepository) {  //뷰모델 생성시 저장소를 인수로 등록
        this.loginRepository = loginRepository;
        필수입력란확인.setValue(valid);
        비번찾기필수입력란확인.setValue(findPwValid);
    }

    //로그인 버튼 클릭시 서버와 통신검증
    public void login(String username, String password) {

    }

    public LoginRepository getLoginRepository() {//저장소 getter
        return loginRepository;
    }


    public MutableLiveData<Boolean[]> 필수입력란확인 = new MutableLiveData<>(); //필수 입력란 모두 입력했는지 체크 - 4칸 모두 true이면 회원가입 버튼 활성화
    Boolean[] valid = new Boolean[4];
    public MutableLiveData<Boolean[]> 비번찾기필수입력란확인 = new MutableLiveData<>(); //필수 입력란 모두 입력했는지 체크 - 2칸 모두 true이면 비번찾기 버튼 활성화
    Boolean[] findPwValid = new Boolean[2];



    public Call<JsonObject> 이메일중복검사통신(String email) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpLogin httpLogin = retrofit.create(Http.HttpLogin.class); //로그인을 위한 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<JsonObject> call = httpLogin.isEmailRedundant(email);        //call로 비동기 통신 가능하다.
        return call;
    }

    public Call<JsonObject> 회원가입(HashMap<String, String> info) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpLogin httpLogin = retrofit.create(Http.HttpLogin.class); //로그인을 위한 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<JsonObject> call = httpLogin.joinComplete(
                info.get("user_email"),
                info.get("user_pwd"),
                info.get("user_pwdc"),
                info.get("user_nick"),
                info.get("user_name")
        );
        return call;
    }

    public Call<JsonObject> 비번찾기인증번호발송클릭(HashMap<String, String> info) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpLogin httpLogin = retrofit.create(Http.HttpLogin.class); //로그인을 위한 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<JsonObject> call = httpLogin.findpwMailSend(
                info.get("user_name"),
                info.get("user_email")
        );
        return call;
    }

    public Call<JsonObject> 인증번호확인클릭(HashMap<String, String> info) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpLogin httpLogin = retrofit.create(Http.HttpLogin.class); //로그인을 위한 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<JsonObject> call = httpLogin.findpwMailVnumConfirm(
                info.get("findpw_number"),
                info.get("name"),
                info.get("email")
        );
        return call;
    }


    public Call<JsonObject> 새비밀번호변경완료클릭(HashMap<String, String> info) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpLogin httpLogin = retrofit.create(Http.HttpLogin.class); //로그인을 위한 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<JsonObject> call = httpLogin.findpwNewPw(
                info.get("user_email"),
                info.get("user_pwd")
        );
        return call;
    }

    public Call<LoginDto> 로그인클릭(LoginDto dto) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpLogin httpLogin = retrofit.create(Http.HttpLogin.class); //로그인을 위한 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<LoginDto> call = httpLogin.login(
//                info.get("user_email"),
//                info.get("user_pwd"),
//                info.get("user_autologin")
                dto.getUser_email(), dto.getUser_pwd(), dto.isUser_autologin()

        );
        return call;
    }

    public Call<LoginDto> 유저정보가져오기(String user_email) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpLogin httpLogin = retrofit.create(Http.HttpLogin.class); //로그인을 위한 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<LoginDto> call = httpLogin.getAutoLoginInfo(user_email);
        return call;
    }


}