package com.example.androidclient.moreinfo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.androidclient.R;
import com.example.androidclient.databinding.MoreFmBinding;

public class MoreFm extends Fragment {

    private MoreVm moreVm;
    private MoreFmBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = MoreFmBinding.inflate(inflater, container, false);
        moreVm = new ViewModelProvider(this).get(MoreVm.class);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //하이라이트 클릭시 이동
        int ids[] = binding.moreFmHighlightGroup.getReferencedIds();
        for (int id: ids){
            binding.getRoot().findViewById(id).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_global_myHighLightFm));
        }

        //노트 클릭시 이동
        for (int id: binding.moreFmNoteGroup.getReferencedIds()){
            binding.getRoot().findViewById(id).setOnClickListener(v -> Navigation.findNavController(v).navigate(R.id.action_global_myNoteFm));
        }

    }




    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}