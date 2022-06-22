package com.example.androidclient.home;

import android.annotation.SuppressLint;
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
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.androidclient.MyApp;
import com.example.androidclient.R;
import com.example.androidclient.databinding.MainActivityBinding;
import com.example.androidclient.login.LoginActivity;

public class MainActivity extends AppCompatActivity {

    private MainActivityBinding binding;
    AppBarConfiguration appBarConfiguration;
    NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.mainToolbar);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.home_fm, R.id.bible_fm, R.id.group_fm, R.id.more_fm)
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_main_activity);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.mainBottomNav, navController);
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {//툴바에 메뉴 객체화세팅
//        return super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_toolbar_menu, menu);

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