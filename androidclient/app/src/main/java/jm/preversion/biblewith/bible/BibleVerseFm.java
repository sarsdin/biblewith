package jm.preversion.biblewith.bible;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import jm.preversion.biblewith.MyApp;
import jm.preversion.biblewith.R;
import jm.preversion.biblewith.databinding.BibleVerseBtsListBinding;
import jm.preversion.biblewith.databinding.BibleVerseFmListBinding;
import jm.preversion.biblewith.util.Http;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class BibleVerseFm extends Fragment {

    private BibleVm bibleVm;
    public BibleVerseFmListBinding binding;
    public BibleVerseRva rva;
    public RecyclerView recyclerView;  //절 목록
    public RecyclerView btsR;          //바텀시트뷰안의 리사이클러뷰(색깔목록)
    public BottomSheetBehavior btsb;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = BibleVerseFmListBinding.inflate(inflater, container, false);
        bibleVm = new ViewModelProvider(requireActivity()).get(BibleVm.class);
        bibleVm.절목록가져오기(bibleVm.책장번호[0], bibleVm.책장번호[1]);

//        ViewDataBinding btsBinding = DataBindingUtil.inflate(inflater, R.layout.bible_verse_bts_list, container, false);
//        (BibleVerseBtsListBinding)btsBinding.getRoot().bibleVerseFmList;

        //절 목록 리스트 어댑터 세팅
        recyclerView = binding.bibleVerseFmList;
        recyclerView.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
//        recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        recyclerView.setAdapter(new BibleVerseRva(bibleVm, this));
        rva = (BibleVerseRva) recyclerView.getAdapter();
//        rva.notifyDataSetChanged();

        //절화면 바텀시트 어댑터 세팅
        LinearLayoutManager lm = new LinearLayoutManager(binding.getRoot().getContext());
        lm.setOrientation(LinearLayoutManager.HORIZONTAL);
        btsR = binding.includeLayout.bibleVerseBtsList;
        btsR.setLayoutManager(lm);
        btsR.setAdapter(new BibleVerseBtsRva(bibleVm, this));

//        recyclerView.layoutManager = LinearLayoutManager(context).apply { orientation = LinearLayoutManager.HORIZONTAL } //코틀린 문법
        btsb = BottomSheetBehavior.from(binding.includeLayout.getRoot());
//        btsb = BottomSheetBehavior.from(binding.getRoot().findViewById(R.id.include_layout));
//        btsb = BottomSheetBehavior.from(binding.getRoot().findViewById(R.id.bible_verse_bts));
        binding.includeLayout.setVm(bibleVm); //vm databinding 변수에 vm 맵핑



        //바텀시트뷰 감추기
        btsb.setState(BottomSheetBehavior.STATE_HIDDEN); //일단 시작을 감추기로 시작함

        btsb.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
//                Toast.makeText(getContext(), "gkgkgk", Toast.LENGTH_SHORT).show();
                switch(newState){
                case BottomSheetBehavior.STATE_COLLAPSED:
                    ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.setUserInputEnabled(true); //뷰페이저2 어댑터에서 유저스와이프 on
                    Log.e("[BibleVerseFm]", "스와이프 on ");
//                    Toast.makeText(getContext(),"STATE_COLLAPSED", Toast.LENGTH_SHORT).show();
                    break;
                case BottomSheetBehavior.STATE_EXPANDED:
//                    Toast.makeText(getContext(), "STATE_EXPANDED", Toast.LENGTH_SHORT).show();
                    break;
                case BottomSheetBehavior.STATE_DRAGGING:
//                    Toast.makeText(getContext(), "STATE_DRAGGING", Toast.LENGTH_SHORT).show();
                    break;
                case BottomSheetBehavior.STATE_SETTLING:
//                    Toast.makeText(getContext(), "STATE_SETTLING", Toast.LENGTH_SHORT).show();
                    break;
                case BottomSheetBehavior.STATE_HIDDEN:
                    ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.setUserInputEnabled(true); //뷰페이저2 어댑터에서 유저스와이프 on
                    Log.e("[BibleVerseFm]", "스와이프 on ");
//                    Toast.makeText(getContext(), "STATE_HIDDEN", Toast.LENGTH_SHORT).show();
                    break;
                default:
//                    Toast.makeText(getContext(), "OTHER_STATE", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });


//        binding.bibleVerseFmNextFab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (btsb.getState() == BottomSheetBehavior.STATE_EXPANDED) {
//                    btsb.setState(BottomSheetBehavior.STATE_COLLAPSED);
//                } else {
//                    btsb.setState(BottomSheetBehavior.STATE_EXPANDED);
//                }
//            }
//        });

        //todo 절화면 프로그래스바 처리 - 비동기통신 성공되어 데이터 변화되면 처리완료로 보고 프로그래스바 끄기
        bibleVm.liveVerseL.observe(getViewLifecycleOwner(), new Observer<List<BibleDto>>() {
            @Override
            public void onChanged(List<BibleDto> bibleDtos) {
                binding.bibleVerseFmProgressbar.setVisibility(View.GONE);
                Log.e("[BibleVerseFm]", "onCreateView liveVerseL observe onChanged 프로그래스바 test: 작동후 오프" );
//                rva.notifyDataSetChanged();//todo 이걸 키면 보던위치로 자동스크롤이 동작하지 않음.. 왜일까?? -스크롤전에 구조를 바꿔버리니깐 스크롤이 작동안하는 것일듯
            }
        });

     

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //todo 스크롤 움직이면 위쪽 처음 보이는 아이템의 그 위치(position) 쉐어드에 저장.
        recyclerView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
//                String format = String.format("scrollX: %d, Y: %d, oldX: %d, oldY: %d",scrollX,scrollY,oldScrollX,oldScrollY);
//                Log.e("[BibleVerseFm]", "onResume scroll: " + format );
//                Log.e("[BibleVerseFm]", "onResume scroll2: " + v.getTop()+ " "+v.getBottom() );
//                Log.e("[BibleVerseFm]", "onResume smoothscroll targetposition: " + smoothScroller.getTargetPosition() );
                int fPosition = ((LinearLayoutManager)  recyclerView.getLayoutManager()).findFirstVisibleItemPosition() != -1? //int NO_POSITION = -1 이라서 포지션있으면 무조건 참
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

        //노트 추가 버튼 클릭시
        binding.includeLayout.bibleVerseBtsNoteBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //이동하고 bibleVm.verseL의 highlight_selected 속성이 true 인 절을 노트에 추가할 절로 판단하고 가져오면 된다.
                Navigation.findNavController(v).navigate(R.id.action_global_myNoteFmAdd);
            }
        });





    }


    @Override
    public void onStart() {
        super.onStart();

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


        //BTS 바텀시트뷰에서 현재 책장절 페이지 정보 Ui 갱신용
        //비동기 데이터인 bookL 을 참조하기 때문에 onStart()이하의 생명주기에서는 인덱스아웃오브바운드가 뜨면서 찾지 못하는 오류가 뜬다.
        //데이터 처리가 완료되고 화면에 모든 ui가 완료되는 시점인 onResume()의 시점부터 옵져버를 띄워야 하는 듯하다.
        bibleVm.live책장번호.observe(getViewLifecycleOwner(), new Observer<int[]>() {
            @Override
            public void onChanged(int[] ints) {
                Log.e("오류태그", "biblevm bookl "+ bibleVm.bookL) ;
                binding.includeLayout.bibleVerseBtsTv.setText(bibleVm.bookLForSearch.get(bibleVm.책장번호[0] - 1).getBook_name() +" "+ bibleVm.chapterL.get(bibleVm.책장번호[1] - 1).getChapter()+"장" );
//                binding.includeLayout.bibleVerseBtsTvVerse.setText(bibleVm.verseL.get(bibleVm.책장번호[2] - 1).getVerse() +" ");
                StringBuilder sb = new StringBuilder();
                for (BibleDto dto : bibleVm.verseL) {
                    if (dto.getHighlight_selected()) {
                        sb.append(dto.getVerse()).append(" ");
                    }
                }
                binding.includeLayout.bibleVerseBtsTvVerse.setText(sb.toString());
//                Log.e("[BibleVerseFm]", "live책장번호.observe onChange 실행: "+sb.toString() );

                //이전 , 다음 플로팅 버튼 ui 숨김,보이기 처리 옵저버
                if (bibleVm.책장번호[1]-1 <= 0 ) {
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


//        bibleVm.liveVerseL.observe(getViewLifecycleOwner(), new Observer<List<BibleDto>>() {
//            @Override
//            public void onChanged(List<BibleDto> bibleDtos) {
//            }
//        });

        //바텀시트뷰가 열린상태일때 뷰페이저의 스와이프 막기
/*        binding.includeLayout.getRoot().setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //BibleVerseRva의 홀더 터치시 뷰페이저 스와이프를 off 처리함. 여기서는 접히거나 감춰졌을때 스와이프 다시 가능하게 처리.
                if (btsb.getState() == BottomSheetBehavior.STATE_COLLAPSED || btsb.getState() == BottomSheetBehavior.STATE_HIDDEN ) {
//                    if ( event.getAction() == MotionEvent.ACTION_UP) {
//                    ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.requestDisallowInterceptTouchEvent(true);
//                    ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.getAdapter().;
//                    recyclerView.requestDisallowInterceptTouchEvent(true);
//                    v.dispatchTouchEvent(event);
                        //위는 실패 사례들..ㅠ
//                    ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.requestDisallowInterceptTouchEvent(false);
                        ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.setUserInputEnabled(true); //뷰페이저2 어댑터에서 유저스와이프 on
//                    recyclerView.requestDisallowInterceptTouchEvent(true);
                        Log.e("[BibleVerseFm]", "스와이프 on ");
//                    }
                }*/
/* else if ( event.getAction() == MotionEvent.ACTION_DOWN){
                    ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.setUserInputEnabled(false);
//                    ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.requestDisallowInterceptTouchEvent(true);
//                    recyclerView.requestDisallowInterceptTouchEvent(false);
                    Log.e("[BibleVerseFm]", "test down " );
                }*//*

                return true;  //이걸 true로 두면 터치이벤트의 전파를 막아버리는 것 같다. 바텀시트뷰와 같은 위치에 터치(정확히는 클릭이벤트임)되는 Fab(이전다음)버튼과 리사이클러뷰홀더들이
                // 클릭되지 않게 된다. 하지만, false로 설정하면 바텀시트뷰가 버튼이나 리사이클러뷰홀더들을 가려버려도 터치이벤트가 전파되어 버튼들이 보이지 않더라도 중복해서 같이 터치된다.
            }
        });
*/


        //highlightRva 의 뷰홀더 클릭시 해당하는 하이라이트 위치로 가기
        if(bibleVm.tempObj.get("signal").getAsString().equals("hl_verse_page")){
            recyclerView.scrollToPosition(bibleVm.책장번호[2]-1);
            bibleVm.tempObj.addProperty("signal", ""); //속성값 초기화
        }

    }


    void 절목록가져오기(){
        Retrofit retrofit = Http.getRetrofitInstance(Http.HOST_IP);
        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<List<BibleDto>> call = httpBible.getVerseList( bibleVm.책장번호[0], bibleVm.책장번호[1] );
        call.enqueue(new Callback<List<BibleDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<BibleDto>> call, @NonNull Response<List<BibleDto>> response) {
                if (response.isSuccessful()) {
                    List<BibleDto> res = response.body();
                    for (BibleDto item: res) {
                        if (item.getChapter() == bibleVm.책장번호[2] ) { // 현재 사용은 안하지만 일단 로직은 남겨둠.
                            item.setCurrentItem(true); //클릭된 포지션을 변화 - 북,장 페이지의 선택된 ui 표시용으로 쓰임. 절은 어디서 쓰이지? 찾아봐야함
//                            Log.e("BibleVerseFm", "절목록가져오기(): "+item);
                        }
                    }

                    // 목록안에 받은 정보 업데이트
                    bibleVm.verseL = res;

                    //verseL 에 색깔 정보 업데이트하기
                    for(BibleDto item : bibleVm.verseL  ) {
                        for(BibleDto itemIner : bibleVm.highL  ) {
                            if (item.getBible_no() == itemIner.getBible_no()) {
                                item.setHighlight_color(itemIner.getHighlight_color());
                                break;
                            }
                        }
                    }

                    bibleVm.liveVerseL.setValue(bibleVm.verseL);
//                    Log.e("[BibleVerseFm]", "절목록가져오기 onResponse: "+ bibleVm.verseL );

                    //홀더 클릭시 절탭으로 넘어가기
//                    assert BibleVerseFm.this.getParentFragment() != null;
//                    ((BibleFm) BibleVerseFm.this.getParentFragment()).binding.bibleTabLayoutViewpager.setCurrentItem(2);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}