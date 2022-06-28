package com.example.androidclient.bible;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidclient.databinding.BibleChapterFmListBinding;

import java.util.List;

public class BibleChapterFm extends Fragment {

    private BibleVm bibleVm;
    public BibleChapterFmListBinding binding;
    public BibleChapterRva rva;
//    public MainActivity mainA;
//
//    public BibleChapterFm(MainActivity mainA) {
//        this.mainA = mainA;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.bible_book_fm_list, container, false);
        binding = BibleChapterFmListBinding.inflate(inflater, container, false);
        bibleVm = new ViewModelProvider(requireActivity()).get(BibleVm.class);
        bibleVm.장목록가져오기(bibleVm.책장번호[0], "vmInit");  //가져올때는 창세기의 번호가 1 이니깐 인덱스0번에 +1을 하여 db에서 조회해야함. -- 6/27다시 +1안하게 고침
        RecyclerView recyclerView = binding.bibleChapterFmList;


//        recyclerView.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
        recyclerView.setLayoutManager(new GridLayoutManager(binding.getRoot().getContext(), 4));
        recyclerView.setAdapter(new BibleChapterRva(bibleVm, this));
        rva = (BibleChapterRva) recyclerView.getAdapter();
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
    public void onResume() {
        super.onResume();
        rva.notifyDataSetChanged();
    }

}