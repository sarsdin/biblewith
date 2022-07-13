package com.example.androidclient.bible;
import android.util.Log;

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
import androidx.navigation.Navigation;

import com.example.androidclient.R;
import com.example.androidclient.databinding.BibleFmBinding;
import com.example.androidclient.home.MainActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.List;

public class BibleFm extends Fragment  { //implements BibleBookRav.뷰페이저어뎁터컨트롤리스너 작동x


    private BibleVm bibleVm;
    public BibleFmBinding binding;
    public BibleVpa bibleVpa;
    private final List<Fragment> pageFmList = new ArrayList<Fragment>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageFmList.add(new BibleBookFm());
        pageFmList.add(new BibleChapterFm());
        pageFmList.add(new BibleVerseFm());


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = BibleFmBinding.inflate(inflater, container, false);
        bibleVm = new ViewModelProvider(requireActivity()).get(BibleVm.class);


        //-----------------------------------------------성경 뷰페이저2세팅

        bibleVpa = new BibleVpa(pageFmList, getChildFragmentManager(), getLifecycle() );
        binding.bibleTabLayoutViewpager.setOffscreenPageLimit(3); //페이지양쪽 프래그먼트보관수
        binding.bibleTabLayoutViewpager.setAdapter(bibleVpa);
//        freeBoardFmSubVpAdapter.setStateRestorationPolicy(RecyclerView.Adapter.StateRestorationPolicy.PREVENT);

        TabLayoutMediator tym = new TabLayoutMediator(binding.bibleTabLayout, binding.bibleTabLayoutViewpager,
            new TabLayoutMediator.TabConfigurationStrategy() {
                @Override
                public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                    if (position == 0){
                        tab.setText("책");
                    } else if (position==1){
                        tab.setText("장");
                    } else if (position==2){
                        tab.setText("절");
                    } else{
                        tab.setText("기타");
                    }
                }
            });
        tym.attach();
        //-------------------------------------------------------------------------

//        ((BibleBookFm)pageFmList.get(0)).rav.bibleFmBinding = binding;

        //myhighlight_fm 에서 각 뷰홀더에 링크를 걸어 해당하는 절페이지를 보여주기 위해 Bundle을 이용해 이 fragment에 신호를 보냄. -- Bundle대신 vm(tempObj)을 사용하도록 수정
        if(bibleVm.tempObj.get("signal").getAsString().equals("hl_verse_page")){
            binding.bibleTabLayoutViewpager.setCurrentItem(2);
//                int id = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_main_activity).getCurrentDestination().getId();
//                ((BibleVerseFm) ( getParentFragmentManager().findFragmentById(id))).recyclerView.scrollToPosition(0);
//                binding.bibleTabLayoutViewpager.getAdapter().getItemId(2);
//                ((BibleVerseFm) pageFmList.get(2)).binding.bibleVerseFmList.scrollToPosition(0); //다른 fragment에서는 먹히지 않는 듯..
//                Log.e("오류태그", "1: "+((BibleVerseFm) pageFmList.get(2)));
//                Log.e("오류태그", "2: "+((BibleVpa)binding.bibleTabLayoutViewpager.getAdapter()).pageFmList.get(2));
        }

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
//        MainActivity mainA = ((MainActivity)requireActivity());
//        mainA.binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS); //검색메뉴 보이기
//        mainA.binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setVisible(true);
        Log.e("오류태그", "biblefm resume");

        //toolbar에 책 제목과 몇장인지 나타내기 위한 옵저버를 생성하고 ui데이터가 바뀔때마다 ui(책제목,장)를 변경해줌.
        bibleVm.live책장번호.observe(getViewLifecycleOwner(), new Observer<int[]>() {
            @Override
            public void onChanged(int[] ints) {
                // 책장번호 -1을 해줘야 인덱스 순서가 맞음 창세기의 번호가 1부터 시작이기때문. (적재된 데이터의 시작번호가 0이 아닌 1번부터)
                MainActivity mainA = ((MainActivity)requireActivity());
//                ((MainActivity)requireActivity()).binding.mainAppbarBibleTv.setText(mainA.bookinfo.get(bibleVm.책장번호[0]-1).getAsJsonObject().get("book_name").getAsString());
                //고정값을 가진 검색용 리스트를 써야 온전한 전체 책제목데이터에서 정확한 값을 가져올 수 있다.
                ((MainActivity)requireActivity()).binding.mainAppbarBibleTv.setText(bibleVm.bookLForSearch.get(bibleVm.책장번호[0]-1).getBook_name());
                ((MainActivity)requireActivity()).binding.mainAppbarChapterTv.setText((bibleVm.책장번호[1]) + "장");
//                mainA.binding.mainAppbarChapterTv.setVisibility(View.GONE);
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
//    @Override
//    public BibleFmBinding 바인딩가져오기() { //인터페이스 작동x
//        return binding;
//    }

}