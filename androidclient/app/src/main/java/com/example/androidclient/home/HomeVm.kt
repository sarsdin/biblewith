package com.example.androidclient.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.example.androidclient.MyApp
import com.example.androidclient.util.Http
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.unsplash.pickerandroid.photopicker.data.UnsplashPhoto
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class HomeVm : ViewModel() {

    val tagName = "[HomeVm]"
    val gson = Gson()
    var unsplashL = mutableListOf<UnsplashPhoto>()
    val liveUnsplashL = MutableLiveData<MutableList<UnsplashPhoto>>()

    var todayVerse = JsonObject()   //홈 페이지 성경 일독 : 가져올때마다 랜덤으로 구절이 변함
    val liveTodayVerse = MutableLiveData<JsonObject>()

    var unsplashRandomL = JsonArray()   //홈 페이지 이미지 : api에서 10개식 랜덤으로 가져오기
    val liveUnsplashRandomL = MutableLiveData<JsonArray>()

//    val text: LiveData<String>
//        get() = mText

    init {
//        mText = new MutableLiveData<>();
//        mText.value = "home"
    }





    fun 성경일독(isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(Http.HOST_IP)
        val httpHome = retrofit.create(Http.HttpHome::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpHome.성경일독(/*MyApp.userInfo.user_no*/)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
//            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            todayVerse = res.get("result").asJsonObject
                            liveTodayVerse.value = todayVerse
                            Log.e(tagName, "성경일독 onResponse: ${gson.toJson(res)}")
//                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e(tagName, "성경일독 onFailure: " + t.message)
                    }
                })
//            }
        }
        return call
    }

    fun 랜덤이미지(isExeInVm: Boolean): Call<JsonArray>? {
        val retrofit = Http.getRetrofitInstance(Http.UNSPLASH_API_URL) //api로 이미지 10개식 받아오기
        val httpHome = retrofit.create(Http.HttpHome::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpHome.랜덤이미지("christian", 10)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
//            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonArray?> {
                    override fun onResponse(call: Call<JsonArray?>, response: Response<JsonArray?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            unsplashRandomL = res.asJsonArray
                            liveUnsplashRandomL.value = unsplashRandomL
                            Log.e(tagName, "랜덤이미지 onResponse: ${gson.toJson(res)}")
//                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonArray?>, t: Throwable) {
                        Log.e(tagName, "랜덤이미지 onFailure: " + t.message)
                    }
                })
//            }
        }
        return call
    }













}