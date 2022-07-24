package com.example.androidclient.group

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.androidclient.MyApp
import com.example.androidclient.util.Http
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class GroupVm : ViewModel() {

    val host = "15.165.174.226"
    val gson = GsonBuilder().setPrettyPrinting().create()

    var groupL = JsonArray() //모임 목록
    var liveGroupL = MutableLiveData<JsonArray>()
    var gboardL = JsonArray() //모임 상세페이지 게시물 목록
    var liveGboardL = MutableLiveData<JsonArray>()
    var memberL = JsonArray() //모임 상세페이지 멤버 목록
    var liveMemberL = MutableLiveData<JsonArray>()
    var groupInfo = JsonObject() //모임 상세페이지 모임요약 정보
    var liveGroupInfo = MutableLiveData<JsonObject>()

    var gboardInfo = JsonObject() //모임 게시물 디테일 정보 - 내용(글,좋아요수,히트수,) + 이미지목록 + 댓글 목록
    var liveGboardInfo = MutableLiveData<JsonObject>()
    var gboardReplyL = JsonArray() //모임 게시물 디테일 댓글 정보
    var liveGboardReplyL = MutableLiveData<JsonArray>()


    var sortState = "name"  //모임목록페이지 정렬 초기값 모임이름순
    var sortStateGroupIn = "board"  //모임상세페이지 게시물 정렬 초기값 최신게시물순(board) & 최신댓글순(reply)
    var clickedReplyNoGroupIn = 0  //모임상세페이지 게시물 댓글 클릭시 댓글번호 - 글상세에서 그댓글존재시 번호위치로 스크롤이동
    var currentGroupIn = 0  //현재 선택된 모임의 pk 번호

    //모임 글쓰기,수정에 쓰이는 변수들
    var groupWriteImageUriL : List<Uri>? = null //이미지+ 버튼 클릭시 받아오는 데이터 대입
    var gboardUpdateO = JsonObject() //모임 글 수정용 임시객체
    //모임 글의 댓글에 쓰이는 변수 - snackBar
    var currentReplyNoToReply = 0 //해당 댓글의 번호pk
    var currentUserNoToReply = 9999 //댓글과 답글 달시 - 현재 답글 버튼(홀더안에 답글버튼) 누를 시 그 대상의 번호를 넣어줘야함.9999일때는 답글이 아닌 댓글로 판단한다. 댓글 restapi를 보내야함
    var currentReplyGroupNoToReply = 0 //댓글 그룹번호도 같이 보낸다.

    init {
        모임목록가져오기(MyApp.userInfo.user_no, true)
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// http 통신

    fun 모임만들기(groupInfo: Map<String, RequestBody>, groupImage: MultipartBody.Part, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.createGroup(groupInfo, groupImage)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()
//                        Log.e("[GroupVm]", "모임만들기 onResponse: $res")
//                        groupVm.noteL = res;
//                        groupVm.liveNoteL.value = bibleVm.noteL
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e("[GroupVm]", "모임만들기 onFailure: " + t.message)
                }
            })
        }
        return call
    }


    fun 모임목록가져오기(user_no :Int ,  isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.getGroupL(user_no, sortState)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
//                        Log.e("[GroupVm]", "모임목록가져오기 onResponse: $res")
                        groupL = res.get("result").asJsonArray
                        liveGroupL.value = groupL
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e("[GroupVm]", "모임목록가져오기 onFailure: " + t.message)
                }
            })
        }
        return call
    }

    fun 모임상세불러오기(isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.getGroupIn(currentGroupIn, sortStateGroupIn, MyApp.userInfo.user_no)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
//                        Log.e("[GroupVm]", "모임상세불러오기 onResponse: ${gson.toJson(res)}")
                        gboardL = res.get("result").asJsonObject.get("gboardL").asJsonArray
                        liveGboardL.value = gboardL
                        memberL = res.get("result").asJsonObject.get("memberL").asJsonArray
                        liveMemberL.value = memberL
                        groupInfo = res.get("result").asJsonObject.get("0").asJsonObject
                        liveGroupInfo.value = groupInfo
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e("[GroupVm]", "모임상세불러오기 onFailure: " + t.message)
                }
            })
        }
        return call
    }


    fun 모임글쓰기(writeInfo: Map<String, RequestBody>, writeImage: List<MultipartBody.Part>, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.writeGroupIn(writeInfo, writeImage)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e("[GroupVm]", "모임글쓰기 onFailure: " + t.message)
                }
            })
        }
        return call
    }

    fun 모임글수정(updateInfo: Map<String, RequestBody>, updateImage: List<MultipartBody.Part>, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.updateBoardGroupIn(updateInfo, updateImage)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e("[GroupVm]", "모임글쓰기 onFailure: " + t.message)
                }
            })
        }
        return call
    }

     fun 모임글삭제(gboard_no: Int,  isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.deleteBoardGroupIn(gboard_no, MyApp.userInfo.user_no)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e("[GroupVm]", "모임글삭제 onFailure: " + t.message)
                }
            })
        }
        return call
    }

     suspend fun 모임글상세가져오기(gboard_no: Int, whereIs : String, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.getGboardDetail(gboard_no, whereIs, MyApp.userInfo.user_no)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            gboardInfo = res.get("result").asJsonObject.get("gboardInfo").asJsonObject
                            liveGboardInfo.value = gboardInfo
                            gboardReplyL = res.get("result").asJsonObject.get("gboardReplyL").asJsonArray
                            liveGboardReplyL.value = gboardReplyL
//                            Log.e("[GroupVm]", "모임글상세가져오기 완료: $res")
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "모임글상세가져오기 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }


    suspend fun 모임댓글쓰기(replyInfo: JsonObject,  isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.writeGboardReply(replyInfo)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
//                            gboardInfo = res.get("result").asJsonObject.get("gboardInfo").asJsonObject
//                            liveGboardInfo.value = gboardInfo
//                            gboardReplyL = res.get("result").asJsonObject.get("gboardReplyL").asJsonArray
//                            liveGboardReplyL.value = gboardReplyL
//                            Log.e("[GroupVm]", "모임글상세가져오기 완료: $res")
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "모임글상세가져오기 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }


    suspend fun 모임좋아요클릭(gboard_no:Int, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.clickGboardLike(gboard_no, MyApp.userInfo.user_no)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "모임좋아요클릭 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }

    suspend fun 모임댓글삭제(params: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.deleteGboardReply(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "모임댓글삭제 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }

    suspend fun 모임댓글수정(params: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.modifyGboardReply(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "모임댓글삭제 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }

}


//      result - 모임상세불러오기() 데이터 구조
//{
//    "result": {
//    "0": {
//    "group_no": "1",
//    "chat_room_no": null,
//    "user_no": "0",
//    "group_name": "테스트모임1",
//    "group_desc": "설명1",
//    "group_main_image": "20220716/1657964677_583fb954bab2aefb90d1.jpg",
//    "create_date": "2022-07-16 18:44:37"
//},
//    "gboardL": [
//    {
//        "gboard_no": "6",
//        "group_no": "1",
//        "user_no": "0",
//        "gboard_title": null,
//        "gboard_content": "테스트6",
//        "create_date": "2022-07-19 21:20:22",
//        "user_email": "sjeys14@gmail.com",
//        "user_pwd": "!",
//        "user_nick": "정목",
//        "user_create_date": "2022-06-21 12:26:20",
//        "user_name": "설정목",
//        "user_image": null,
//          "is_like" : false ,
//          "gboard_like_count" : "11" ,
//        "gboard_image": [
//        {
//            "gboard_image_no": "1",
//            "gboard_no": "6",
//            "original_file_name": null,
//            "stored_file_name": "20220719/1658233222_b166279651bca7727557.jpeg",
//            "file_size": "92048",
//            "create_date": "2022-07-19 21:20:22"
//        },
//        {
//            "gboard_image_no": "2",
//            "gboard_no": "6",
//            "original_file_name": null,
//            "stored_file_name": "20220719/1658233222_22ef7a261355e1238758.jpeg",
//            "file_size": "107520",
//            "create_date": "2022-07-19 21:20:22"
//        }
//        ]
//    },
//    {
//        "gboard_no": "5",
//        "group_no": "1",
//        "user_no": "0",
//        "gboard_title": null,
//        "gboard_content": "테스트5",
//        "create_date": "2022-07-19 21:05:41",
//        "user_email": "sjeys14@gmail.com",
//        "user_pwd": "!",
//        "user_nick": "정목",
//        "user_create_date": "2022-06-21 12:26:20",
//        "user_name": "설정목",
//        "user_image": null,
//        "gboard_image": []
//    },
//    {
//        "gboard_no": "4",
//        "group_no": "1",
//        "user_no": "0",
//        "gboard_title": null,
//        "gboard_content": "테스트4",
//        "create_date": "2022-07-19 20:43:00",
//        "user_email": "sjeys14@gmail.com",
//        "user_pwd": "!",
//        "user_nick": "정목",
//        "user_create_date": "2022-06-21 12:26:20",
//        "user_name": "설정목",
//        "user_image": null,
//        "gboard_image": []
//    },
//    {
//        "gboard_no": "3",
//        "group_no": "1",
//        "user_no": "0",
//        "gboard_title": null,
//        "gboard_content": "테스트3",
//        "create_date": "2022-07-19 20:41:05",
//        "user_email": "sjeys14@gmail.com",
//        "user_pwd": "!",
//        "user_nick": "정목",
//        "user_create_date": "2022-06-21 12:26:20",
//        "user_name": "설정목",
//        "user_image": null,
//        "gboard_image": []
//    },
//    {
//        "gboard_no": "2",
//        "group_no": "1",
//        "user_no": "0",
//        "gboard_title": null,
//        "gboard_content": "테스트2",
//        "create_date": "2022-07-19 20:37:24",
//        "user_email": "sjeys14@gmail.com",
//        "user_pwd": "!",
//        "user_nick": "정목",
//        "user_create_date": "2022-06-21 12:26:20",
//        "user_name": "설정목",
//        "user_image": null,
//        "gboard_image": []
//    },
//    {
//        "gboard_no": "1",
//        "group_no": "1",
//        "user_no": "0",
//        "gboard_title": null,
//        "gboard_content": "테스트1",
//        "create_date": "2022-07-19 20:31:05",
//        "user_email": "sjeys14@gmail.com",
//        "user_pwd": "!",
//        "user_nick": "정목",
//        "user_create_date": "2022-06-21 12:26:20",
//        "user_name": "설정목",
//        "user_image": null,
//        "gboard_image": []
//    }
//    ],
//    "memberL": [
//    {
//        "group_no": "1",
//        "user_no": "0",
//        "user_email": "sjeys14@gmail.com",
//        "user_pwd": "!",
//        "user_nick": "정목",
//        "user_create_date": "2022-06-21 12:26:20",
//        "user_name": "설정목",
//        "user_image": null
//    }
//    ]
//},
//    "msg": "ok"
//}