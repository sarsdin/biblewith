package com.example.androidclient.home;

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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.androidclient.MyApp;
import com.example.androidclient.R;
import com.example.androidclient.bible.BibleDto;
import com.example.androidclient.bible.BibleVm;
import com.example.androidclient.databinding.MainActivityBinding;
import com.example.androidclient.login.LoginActivity;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public MainActivityBinding binding;
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private BibleVm bibleVm;
    public JsonArray bookinfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = MainActivityBinding.inflate(getLayoutInflater());
//        DataBindingUtil.setContentView(this, R.layout.main_activity).invalidateAll();//
        setContentView(binding.getRoot());
        setSupportActionBar(binding.mainToolbar);
        쉐어드에서책정보불러오기(); //JsonArray bibleinfo 에 저장
        bibleVm = new ViewModelProvider(this).get(BibleVm.class);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.home_fm, R.id.bible_fm, R.id.group_fm, R.id.more_fm)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_main_activity);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.mainBottomNav, navController);


        //bibleFm안에서 책제목,몇장인지 나타내는 텍스트뷰 표시 제어
        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController, @NonNull NavDestination navDestination, @Nullable Bundle bundle) {
                if (navDestination.getId() == R.id.bible_fm) {
//                    binding.mainAppbarTvGroup.setVisibility(View.VISIBLE);
                    //네비게이션뷰, 앱레이아웃에서는 그룹이 안먹히네? 왜 그렇지? - main_activity.xml 안에 이유 설명되어 있음 - 컨스트레인레이아웃 스코프 문제임.
                    binding.mainAppbarBibleTv.setVisibility(View.VISIBLE);
                    binding.mainAppbarChapterTv.setVisibility(View.VISIBLE);
                } else {
                    binding.mainAppbarBibleTv.setVisibility(View.GONE);
                    binding.mainAppbarChapterTv.setVisibility(View.GONE);
                    //검색이 메뉴항목(수직...)에서도 숨겨짐. 바로가기로 표시되는 경우(툴바에표시)에는 안숨겨짐. 이때는 setEnabled(false)로 숨겨야함. 둘다 구분되어 작동
                    //다만, navigation이 시작되는 startDestination에 지정된 Fm에서는 액티비티의 MenuInflate가 이루어지기 전인 onCreate 때 fm가 붙어버리기에
                    //MenuInflate 이 후에 적용될 수 있는 옵션인 setVisible(false) 같은 경우 적용되지 않는다. 다음 onDestinationChanged 가 이루어지면 동작한다.
                    //그렇기에 startDestination 에 setVisible(false)을 적용시키고 싶다면 activity의 onCreateOptionsMenu()에서 MenuInflate 이후 메소드를 쓴다.
                    binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setVisible(false);
//                    binding.mainToolbar.setVisibility(View.GONE);  // << 이것이 위와 달리 적용되는 이유는 toolbar는 menu가 아니고 상위 액티비티 레이아웃
                    //에 소속한 view 이기 때문에 onCreate()에서 이미 inflate 되었기 때문에 작동하는 것!
                }
            }
        });





    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    //로컬db에서 책제목 정보 불러와서 사용. 원격db에서 불러오는 것과 비슷한 용도.
    void 쉐어드에서책정보불러오기(){
        SharedPreferences sp = MyApp.getApplication().getSharedPreferences("bookinfo", Context.MODE_PRIVATE);
//        SharedPreferences.Editor spEditor = sp.edit();
        String pp = sp.getString("bookinfo", "");
//        Log.e("[MainActivity]", "쉐어드 로딩 test: "+ pp );
        bookinfo = JsonParser.parseString(pp).getAsJsonArray();
//        Log.e("[MainActivity]", "쉐어드 로딩 test2: "+ array );
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {//툴바에 메뉴 객체화세팅
//        return super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_toolbar_menu, menu);
        //메뉴 인플레이트가 되고 난 뒤에야 toolbar의 기능이 인식된다. onCreate 등에서 binding.mainToolbar가 먼저 안먹히는것은 순서상 onCreateOptionMenu가
        //onCreate보다 늦게 시작되기 때문이다. 그리고, 앞서 말한대로 메뉴인플레이트가 이루어져야 toolbar의 기능이 동작하게 된다.
//        binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setVisible(false); // << 이것과
        menu.findItem(R.id.app_bar_search).setVisible(false);                         // << 이것은 같다. binding.mainToolbar == menu 참조위치가 같은 객체임. 인플레이트 시기에 따라 활성화가 안되는 것도 같음.
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
//                List<BibleDto> searchList = new ArrayList<BibleDto>();
                Gson gson = new Gson();
                JsonArray searchList = new JsonArray();
                for (JsonElement item:  bookinfo ) {
                    if (item.getAsJsonObject().get("book_name").getAsString().contains(newText)) {
                        searchList.add(item);
                    }
                }
                List<BibleDto> searchL = gson.fromJson(searchList, new TypeToken<ArrayList<BibleDto>>(){}.getType());
//                bibleVm.책검색(bookinfo, newText);
                bibleVm.책검색(searchL);

                return true;
            }
        });

        return true;
    }

    @SuppressLint("NonConstantResourceId")
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

    }

    @Override
    public boolean onSupportNavigateUp() {//up button을 작동하게 함..
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
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