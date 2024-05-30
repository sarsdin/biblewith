package jm.preversion.biblewith.login;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import jm.preversion.biblewith.R;
import jm.preversion.biblewith.databinding.LoginActivityBinding;

public class LoginActivity extends AppCompatActivity {

    private LoginVm loginVm;
    public LoginActivityBinding binding;
    private AppBarConfiguration appBarConfiguration;
    private NavController navController;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Toast.makeText(getApplicationContext(), "oncreate!",Toast.LENGTH_SHORT).show();

        binding = LoginActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.loginToolbar);
//        ((AppCompatActivity)this).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
//        ((AppCompatActivity)this).getSupportActionBar().setHomeButtonEnabled(false);
//        this.getSupportActionBar().hide();

        //스테이터스바 (최고상단 배터리표시되는 곳) 색상변경
//        Window window = getWindow();
//        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//        window.setStatusBarColor(Color.parseColor("#FFFFFFFF"));



        navController = Navigation.findNavController(this, R.id.login_navi_fragment); //.xml file
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    @Override
    public boolean onSupportNavigateUp() {//up button을 작동하게 함..
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
    }









}