package jm.preversion.biblewith.login;

import android.os.Bundle;
import android.os.Handler;
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

import jm.preversion.biblewith.R;
import jm.preversion.biblewith.databinding.LoginFindPwFmBinding;
import com.google.gson.JsonObject;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FindPwFm extends Fragment {

    private LoginFindPwFmBinding binding;
    private LoginVm loginVm;
    private Thread newsT;

    public FindPwFm() {    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = LoginFindPwFmBinding.inflate(inflater, container, false);
        loginVm = new ViewModelProvider(requireActivity()).get(LoginVm.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // todo 버튼 만들기 - 이메일, 이름 같이 입력창 - 찾기버튼 누르기 - 서버에 메일발송요청 - 서버에서인증메일발송 - 발송완료 리턴 - 클라에서 번호인증 확인
        // todo - 서버로 번호인증요청 - 요청된번호 검증(테이블) - 확인시 true반환 - 클라에서 확인후 변경페이지로 이동

        //회원가입 시 형식 유효성 검사를 위한 텍스트 와쳐 생성 및 실행
        validationCheck();

        //인증번호 발송 버튼 클릭시 회원가입 이메일 검증 메일(번호)보내기
        binding.findpwEmailSendBt.bringToFront();
        binding.findpwEmailSendBt.setOnClickListener(v -> {
            HashMap<String, String> infoMap = new HashMap<String, String>();
            infoMap.put("user_email", binding.findpwEmailInput.getText().toString());
            infoMap.put("user_name",binding.findpwNameInput.getText().toString());

            loginVm.비번찾기인증번호발송클릭(infoMap).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        JsonObject res = response.body();
                        Log.e("[findpwFm]", "비번찾기인증번호발송클릭 onResponse: 통신성공, 결과는: "+ res.toString() );

                        //입력한 이름과 이메일이 존재하지않으면 toast 로 '존재하지 않는 회원입니다' 이라고 띄운다.
                        if (res.get("result").getAsBoolean()){
                            Toast.makeText(getActivity(), "인증 메일을 보냈습니다.", Toast.LENGTH_SHORT).show();
                            // todo: 인증메일 보내기 smtp - 서버에서 메일 보내기
                            binding.findpwEmailVerifyGroup1.setVisibility(View.VISIBLE); //그리고 인증번호 입력 그룹 보여줌.

                            binding.findpwEmailVerifyNumberTv.start(180000); //인증 타이머 시작
                            binding.findpwEmailVerifyBt.setEnabled(true);   //번호 확인 버튼 활성화
                            binding.findpwEmailConfirmInput.setError(null); // 이메일 입력란에서 에러났던 부분 해제
                            binding.findpwEmailConfirmInput.setText("");    // 에러 힌트 문구도 없앰
                            만료시간타이머(); // 새로운 쓰레드에서 인증 타이머를 계속 계산하여 0이 되면 만료되었다는 setError를 설정하는 핸들러 생성.


                        } else {
                            Toast.makeText(getActivity(), res.get("msg").getAsString(), Toast.LENGTH_SHORT).show();
                        }

                    }
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("[findpwFm]", "비번찾기인증번호발송클릭 onFailure: "+ t.getMessage() );
                }
            });
        });

        //메일의 번호를 쓰고 인증번호 확인 버튼 클릭시
        binding.findpwEmailVerifyBt.setOnClickListener(v -> {
            HashMap<String, String> infoMap = new HashMap<String, String>();
            infoMap.put("findpw_number",binding.findpwEmailConfirmInput.getText().toString());
            infoMap.put("name",binding.findpwNameInput.getText().toString());
            infoMap.put("email", binding.findpwEmailInput.getText().toString());

            loginVm.인증번호확인클릭(infoMap).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        JsonObject res = response.body();
                        Log.e("[findpwFm]", "인증번호확인클릭 onResponse: 통신성공, 결과는: "+ res.toString() );

                        if (res.get("result").getAsBoolean()){
                            Toast.makeText(getActivity(), res.get("msg").getAsString(), Toast.LENGTH_SHORT).show();
                            binding.findpwNameEmailInputGroup3.setVisibility(View.GONE); //이메일, 이름 입력 그룹 해제
                            binding.findpwEmailVerifyGroup1.setVisibility(View.GONE);   //인증번호 입력 그룹 해제
                            binding.findpwNewpwGroup2.setVisibility(View.VISIBLE);      //새 비밀번호 입력 그룹 활성
                            binding.findpwCompleteBt.setVisibility(View.VISIBLE);       //비번 변경 완료버튼 보이기
                            binding.findpwTv.setText("새 비밀번호 설정");

                        } else {
                            Toast.makeText(getActivity(), res.get("msg").getAsString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("[findpwFm]", "인증번호확인클릭 onFailure: "+ t.getMessage() );
                }
            });

        });


        // 비밀번호 변경 완료 버튼 클릭시
        binding.findpwCompleteBt.setOnClickListener(v -> {
            //todo 입력된 비번 가져와서 서버로 변경 요청 - 성공유무 반환 - true면 변경 toast와 함께 로그인화면으로 이동
            HashMap<String, String> info = new HashMap<String,String>();
            info.put("user_pwd", binding.findpwPwInput.getText().toString());
            info.put("user_email", binding.findpwEmailInput.getText().toString());

            loginVm.새비밀번호변경완료클릭(info).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        JsonObject res = response.body();
                        Log.e("[findpwFm]", "새비밀번호변경완료클릭 onResponse: 통신성공, 결과는: "+ res.toString() );

                        if (res.get("result").getAsBoolean()){
                            Toast.makeText(getActivity(), res.get("msg").getAsString(), Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(FindPwFm.this).navigate(R.id.action_global_loginMainFm);

                        } else {
                            Toast.makeText(getActivity(), res.get("msg").getAsString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("[findpwFm]", "새비밀번호변경완료클릭 onFailure: "+ t.getMessage() );
                }
            });
        });


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
                    binding.findpwEmailSendBt.setEnabled(true); //로긴폼형식이 전체다 맞으면 인증번호 발송 버튼 활성
                    binding.findpwEmailSendBt.setVisibility(View.VISIBLE); //

                } else{
                    binding.findpwEmailSendBt.setEnabled(false); //아니면 비활성
                    binding.findpwEmailSendBt.setVisibility(View.GONE); //
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
                Boolean 타이머시간다됐나 = binding.findpwEmailVerifyNumberTv.isCertification();
                if ( !타이머시간다됐나) {
                    binding.findpwEmailConfirmTextlayout.setError("인증시간 만료");
                    //인증 시간이 만료되면 만료됐다고 띄우고, 확인버튼 비활성화시킴.
                    binding.findpwEmailVerifyBt.setEnabled(false);
                } else{
                    binding.findpwEmailConfirmTextlayout.setError(null);
                    binding.findpwEmailVerifyBt.setEnabled(true);
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
                    loginVm.findPwValid[0] = true;

                } else{
                    loginVm.findPwValid[0] = false;
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
                    binding.findpwCompleteBt.setEnabled(true); //비번 형식 맞으면 활성
                    loginVm.findPwValid[1] = true;
                } else{
                    binding.findpwCompleteBt.setEnabled(false); //아니면 비활성
                    loginVm.findPwValid[1] = false;
                }
            }
        };

        // 필수 입력란 모두 충족되는지 관찰하는 옵져버 생성
        loginVm.비번찾기필수입력란확인.observe(getViewLifecycleOwner(), findPwValid -> {
            int count = 0;
            for (Boolean aBoolean : findPwValid) {
                if (!aBoolean) {
                    count++;
                    if (count == 2){
                        binding.findpwCompleteBt.setEnabled(true);
                    } else{
                        binding.findpwCompleteBt.setEnabled(false);
                    }
                }
            }
        });



        binding.findpwEmailInput.addTextChangedListener(emailWatcher);
        binding.findpwEmailConfirmInput.addTextChangedListener(이메일인증번호타이머와쳐); //옵저버로 시간타이머를 본다면 timerview 의 시간필드를 라이브데이터로 짜고 관찰해야함.
        binding.findpwNameInput.addTextChangedListener(nameWatcher);
        binding.findpwPwInput.addTextChangedListener(pwWatcher);
        binding.findpwPwVerifyInput.addTextChangedListener(pwVerifyWatcher);
    }



    /**
     * 회원가입시 이메일,패스워드 등등의 형식 맞는지 검사 후 textInputLayout 에 오류유무 보여주기
     */
    private Boolean validateEmail() {
        String value = String.valueOf(binding.findpwEmailInput.getText());
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        if (value.isEmpty()) {
            binding.findpwEmailTextlayout.setError("이메일을 입력해주세요.");
            return false;
        } else if (!value.matches(emailPattern)){
            binding.findpwEmailTextlayout.setError("이메일 형식이 맞지 않습니다.");
            return false;
        } else {
            binding.findpwEmailTextlayout.setError(null);
            return true;
        }
    }
    private Boolean validateName() {
        String value = String.valueOf(binding.findpwNameInput.getText());
        if (value.isEmpty()) {
            binding.findpwNameTextlayout.setError("이름을 입력해주세요.");
            return false;
        } else {
            binding.findpwNameTextlayout.setError(null);
            return true;
        }
    }
    private Boolean validatePw() {
        String value = String.valueOf(binding.findpwPwInput.getText());
        String pwPattern = "^.*(?=^.{8,20}$)(?=.*[0-9])(?=.*[a-zA-Z])(?=.*[!@#$%^&+=]).*$";
        if (value.isEmpty()) {
            binding.findpwPwTextlayout.setError("비밀번호를 입력해주세요.");
            return false;
        } else if (!value.matches(pwPattern)){
            binding.findpwPwTextlayout.setError("비밀번호 형식이 맞지 않습니다.");
            return false;
        } else {
            binding.findpwPwTextlayout.setError(null);
            return true;
        }
    }
    private Boolean validatePwVerify() {
        String value = String.valueOf(binding.findpwPwInput.getText());
        String value2 = String.valueOf(binding.findpwPwVerifyInput.getText());
        if (value.isEmpty()) {
            binding.findpwPwVerifyTextlayout.setError("비밀번호 확인을 입력해주세요.");
            return false;
        } else if (!value.equals(value2)) {
            binding.findpwPwVerifyTextlayout.setError("비밀번호가 다릅니다.");
            return false;
        } else {
            binding.findpwPwVerifyTextlayout.setError(null);
            return true;
        }
    }


    /**
     *  이메일 인증 버튼 클릭시 실행
     */
    private void 만료시간타이머(){
        Handler handler = new Handler();
        newsT = new Thread(() -> {
            for (int i = 0; !newsT.isInterrupted(); i++) {
                try {
                    int finalI = i;
                    //꾸준히 모니터함 0인지
                    if (binding.findpwEmailVerifyNumberTv.getTime() == 0) {
                        handler.post(() -> {
                            binding.findpwEmailConfirmInput.setError("인증시간이 만료되었습니다.");
                            binding.findpwEmailVerifyBt.setEnabled(false);
                        });
                    }

                    //지연시간을 줘서 성능향상을 노림
                    Thread.sleep(300);

                } catch (InterruptedException e) {
                    newsT.interrupt();
//                            e.printStackTrace();
                } catch (NullPointerException e) {
                    newsT.interrupt();
                }
            }
        });
        newsT.start();
    }


    @Override
    public void onStart() {
        super.onStart();
        // appbar 보여주기 - navigationUP 버튼 보여주기위함
        ((AppCompatActivity)requireActivity()).getSupportActionBar().show();
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}