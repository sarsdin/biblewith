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
import com.example.androidclient.util.Http;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

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


        //todo 절화면 프로그래스바 처리 - 비동기통신 성공되어 데이터 변화되면 처리완료로 보고 프로그래스바 끄기
        bibleVm.liveVerseL.observe(getViewLifecycleOwner(), new Observer<List<BibleDto>>() {
            @Override
            public void onChanged(List<BibleDto> bibleDtos) {
                Log.e("[BibleVerseFm]", "onCreateView liveVerseL observe onChanged 프로그래스바 test: " );
                binding.bibleVerseFmProgressbar.setVisibility(View.GONE);
//                rva.notifyDataSetChanged();//todo 이걸 키면 보던위치로 자동스크롤이 동작하지 않음.. 왜일까?? -스크롤전에 구조를 바꿔버리니깐 스크롤이 작동안하는 것일듯
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
                JsonObject save = new JsonObject();
                save.addProperty("user_email", MyApp.getUserInfo().getUser_email());
                save.addProperty("firstVisiblePosition", fPosition);
                spEditor.putString(MyApp.getUserInfo().getUser_email(), save.toString()).apply(); //저장하는 키값을 이메일(id)로 이용
            }
        });


        /*binding.bibleVerseFmNextFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.bibleVerseFmProgressbar.setVisibility(View.VISIBLE);
                bibleVm.장번호업데이트(bibleVm.책장번호[1]+1); //현재 장번호 다음 번호를 보여주기 위해 +1해줌
                bibleVm.절목록가져오기(bibleVm.책장번호[0], bibleVm.책장번호[1]);
                //홀더 클릭시 절탭으로 넘어가기
                assert BibleVerseFm.this.getParentFragment() != null;
                ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.setCurrentItem(2);
                //스크롤 제일 위로 가기
//                        ((BibleVerseFm) ( bibleChapterFm.getParentFragmentManager().getFragments().get(2))).recyclerView.scrollToPosition(0);
//                        Log.e("[BibleBookRav]", "getParentFragmentManager: " +  bibleChapterFm.getParentFragmentManager().getFragments().get(0).getId() );
                recyclerView.scrollToPosition(0);
                rva.notifyDataSetChanged();
            }
        });*/
//        bibleVm.liveChapterL.ob

        //다음 버튼 클릭시
        binding.bibleVerseFmNextFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.bibleVerseFmProgressbar.setVisibility(View.VISIBLE);
                bibleVm.장번호업데이트(bibleVm.책장번호[1]+1); //현재 장번호 다음 번호를 보여주기 위해 +1해줌
//                bibleVm.절목록가져오기(bibleVm.책장번호[0], bibleVm.책장번호[1]); //vm에서 비동기통신을 처리하는 것은 ui를 다루는데 매우 큰 제약을 준다. 콜백이 vm에 존재하게 되기 때문이다...
                절목록가져오기();
            }
        });
        //이전 버튼 클릭시
        binding.bibleVerseFmPreFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.bibleVerseFmProgressbar.setVisibility(View.VISIBLE);
                bibleVm.장번호업데이트(bibleVm.책장번호[1]-1); //현재 장번호 다음 번호를 보여주기 위해 +1해줌
