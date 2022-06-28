package com.example.androidclient.bible;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidclient.MyApp;
import com.example.androidclient.databinding.BibleVerseFmListBinding;

import java.util.List;

public class BibleVerseFm extends Fragment {

    private BibleVm bibleVm;
    public BibleVerseFmListBinding binding;
    public BibleVerseRva rva;
    RecyclerView recyclerView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = BibleVerseFmListBinding.inflate(inflater, container, false);
        bibleVm = new ViewModelProvider(requireActivity()).get(BibleVm.class);
        bibleVm.절목록가져오기(bibleVm.책장번호[0], bibleVm.책장번호[1]);

        recyclerView = binding.bibleVerseFmList;
        recyclerView.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
//        recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        recyclerView.setAdapter(new BibleVerseRva(bibleVm, this));
        rva = (BibleVerseRva) recyclerView.getAdapter();
//        rva.notifyDataSetChanged();


        //todo 리사이클러뷰 갱신 dataset~
        bibleVm.liveVerseL.observe(getViewLifecycleOwner(), new Observer<List<BibleDto>>() {
            @Override
            public void onChanged(List<BibleDto> bibleDtos) {
                Log.e("[BibleVerseFm]", "onCreateView liveVerseL observe onChanged 프로그래스바 test: " );
                binding.bibleVerseFmProgressbar.setVisibility(View.GONE);

//                rva.notifyDataSetChanged(); //todo 이걸 키면 자동스크롤이 동작하지 않음.. 왜일까?? - 스크롤전에 구조를 바꿔버리니깐 스크롤이 작동안하는 것일듯
            }
        });

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        //스크롤 움직이면 위쪽 처음 보이는 아이템의 그 위치(position) 쉐어드에 저장.
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//                String format = String.format("scrollX: %d, Y: %d, oldX: %d, oldY: %d",scrollX,scrollY,oldScrollX,oldScrollY);
//                Log.e("[BibleVerseFm]", "onResume scroll: " + format );
//                Log.e("[BibleVerseFm]", "onResume scroll2: " + v.getTop()+ " "+v.getBottom() );
//                Log.e("[BibleVerseFm]", "onResume smoothscroll targetposition: " + smoothScroller.getTargetPosition() );

                int fPosition = ((LinearLayoutManager)  recyclerView.getLayoutManager()).findFirstVisibleItemPosition() != -1?
                        ((LinearLayoutManager)  recyclerView.getLayoutManager()).findFirstVisibleItemPosition():0;
                SharedPreferences sp = MyApp.getApplication().getSharedPreferences("scrollverse", Context.MODE_PRIVATE);
                SharedPreferences.Editor spEditor = sp.edit();
                sp.getInt("firstVisiblePosition", 0);
                spEditor.putInt("firstVisiblePosition", fPosition);
                spEditor.apply();
            }
        });




    }

    @Override
    public void onResume() {
        super.onResume();
        rva.notifyDataSetChanged(); //책 리사이클러뷰 갱신
//        recyclerView.scrollToPosition(10);
//        할당한 포지션을 리사이클러뷰 상단으로 스크롤 이동시킴.
        if (!bibleVm.onceExecuted) {
            Log.e("[BibleVerseFm]", "onResume onceExecuted test: 이거 보이면 실행된거. 한번만 실행되야하고 그담부턴 안보여야함." );

            RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
                @Override protected int getVerticalSnapPreference(){
                    return LinearSmoothScroller.SNAP_TO_START;
                }
            };
            //viewcreate에서 저장된 firstposition의 숫자를 받아와 타겟넘버로 정해준다. 그리고 SmoothScroller start 함
            SharedPreferences sp = MyApp.getApplication().getSharedPreferences("scrollverse", Context.MODE_PRIVATE);
            smoothScroller.setTargetPosition( sp.getInt("firstVisiblePosition", 0) );
            recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
            bibleVm.onceExecuted = true; //이후부턴 onResume() 실행되도 if문 실행안되게끔 true로 바꿔줌
        }



    }

}