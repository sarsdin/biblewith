package com.example.androidclient.home;
import android.util.Log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavGraph;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.androidclient.MyApp;
import com.example.androidclient.R;
import com.example.androidclient.bible.BibleDto;
import com.example.androidclient.bible.BibleVm;
import com.example.androidclient.databinding.MainActivityBinding;
import com.example.androidclient.login.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public MainActivityBinding binding;
    public AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private BibleVm bibleVm;
    public JsonArray bookinfo;
    public boolean canNavigateUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = MainActivityBinding.inflate(getLayoutInflater());
//        DataBindingUtil.setContentView(this, R.layout.main_activity).invalidateAll();//
        setContentView(binding.getRoot());
//        setSupportActionBar(binding.mainToolbar);
//        쉐어드에서책정보불러오기(); //JsonArray bibleinfo 에 저장 -- 7/9 쉐어드까지 배포가 안되므로 결국 서버에서 데이터를 로딩해오는 방식으로 가야함
        bibleVm = new ViewModelProvider(this).get(BibleVm.class);
//        Gson gson = new Gson();
        //책 제목 정보 vm에 초기화넣어주기 - 노트목록가져오기 뷰홀더 where_tv에서 참조함 - notefm: vm에서 미리 로딩하는게 되더라..createView에서 안하고 vm안에서 비동기통신하니깐 미리 로딩되어있더라
//        bibleVm.bookL = gson.fromJson(bookinfo, new TypeToken<ArrayList<BibleDto>>(){}.getType()); 

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        /*appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.home_fm, R.id.bible_fm, *//*R.id.group_fm,*//* R.id.more_fm)
                .build();*/
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_main_activity);
        //appBarConfiguration 은 탑레벨 destinations 를 지정하기위한 appbar의 옵션구성임. 바텀네비게이션은 이미 각 메뉴탭이 최상위레벨로 지정되어 만들어져있는 느낌임.
        //그래서 이옵션을 지정하지 않아도 각 탭이 최상위레벨 destination 으로 작동하는데, 만약 toolBar & navigationDrawerview 를 구성할려면 이 옵션을 이용해 최상위 destination
        //을 설정할 수 있을 것임. 위에서처럼 개별 topLevelDestinationIds를 지정할 수 도있고 해당 view가 속한 navController의 NavGraph를 이용해 전체 네비게이션 그래프를 전달하여
        //설정할 수도 있음. 탑레벨 destination을 지정하면 up버튼 ui를 최상위 destination을 기준으로 표시되게 할 수 있음!
        // activity에서 네비게이션을 만들든 fragment에서 만들든 new AppBarConfiguration 은 onCreate 또는 onViewCreate에서 반드시 선언되어야 up버튼등이 최상위메뉴 기준으로 설정가능.

        // setupActionBarWithNavController는 앱바에 각destination의 label이 표시되고 appBarConfiguration에 따라 up버튼이 동작하는 방식을 설정할 수 있도록 하는 메소드.
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
//        NavigationUI.setupWithNavController(binding.mainToolbar, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.mainBottomNav, navController);

        //바텀네비게이션 메뉴 클릭시
        binding.mainBottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                NavigationUI.onNavDestinationSelected(item, navController); //메뉴클릭시 백스택 시작이 destination start 지점부터 시작됨
                // onNavDestinationSelected 부터 먼저 선언하고 시작해야 리스너가 작동함.. 위의 상황에서는 하이라이트 화면에서 홈이나
                // 성경페이지로 갔다가 다시 더보기 메뉴클릭하면 하이라이트 화면(백스택)이 보여짐.
                // 그러나 아래처럼 설정하면 더보기 화면이 나오게 됨. 더보기메뉴의 아디가 more_navigation인데 클릭시 조건에 맞으니깐 -- more_fm으로 상위메뉴 변경
                // popBackStack 이 실행되고 어디까지 팝되냐면 more_fm(더보기메인화면)까지 화면(백스택)이 팝됨.
                // 이런 조건을 이용해서 각각의 최상위 메뉴로부터 멀티백스택을 가질지 말지 결정할 수 있음.
                if(item.getItemId()  == R.id.more_fm){
//                    navController.popBackStack(R.id.myHighLightFm, true);
                    navController.popBackStack(R.id.more_fm, false);
//                    navController.popBackStack(R.id.more_navigation, false);
                }
