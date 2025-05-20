package jm.preversion.biblewith.bible;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import jm.preversion.biblewith.MyApp;
import jm.preversion.biblewith.bible.dto.BibleBtsDto;
import jm.preversion.biblewith.bible.dto.BibleDto;
import jm.preversion.biblewith.util.Http;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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

    public String host = Http.HOST_IP;

    public MutableLiveData<List<BibleDto>> liveBookL = new MutableLiveData<>(); //책이름 검색에서 쓰임
    public MutableLiveData<List<BibleDto>> liveVerseL = new MutableLiveData<>();
    public MutableLiveData<List<BibleDto>> liveHighL = new MutableLiveData<>();
    public MutableLiveData<JsonArray> liveNoteL = new MutableLiveData<>();
    public MutableLiveData<int[]> live책장번호 = new MutableLiveData<>();
    public List<BibleDto> bookL = new ArrayList<BibleDto>(); //책이름목록
    public List<BibleDto> bookLForSearch = new ArrayList<BibleDto>(); //책이름목록 - 툴바 검색용: 변화없이 고정값을 가져야함
    public List<BibleDto> chapterL = new ArrayList<BibleDto>(); //장목록
    public List<BibleDto> verseL = new ArrayList<BibleDto>(); //절목록
    public List<BibleDto> highL = new ArrayList<BibleDto>(); //유저 하이라이트 목록
    public JsonArray noteL = new JsonArray(); //유저 노트 목록 - 노트목록가져오기()에서 가져옴
    public List<BibleBtsDto> colorL = new ArrayList<BibleBtsDto>(); //BtsR의 하이라이트 색깔 목록


    public int[] 책장번호 = new int[]{1, 1, 1}; //todo 추후 유저테이블에 3개의 컬럼 추가후 이 데이터(마지막봤던)를 서버로 insert해줌
    public boolean onceExecuted = false; //한번실행 후 스크롤 처리 x - BibleVerseFm
    public JsonObject noteUpdateO = new JsonObject(); //노트 수정용 - 구조내용: note table + note_verseL:Array(noteverse & bible_korHRV)
    public JsonObject tempObj = new JsonObject(); //임시데이터 - 자유롭게 사용가능 - 임시라 1회용 명령에만 쓰임 반드시 작업을 한번에 깔끔히 마무리해야함! - 툴바책검색(bookSearchText)

    {
        순차색(); //colorL에 색깔정보 dto 채우기 - 초기화
        하이라이트목록가져오기(); //서버에서 이 사용자의 하이라이트 목록을 받아와 highL 데이터 초기화하기 - 이후 이리스트의 색깔 정보를 이용해 verseL에 추가하기 위함.
        노트목록가져오기(true);
        쉐어드에서책장절저장값가져오기("book");
        쉐어드에서책장절저장값가져오기("chapter");
        쉐어드에서책장절저장값가져오기("verse");
        live책장번호.setValue(책장번호);
        성경책목록가져오기(); //서버에서 가져와 bookL에 책이름 정보를 넣음
//        장목록가져오기(책장번호[0], "vmInit");  //가져올때는 창세기의 번호가 1 이니깐 인덱스0번에 +1을 하여 db에서 조회해야함. -- 6/27다시 +1안하게 고침
//        절목록가져오기(책장번호[0],책장번호[1]);
        tempObj.addProperty("signal", ""); //초기화
        tempObj.addProperty("bookSearchText", ""); //초기화
    }

    //보던 페이지 정보를 가져옴
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
            if (item.getCurrentItem()) {
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
        live책장번호.setValue(책장번호); //BibleFm의 observer 로 title ui 변경위해 livedata update
        쉐어드에책장절저장("book");
    }

    //장 홀더 클릭시
    public void 장번호업데이트(int chapter) {  //position은 bindholder 인덱스가 0으로 시작하기에 bind()메소드에서 인수를 position+1해서 여기로 넘어옴. -- 다시 변경 그냥 chapter번호로
        for (BibleDto item: chapterL) {
            if (item.getCurrentItem()) {
                item.setCurrentItem(false); //ui true 이면 false 으로 바꿈
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
            if (item.getCurrentItem()) {
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

    //BibleBookFm - onCreateOptionsMenu
    public void 책검색( List<BibleDto> searchL ) {
        bookL = searchL;
        for (BibleDto item: bookL) {
            if (item.getBook() == 책장번호[0] ) {
                item.setCurrentItem(true); //현재 검색된 제목 리스트들 중에 현재 클릭된 책이랑 같은 번호가 있는지 확인후 있으면 그 책아이템을 현재 선택표시되게 해야함
                Log.e("BibleVm", "책검색() setCurrentItem(true): "+item);
            } else {
//                item.setCurrentItem(false); //같은 번호가 아닌 것들은 모두 false 처리해서 색ui 업데이트때 표시안되게 해야함
//                Log.e("BibleVm", "책검색() setCurrentItem(false): "+item);
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

                    List<BibleDto> tmpL = new ArrayList<BibleDto>();
                    for (BibleDto item: res) {
                        tmpL.add(new BibleDto(item));//깊은복사로 리스트안의 객체까지 독립객체로 생성. 필수! 중요!!
                        if (item.getBook() == 책장번호[0] ) {
                            item.setCurrentItem(true); // 차후 선택된 아이템의 ui(글자색)을 변경하게 할때 체크된 것만 변경하게함.(ui변경용)
//                            Log.e("BibleVm", "성경책목록가져오기(): "+item);
                        }
                    }
                    //책이름 목록에 받은 정보 업데이트
                    bookL = res;
//                Log.e("[BibleVm]", "성경책목록가져오기 tmpL: "+ tmpL );
                    bookLForSearch = tmpL;
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
                            item.setCurrentItem(true); //클릭된 포지션을 변화 - 북,장 페이지의 선택된 ui 표시용으로 쓰임
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
                            item.setCurrentItem(true); //클릭된 포지션을 변화 - 북,장 페이지의 선택된 ui 표시용으로 쓰임. 절은 어디서 쓰이지? 찾아봐야함
//                            Log.e("BibleVm", "절목록가져오기(): "+item);
                        }
                    }

                    // 목록안에 받은 정보 업데이트
                    verseL = res;

                    //verseL 에 색깔 정보 업데이트하기
                    for(BibleDto item : verseL  ) {
                        for(BibleDto itemIner : highL  ) {
                            if (item.getBible_no() == itemIner.getBible_no()) {
                                item.setHighlight_color(itemIner.getHighlight_color());
                                break;
                            }
                        }
                    }

                    liveVerseL.setValue(verseL);
//                    Log.e("[BibleVm]", "절목록가져오기 onResponse: "+ verseL );
                }
            }
            @Override
            public void onFailure(@NonNull Call<List<BibleDto>> call, Throwable t) {
                Log.e("[BibleVm]", "절목록가져오기 onFailure: "+ t.getMessage() );
            }
        });
        return call;
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// 하이라이트, 노트

    public Call<List<BibleDto>> 하이라이트목록가져오기() {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<List<BibleDto>> call = httpBible.getHlList(MyApp.userInfo.getUser_no());
        call.enqueue(new Callback<List<BibleDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<BibleDto>> call, @NonNull Response<List<BibleDto>> response) {
                if (response.isSuccessful()) {
                    List<BibleDto> res = response.body();
                    highL = res;
                    liveHighL.setValue(highL);
                }
            }
            @Override
            public void onFailure(Call<List<BibleDto>> call, Throwable t) {
                Log.e("[BibleVm]", "하이라이트목록가져오기 onFailure: "+ t.getMessage() );

            }
        });
        return call;
    }

    public Call<JsonArray> 노트목록가져오기(Boolean isExeInVm) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<JsonArray> call = httpBible.getNoteList(MyApp.userInfo.getUser_no());
        if(isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(new Callback<JsonArray>() {
                @Override
                public void onResponse(@NonNull Call<JsonArray> call, @NonNull Response<JsonArray> response) {
                    if (response.isSuccessful()) {
                        JsonArray res = response.body();
                        noteL = res;
                        liveNoteL.setValue(noteL);
                    }
                }
                @Override
                public void onFailure(Call<JsonArray> call, Throwable t) {
                    Log.e("[BibleVm]", "노트목록가져오기 onFailure: "+ t.getMessage() );

                }
            });
        }
        return call;
    }

    public Call<JsonObject> 노트추가(JsonObject noteinfo, Boolean isExeInVm) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<JsonObject> call = httpBible.getNoteAdd(noteinfo);
        if(isExeInVm){ //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        JsonObject res = response.body();
    //                    highL = res;
    //                    liveHighL.setValue(highL);
                    }
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("[BibleVm]", "노트추가 onFailure: "+ t.getMessage() );

                }
            });
        }
        return call;
    }

    public Call<JsonObject> 노트수정(JsonObject noteinfo, Boolean isExeInVm) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<JsonObject> call = httpBible.getNoteUpdate(noteinfo);
        if(isExeInVm){ //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        JsonObject res = response.body();
                        //                    highL = res;
                        //                    liveHighL.setValue(highL);
                    }
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("[BibleVm]", "노트수정 onFailure: "+ t.getMessage() );

                }
            });
        }
        return call;
    }

    public Call<JsonObject> 노트삭제(int note_no, Boolean isExeInVm) {
        Retrofit retrofit = Http.getRetrofitInstance(host);
        Http.HttpBible httpBible = retrofit.create(Http.HttpBible.class); // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        Call<JsonObject> call = httpBible.deleteNote(note_no);
        if(isExeInVm){ //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                    if (response.isSuccessful()) {
                        JsonObject res = response.body();
                    }
                }
                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    Log.e("[BibleVm]", "노트삭제 onFailure: "+ t.getMessage() );

                }
            });
        }
        return call;
    }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    void 순차색() {
        String c0 = "#FFFF96";
        String c1 = "#FFD3D3";
        String c2 = "#FFDCFF";
        String c3 = "#65FFBA";
        String c4 = "#ACFFEF";
        String c5 = "#6DD66D";
        String c6 = "#C8FFFF";
        String c7 = "#28E7FF";
        String c8 = "#FF8A19";
        String c9 = "#FF71F8";
        String[] c = new String[]{ c0, c1, c2, c3, c4, c5, c6, c7, c8, c9 };
//        this.colorL = Color.parseColor(c[position]);

        BibleBtsDto firstDto = new BibleBtsDto();
        firstDto.setViewType(2); //제일 처음에 보여지게될 뷰홀더의 Dto 정보를 먼저 추가해 넣음.
        firstDto.setHighlight_color(Color.parseColor("#FFFFFF")); //-1 로 해석됨(흰색)
        colorL.add(firstDto);

        // 배열 c 에 담긴 색깔들을 colorL 리스트에 색깔 변환기로 분석하여 dto에 담아 추가한다.
        for (String item: c) {
            BibleBtsDto dto = new BibleBtsDto();
            dto.setHighlight_color(Color.parseColor(item));
            colorL.add(dto);
        }
//        this.colorL.add()
//        return c[position]
    }

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
