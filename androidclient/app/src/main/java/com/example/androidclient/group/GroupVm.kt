package com.example.androidclient.group

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide.init
import com.example.androidclient.MyApp
import com.example.androidclient.bible.BibleDto
import com.example.androidclient.util.Http
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GroupVm : ViewModel() {

    val host = "15.165.174.226"

    var groupL = JsonArray() //모임 목록
    var liveGroupL = MutableLiveData<JsonArray>()
    var gboardL = JsonArray() //모임 상세페이지 게시물 목록
    var liveGboardL = MutableLiveData<JsonArray>()
    var memberL = JsonArray() //모임 상세페이지 멤버 목록
    var liveMemberL = MutableLiveData<JsonArray>()
    var groupInfo = JsonObject() //모임 상세페이지 모임 정보
    var liveGroupInfo = MutableLiveData<JsonObject>()


    var sortState = "name"  //모임목록페이지 정렬 초기값 모임이름순
    var sortStateGroupIn = "board"  //모임상세페이지 게시물 정렬 초기값 최신게시물순(board) & 최신댓글순(reply)
    var currentGroupIn = 0  //현재 선택된 모임의 pk 번호

    //모임 글쓰기에 쓰이는 변수들
    var groupWriteImageUriL : List<Uri>? = null

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
                        Log.e("[GroupVm]", "모임목록가져오기 onResponse: $res")
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
        val call = httpGroup.getGroupIn(currentGroupIn, sortStateGroupIn)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            call.enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    if (response.isSuccessful) {
                        val res = response.body()!!
                        Log.e("[GroupVm]", "모임상세불러오기 onResponse: $res")
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
//                        Log.e("[GroupVm]", "모임글쓰기 onResponse: $res")
//                        groupVm.noteL = res;
//                        groupVm.liveNoteL.value = bibleVm.noteL
                    }
                }
                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    Log.e("[GroupVm]", "모임글쓰기 onFailure: " + t.message)
                }
            })
        }
        return call
    }

}