//                if(item.getItemId()  == R.id.more_navigation){
//                    navController.popBackStack(R.id.myHighLightFm, true);
//                    navController.popBackStack(R.id.more_fm, false);
//                    navController.popBackStack(R.id.more_navigation, false);
//                }

               /*if(item.getItemId()  == R.id.myHighLightFm){
//                    navController.popBackStack(R.id.myHighLightFm, false, false);
                    navController.popBackStack(R.id.more_fm, false, false);
//                    navController.popBackStack(R.id.more_navigation, false);
                }*/

                return false;
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
//                binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setVisible(false);
                /*if (navDestination.getId() == R.id.bible_fm) {
//                    binding.mainAppbarTvGroup.setVisibility(View.VISIBLE);
                    //네비게이션뷰, 앱레이아웃에서는 그룹이 안먹히네? 왜 그렇지? - main_activity.xml 안에 이유 설명되어 있음 - 컨스트레인레이아웃 스코프 문제임.
                    binding.mainAppbarBibleTv.setVisibility(View.VISIBLE);
                    binding.mainAppbarChapterTv.setVisibility(View.VISIBLE);
                    Log.e("[MainActivity]", "onDestinationChanged bible_fm create if");
                } else {
                    Log.e("[MainActivity]", "onDestinationChanged bible_fm create else");
                    binding.mainAppbarBibleTv.setVisibility(View.GONE);
                    binding.mainAppbarChapterTv.setVisibility(View.GONE);
                    //검색이 메뉴항목(수직...)에서도 숨겨짐. 바로가기로 표시되는 경우(툴바에표시)에는 안숨겨짐. 이때는 setEnabled(false)로 숨겨야함. 둘다 구분되어 작동
                    //다만, navigation이 시작되는 startDestination에 지정된 Fm에서는 액티비티의 MenuInflate가 이루어지기 전인 onCreate 때 fm가 붙어버리기에
                    //MenuInflate 이 후에 적용될 수 있는 옵션인 setVisible(false) 같은 경우 적용되지 않는다. 다음 onDestinationChanged 가 이루어지면 동작한다.
                    //그렇기에 startDestination 에 setVisible(false)을 적용시키고 싶다면 activity의 onCreateOptionsMenu()에서 MenuInflate 이후 메소드를 쓴다.
//                    binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setVisible(false);
//                    binding.mainToolbar.setVisibility(View.GONE);  // << 이것이 위와 달리 적용되는 이유는 toolbar는 menu가 아니고 상위 액티비티 레이아웃
                    //에 소속한 view 이기 때문에 onCreate()에서 이미 inflate 되었기 때문에 작동하는 것!
                }*/

                //노트 추가 화면으로 이동시 바텀 네비게이션 숨김
                switch(navDestination.getId()){
                    case  R.id.myNoteFmAdd:

                    case  R.id.groupInFm:
                    case  R.id.groupInWriteFm:
                    case  R.id.groupInUpdateFm:
                    case  R.id.groupBoardDetail:
                    case  R.id.myNoteFmUpdate:
                        binding.mainBottomNav.setVisibility(View.GONE);
//                        binding.mainAppbarNoteAddBt.setVisibility(View.VISIBLE);
                        break;
//                        binding.mainAppbarNoteUpdateBt.setVisibility(View.VISIBLE);
                    //                        binding.mainAppbarNoteUpdateBt.setVisibility(View.VISIBLE);
                    default:
                        binding.mainBottomNav.setVisibility(View.VISIBLE);
//                        binding.mainAppbarNoteAddBt.setVisibility(View.GONE);
//                        binding.mainAppbarNoteUpdateBt.setVisibility(View.GONE);
                        break;
                }

                //모임탭에서 메인 앱바,툴바 감추기 처리
                /*if(navDestination.getId() == R.id.group_fm || navDestination.getId() == R.id.groupCreateFm){
                    binding.mainAppbar.setVisibility(View.GONE);
                } else {
                    binding.mainAppbar.setVisibility(View.VISIBLE);
                }*/

            }
        });

        // Add your own reselected listener 다중백스택관련 navigation 2.4.0 rc1
//        binding.mainBottomNav.setOnItemReselectedListener(new NavigationBarView.OnItemReselectedListener() {
//            @Override
//            public void onNavigationItemReselected(@NonNull MenuItem item) {
//                int reselectedDestinationId = item.getItemId();
//                navController.popBackStack(reselectedDestinationId, false, false);
//            }
//        });


        //menuProvider를 이용한 메뉴관리
     /*   binding.mainToolbar.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.main_toolbar_menu, menu);

            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()){
                    case R.id.main_toolbar_menu_logout:
                        //쉐어드 프리퍼런스에서 자동로그인 정보 해제하기
                        SharedPreferences spAutologin = MyApp.getApplication().getSharedPreferences("autologin", MODE_PRIVATE);
                        SharedPreferences.Editor editor = spAutologin.edit();
                        String user_email = spAutologin.getString("user_email", "");
                        if (!user_email.equals("")) {    //null 아니면(자동로긴상태라면) 내용 삭제
                            editor.clear();
                            editor.apply();
                        }

                        Intent movelogin = new Intent(MyApp.getApplication(), LoginActivity.class);
                        startActivity(movelogin);
                        MainActivity.this.finish();
                        Toast.makeText(getApplicationContext(), "로그아웃",Toast.LENGTH_SHORT).show();
                        return true;

                    default:
                        return false;
                }
//                return false;
            }
        });*/

    }

    @Override
    protected void onResume() {
        super.onResume();
        //일단 검색버튼이 계속 보여야함 안그러면 ui갱신이 안되더라..버근가?
//        binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//        binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setVisible(true);
        Log.e("오류태그", "MainActivity onResume");
    }

    //로컬db에서 책제목 정보 불러와서 사용. 원격db에서 불러오는 것과 비슷한 용도.
    // -- 7/9 쉐어드는 일회용 임시 데이터라 배포단계까지가면 같이 앱에 데이터를 포함해서 배포가 안되니깐 영구데이터는 쓰기 어려운듯 - 쓸수있는 수단이 있으면 가능하지만.. 아직 모르겠다.
    void 쉐어드에서책정보불러오기(){
        SharedPreferences sp = MyApp.getApplication().getSharedPreferences("bookinfo", Context.MODE_PRIVATE);
//        SharedPreferences.Editor spEditor = sp.edit();
        String pp = sp.getString("bookinfo", "");
//        Log.e("[MainActivity]", "쉐어드 로딩 test: "+ pp );
        bookinfo = JsonParser.parseString(pp).getAsJsonArray();
//        Log.e("[MainActivity]", "쉐어드 로딩 test2: "+ array );
    }



