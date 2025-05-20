package jm.preversion.biblewith;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PictureInPictureParams;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.Rational;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import jm.preversion.biblewith.MyApp;
import jm.preversion.biblewith.MyService;
import jm.preversion.biblewith.R;
import jm.preversion.biblewith.bible.BibleVm;
import jm.preversion.biblewith.databinding.MainActivityBinding;
import jm.preversion.biblewith.login.LoginActivity;
import jm.preversion.biblewith.rtc.RtcFm;
import jm.preversion.biblewith.rtc.webrtc.sessions.WebRtcSessionManagerImpl;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    public String tagName = "[MainActivity]";
    public MainActivityBinding binding;
    public AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private BibleVm bibleVm;
    public boolean canNavigateUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if(MyApp.userInfo != null){
            bibleVm = new ViewModelProvider(this).get(BibleVm.class);
        }

        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        /*appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.home_fm, R.id.bible_fm, R.id.group_fm, R.id.more_fm)
                .build();*/
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_main_activity);
        //appBarConfiguration 은 탑레벨 destinations 를 지정하기위한 appbar의 옵션구성임. 바텀네비게이션은 이미 각 메뉴탭이 최상위레벨로 지정되어 만들어져있다.
        //그래서 이옵션을 지정하지 않아도 각 탭이 최상위레벨 destination 으로 작동하는데, 만약 toolBar & navigationDrawerview 를 구성할려면 이 옵션을 이용해 최상위 destination
        //을 설정할 수 있을 것임. 위에서처럼 개별 topLevelDestinationIds를 지정할 수도있고 해당 view가 속한 navController의 NavGraph를 이용해 전체 네비게이션 그래프를 전달하여
        //설정할 수도 있음. 탑레벨 destination을 지정하면 up버튼 ui를 최상위 destination을 기준으로 표시되게 할 수 있음!
        // activity에서 네비게이션을 만들든 fragment에서 만들든 new AppBarConfiguration 은 onCreate 또는 onViewCreate에서 반드시 선언되어야 up버튼등이 최상위메뉴 기준으로 설정가능.

        // setupActionBarWithNavController는 앱바에 각destination의 label이 표시되고 appBarConfiguration에 따라 up버튼이 동작하는 방식을 설정할 수 있도록 하는 메소드.
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
//        NavigationUI.setupWithNavController(binding.mainToolbar, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.mainBottomNav, navController);

        //바텀네비게이션 메뉴 클릭시 동작 리스너 설정.
        binding.mainBottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                NavigationUI.onNavDestinationSelected(item, navController); //메뉴클릭시 백스택 시작이 destination start 지점부터 시작됨
                // onNavDestinationSelected 부터 먼저 선언하고 시작해야 리스너가 작동함.. 위의 상황에서는 하이라이트 화면에서 홈이나
                // 성경페이지로 갔다가 다시 더보기 메뉴클릭하면 하이라이트 화면(백스택)이 보여짐.
                // 그러나 아래처럼 설정하면 더보기 화면이 나오게 됨. 더보기메뉴의 ID가 more_fm인데 클릭시 조건에 맞으니깐
                // popBackStack 이 실행됨. 어디까지 팝되냐면 more_fm(더보기메인화면)까지 화면(백스택)이 팝됨.
                // 이런 조건을 이용해서 각각의 최상위 메뉴로부터 멀티백스택을 가질지 말지 결정할 수 있음.
                if(item.getItemId()  == R.id.more_fm){
                    navController.popBackStack(R.id.more_fm, false);
                }

                return true;
            }
        });

        /*NavOptions navOptions = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(NavGraph.findStartDestination(navController.getGraph()).getId(),
                        false, // inclusive
                        true) // saveState
                .build();
        navController.navigate(R.id.more_navigation, null, navOptions);*/

        //네비게이션 컨트롤러 이벤트 제어 - Navigation navigate()로 목적지 화면으로 이동할때 목적지가 변함을 감지하고 그에 따라 이벤트를 발동하는 이벤트 리스너.
        //bibleFm안에서 책제목,몇장인지 나타내는 텍스트뷰 표시 제어
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {

                //노트 추가 화면으로 이동시 바텀 네비게이션 숨김
                if (navDestination.getId() == R.id.rtcFm ||
                        navDestination.getId() == R.id.loginMainFm ||
                        navDestination.getId() == R.id.findPwFm ||
                        navDestination.getId() == R.id.joinFm ||
                        navDestination.getId() == R.id.myNoteFmAdd ||
                        navDestination.getId() == R.id.myNoteFmUpdate ||
                        navDestination.getId() == R.id.groupInFm ||
                        navDestination.getId() == R.id.groupInWriteFm ||
                        navDestination.getId() == R.id.groupInUpdateFm ||
                        navDestination.getId() == R.id.groupBoardDetail ||
                        navDestination.getId() == R.id.group_in_challenge_fm ||
                        navDestination.getId() == R.id.challengeCreateFm ||
                        navDestination.getId() == R.id.challengeCreateNextFm ||
                        navDestination.getId() == R.id.challengeDetailFm ||
                        navDestination.getId() == R.id.challengeDetailAfterFm ||
                        navDestination.getId() == R.id.challengeDetailListFm ||
                        navDestination.getId() == R.id.groupChatFm ||
                        navDestination.getId() == R.id.groupInChatFm ||
                        navDestination.getId() == R.id.groupChatInnerFm ||
                        navDestination.getId() == R.id.groupInMemberFm
                ) {
                    binding.mainBottomNav.setVisibility(View.GONE);

                } else {
                    binding.mainBottomNav.setVisibility(View.VISIBLE);
                }


                if (navDestination.getId() == R.id.rtcFm) {
                    binding.mainAppbar.setVisibility(View.GONE);
                } else {
                    binding.mainAppbar.setVisibility(View.VISIBLE);
                }

            }
        });


        //뒤로가기 종료 로직
        handleOnBackPressed();

        //서비스 시작 - BIND_AUTO_CREATE : 서비스가 켜저있으면 자동으로 바인딩하고, 없으면 만들어서 바인딩함
        Intent intent = new Intent(this, MyService.class);
        bindService(intent, serviceConn, Context.BIND_AUTO_CREATE);
    }



    //서비스 관련 변수들 및 바인딩 서비스 연결 구현
    public MyService myService; // 서비스 객체
    public boolean isService = false; // 서비스 중인지 확인용
    public ServiceConnection serviceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            // 서비스와 연결되었을 때 호출되는 메서드
            // 서비스 객체를 전역변수로 저장
            MyService.MyBinder binder = (MyService.MyBinder) service; //서비스에서 받아온 MyBinder 객체
            myService = binder.getService();                //위의 객체로부터 MyService 객체를 얻어옴
            // 서비스쪽 객체를 전달받을수 있음
            isService = true;// 서비스가 실행중이면 true - 서비스 On 이라고 처리
            Log.e(tagName, "서비스: MyService에 연결되었습니다.");

        }
        public void onServiceDisconnected(ComponentName name) {
            //서비스 Off 이라고 처리. 이 메소드는 비정상 서비스 종료시에만 호출됨. 정상 종료시에는 호출안되니 주의!!
            isService = false;
            Log.e(tagName, "서비스: MyService가 비정상 종료되었습니다.");
        }
    };

    public void 클라이언트스레드등록확인() {
//        cli = ChatClient("10.0.2.2", groupVm) //127.0.0.1 << avd에서 안드로이드os 자신을 가리킴. 내 컴퓨터의 로컬 서버가 아님..!
//        cli.start()
        Gson gson = new Gson();
        JsonObject jo = new JsonObject();
        jo.addProperty("user_no", MyApp.userInfo.getUser_no());
        jo.addProperty("user_nick", MyApp.userInfo.getUser_nick());
        jo.addProperty("user_image", MyApp.userInfo.getUser_image());
        jo.addProperty("cmd", "초기화");
        jo.addProperty("cmd_type", "초기화");
        Thread thread = new Thread(() -> {
            try{
                myService.cli.outMsg.println(gson.toJson(jo));

            }catch(Exception e){
                e.printStackTrace();
            }
        });
        thread.start();

    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.e("-- MainActivity --", "MainActivity onResume");

    }


    /**
     * 새로운 인텐트 발생시 설정 - 하위 프래그먼트에서 사용하기 위함 - 딥링크등..<br>
     * 앱이 이미 켜져있는 상태에서는 외부 딥링크가 안먹히는 것 같다.
     * 자동으로 인식이 안되는 것 같지만, 여기서 수동으로 받는 것은 가능하다!
     * @param intent The new intent that was started for the activity.
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        String action = intent.getAction();
        Uri data = intent.getData();
        Log.e(tagName, "deepLinkInfo: "+ action+", "+data ); //android.intent.action.VIEW, http://biblewith.com/invite/1
        if(data != null){
            Log.e(tagName, "getPath: "+ data.getPath() );                             //  /invite/1
            Log.e(tagName, "getPathSegments: "+ data.getPathSegments() );             // [invite, 1]
            Log.e(tagName, "getLastPathSegment: "+ data.getLastPathSegment() );       // 1
            Log.e(tagName, "getQuery: "+ data.getQuery() );                           // null
            Log.e(tagName, "getScheme: "+ data.getScheme() );                         // http
            Log.e(tagName, "getSchemeSpecificPart: "+ data.getSchemeSpecificPart() ); //  //biblewith.com/invite/1
            Log.e(tagName, "getFragment: "+ data.getFragment() );                     // null
            Log.e(tagName, "getHost: "+ data.getHost() );                             // biblewith.com
            Log.e(tagName, "getQuery: "+ data.getQuery() );                             //

            if(MyApp.userInfo != null){
                if (data != null && data.getPathSegments().get(0).equals("invite") ) {
                    Log.e(tagName, "group_fm으로 가기전");

                    String group_no = data.getPathSegments().get(1);
                    String invite_code = data.getPathSegments().get(3);
//
                    Bundle bd = new Bundle();
                    bd.putString("group_no", group_no); //getPathSegments: [invite, 1, c, 660998]
                    bd.putString("invite_code", invite_code);
                    //초대링크를 타고 들어왔냐는 시그널용도 - 모임 홈에서 viewmodel 처리 후, 이 시그널에 따른 로직을 처리 후(서버에 invite_code 검증 후 멤버추가)
                    // 위의 group_no를 이용해 해당 모임으로이동
                    bd.putBoolean("is_invited", true);
                    Log.e(tagName, "group_fm으로 가기전2: "+ group_no + " "+ invite_code);
                    navController.navigate(R.id.group_fm, bd);
                    Log.e(tagName, "group_fm으로 가기후: "+ group_no + " "+ invite_code);



                } else { //조건에 맞지 않으면 일반적인 앱의 흐름으로 home_fm 화면으로 간다.
                    Log.e(tagName, "home_fm으로 간다");
                    navController.navigate(R.id.action_startFm_to_home_fm);
                }


            } else { //LoginActivity 로 보내서 로그인 진행 처리
                Log.e(tagName, "LoginActivity으로 돌아간다");
                Intent toLogin = new Intent(MyApp.getApplication(), LoginActivity.class);
                startActivity(toLogin);
                finish();
            }


        }


    }


    @Override
    public boolean onSupportNavigateUp() {//up button을 작동하게 함..
            return /*NavigationUI.navigateUp(navController, appBarConfiguration) ||*/ super.onSupportNavigateUp();
    }

    public void setNavigation(boolean navigationUpEnabled) {
        if(getSupportActionBar() != null){
            canNavigateUp = navigationUpEnabled;
            if(navigationUpEnabled){
                //up 버튼 설정 null 설정시 기본아이콘으로 보임
                getSupportActionBar().setHomeAsUpIndicator(null);

            } else {
//                getSupportActionBar().setDisplayShowHomeEnabled(true);
            }

        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.e(tagName, "Main 뒤로가기 클릭");
    }

    private void handleOnBackPressed() { //뒤로가기 종료 구현부
        Log.e(tagName, "뒤로가기 디버깅용");
        getOnBackPressedDispatcher().addCallback(
            this,
            new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    Toast.makeText(MainActivity.this,"뒤로 버튼 클릭1.",Toast.LENGTH_SHORT).show();
                    Log.e(tagName, "뒤로가기 클릭1");
                    if (Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.home_fm) {
//                        if(getParentFragmentManager().getBackStackEntryAt(0).getId() == R.id.startFm){
                        뒤로가기종료(); //두번 클릭시 종료처리

                    } else if(Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.bible_fm ||
                                Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.group_fm ||
                                Objects.requireNonNull(navController.getCurrentDestination()).getId() == R.id.more_fm
                        ){
                        Log.e(tagName, "뒤로가기 클릭 home_fm으로");
                        navController.navigate(R.id.action_global_home_fm);

                    } else {
                        Toast.makeText(MainActivity.this,"뒤로 버튼 클릭3.",Toast.LENGTH_SHORT).show();
                        Log.e(tagName, "뒤로가기 클릭 navigateUp()");
                        (MainActivity.this).navController.navigateUp(); //뒤로가기(백스택에서 뒤로가기)
                    }
                }
            }
        );
    }

    private Long backKeyPressedTime = 0L;
    private void 뒤로가기종료() {
        Log.e(tagName, "뒤로가기 클릭2");
        Toast.makeText(MainActivity.this,"뒤로 버튼 클릭2.",Toast.LENGTH_SHORT).show();
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            this.backKeyPressedTime = System.currentTimeMillis();
            Toast.makeText(MainActivity.this,"뒤로 버튼을 한번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT).show();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            MainActivity.this.finish();
            //            super.onBackPressed();
        }
    }


    //일단 이 메소드를 오버라이딩해서 super로 인자들을 넘겨줘야 fragment에서도 startActivityForResult의
    // 결과를 fragment onActivityResult()에서 받아 쓸 수 있다.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }



    /**
     * Pip Mode 에서 화면을 볼 수 있도록 하는 객체.
     */
    PictureInPictureParams.Builder pipBuilder;
    @Override
    protected void onUserLeaveHint() {
        Log.e(tagName, "onUserLeaveHint() pip mode실행 ");
        super.onUserLeaveHint();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            pipBuilder = new PictureInPictureParams.Builder();
            //pip mode 일때의 화면 비율을 설정. 가로 세로 비율임.
            pipBuilder.setAspectRatio(new Rational(400, 600));

            // 안드로이드12 api 31 이상일때 pip mode size를 변경가능하게 함.
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R){
                pipBuilder.setSeamlessResizeEnabled(true);
            }
            enterPictureInPictureMode(pipBuilder.build());
        }
    }


    /**
     * groupIn (모임)안에서 생성된 전용 바텀네비게이션을 컨트롤할 수 있는 리스너 객체 생성.
     * @return
     */
    public NavigationBarView.OnItemSelectedListener 모임네비게이션리스너 = 모임네비게이션리스너생성();
    public NavigationBarView.OnItemSelectedListener 모임네비게이션리스너생성(){
        return new NavigationBarView.OnItemSelectedListener() {
            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {

                int currentItemDestinationId = Objects.requireNonNull(navController.getCurrentDestination()).getId();
                if (item.getItemId() != currentItemDestinationId){
                    switch(item.getItemId()){
                        case  R.id.group_in_challenge_fm:
                            navController.navigate(R.id.action_global_group_in_challenge_fm);
                            break;
                        case  R.id.groupInMemberFm:
                            navController.navigate(R.id.action_global_groupInMemberFm);
                            break;
                        case  R.id.groupInChatFm:
                            navController.navigate(R.id.action_global_groupInChatFm);
                            break;
                        case  R.id.rtc_fm:
                            navController.navigate(R.id.action_global_rtcFm);
                            break;
                        case  R.id.groupInFm:
                            navController.navigate(R.id.action_global_groupInFm);
                            break;
                        default:
                            break;
                    }
                }
                return false;
            }
        };
    }


}


