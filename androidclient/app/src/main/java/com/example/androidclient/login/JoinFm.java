package com.example.androidclient.login;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidclient.BuildConfig;
import com.example.androidclient.MyApp;
import com.example.androidclient.R;
import com.example.androidclient.databinding.LoginJoinFmBinding;
import com.example.androidclient.util.GmailSender;
import com.google.gson.JsonObject;

import java.util.HashMap;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JoinFm extends Fragment {

    private LoginJoinFmBinding binding;
    private LoginVm loginVm;
    SharedPreferences sp = MyApp.getDefaultSp();
    SharedPreferences.Editor spEditor = sp.edit();

    public JoinFm() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = LoginJoinFmBinding.inflate(inflater, container, false);
        loginVm = new ViewModelProvider(requireActivity()).get(LoginVm.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //회원가입 시 형식 유효성 검사를 위한 텍스트 와쳐 생성 및 실행
        validationCheck();

        //인증번호 발송 버튼 클릭시 회원가입시 사용할 이메일 중복 검사 후 인증메일(검증번호)보내기
        binding.joinEmailSendBt.bringToFront();
        binding.joinEmailSendBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginVm.이메일중복검사통신(binding.joinEmailInput.getText().toString()).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                        if (response.isSuccessful()) {
                            JsonObject res = response.body();
                            Log.e("[JoinFm]", "이메일중복검사통신 onResponse: 통신성공, 결과는: "+ res.toString() );

                            //검사한 이메일이 존재하지않으면 인증메일을 바로보내고, 존재하면 toast 로 '사용중' 이라고 띄운다.
                            if (res.get("result").getAsBoolean()){
                                Toast.makeText(getActivity(), "인증 메일을 보냈습니다.", Toast.LENGTH_SHORT).show();
                                // todo: 인증메일 보내기 smtp
                                인증메일보내기(binding.joinEmailInput.getText().toString());
                                binding.joinEmailVerifyGroup1.setVisibility(View.VISIBLE); //그리고 인증번호 입력 창 보여줌.

                                binding.joinEmailVerifyNumberTv.start(180000); //인증 타이머 시작
                                binding.joinEmailVerifyBt.setEnabled(true);
                                binding.joinEmailConfirmInput.setError(null);
                                binding.joinEmailConfirmInput.setText("");


                            } else {
                                Toast.makeText(getActivity(), "이미 사용중인 이메일 입니다.", Toast.LENGTH_SHORT).show();
                            }

                        }
                    }
                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        Log.e("[JoinFm]", "이메일중복검사통신 onFailure: "+ t.getMessage() );
                    }
                });


            }
        });

        //번호 확인 버튼 클릭시 - 인증번호입력창의 번호를 쉐어드에 저장된 인증코드와 비교. 맞으면 다음 입력 그룹 활성. 아니면 잘못이라고 toast 띄움
        binding.joinEmailVerifyBt.setOnClickListener(v -> {
            String 인증코드 = binding.joinEmailConfirmInput.getText().toString();
            String 인증코드쉐어드 = sp.getString("emailcode", ""); //쉐어드에 저장된 코드 가져오기.


            if (인증코드.equals(인증코드쉐어드)){
                binding.joinGroup2.setVisibility(View.VISIBLE); //다음 입력 그룹 나오게
                binding.joinEmailVerifyGroup1.setVisibility(View.GONE); //인증번호 창 사라지게하기
                binding.joinEmailSendBt.setEnabled(false); // 인증번호 발송 버튼 비활성하기 - 인증됐으니..

            } else {
                Toast.makeText(getActivity(), "인증번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        //회원가입 완료 버튼 클릭시
        binding.joinCompleteBt.setOnClickListener(v -> {
            //todo 레트로핏서버로통신 - 회원정보들 json으로 넘김. - 서버에서 디비로 정보insert - 성공유무 정보 반환 - 가입완료하면(토스트띄우고) 로그인화면으로가기
            HashMap<String, String> infoMap = new HashMap<String, String>();
            infoMap.put("user_email", binding.joinEmailInput.getText().toString());
            infoMap.put("user_pwd",binding.joinPwInput.getText().toString());
            infoMap.put("user_pwdc",binding.joinPwVerifyInput.getText().toString());
            infoMap.put("user_nick",binding.joinNicknameInput.getText().toString());
            infoMap.put("user_name",binding.joinNameInput.getText().toString());
            loginVm.회원가입(infoMap).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    Log.e("[JoinFm]", "회원가입 onResponse: 통신성공, code는: "+ response.code() );
                    if (response.isSuccessful()) {
                        JsonObject res = response.body();
                        Log.e("[JoinFm]", "회원가입 onResponse: 통신성공, body는: "+ res.toString() );

                        if (res.get("result").getAsBoolean()){
                            Toast.makeText(getActivity(), res.get("msg").getAsString(), Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(JoinFm.this).navigate(R.id.action_global_loginMainFm);

                        } else {
                            Toast.makeText(getActivity(), "이미 존재하는 회원입니다.", Toast.LENGTH_SHORT).show();
                        }

                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("[JoinFm]", "이메일중복검사통신 onFailure: "+ t.getMessage() );
                }
            });

        });



    }

    @Override
    public void onResume() {
        super.onResume();


    }

    /**
     * joinEmailSendBt 인증번호 발송 버튼 클릭시 실행됨.
     * @param recipient 메일주소
     */
    private void 인증메일보내기(String recipient) {
        String subject = "성경with 회원가입 인증번호입니다.";
        String emailbody = "인증번호: ";

        new Thread(){
            @Override
            public void run() {
                super.run();

                GmailSender gMailSender = new GmailSender("sjeys14@gmail.com", BuildConfig.GMAIL_PW);
                //GMailSender.sendMail(제목, 본문내용, 받는사람);
                try {
                    gMailSender.sendMail(subject, emailbody + gMailSender.getEmailCode(), recipient );
                    spEditor.putString("emailcode", gMailSender.getEmailCode()); //emailcode라는 key에 쉐어드에 인증번호 임시저장
                    spEditor.apply();

                } catch(SendFailedException e) {
                    //쓰레드에서는 Toast를 띄우지 못하여 runOnUiThread를 호출해야 한다.
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MyApp.getApplication(), "이메일 형식이 잘못되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch(MessagingException e){
                    System.out.println("인터넷 문제 "+e);
                    //쓰레드에서는 Toast를 띄우지 못하여 runOnUiThread를 호출해야 한다.
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(requireActivity(),"인터넷 연결을 확인 해 주십시오", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();
                }

                //쓰레드에서는 Toast를 띄우지 못하여 runOnUiThread를 호출해야 한다.
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(requireActivity(), "송신 완료", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }.start();

    }


    /**
     * 회원가입 시 형식 유효성 검사를 위한 텍스트 와쳐 생성!
     * */
    private void validationCheck(){

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
                if ( 이메일형식맞나) {
                    binding.joinEmailSendBt.setEnabled(true); //로긴폼형식이 전체다 맞으면 인증번호 발송 버튼 활성
                    binding.joinEmailSendBt.setVisibility(View.VISIBLE); //

                } else{
                    binding.joinEmailSendBt.setEnabled(false); //아니면 비활성
                    binding.joinEmailSendBt.setVisibility(View.GONE); //
                }
            }
        };
        TextWatcher 이메일인증번호타이머와쳐 = new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                Boolean 타이머시간다됐나 = binding.joinEmailVerifyNumberTv.isCertification();
                if ( !타이머시간다됐나) {
                    binding.joinEmailConfirmTextlayout.setError("인증시간 만료");
                    //인증 시간이 만료되면 만료됐다고 띄우고, 확인버튼 비활성화시킴.
                    binding.joinEmailVerifyBt.setEnabled(false);
                } else{
                    binding.joinEmailConfirmTextlayout.setError(null);
                    binding.joinEmailVerifyBt.setEnabled(true);
                }
            }
        };
        TextWatcher nameWatcher = new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                Boolean 이름입력 = validateName();
                if ( 이름입력) {
                    loginVm.valid[0] = true;

                } else{
                    loginVm.valid[0] = false;
                }
            }
        };
        TextWatcher nickWatcher = new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                Boolean 닉네임입력 = validateNick();
                if ( 닉네임입력) {
                    loginVm.valid[1] = true;

                } else{
                    loginVm.valid[1] = false;
                }
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
                    loginVm.valid[2] = true;

                } else{
                    loginVm.valid[2] = false;
                }
            }
        };
        TextWatcher pwVerifyWatcher = new TextWatcher(){
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {
                Boolean 비밀번호맞나 = validatePwVerify();
                if ( 비밀번호맞나) {
                    binding.joinCompleteBt.setEnabled(true); //로긴폼형식이 전체다 맞으면 로그인버튼 활성
                    loginVm.valid[3] = true;
                } else{
                    binding.joinCompleteBt.setEnabled(false); //아니면 비활성
                    loginVm.valid[3] = false;
                }
            }
        };

        // 필수 입력란 모두 충족되는지 관찰하는 옵져버 생성
        loginVm.필수입력란확인.observe(getViewLifecycleOwner(), booleans -> {
            int count = 0;
            for (Boolean aBoolean : booleans) {
                if (!aBoolean) {
                    count++;
                    if (count == 4){
                        binding.joinCompleteBt.setEnabled(true);
                    } else{
                        binding.joinCompleteBt.setEnabled(false);
                    }
                }
            }
        });


        binding.joinEmailInput.addTextChangedListener(emailWatcher);
        binding.joinEmailConfirmInput.addTextChangedListener(이메일인증번호타이머와쳐);
        binding.joinNameInput.addTextChangedListener(nameWatcher);
        binding.joinNicknameInput.addTextChangedListener(nickWatcher);
        binding.joinPwInput.addTextChangedListener(pwWatcher);
        binding.joinPwVerifyInput.addTextChangedListener(pwVerifyWatcher);
    }


    /**
     * 회원가입시 이메일,패스워드 등등의 형식 맞는지 검사 후 textInputLayout 에 오류유무 보여주기
     */
    private Boolean validateEmail() {
        String value = String.valueOf(binding.joinEmailInput.getText());
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (value.isEmpty()) {
            binding.joinEmailTextlayout.setError("이메일을 입력해주세요.");
            return false;
        } else if (!value.matches(emailPattern)){
            binding.joinEmailTextlayout.setError("이메일 형식이 맞지 않습니다.");
            return false;
        } else {
            binding.joinEmailTextlayout.setError(null);
            return true;
        }
    }
    private Boolean validateName() {
        String value = String.valueOf(binding.joinNameInput.getText());
        if (value.isEmpty()) {
            binding.joinNameTextlayout.setError("이름을 입력해주세요.");
            return false;
        } else {
            binding.joinNameTextlayout.setError(null);
            return true;
        }
    }
    private Boolean validateNick() {
        String value = String.valueOf(binding.joinNicknameInput.getText());
        if (value.isEmpty()) {
            binding.joinNicknameTextlayout.setError("닉네임을 입력해주세요.");
            return false;
        } else if (value.length() > 10){
            binding.joinNicknameTextlayout.setError("닉네임 길이를 초과하였습니다.");
            return false;
        } else {
            binding.joinNicknameTextlayout.setError(null);
            return true;
        }
    }
    private Boolean validatePw() {
        String value = String.valueOf(binding.joinPwInput.getText());
        String pwPattern = "^.*(?=^.{8,20}$)(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[!@#$%^&+=]).*$";
        if (value.isEmpty()) {
            binding.joinPwTextlayout.setError("비밀번호를 입력해주세요.");
            return false;
        } else if (!value.matches(pwPattern)){
            binding.joinPwTextlayout.setError("비밀번호 형식이 맞지 않습니다.");
            return false;
        } else {
            binding.joinPwTextlayout.setError(null);
            return true;
        }
    }
    private Boolean validatePwVerify() {
        String value = String.valueOf(binding.joinPwInput.getText());
        String value2 = String.valueOf(binding.joinPwVerifyInput.getText());
        if (value.isEmpty()) {
            binding.joinPwVerifyTextlayout.setError("비밀번호 확인을 입력해주세요.");
            return false;
        } else if (!value.equals(value2)) {
            binding.joinPwVerifyTextlayout.setError("비밀번호가 다릅니다.");
            return false;
        } else {
            binding.joinPwVerifyTextlayout.setError(null);
            return true;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // appbar 보여주기 - navigationUP 버튼 보여주기위함
        ((AppCompatActivity)requireActivity()).getSupportActionBar().show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}