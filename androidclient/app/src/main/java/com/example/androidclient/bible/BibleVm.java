package com.example.androidclient.bible;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.androidclient.MyApp;
import com.example.androidclient.util.Http;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class BibleVm extends ViewModel {

    private String host = "15.165.174.226";

    public MutableLiveData<List<BibleDto>> liveBookL = new MutableLiveData<>();
    public MutableLiveData<List<BibleDto>> liveVerseL = new MutableLiveData<>();
    public MutableLiveData<int[]> live책장번호 = new MutableLiveData<>();
    public List<BibleDto> bookL = new ArrayList<BibleDto>(); //책이름목록
    public List<BibleDto> chapterL = new ArrayList<BibleDto>(); //장목록
    public List<BibleDto> verseL = new ArrayList<BibleDto>(); //절목록

    public int[] 책장번호 = new int[]{1, 1, 1}; //todo 추후 유저테이블에 3개의 컬럼 추가후 이 데이터(마지막봤던)를 서버로 insert해줌
    public boolean onceExecuted = false; //한번실행 후 스크롤 처리 x - BibleVerseFm

    {
        쉐어드에서책장절저장값가져오기("book");
        쉐어드에서책장절저장값가져오기("chapter");
        쉐어드에서책장절저장값가져오기("verse");
        live책장번호.setValue(책장번호);
//        성경책목록가져오기(); //서버에서 가져와 bookL에 책이름 정보를 넣음
//        장목록가져오기(책장번호[0], "vmInit");  //가져올때는 창세기의 번호가 1 이니깐 인덱스0번에 +1을 하여 db에서 조회해야함. -- 6/27다시 +1안하게 고침
//        절목록가져오기(책장번호[0],책장번호[1]);
    }

    public void 쉐어드에서책장절저장값가져오기(String 저장종류){
        SharedPreferences sp = null;
        if (저장종류.equals("book")) {
            sp = MyApp.getApplication().getSharedPreferences("booksave", Context.MODE_PRIVATE);
        } else if (저장종류.equals("chapter")) {
            sp = MyApp.getApplication().getSharedPreferences("chaptersave", Context.MODE_PRIVATE);
        } else if (저장종류.equals("verse")) {
            sp = MyApp.getApplication().getSharedPreferences("versesave", Context.MODE_PRIVATE);
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create(); //json직렬화 : java obj -> json
        JsonObject save = new JsonObject();
        String savedInfo = sp.getString(MyApp.getUserInfo().getUser_email(), "{}"); //이메일에 해당하는 저장된 데이터 가져옴
        save = JsonParser.parseString(savedInfo).getAsJsonObject(); //문자열을 파서로 json obj 로 변환하여 사용.
        if (저장종류.equals("book")) {
            책장번호[0] = save.get("book")!=null? save.get("book").getAsInt():1;
        } else if (저장종류.equals("chapter")) {
            책장번호[1] = save.get("chapter")!=null? save.get("chapter").getAsInt():1;
        } else if (저장종류.equals("verse")) {
            책장번호[2] = save.get("verse")!=null? save.get("verse").getAsInt():1;
        }

        Log.e("BibleVm", "쉐어드에서책장절저장값가져오기(): "+ Arrays.toString(책장번호));
    }

    //책 홀더 클릭시
    public void 책장번호업데이트(int book) {
        for (BibleDto item: bookL) {
            if (item.isCurrentItem()) {
                item.setCurrentItem(false); //true 이면 false 으로 바꿈
            }
            if (item.getBook() == book ) {
                item.setCurrentItem(true); //클릭된 포지션을 변화
                Log.e("BibleVm", "책장번호업데이트(): "+item);
            }
        }
        책장번호[0] = book; //책번호 저장
        책장번호[1] = 1; //장번호 초기화
        책장번호[2] = 1; //절번호 초기화
        live책장번호.setValue(책장번호); //BibleFm의 observer 로 ui 변경위해 livedata update
        쉐어드에책장절저장("book");
    }

    //장 홀더 클릭시
    public void 장번호업데이트(int chapter) {  //position은 bindholder 인덱스가 0으로 시작하기에 bind()메소드에서 인수를 position+1해서 여기로 넘어옴. -- 다시 변경 그냥 chapter번호로
        for (BibleDto item: chapterL) {
            if (item.isCurrentItem()) {
                item.setCurrentItem(false); //true 이면 false 으로 바꿈
            }
            if (item.getChapter() == chapter ) {
                item.setCurrentItem(true); //클릭된 포지션을 변화
                Log.e("BibleVm", "장번호업데이트(): "+item);
            }
        }
        책장번호[1] = chapter; //장번호 저장
        책장번호[2] = 1; //절번호 초기화
        live책장번호.setValue(책장번호);//BibleFm의 observer 로 ui 변경위해 livedata update
        쉐어드에책장절저장("chapter");
    }

    //절 홀더 클릭시
    public void 절번호업데이트(int verse) {
        for (BibleDto item: verseL) {
            if (item.isCurrentItem()) {
                item.setCurrentItem(false); //true 이면 false 으로 바꿈
            }
            if (item.getChapter() == verse ) {
                item.setCurrentItem(true); //클릭된 포지션을 변화
                Log.e("BibleVm", "절번호업데이트(): "+item);
            }
        }
        책장번호[2] = verse; //장번호 저장
        live책장번호.setValue(책장번호);//BibleFm의 observer 로 ui 변경위해 livedata update
        쉐어드에책장절저장("verse");
    }

    //책장절 번호 갱신시 앱껏다켜도 다시 그 위치에서 볼 수 있도록 쉐어드에 저장
    public void 쉐어드에책장절저장(String 저장종류){
        SharedPreferences sp = null;
        if (저장종류.equals("book")) {
             sp = MyApp.getApplication().getSharedPreferences("booksave", Context.MODE_PRIVATE);
        } else if (저장종류.equals("chapter")) {
             sp = MyApp.getApplication().getSharedPreferences("chaptersave", Context.MODE_PRIVATE);
        } else if (저장종류.equals("verse")) {
             sp = MyApp.getApplication().getSharedPreferences("versesave", Context.MODE_PRIVATE);
        }
        SharedPreferences.Editor spEditor = sp.edit();
        Gson gson = new GsonBuilder().setPrettyPrinting().create(); //json직렬화 : java obj -> json
        JsonObject save = new JsonObject();
        sp.getString(MyApp.getUserInfo().getUser_email(), ""); //
        save.addProperty("user_email", MyApp.getUserInfo().getUser_email());
        if (저장종류.equals("book")) {
            save.addProperty("book", 책장번호[0]);
        } else if (저장종류.equals("chapter")) {
            save.addProperty("chapter", 책장번호[1]);
        } else if (저장종류.equals("verse")) {
            save.addProperty("verse", 책장번호[2]);
        }
        spEditor.putString(MyApp.getUserInfo().getUser_email(), save.toString()).apply(); //저장하는 키값을 이메일(id)로 이용
    }

    public void 책검색( List<BibleDto> searchL ) {
        bookL = searchL;
        for (BibleDto item: bookL) {
            if (item.getBook() == 책장번호[0] ) {
                item.setCurrentItem(true); //클릭된 포지션을 변화
                Log.e("BibleVm", "책검색(): "+item);
            }
        }
        liveBookL.setValue(bookL); //ui 업뎃
    }


    public void getBookList(){
    }



/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Http


    public Call<List<BibleDto>> 성경책목록가져오기() { //책번호, 이름, 신구약
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<List<BibleDto>> call = httpBible.getBookList();
        call.enqueue(new Callback<List<BibleDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<BibleDto>> call, @NonNull Response<List<BibleDto>> response) {
                if (response.isSuccessful()) {
                    List<BibleDto> res = response.body();
                    //데이터를 불러오면 현재 클릭된 정보를 담는 책장번호[]에 저장된 책번호를 ui 적용위해 체크. 다만, get()처럼 인덱스가 0부터 시작되면 -1해줘야됨 창세기가 1번부터 시작이라 0번 인덱스로 만들어줘야하기때문
//                    res.get(책장번호[0]-1).setCurrentItem(true);
                    for (BibleDto item: res) {
                        if (item.getBook() == 책장번호[0] ) {
                            item.setCurrentItem(true); // 차후 선택된 아이템의 ui(글자색)을 변경하게 할때 체크된 것만 변경하게함.(ui변경용)
//                            Log.e("BibleVm", "성경책목록가져오기(): "+item);
                        }
                    }
                    //책이름 목록에 받은 정보 업데이트
                    bookL = res;
                    liveBookL.setValue(bookL);
//                    Log.e("[BibleVm]", "성경책목록가져오기 onResponse: "+ res );
                }
            }
            @Override
            public void onFailure(Call<List<BibleDto>> call, Throwable t) {
                Log.e("[BibleVm]", "성경책목록가져오기 onFailure: "+ t.getMessage() );

            }
        });
        return call;
    }



    public Call<List<BibleDto>> 장목록가져오기(int book, String 가져오는위치) { //장번호
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<List<BibleDto>> call = httpBible.getChapterList( book );
        call.enqueue(new Callback<List<BibleDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<BibleDto>> call, @NonNull Response<List<BibleDto>> response) {
                if (response.isSuccessful()) {
                    List<BibleDto> res = response.body();
//                    if (가져오는위치.equals("BibleBookVh")) {
//                        res.get(0).setCurrentItem(true); //데이터를 불러오면 현재 클릭된 정보를 담는 책장번호[]에 저장된 책번호를 ui 적용위해 체크
//
//                    } else if (가져오는위치.equals("vmInit")){
//                        res.get(0).setCurrentItem(true);    //현재는 0인데 쉐어드에 저장 후 가져오는 걸로 로직바꾸면 책장번호[1]-1 과 같은 형식으로 바꿔야함
//                    }
                    for (BibleDto item: res) {
                        if (item.getChapter() == 책장번호[1] ) {
                            item.setCurrentItem(true); //클릭된 포지션을 변화
//                            Log.e("BibleVm", "장목록가져오기(): "+item);
                        }
                    }
                    //책이름 목록에 받은 정보 업데이트
                    chapterL = res;
//                    Log.e("[BibleVm]", "장목록가져오기 onResponse: "+ res );
//                    liveChapterL.setValue(chapterL);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<BibleDto>> call, Throwable t) {
                Log.e("[BibleVm]", "장목록가져오기 onFailure: "+ t.getMessage() );

            }
        });
        return call;
    }

    public Call<List<BibleDto>> 절목록가져오기(int book, int chapter) { //장번호
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<List<BibleDto>> call = httpBible.getVerseList( book, chapter );
        call.enqueue(new Callback<List<BibleDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<BibleDto>> call, @NonNull Response<List<BibleDto>> response) {
                if (response.isSuccessful()) {
                    List<BibleDto> res = response.body();
                    for (BibleDto item: res) {
                        if (item.getChapter() == 책장번호[2] ) {
                            item.setCurrentItem(true); //클릭된 포지션을 변화
//                            Log.e("BibleVm", "절목록가져오기(): "+item);
                        }
                    }
                    // 목록안에 받은 정보 업데이트
                    verseL = res;
//                    Log.e("[BibleVm]", "절목록가져오기 onResponse: "+ res );

                    liveVerseL.setValue(verseL);
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<BibleDto>> call, Throwable t) {
                Log.e("[BibleVm]", "절목록가져오기 onFailure: "+ t.getMessage() );
            }
        });
        return call;
    }



//    public Call<List<BibleDto>> 책검색(String newText) {
//        Retrofit retrofit = Http.getRetrofitInstance(host);
//        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
//        Call<List<BibleDto>> call = httpBible.getSearchBookList( newText );
//        call.enqueue(new Callback<List<BibleDto>>() {
//            @Override
//            public void onResponse(@NonNull Call<List<BibleDto>> call, @NonNull Response<List<BibleDto>> response) {
//                if (response.isSuccessful()) {
//                    List<BibleDto> res = response.body();
//                    for (BibleDto item: res) {
//                        if (item.getChapter() == 책장번호[2] ) {
//                            item.setCurrentItem(true); //클릭된 포지션을 변화
//                            Log.e("BibleVm", "책검색(): "+item);
//                        }
//                    }
//                    // 목록안에 받은 정보 업데이트
//                    verseL = res;
////                    Log.e("[BibleVm]", "절목록가져오기 onResponse: "+ res );
//                }
//            }
//            @Override
//            public void onFailure(@NonNull Call<List<BibleDto>> call, Throwable t) {
//                Log.e("[BibleVm]", "책검색 onFailure: "+ t.getMessage() );
//            }
//        });
//        return call;
//    }

}