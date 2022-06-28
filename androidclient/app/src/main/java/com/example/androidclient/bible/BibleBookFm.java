package com.example.androidclient.bible;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidclient.R;
import com.example.androidclient.databinding.BibleBookFmListBinding;
import com.example.androidclient.home.MainActivity;

import java.util.List;

public class BibleBookFm extends Fragment {

    private BibleVm bibleVm;
    public BibleBookFmListBinding binding;
    public BibleBookRva rva;
//    public MainActivity mainA;
//
//    public BibleBookFm(MainActivity mainA) {
//        this.mainA = mainA;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.bible_book_fm_list, container, false);
        binding = BibleBookFmListBinding.inflate(inflater, container, false);
        bibleVm = new ViewModelProvider(requireActivity()).get(BibleVm.class);
        bibleVm.성경책목록가져오기(); //서버에서 가져와 bookL에 책이름 정보를 넣음

        RecyclerView recyclerView = binding.bibleBookFmList;
        recyclerView.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
//        recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        recyclerView.setAdapter(new BibleBookRva(bibleVm, this));
        rva = (BibleBookRva) recyclerView.getAdapter();
//        rva.notifyDataSetChanged();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //todo 리사이클러뷰 갱신 dataset~
        bibleVm.liveBookL.observe(getViewLifecycleOwner(), new Observer<List<BibleDto>>() {
            @Override
            public void onChanged(List<BibleDto> bibleDtos) {
                rva.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        MainActivity mainA = ((MainActivity)requireActivity());
        mainA.binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS); //검색메뉴 보이기
        rva.notifyDataSetChanged(); //책 리사이클러뷰 갱신

    }

    @Override
    public void onPause() {
        super.onPause();
        MainActivity mainA = ((MainActivity)requireActivity());
        mainA.binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setShowAsAction(0); //감추기
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}