/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {//툴바에 메뉴 객체화세팅 , onCreateOptionsMenu 는 onResume 보다 늦게 실행됨
//        super.onCreateOptionsMenu(menu);
        Log.e("오류태그", "onCreateOptionsMenu 호출됨");
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_toolbar_menu, menu);
        //메뉴 인플레이트가 되고 난 뒤에야 toolbar의 기능이 인식된다. onCreate 등에서 binding.mainToolbar가 먼저 안먹히는것은 순서상 onCreateOptionMenu가
        //onCreate보다 늦게 시작되기 때문이다. 그리고, 앞서 말한대로 메뉴인플레이트가 이루어져야 toolbar의 기능이 동작하게 된다.
//        binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setVisible(false); // << 이것과
//        menu.findItem(R.id.app_bar_search).setVisible(false);                         // << 이것은 같다. binding.mainToolbar == menu 참조위치가 같은 객체임. 인플레이트 시기에 따라 활성화가 안되는 것도 같음.
        SearchView searchView = (SearchView)menu.findItem(R.id.app_bar_search).getActionView(); //searchView를 찾아서 반환
        searchView.setMaxWidth(600);
//        binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).getActionView();
//        searchView.setSubmitButtonEnabled(true); //검색창 확인버튼 활성화

        //책제목 검색 - 툴바에서
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                //todo 검색 텍스트 변할때 마다 수행 할 작업 - bookL 에 검색버튼에 입력한 텍스트에 해당하는 책들만 출력해야함.
                List<BibleDto> searchList = new ArrayList<BibleDto>();
//                Gson gson = new Gson();
//                JsonArray searchList = new JsonArray();
//                for (JsonElement item:  bookinfo ) {
//                    if (item.getAsJsonObject().get("book_name").getAsString().contains(newText)) {
//                        searchList.add(item);
//                    }
//                }
//                List<BibleDto> searchL = gson.fromJson(searchList, new TypeToken<ArrayList<BibleDto>>(){}.getType());
                for (BibleDto item:  bibleVm.bookL ) {
                    if (item.getBook_name().contains(newText)) {
                        searchList.add(item);
                    }
                }
                bibleVm.책검색(searchList);

                return true;
            }
        });

        return true;
    }*/

/*    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.main_toolbar_menu_logout:
                //쉐어드 프리퍼런스에서 자동로그인 정보 해제하기
                SharedPreferences spAutologin = MyApp.getApplication().getSharedPreferences("autologin", MODE_PRIVATE);
                SharedPreferences.Editor editor = spAutologin.edit();
                String user_email = spAutologin.getString("user_email", "");
                if (!user_email.equals("")) {    //null 아니면(자동로긴상태라면) 내용 삭제
                    editor.clear();
                    editor.apply();
                }

                Intent movelogin = new Intent(MyApp.getApplication(), LoginActivity.class);
                startActivity(movelogin);
                this.finish();
                Toast.makeText(getApplicationContext(), "로그아웃",Toast.LENGTH_SHORT).show();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }

    }*/

    @Override
    public boolean onSupportNavigateUp() {//up button을 작동하게 함..
//        if(canNavigateUp){
//            return navController.navigateUp() || super.onSupportNavigateUp();
//        } else{
//        }
            return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
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

    //일단 이 메소드를 오버라이딩해서 super로 인자들을 넘겨줘야 fragment에서도 startActivityForResult의
    // 결과를 fragment onActivityResult()에서 받아 쓸 수 있다.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

    }

}


/*    private void 바텀네비동작설정() { //이건 NavigationHost를 사용하면 동작안하는 듯. 예전 방식이면 가능
        binding.mainBottomNav.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.bible_fm) {
                    binding.mainAppbarBibleTv.setVisibility(View.VISIBLE);
                    binding.mainAppbarChapterTv.setVisibility(View.VISIBLE);
                } else {
                    binding.mainAppbarBibleTv.setVisibility(View.GONE);
                    binding.mainAppbarChapterTv.setVisibility(View.GONE);
                }
                return true;
            }
        });
    }*/