package com.example.androidclient.util;

import com.example.androidclient.bible.BibleDto;
import com.example.androidclient.login.LoginDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
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
    public static final String UPLOADS_URL = "http://15.165.174.226/uploads/";                             //검색어
    public static final String HOST_IP = "15.165.174.226";                             //서버 ip


    public static Retrofit getRetrofitInstance(String host){
        // RFC 4627만을 허용할정도로 엄격한 parse 규칙을 사용하지만 setLenient 를 적용하여 완화해줌. (오류전문)--> Use JsonReader.setLenient(true) to accept malformed JSON at line 1 column 1 path $
        Gson gson = new GsonBuilder().setPrettyPrinting().setLenient().create();

        // 로그를 중간에 가로채서 로그캣에 보여줌
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
//        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        interceptor.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(interceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL + host + "/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        return retrofit;
    }


    //retrofit2 service interface 설정
    public interface HttpLogin{

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


        @Multipart
        @POST("home/userProfileImageSelect")
        Call<JsonObject> 사용자이미지선택(@PartMap Map<String, RequestBody> userInfo,
                                     @Part/*("group_main_image")*/ MultipartBody.Part userImage);

        @POST("home/nickModify")
        Call<JsonObject> 유저닉네임수정( @Query("user_no") int user_no,
                                  @Query("user_nick") String user_nick
        );
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

        //유저 하이라이트 목록
        @GET("bible/getHlList")
        Call<List<BibleDto>> getHlList(@Query("user_no")int user_no );

        //유저 하이라이트 업데이트 목록
        @Headers("content-type: application/json")
//        @FormUrlEncoded
        @POST("bible/getHlUpdate")
        Call<List<BibleDto>> getHlUpdate(@Body Map<String, Object> map);
//        Call<List<BibleDto>> getHlUpdate(@Field("user_no") int user_no, @Field("tmpHighL") List<BibleDto> tmpHighL,
//                                         @Field("delHighL") List<Integer> delHighL);

        //유저 하이라이트 삭제
        @Headers("content-type: application/json")
        @POST("bible/getHlDelete")
        Call<List<BibleDto>> getHlDelete(@Body Map<String, Object> map);


        //유저 노트 목록 가져오기
//        @Headers("content-type: application/json")
        @GET("bible/getNoteList")
        Call<JsonArray> getNoteList(@Query("user_no") int user_no);

        //유저 노트 추가
        @Headers("content-type: application/json")
        @POST("bible/getNoteAdd")
        Call<JsonObject> getNoteAdd(@Body JsonObject noteinfo);

        //유저 노트 수정
        @Headers("content-type: application/json")
        @POST("bible/getNoteUpdate")
        Call<JsonObject> getNoteUpdate(@Body JsonObject noteinfo);


        //유저 노트 삭제
        @Headers("content-type: application/json")
        @POST("bible/deleteNote")
        Call<JsonObject> deleteNote(@Query("note_no") int note_no);
    }


    public interface HttpGroup {
        //모임 만들기
        @Multipart
        @POST("group/createGroup")
        Call<JsonObject> createGroup(@PartMap Map<String, RequestBody> groupInfo,
                                     @Part/*("group_main_image")*/ MultipartBody.Part groupImage);

        @Multipart
        @POST("group/groupProfileImageSelect")
        Call<JsonObject> 모임이미지선택(@PartMap Map<String, RequestBody> groupInfo,
                                  @Part/*("group_main_image")*/ MultipartBody.Part groupImage);

        //모임 목록 가져오기
        @Headers("content-type: application/json")
        @POST("group/getGroupL")
        Call<JsonObject> getGroupL(@Query("user_no") int userNo, @Query("sortState") String sortState);


        //모임 상세 가져오기
        @Headers("content-type: application/json")
        @POST("group/getGroupIn")
        Call<JsonObject> getGroupIn(@Query("group_no") int currentGroupIn, @Query("sortStateGroupIn") String sortStateGroupIn, @Query("user_no") int userNo);

        //모임 게시물 목록 가져오기
        @Headers("content-type: application/json")
        @POST("group/getGroupInL")
        Call<JsonObject> getGroupInL(@Query("group_no") int currentGroupIn, @Query("sortStateGroupIn") String sortStateGroupIn);

        //모임 글쓰기
        @Multipart
        @POST("group/writeGroupIn")
        Call<JsonObject> writeGroupIn(@PartMap Map<String, RequestBody> writeInfo,
                                      @Part List<MultipartBody.Part> writeImage);

        //모임 글수정
        @Multipart
        @POST("group/updateBoardGroupIn")
        Call<JsonObject> updateBoardGroupIn(@PartMap Map<String, RequestBody> updateInfo,
                                            @Part List<MultipartBody.Part> updateImage);

        //모임 글삭제
        @Headers("content-type: application/json")
        @POST("group/deleteBoardGroupIn")
        Call<JsonObject> deleteBoardGroupIn(@Query("gboard_no") int gboard_no, @Query("user_no") int user_no);

        //모임 글 상세 가져오기
        @Headers("content-type: application/json")
        @POST("group/getGboardDetail")
        Call<JsonObject> getGboardDetail(@Query("gboard_no") int gboard_no, @Query("whereIs") String whereIs, @Query("user_no") int userNo);

        //모임 댓글 쓰기
        @Headers("content-type: application/json")
        @POST("group/writeGboardReply")
        Call<JsonObject> writeGboardReply(@Body JsonObject replyInfo);

        //모임 댓글 삭제
        @Headers("content-type: application/json")
        @POST("group/deleteGboardReply")
        Call<JsonObject> deleteGboardReply(@Body JsonObject replyInfo);

        //모임 댓글 수정
        @Headers("content-type: application/json")
        @POST("group/modifyGboardReply")
        Call<JsonObject> modifyGboardReply(@Body JsonObject replyInfo);

        //모임 좋아요 클릭 - insert or delete
        @Headers("content-type: application/json")
        @POST("group/clickGboardLike")
        Call<JsonObject> clickGboardLike(@Query("gboard_no") int gboard_no, @Query("user_no") int user_no);


        //챌린지만들기총분량수계산
        @Headers("content-type: application/json")
        @POST("group/getCountVerseForChalCreate")
        Call<JsonObject> 챌린지만들기총분량수계산(@Body JsonArray params);

        //챌린지만들기완료하기
        @Headers("content-type: application/json")
        @POST("group/createChallenge")
        Call<JsonObject> 챌린지만들기완료하기(@Body JsonObject params);

        //챌린지목록가져오기
        @Headers("content-type: application/json")
        @POST("group/getChallengeList")
        Call<JsonObject> 챌린지목록가져오기(@Query("user_no") int user_no, @Query("group_no") int group_no);


        //챌린지상세목록가져오기
        @Headers("content-type: application/json")
        @POST("group/getChallengeDetailList")
        Call<JsonObject> 챌린지상세목록가져오기(@Query("chal_no") int chal_no, @Query("user_no") int user_no, @Query("group_no") int group_no);


        @Headers("content-type: application/json")
        @POST("group/getChallengeDetailVerseList")
        Call<JsonObject> 챌린지인증진행하기(@Body JsonObject params);

        @Headers("content-type: application/json")
        @POST("group/updateChallengeDetailVerse")
        Call<JsonObject> 챌린지인증체크업데이트(@Body JsonObject params);

        @Multipart
        @POST("group/createChalDetailVideo")
        Call<JsonObject> 챌린지인증영상업로드(@PartMap Map<String, RequestBody> groupInfo,
                                          @Part MultipartBody.Part groupImage);

        @POST("group/createChalDetailVideoFirstWork")
        Call<JsonObject> 챌린지인증영상업로드사전작업(@Query("chal_detail_no") int chal_detail_no);

        @POST("group/chalLikeClicked")
        Call<JsonObject> 챌린지상세좋아요클릭(@Body JsonObject params);


        //////////////모임 초대 관련
        @POST("group/isInviteNumber")
        Call<JsonObject> 모임초대번호있는지확인(@Body JsonObject params);

        @POST("group/memberInviteNumber")
        Call<JsonObject> 모임초대번호재생성요청(@Body JsonObject params);

        @POST("group/memberInviteLink")
        Call<JsonObject> 모임초대링크생성(@Body JsonObject params);


        @POST("group/memberInviteLinkConfirm")
        Call<JsonObject> 모임초대링크유효한지확인(@Query("group_no") int group_no,
                                      @Query("invite_code") String invite_code,
                                      @Query("user_no") int user_no
        );

        @POST("group/memberInviteLinkMemberAdd")
        Call<JsonObject> 모임초대링크로멤버추가하기(@Query("group_no") int group_no,
                                      @Query("invite_code") String invite_code,
                                      @Query("user_no") int user_no
        );

        @POST("group/memberInviteNumberMemberAdd")
        Call<JsonObject> 모임초대번호로멤버추가하기(/*@Query("group_no") int group_no,*/
                                      @Query("invite_code") String invite_code,
                                      @Query("user_no") int user_no
        );
        @POST("group/memberInviteNumberVerify")
        Call<JsonObject> 모임초대번호로멤버추가하기전유효성확인(/*@Query("group_no") int group_no,*/
                                      @Query("invite_code") String invite_code,
                                      @Query("user_no") int user_no
        );

        @POST("group/memberListLoad")
        Call<JsonObject> 모임멤버목록로드(@Body JsonObject params);

        @POST("group/memberOut")
        Call<JsonObject> 모임멤버탈퇴(@Body JsonObject params);

        @POST("group/memberFire")
        Call<JsonObject> 모임멤버추방(@Body JsonObject params);

        @POST("group/memberListSearch")
        Call<JsonObject> 모임멤버검색(@Body JsonObject params);
        
        
        

        //채팅방 만들기 - 만들기 후 채팅방 정보+참가자목록 가져옴
        @Multipart
        @POST("group/chatRoomCreate")
        Call<JsonObject> 채팅방만들기(@PartMap Map<String, RequestBody> chatRoomInfo,
                                  @Part/*("group_main_image")*/ MultipartBody.Part chatRoomImage);

         //채팅방 목록 가져오기
        @POST("group/chatRoomList")
        Call<JsonObject> 채팅방목록(@Body JsonObject params);

        //채팅방 정보 및 채팅 내역 가져오기 - 채팅방 정보+참가자목록, 채팅 내역 목록+채팅쓴사람
        @POST("group/chatRoomJoin")
        Call<JsonObject> 채팅방참가클릭(@Query("chat_room_no") int chat_room_no,
                                 @Query("user_no") int user_no,
                                 @Query("group_no") int group_no
        );



    }



}
