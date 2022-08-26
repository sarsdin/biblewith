package com.example.androidclient.login;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import androidx.navigation.NavDeepLinkBuilder;
import androidx.navigation.NavDeepLinkRequest;
import androidx.navigation.Navigation;
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
    public LoginActivity activity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = LoginMainFmBinding.inflate(inflater, container, false);
        loginVm = new ViewModelProvider(requireActivity()).get(LoginVm.class);

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        자동로그인및초대링크처리(view, (LoginActivity) requireActivity());
        handleOnBackPressed(); //뒤로가기 종료 구현부

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

//    public void 자동로그인및초대링크처리(View view , LoginActivity activity){
    public void 자동로그인및초대링크처리(Context context){
//        Intent intent = requireActivity().getIntent();
//        String action = intent.getAction();
//        Uri data = intent.getData();
//        Log.e("LoginMainFm", "deepLinkInfo: "+ action+", "+data ); //android.intent.action.VIEW, http://biblewith.com/invite/1
//        if(data != null){
//            Log.e("LoginMainFm", "getPath: "+ data.getPath() );                             //  /invite/1
//            Log.e("LoginMainFm", "getPathSegments: "+ data.getPathSegments() );             // [invite, 1]
//            Log.e("LoginMainFm", "getLastPathSegment: "+ data.getLastPathSegment() );       // 1
//            Log.e("LoginMainFm", "getQuery: "+ data.getQuery() );                           // null
//            Log.e("LoginMainFm", "getScheme: "+ data.getScheme() );                         // http
//            Log.e("LoginMainFm", "getSchemeSpecificPart: "+ data.getSchemeSpecificPart() ); //  //biblewith.com/invite/1
//            Log.e("LoginMainFm", "getFragment: "+ data.getFragment() );                     // null
//            Log.e("LoginMainFm", "getHost: "+ data.getHost() );                             // biblewith.com
//            Log.e("LoginMainFm", "getQuery: "+ data.getQuery() );                             //
//        }

        //자동로그인 체크 유무에 따른 자동로그인 처리
        SharedPreferences sp = MyApp.getApplication().getSharedPreferences("autologin", Context.MODE_PRIVATE);
        SharedPreferences.Editor spEditor = sp.edit();
        String userEmail = sp.getString("user_email", ""  );

//        this.activity = activity;
        this.activity = (LoginActivity) context;
        if (!userEmail.equals("")) {
            Log.e("LoginMainFm", "userEmail: "+ userEmail );
            loginVm = new ViewModelProvider(requireActivity()).get(LoginVm.class);
            loginVm.유저정보가져오기(userEmail).enqueue(new Callback<LoginDto>() {
                @Override
                public void onResponse(Call<LoginDto> call, Response<LoginDto> response) {
//                    Toast.makeText((LoginActivity)context, "자동 로그인하였습니다.", Toast.LENGTH_SHORT).show();
                    Toast.makeText((LoginActivity)activity, "자동 로그인하였습니다.", Toast.LENGTH_SHORT).show();
                    if (response.isSuccessful()) {
                        LoginDto res = response.body();
                        MyApp.setUserInfo(res); //사용자 정보 MyApp 클래스에 저장

                        //자동로그인이 성공하고
                        //외부에서 모임초대 딥링크를 타고 들어왔을때 처리
//                        if(data != null && data.getPathSegments().get(0).equals("invite")){
//                            Intent toMain = new Intent(MyApp.getApplication(), MainActivity.class);
//                            toMain.putExtra("group_no", data==null? "":data.getPathSegments().get(1)); //getPathSegments: [invite, 1, c, 660998]
//                            toMain.putExtra("invite_code", data==null? "":data.getPathSegments().get(3));
//                            //초대링크를 타고 들어왔냐는 시그널용도 - 모임 홈에서 viewmodel 처리후 이 시그널에 따른 로직을 처리 후(서버에 invite_code 검증 후 멤버추가)
//                            // 위의 group_no를 이용해 해당 모임으로이동
//                            toMain.putExtra("is_invited", true);
//                            startActivity(toMain);
////                            Uri uri = Uri.parse("http://biblewith.com/groupfm/" + data.getPathSegments().get(1) +
////                                    "/c/" + data.getPathSegments().get(3) + "?dest=group_fm");
//
//                            //딥링크타고 들어온게 아니면 그냥 정상적으로 일반로직 진행
//                        } else {
                            Intent toMain = new Intent(MyApp.getApplication(), MainActivity.class);
                            startActivity(toMain);
//                        }
                        requireActivity().finish();
                    }
                }
                @Override
                public void onFailure(Call<LoginDto> call, Throwable t) {
                    Log.e("[LoginMainFm]", "유저정보가져오기 onFailure: "+ t.getMessage() );
                }
            });

            //자동로그인 상태가 아니라면 == 사용자가 회원이 아니거나, 자동로그인 체크 안해서 '로그인 해야 하는 상황' 이라면..
        } else {
//            Toast.makeText(requireActivity(), "로그인을 해주세요. 회원이 아니라면 회원가입을 진행해주세요.",Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        자동로그인및초대링크처리(context);
//        자동로그인및초대링크처리(view, (LoginActivity) requireActivity());

    }

    @Override
    public void onStart() {
        super.onStart();
        ((AppCompatActivity)requireActivity()).getSupportActionBar().hide(); //액션바 없애기
    }

    @Override
    public void onResume() {
        super.onResume();
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





/*
//  이런 식으로 아래와 같이 2개의 activity 로 나눠져 있을 경우 context 스위칭 문제가 생겨서 정작 navigation으로 이동은 했지만
// 화면이 안보이는 증상이 발생한다. 이것은 LoginActivity 소속의 navhost navigation의 딥링크를 이용해서 이동했기 때문에 MainActivity는
// passing(?) 당하는 상황이 생기게 되어 그런 것이다. 즉, MainActivity 의 context 대신 LoginActivity의 context를 이용하여 페이지(fm)로 이동
//  했기 때문에 이동한 페이지(ex: main navi 소속의 group_fm) 에서 MainActivity를 찾지 못해서 발생한 증상인 것이다.
// 다른 방법이 있는지 모르겠지만(ex:context 스위칭하는 방법) 현재로는 intent 를 이용해 이동한 후 MainActivity에서 bundle안의 시그널을 가져와 처리하는
// 방식을 사용하여 모임 페이지로 이동하는 방법을 사용해야 할 듯하다.

if (response.isSuccessful()) {
        LoginDto res = response.body();
        MyApp.setUserInfo(res); //사용자 정보 MyApp 클래스에 저장

        //자동로그인이 성공하고
        //외부에서 모임초대 딥링크를 타고 들어왔을때 처리
        requireActivity().runOnUiThread(new Runnable() {
@Override
public void run() {
        if(data != null && data.getPathSegments().get(0).equals("invite")){
        Bundle bd = new Bundle();
        bd.putString("group_no", data==null? "":data.getPathSegments().get(1)); //getPathSegments: [invite, 1, c, 660998]
        bd.putString("invite_code", data==null? "":data.getPathSegments().get(3));
        //초대링크를 타고 들어왔냐는 시그널용도 - 모임 홈에서 viewmodel 처리후 이 시그널에 따른 로직을 처리 후(서버에 invite_code 검증 후 멤버추가)
        // 위의 group_no를 이용해 해당 모임으로이동
        bd.putBoolean("is_invited", true);
        Uri uri = Uri.parse("http://biblewith.com/groupfm/" + data.getPathSegments().get(1) +
        "/c/" + data.getPathSegments().get(3) + "?dest=group_fm");
        NavDeepLinkRequest request = NavDeepLinkRequest.Builder
        //                                .fromUri(Uri.parse("android-app://androidx.navigation.app/profile"))
        .fromUri(uri)
        .build();

        //                            Navigation.findNavController(activity, R.id.login_navi_fragment); //<< 이건 안됨..activity 나 view를 못찾음
        //                            NavController nc = Navigation.findNavController(view); //<< 이건 안됨..activity 나 view를 못찾음

        NavController nc = NavHostFragment.findNavController(LoginMainFm.this);
        //                            nc.navigate(uri);
        nc.navigate(request);

        //딥링크타고 들어온게 아니면 그냥 정상적으로 일반로직 진행
        } else {
        Intent toMain = new Intent(MyApp.getApplication(), MainActivity.class);
        startActivity(toMain);
        }
        requireActivity().finish();
        }
        });

        }


명시적 딥링크 << 이것도 안됨.. 이건 에러는 안나도 화면이 안뜸
                           new NavDeepLinkBuilder(MyApp.getApplication())
                                    .setGraph(R.navigation.main_navi)
                                    .setDestination(R.id.home_fm)
                                    .setArguments(bd)
                                    .setComponentName(MainActivity.class)
                                    .createPendingIntent();

                            NavHostFragment.findNavController(LoginMainFm.this).createDeepLink()
                                    .setComponentName(MainActivity.class)
                                    .setGraph(R.navigation.main_navi)
                                    .setDestination(R.id.home_fm)
//                                    .setArguments(bd)
                                    .createPendingIntent();


*/
