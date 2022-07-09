package com.example.androidclient.login;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidclient.MyApp;
import com.example.androidclient.R;
import com.example.androidclient.databinding.LoginMainFmBinding;
import com.example.androidclient.home.MainActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginMainFm extends Fragment {

    private LoginMainFmBinding binding;
    private LoginVm loginVm;
    private long backKeyPressedTime=0;
//    private Toast toast;

    public LoginMainFm() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = LoginMainFmBinding.inflate(inflater, container, false);
        loginVm = new ViewModelProvider(requireActivity()).get(LoginVm.class);
        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handleOnBackPressed(); //뒤로가기 종료 구현부

//        binding.loginLoginBt.setOnClickListener();


        // 로그인 할때 형식 검사
        TextWatcher emailWatcher = new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                Boolean 이메일형식맞나 = validateEmail();
            }
        };
        TextWatcher pwWatcher = new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                Boolean 비밀번호형식맞나 = validatePw();
                if ( 비밀번호형식맞나) {
                    binding.loginLoginBt.setEnabled(true); //로긴폼형식이 전체다 맞으면 로그인버튼 활성
                } else{
                    binding.loginLoginBt.setEnabled(false); //아니면 비활성
                }
            }
        };
        binding.loginMainEmailInput.addTextChangedListener(emailWatcher);
        binding.loginMainPwdInput.addTextChangedListener(pwWatcher);


        //회원가입 하기 버튼 클릭시
        binding.loginJoinBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(LoginMainFm.this).navigate(R.id.action_loginMainFm_to_joinFm);
            }
        });

        //비밀번호 찾기 버튼 클릭시
        binding.loginFindpwBt.setOnClickListener(v -> {
            NavHostFragment.findNavController(LoginMainFm.this).navigate(R.id.action_loginMainFm_to_findPwFm);

        });


        //로그인 버튼 클릭시
        binding.loginLoginBt.setOnClickListener(v -> {
//            HashMap<String, String> infoMap = new HashMap<String, String>();
//            infoMap.put("user_email", binding.loginMainEmailInput.getText().toString());
//            infoMap.put("user_pwd", binding.loginMainPwdInput.getText().toString());
//            infoMap.put("user_autologin", String.valueOf(binding.loginAutologinCkbox.isChecked()));
            LoginDto loginDto = new LoginDto(
                    binding.loginMainEmailInput.getText().toString(),
                    binding.loginMainPwdInput.getText().toString(),
                    binding.loginAutologinCkbox.isChecked());

            loginVm.로그인클릭(loginDto).enqueue(new Callback<LoginDto>() {
                @Override
                public void onResponse(Call<LoginDto> call, Response<LoginDto> response) {
                    if (response.isSuccessful()) {
                        LoginDto res = response.body();
                        if(res.getUser_email() != null || !res.getUser_email().equals("")){
                            MyApp.setUserInfo(res); //사용자 정보 MyApp 클래스에 저장
                            Log.e("[LoginMainFm]", "로그인클릭 onResponse: "+ MyApp.getUserInfo() );

                            Log.e("[LoginMainFm]", "로그인클릭 loginDto: "+ loginDto );
                            //자동로그인 체크라면 쉐어드에 자신의 이메일을 등록한다.
                            if (loginDto.isUser_autologin()) {
                                SharedPreferences sp = MyApp.getApplication().getSharedPreferences("autologin", Context.MODE_PRIVATE);
                                SharedPreferences.Editor spEditor = sp.edit();
                                sp.getString("user_email", "");
                                spEditor.putString("user_email", loginDto.getUser_email());
                                spEditor.apply();
                                Log.e("[LoginMainFm]", "로그인클릭 자동로그인 쉐어드 유저이메일: "+ sp.getString("user_email", "") );
                            }
                            Toast.makeText(getActivity(), "로그인하였습니다.", Toast.LENGTH_SHORT).show();
                            Intent toMain = new Intent(MyApp.getApplication(), MainActivity.class);
                            startActivity(toMain);
                            requireActivity().finish();

                        } else {
                            Toast.makeText(getActivity(), "로그인에 실패하였습니다. 아이디/비밀번호를 다시 확인해주세요.", Toast.LENGTH_SHORT).show();

                        }
                    }
                }
                @Override
                public void onFailure(Call<LoginDto> call, Throwable t) {
                    Log.e("[LoginMainFm]", "로그인클릭 onFailure: "+ t.getMessage() );
                }
            });



        });

    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        //자동로그인 체크 유무에 따른 자동로그인 처리
        SharedPreferences sp = MyApp.getApplication().getSharedPreferences("autologin", Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = sp.edit();
        String userEmail = sp.getString("user_email", ""  );
        if (!userEmail.equals("")) {
            Log.e("LoginMainFm", "userEmail: "+ userEmail );
            loginVm = new ViewModelProvider(requireActivity()).get(LoginVm.class);
            loginVm.유저정보가져오기(userEmail).enqueue(new Callback<LoginDto>() {
                @Override
                public void onResponse(Call<LoginDto> call, Response<LoginDto> response) {
                    Toast.makeText(getActivity(), "자동 로그인하였습니다.", Toast.LENGTH_SHORT).show();
                    if (response.isSuccessful()) {
                        LoginDto res = response.body();
                        MyApp.setUserInfo(res); //사용자 정보 MyApp 클래스에 저장

                        Intent toMain = new Intent(MyApp.getApplication(), MainActivity.class);
                        startActivity(toMain);
                        requireActivity().finish();
                    }
                }
                @Override
                public void onFailure(Call<LoginDto> call, Throwable t) {
                    Log.e("[LoginMainFm]", "유저정보가져오기 onFailure: "+ t.getMessage() );
                }
            });

        }
    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity)requireActivity()).getSupportActionBar().hide(); //액션바 없애기
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private Boolean validateEmail() {
        String value = String.valueOf(binding.loginMainEmailInput.getText());
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (value.isEmpty()) {
//            binding.editId.setError("이메일을 입력해주세요.");
            binding.loginMainEmailLayout.setError("이메일을 입력해주세요.");
            return false;
        } else if (!value.matches(emailPattern)){
//            binding.editId.setError("이메일 형식이 맞지 않습니다.");
            binding.loginMainEmailLayout.setError("이메일 형식이 맞지 않습니다.");
            return false;
        } else {
//            binding.editId.setError(null);
            binding.loginMainEmailLayout.setError(null);
            return true;
        }
    }
    private Boolean validatePw() {
        String value = String.valueOf(binding.loginMainPwdInput.getText());
        String pwPattern = "^.*(?=^.{8,20}$)(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[!@#$%^&+=]).*$";
        if (value.isEmpty()) {
//            binding.editPwd.setError("비밀번호를 입력해주세요.");
            binding.loginMainPwdLayout.setError("비밀번호를 입력해주세요.");
            return false;
        } else if (!value.matches(pwPattern)){
//            binding.editPwd.setError("비밀번호 형식이 맞지 않습니다.");
            binding.loginMainPwdLayout.setError("비밀번호 형식이 맞지 않습니다.");
            return false;
        } else {
//            binding.editPwd.setError(null);
            binding.loginMainPwdLayout.setError(null);
            return true;
        }
    }



    private void handleOnBackPressed(){ //뒤로가기 종료 구현부
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {//백버튼을 조각에서 조종하기 위한 메소드.
                        if(getParentFragmentManager().getBackStackEntryCount()==0){
                            뒤로가기종료();
                        } else{
                            NavController navcon = NavHostFragment.findNavController(LoginMainFm.this);
                            navcon.navigateUp();
                        }
                    }
                });
    }
    private void 뒤로가기종료(){
        if(System.currentTimeMillis() > backKeyPressedTime + 2000){//2초보다크다면 한번 보류
            backKeyPressedTime = System.currentTimeMillis();//2번째클릭시 종료를 위해 대입
            Toast.makeText(getActivity(), "뒤로 버튼을 한번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT).show();
            return;
        }

        if(System.currentTimeMillis() <= backKeyPressedTime + 2000){
            //현재시간이 이전클릭시점으로부터 2초안쪽이라면 종료함.
            getActivity().finish();
//            toast.cancel();
//            super.onBackPressed();
        }
    }


}