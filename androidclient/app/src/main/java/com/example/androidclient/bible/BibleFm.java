package com.example.androidclient.bible;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.androidclient.databinding.BibleFmBinding;

public class BibleFm extends Fragment {

    private BibleVm bibleVm;
    private BibleFmBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BibleFmBinding.inflate(inflater, container, false);
        bibleVm = new ViewModelProvider(this).get(BibleVm.class);

        return binding.getRoot();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}