package jm.preversion.biblewith.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.util.Http
import com.google.gson.Gson
import jm.preversion.biblewith.bible.dto.HomeVerseDto
import jm.preversion.biblewith.bible.dto.RandomImageDto
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

    var todayVerse: HomeVerseDto? = null   //홈 페이지 성경 일독 : 가져올때마다 랜덤으로 구절이 변함
    val liveTodayVerse = MutableLiveData<HomeVerseDto>()

    var unsplashRandomL = mutableListOf<RandomImageDto>()   //홈 페이지 이미지 : api에서 10개식 랜덤으로 가져오기
    val liveUnsplashRandomL = MutableLiveData<List<RandomImageDto>>()

//    val text: LiveData<String>
//        get() = mText

    init {
//        mText = new MutableLiveData<>();
//        mText.value = "home"
    }





    fun 성경일독(isExeInVm: Boolean): Call<HomeVerseDto>? {
        val retrofit = Http.getRetrofitInstance(Http.HOST_IP)
        val httpHome = retrofit.create(Http.HttpHome::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpHome.성경일독()
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
//            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<HomeVerseDto?> {
                    override fun onResponse(call: Call<HomeVerseDto?>, response: Response<HomeVerseDto?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            todayVerse = res
                            liveTodayVerse.value = todayVerse
                            Log.e(tagName, "성경일독 onResponse: ${gson.toJson(res)}")
//                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<HomeVerseDto?>, t: Throwable) {
                        Log.e(tagName, "성경일독 onFailure: " + t.message)
                    }
                })
//            }
        }
        return call
    }

    fun 랜덤이미지(isExeInVm: Boolean): Call<List<RandomImageDto>>? {
        val retrofit = Http.getRetrofitInstance(Http.UNSPLASH_API_URL) //api로 이미지 10개식 받아오기
        val httpHome = retrofit.create(Http.HttpHome::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpHome.랜덤이미지("christian", 10)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
//            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<List<RandomImageDto>?> {
                    override fun onResponse(call: Call<List<RandomImageDto>?>, response: Response<List<RandomImageDto>?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            unsplashRandomL = res.toMutableList()
                            liveUnsplashRandomL.value = unsplashRandomL
                            Log.e(tagName, "랜덤이미지 onResponse: ${gson.toJson(res)}")
//                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<List<RandomImageDto>?>, t: Throwable) {
                        Log.e(tagName, "랜덤이미지 onFailure: " + t.message)
                    }
                })
//            }
        }
        return call
    }













}