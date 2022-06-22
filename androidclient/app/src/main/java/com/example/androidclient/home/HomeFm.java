package com.example.androidclient.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.androidclient.databinding.HomeFmBinding;


public class HomeFm extends Fragment {

    private HomeFmBinding binding;
    private HomeVm homevm;
    private long backKeyPressedTime = 0;
    private Toast toast;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HomeVm homeVm = new ViewModelProvider(this).get(HomeVm.class);

        binding = HomeFmBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        homeVm.getText().observe(getViewLifecycleOwner(), textView::setText);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handleOnBackPressed(); //뒤로가기 종료 구현부

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }



    private void handleOnBackPressed(){ //뒤로가기 종료 구현부
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {//백버튼을 조각에서 조종하기 위한 메소드.
                        if(getParentFragmentManager().getBackStackEntryCount()==0){
//                            getActivity().finish();
                            뒤로가기종료(); //두번 클릭시 종료처리
                        } else{
                            NavController navcon = NavHostFragment.findNavController(HomeFm.this);
                            navcon.navigateUp(); //뒤로가기(백스택에서 뒤로가기?)
                        }
                    }
                });
    }

    private void 뒤로가기종료(){
        if(System.currentTimeMillis() > backKeyPressedTime + 2000){
            backKeyPressedTime = System.currentTimeMillis();
            toast = Toast.makeText(getActivity(), "뒤로 버튼을 한번 더 누르면 종료됩니다.",Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if(System.currentTimeMillis() <= backKeyPressedTime + 2000){
            requireActivity().finish();
            toast.cancel();
//            super.onBackPressed();
        }
    }



}