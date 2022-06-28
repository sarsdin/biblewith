package com.example.androidclient.util;

import com.example.androidclient.bible.BibleDto;
import com.example.androidclient.login.LoginDto;
import com.google.gson.JsonObject;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class Http {

    //retrofit2 클래스 설정
//    private static final String BASE_URL = "https://www.googleapis.com/youtube/v3/";        //DataApi 기본 주소(URL)
    private static final String BASE_URL = "http://";
    public static final String KEY = "AIzaSyCxEDgrF8yd1Vkt-m7zZivxL5cr7nWsIi4";             //개발자 키
    public static final String PART = "snippet";                                            //id, snippet 설정가능한데 snippet 만하면 둘가 가져옴.
    public static final int MAX_RESULTS = 15;
    public static final String ORDER = "date";                                              //정렬을 날짜순으로
    public static final String PUBLISHED_AFTER = "2022-02-01T00:00:00Z";                    //지정된 날짜 이후에 등록된 것만 검색
    public static final String Q = "premier league highlights";                             //검색어
    public static final String TYPE = "video";                                              //비디오타입으로 설정
    public static final boolean VIDEO_EMBEDDABLE = true;                                    //퍼가기 허용인 것만 검색

    public static final String CHANNEL_ID_LALIGA = "UCTv-XvfzLX3i4IGWAm4sbmA";                             //채널 ID - laliga 공식 채널
    public static final String Q_LALIGA = "highlight";                             //검색어


    public static Retrofit getRetrofitInstance(String host){
//        Gson gson = new GsonBuilder().setLenient().create();

        // 로그를 중간에 가로채서 로그캣에 보여줌
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
//        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        interceptor.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL + host + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }


    //retrofit2 service interface 설정
    public interface HttpLogin{
        @GET("search")
        Call<JsonObject> getSearch(@Query("key") String key, @Query("part") String part, @Query("maxResults") int maxResults,
                                   @Query("order") String order, @Query("publishedAfter") String publishedAfter,
                                   @Query("q") String q, @Query("type") String type, @Query("videoEmbeddable") boolean videoEmbeddable);


        // 이메일 중복확인 통신
        @GET("home/isEmailRedundant")
        Call<JsonObject> isEmailRedundant(@Query("user_email") String user_email );

        //회원가입 버튼 클릭시 - 신청
        @GET("home/join")
        Call<JsonObject> joinComplete(@Query("user_email") String user_email,
                                          @Query("user_pwd") String user_pwd,
                                          @Query("user_pwdc") String user_pwdc,
                                          @Query("user_nick") String user_nick,
                                          @Query("user_name") String user_name
        );

        //비번찾기 이메일 인증번호 발송 버튼 클릭시 -
        @GET("home/findpwMailSend")
        Call<JsonObject> findpwMailSend(@Query("user_name") String user_name,
                                        @Query("user_email") String user_email
        );

        //비번찾기 인증번호 확인 버튼 클릭시
        @GET("home/findpwMailVnumConfirm")
        Call<JsonObject> findpwMailVnumConfirm(@Query("findpw_number") String findpw_number,
                                               @Query("name") String name,
                                               @Query("email") String email
        );


        //새 비밀번호 설정 완료 버튼 클릭시
        @GET("home/findpwNewPw")
        Call<JsonObject> findpwNewPw(@Query("user_email") String user_email, @Query("user_pwd") String user_pwd   );

        //로그인 버튼 클릭시
        @GET("home/login")
        Call<LoginDto> login(@Query("user_email") String user_email,
                             @Query("user_pwd") String user_pwd,
                             @Query("user_autologin") Boolean user_autologin
        );

        //자동로그인시 쉐어드에 유저정보가 있으면 서버에서 그 유저 정보 불러오면서 자동로그인하기
        @GET("home/getAutoLoginInfo")
        Call<LoginDto> getAutoLoginInfo(@Query("user_email") String user_email);
    }


    public interface HttpBible{

        //성경책 목록
        @GET("bible/getBookList")
        Call<List<BibleDto>> getBookList();

        //장 목록
        @GET("bible/getChapterList")
        Call<List<BibleDto>> getChapterList(@Query("book") int book
                                         );//,@Query("chapter") int chapter
        //절 목록
        @GET("bible/getVerseList")
        Call<List<BibleDto>> getVerseList(@Query("book")int book, @Query("chapter")int chapter);

        //책 검색 목록
        @GET("bible/getSearchBookList")
        Call<List<BibleDto>> getSearchBookList(@Query("book_name")String newText);
    }





}
