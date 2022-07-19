package com.example.androidclient.moreinfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.androidclient.R;
import com.example.androidclient.databinding.MoreFmBinding;
import com.example.androidclient.home.MainActivity;

public class MoreFm extends Fragment {

    private MoreVm moreVm;
    private MoreFmBinding binding;
    public AppBarConfiguration appBarConfiguration;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = MoreFmBinding.inflate(inflater, container, false);
        moreVm = new ViewModelProvider(requireActivity()).get(MoreVm.class);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //up버튼 활성화 할려면 nested navigation을 다시 컨트롤러로 지정해야함.. 귀찮음 과정이라 nested navi 는 버리는 걸로..
//        NavigationUI.setupActionBarWithNavController((MainActivity) requireActivity(), Navigation.findNavController(view));

        //모임 툴바 셋팅
        appBarConfiguration = new AppBarConfiguration.Builder(R.id.more_fm).build();
        navController = Navigation.findNavController(view);
        NavigationUI.setupWithNavController( binding.moreToolbar, navController, appBarConfiguration);

        //하이라이트 클릭시 이동 - 그룹핑한 view 들을 아무거나 클릭하면 하이라이트페이지로 가게끔
        int ids[] = binding.moreFmHighlightGroup.getReferencedIds();
        for (int id: ids){
            binding.getRoot().findViewById(id).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_more_fm_to_myHighLightFm));
        }

        //노트 클릭시 이동
        for (int id: binding.moreFmNoteGroup.getReferencedIds()){
            binding.getRoot().findViewById(id).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_more_fm_to_myNoteFm));
        }



    }


    @Override
    public void onPause() {
        super.onPause();
        //up버튼 활성화 할려면 nested navigation을 다시 컨트롤러로 지정해야함.. 귀찮음 과정이라 nested navi 는 버리는 걸로..
//        NavigationUI.setupActionBarWithNavController((MainActivity) requireActivity(), Navigation.findNavController((MainActivity) requireActivity(), R.id.nav_host_fragment_main_activity));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }


}