//                bibleVm.절목록가져오기(bibleVm.책장번호[0], bibleVm.책장번호[1]); //vm에서 비동기통신을 처리하는 것은 ui를 다루는데 매우 큰 제약을 준다. 콜백이 vm에 존재하게 되기 때문이다...
                절목록가져오기();
            }
        });

        //이전 , 다음 플로팅 버튼 ui 숨김,보이기 처리 옵저버
        bibleVm.live책장번호.observe(getViewLifecycleOwner(), new Observer<int[]>() {
            @Override
            public void onChanged(int[] ints) {
                if (bibleVm.책장번호[1]-2 <= 0 ) {
                    binding.bibleVerseFmPreFab.setVisibility(View.GONE);
                } else {
                    binding.bibleVerseFmPreFab.setVisibility(View.VISIBLE);
                }

                if (bibleVm.책장번호[1] < bibleVm.chapterL.size() ) { //변경된 장번호가 장리스트의 크기(마지막장번호)와 같으면 다음버튼 숨기기 처리
                    binding.bibleVerseFmNextFab.setVisibility(View.VISIBLE);
                } else {
                    binding.bibleVerseFmNextFab.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        rva.notifyDataSetChanged(); //책 리사이클러뷰 갱신
//        recyclerView.scrollToPosition(10);
        //절탭이 처음 시작할때 이전에 할당된(쉐어드에저장) 포지션을 이용해 리사이클러뷰 상단으로 스크롤 이동시킴.
        if (!bibleVm.onceExecuted) {
            Log.e("[BibleVerseFm]", "onResume onceExecuted test: 이거 보이면 실행된거. 한번만 실행되야하고 그담부턴 안보여야함." );

            RecyclerView.SmoothScroller smoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {
                @Override protected int getVerticalSnapPreference(){
                    return LinearSmoothScroller.SNAP_TO_START;
                }
            };
            //viewcreate에서 저장된 firstposition의 숫자를 받아와 타겟넘버로 정해준다. 그리고 SmoothScroller start 함
            SharedPreferences sp = MyApp.getApplication().getSharedPreferences("scrollverse", Context.MODE_PRIVATE);
            String savedInfo = sp.getString(MyApp.getUserInfo().getUser_email(), ""); //유저 이메일을 키값으로 저장된 데이터를 불러옴
            if (!savedInfo.equals("")) {
                JsonObject save = JsonParser.parseString(savedInfo).getAsJsonObject(); //문자열을 파서로 json obj 로 변환하여 사용.
                smoothScroller.setTargetPosition( save.get("firstVisiblePosition").getAsInt() );
                recyclerView.getLayoutManager().startSmoothScroll(smoothScroller);
                bibleVm.onceExecuted = true; //이후부턴 onResume() 실행되도 if문 실행안되게끔 true로 바꿔줌
            }
        }


    }


    void 절목록가져오기(){
        Retrofit retrofit = Http.getRetrofitInstance("15.165.174.226");
        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<List<BibleDto>> call = httpBible.getVerseList( bibleVm.책장번호[0], bibleVm.책장번호[1] );
        call.enqueue(new Callback<List<BibleDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<BibleDto>> call, @NonNull Response<List<BibleDto>> response) {
                if (response.isSuccessful()) {
                    List<BibleDto> res = response.body();
                    for (BibleDto item: res) {
                        if (item.getChapter() == bibleVm.책장번호[2] ) { // 현재 사용은 안하지만 일단 로직은 남겨둠.
                            item.setCurrentItem(true); //클릭된 포지션을 변화
//                            Log.e("BibleVerseFm", "절목록가져오기(): "+item);
                        }
                    }
                    // 목록안에 받은 정보 업데이트
                    bibleVm.verseL = res;
//                    Log.e("[BibleVerseFm]", "절목록가져오기 onResponse: "+ res );
                    bibleVm.liveVerseL.setValue(bibleVm.verseL);

                    //홀더 클릭시 절탭으로 넘어가기
                    assert BibleVerseFm.this.getParentFragment() != null;
                    ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.setCurrentItem(2);
                    //스크롤 제일 위로 가기
//                        ((BibleVerseFm) ( bibleChapterFm.getParentFragmentManager().getFragments().get(2))).recyclerView.scrollToPosition(0);
//                        Log.e("[BibleBookRav]", "getParentFragmentManager: " +  bibleChapterFm.getParentFragmentManager().getFragments().get(0).getId() );
                    recyclerView.scrollToPosition(0);
                    rva.notifyDataSetChanged();
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<BibleDto>> call, Throwable t) {
                Log.e("[BibleVerseFm]", "절목록가져오기 onFailure: "+ t.getMessage() );
            }
        });
    }

}