package com.example.androidclient.home;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
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





    }

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
        SearchView searchView = (SearchView)menu.findItem(R.id.app_bar_search).getActionView();
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