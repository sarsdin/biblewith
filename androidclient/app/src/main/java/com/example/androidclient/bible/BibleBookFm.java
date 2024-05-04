package com.example.androidclient.bible;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.androidclient.R;
import com.example.androidclient.databinding.BibleBookFmListBinding;

import java.util.ArrayList;
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

        //메인툴바에 검색뷰 설정
        ((BibleFm) getParentFragment()).binding.bibleToolbar.addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.toolbar_bible_search_menu, menu);
                SearchView searchView = (SearchView)menu.findItem(R.id.bible_toolbar_search).getActionView(); //searchView를 찾아서 반환
                searchView.setMaxWidth(600);
                //검색아이콘 터치시 이전에 검색했던 텍스트를 저장해놨다가 불러와서 검색텍스트뷰에 넣어준다.
                searchView.setQuery( bibleVm.tempObj.get("bookSearchText").getAsString(), false);

                //책제목 검색 - 툴바에서
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }
                    @Override
                    public boolean onQueryTextChange(String newText) {
                        //todo 검색 텍스트 변할때 마다 수행 할 작업 - bookL 에 검색버튼에 입력한 텍스트에 해당하는 책들만 출력해야함.
                        List<BibleDto> searchList = new ArrayList<BibleDto>();
                        for (BibleDto item:  bibleVm.bookLForSearch ) {
                            if (item.getBook_name().contains(newText)) {
                                searchList.add(new BibleDto(item));
                                //bookLForSearch가 res를 얕은복사로 참조만 가져옴 - bookL과 내용이 같은상태(같은리스트요소객체) - bookL의 요소가 BookFm리사이클러뷰에서 클릭될때마다
                                //bookLForSearch의 요소도 당연히 같이 변경됨 - 변경된 bookLForSearch를 여기서 재활용함. currentItem의 값은 당연히 true로 변경된 채로 계속 남아있음
                                // - searchList에 변경되어있는 요소가 그대로 추가되어 검색된 화면에 표시됨 - 당연히 currentItem=ture인 요소들이 쌓이고 있으니 표시되는 요소들이
                                // 중복되어 나올 수 밖에 없음 ------ 이걸 증상을 방지할려면 처음부터 깊은복사로 bookLForSearch를 만들거나, 책검색(searchList)메소드에서
                                // 다시 for문으로 currentItem들을 초기화하고, 현재 선택된 책장번호[0]에 저장된 번호를 이용해 currentItem을 새로 지정해 색표시 정보를 갱신하는 수밖에 없음
                                // 쓸대없이 로직을 늘리느니 처음부터 addAll()을 이용한 깊은복사로 bookLForSearch를 만드는게 현명한 것 같다. add()는 얕은복사(참조만복사)
                                // 바로위 추가 - addAll()은 리스트 객체만 깊은복사하는것이지 리스트안의 요소객체까지 깊은복사하는것은 아니다. 즉 get(index)로 가져올 수 있는 요소들은
                                // 반복문을 이용해 따로 new 요소명(get(index))형태로 요소마다 새로운 객체를 만들어서 추가해줘야한다. 깊은 복사는 여기까지해야 진짜 깊은 복사..
                                // jsonArray를 gson을 이용해서 new TypeToken()으로 리스트를 생성했을때는 깊은복사여서(완전새객체) 문제가 없었던 거였지 싶다..
                                // bookLForSearch 를 깊은복사로 만들어도 여기서도 searchList.add 할때 깊은 복사해야된다. 책검색에서 bookL에 searchList가 최종적으로 들어가기때문!
                                // searchList에 item을 깊은 복사 안하면 결국 item 객체가 계속 재활용되어(참조객체) currentItem이 true로 계속해서 남기때문에 ui 표시가 중복되서 표시됨.
                            }
                            if(item.getCurrentItem()){
                                Log.e("오류태그", "검색 택스트 바뀌기전: bibleVm.bookLForSearch의 currrentItem: true인 것들이다. "+ item.getBook_name()+item.getCurrentItem()+item.getBook());
                            }
                        }
                        for(BibleDto item : searchList  ) {
                            if(item.getCurrentItem()){
                                Log.e("오류태그", "검색 택스트 바뀌고: currrentItem: true인 것들이다. "+ item.getBook_name()+item.getCurrentItem()+item.getBook());
                            } else if(!item.getCurrentItem()){
//                        Log.e("오류태그", "왜 갱신이 안되니?? currrentItem: false인 것들이다. "+ item);
                            }
                        }
//                        Log.e("오류태그", "책 리스트: "+ bibleVm.bookLForSearch );
//                        Log.e("오류태그", "책 번호: "+ bibleVm.책장번호 );
//                        Log.e("오류태그", "책 검색: "+ searchList);
                        bibleVm.tempObj.addProperty("bookSearchText", newText);
                        bibleVm.책검색(searchList);

                        return true;
                    }
                });

            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
//                switch(menuItem.getItemId()) {
//                    case R.id.app_bar_search: //이건 작동안함. searchView는 createOptionMenu에서 선동작하기 때문인듯. 밑의 로그아웃 같이 일반 메뉴는 잘 작동함.
//                        Log.e("오류태그", "onMenuItemSelected: "+bibleVm.tempObj.get("bookSearchText").getAsString());
//                        ((SearchView)menuItem.getActionView()).setQuery( bibleVm.tempObj.get("bookSearchText").getAsString(), false);
//                        return true;
//                    case R.id.main_toolbar_menu_logout:
//                        Log.e("오류태그", "onMenuItemSelected: "+bibleVm.tempObj.get("bookSearchText").getAsString());
//                        Toast.makeText(requireActivity(), "test",Toast.LENGTH_SHORT).show();
//                        return true;
//                    default:
                return false;
//                }
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rva.notifyDataSetChanged();
        //todo 리사이클러뷰 갱신 dataset~ 책이름 검색으로인한 화면 갱신할때 쓰임
        bibleVm.liveBookL.observe(getViewLifecycleOwner(), new Observer<List<BibleDto>>() {
            @Override
            public void onChanged(List<BibleDto> bibleDtos) {
                rva.notifyDataSetChanged();
                for(BibleDto item : bibleDtos  ) {
                    if(item.getCurrentItem()){
                        Log.e("오류태그", "왜 갱신이 안되니?? currrentItem: true인 것들이다. "+ item.getBook_name()+item.getCurrentItem()+item.getBook());
                    } else if(!item.getCurrentItem()){
//                        Log.e("오류태그", "왜 갱신이 안되니?? currrentItem: false인 것들이다. "+ item);
                      }
                }
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();

    }

/*    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
    }*/

    @Override
    public void onResume() {
        super.onResume();
        rva.notifyDataSetChanged(); //책 리사이클러뷰 갱신
//        MainActivity mainA = ((MainActivity)requireActivity());
//        mainA.binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS); //검색메뉴 보이기
//        mainA.binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setVisible(true);
//        Log.e("오류태그", "북 리쥼 ");
//        Log.e("오류태그", "북 리쥼 after notifydata");

        ((BibleFm) getParentFragment()).binding.bibleToolbarIv.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        super.onPause();
//        MainActivity mainA = ((MainActivity)requireActivity());
//        mainA.binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setShowAsAction(0); //검색메뉴 감추기
//        mainA.binding.mainToolbar.getMenu().findItem(R.id.app_bar_search).setVisible(false);
//        Log.e("오류태그", "북 퍼즈 ");
        ((BibleFm) getParentFragment()).binding.bibleToolbarIv.setVisibility(View.GONE);
    }

    @Override
    public void onStop() {
        super.onStop();


    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}