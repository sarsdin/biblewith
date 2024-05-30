package jm.preversion.biblewith.group

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide.init
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.util.Helper.날짜표시기
import jm.preversion.biblewith.util.Http
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.Part
import java.time.LocalDateTime
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class GroupVm : ViewModel() {

    val host = Http.HOST_IP
    val gson = GsonBuilder().setPrettyPrinting().create()

    var groupL = JsonArray() //모임 목록 - 모임목록가져오기()
    var liveGroupL = MutableLiveData<JsonArray>()
    var gboardL = JsonArray() //모임 상세페이지 게시물 목록 - 모임상세불러오기()
    var liveGboardL = MutableLiveData<JsonArray>()
    var memberL = JsonArray() //모임 상세페이지 멤버 목록 - 모임상세불러오기(), 모임멤버목록로드(), 모임멤버추방,탈퇴,검색()
    var liveMemberL = MutableLiveData<JsonArray>()
    var groupInfo = JsonObject() //모임 상세페이지 모임요약 정보 - 모임상세불러오기()에서 불러옴
    var liveGroupInfo = MutableLiveData<JsonObject>()

    var gboardInfo = JsonObject() //모임 게시물 디테일 정보 - 모임글상세가져오기() - 내용(글,좋아요수,히트수,) + 이미지목록 + 댓글 목록
    var liveGboardInfo = MutableLiveData<JsonObject>()
    var gboardReplyL = JsonArray() //모임 게시물 디테일 댓글 정보 - 모임글상세가져오기()
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



    var chalLInfo = JsonObject() //챌린지 목록에서 클릭하면 들어오는 해당 챌린지 정보
    var chalL = JsonArray() //챌린지 목록 - 챌린지정보 + selected_bibleL
    var liveChalL = MutableLiveData<JsonArray>()
    var chalDetailInfo = JsonObject() //챌린지 상세목록 클릭시 페이지 해당 정보 - 영상인증 페이지 및 전송에서 활용
    var liveChalDetailInfo = MutableLiveData<JsonObject>()
    var chalDetailL = JsonArray() //챌린지 상세 목록
    var liveChalDetailL = MutableLiveData<JsonArray>()
    var chalDetailVerseL = JsonArray() //챌린지 상세목록 클릭시 영상인증 페이지의 절 목록
    var liveChalDetailVerseL = MutableLiveData<JsonArray>()
    var chalDetailVideoInfo = JsonObject() //챌린지 상세목록 클릭시 영상인증 페이지의 영상 정보


    //챌린지 만들기 페이지에서 쓰이는 변수들
    var createList = JsonArray() // 챌린지 만들기 - 고정초기 성경책 목록 - ChallengeCreateFm
    var selectedCreateList = JsonArray() // 챌린지 만들기 - 선택된 성경책 목록 - 변화함
    var liveSelectedCreateList = MutableLiveData<JsonArray>()
    var createJo = JsonObject() //챌린지 만들기 서버전송용 임시객체
    var progressCountVerse = 20  //진행할 구절수 - 챌린지 만들기2 seekBar 수치
    var progressCountDay = 3  //진행할 일수 - 챌린지 만들기2 seekBar 수치
    var totalVerseCount = 0     //선택한 책의 총 구절수 - 챌린지 만들기2
    var whatIsSelected = "day"     //선택한 계산방식 - 챌린지 만들기2
    var computedDay = 0               //계산된 완료예정일수 - 서버전송용


    //채팅 관련 변수들
    var chatRoomInfo = JsonObject() //채팅방 정보
    var liveChatRoomInfo =  MutableLiveData<JsonObject>()
    var chatRoomInfoL = JsonArray() //채팅방 정보+참가자목록 - GroupInChatRva(모임 채팅페이지 채팅목록) 에서 쓰임
    var liveChatRoomInfoL =  MutableLiveData<JsonArray>()
    var chatRoomUserL = JsonArray() //채팅방 참가자목록 - GroupChatInnerRva(채팅방안) 에서 쓰임
    var liveChatRoomUserL =  MutableLiveData<JsonArray>()
    var chatL = JsonArray() //채팅+쓴사람 목록 정보
    var liveChatL =  MutableLiveData<JsonArray>()
    //채팅방안 슬라이드쪽 변수들
    var chatImageVhL = JsonArray() //채팅방 전체 이미지 목록 정보
    var liveChatImageVhL =  MutableLiveData<JsonArray>()
    var chatJoinerVhL = JsonArray() //채팅 참가자 목록 정보
    var liveChatJoinerVhL =  MutableLiveData<JsonArray>()

    //GroupInChatCreateDialogFm onDismiss()에서 dismiss하고 true로바꾸면 방생성이 취소됐다는 것을 vm을 통해 옵저버에 알리고 groupInChatFm 채팅방목록갱신을 다시 시작함
    var liveChatCreateDialogFmIsDismiss = MutableLiveData<Boolean>()
    var noticount = 0

    var dayChangeVerify : LocalDateTime? = null //날짜 지났는지 확인용. 이날짜와 chatDate의 값을 비교해보고 일자부분이 변했으면 +1 해주고 dateLayout 을 visible처리해줌!
    var daySaved : LocalDateTime? = null //날짜 지났는지 확인용. 이날짜와 chatDate의 값을 비교해보고 일자부분이 변했으면 +1 해주고 dateLayout 을 visible처리해줌!
    var isDaySaved = false


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

    // 모임상세 목록을 가져옴 - 게시물리스트, 멤버리스트, 모임정보 ...
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
    suspend fun 모임상세불러오기2(isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.getGroupIn(currentGroupIn, sortStateGroupIn, MyApp.userInfo.user_no)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
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
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "모임상세불러오기 onFailure: " + t.message)
                    }
                })
            }
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




////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   챌린지 http

    suspend fun 챌린지목록가져오기( isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.챌린지목록가져오기(MyApp.userInfo.user_no, groupInfo.get("group_no").asInt)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            chalL = res.get("result").asJsonArray
                            liveChalL.value = chalL
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "챌린지목록가져오기 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }

    suspend fun 챌린지상세목록가져오기(chal_no:Int, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.챌린지상세목록가져오기(chal_no, MyApp.userInfo.user_no, groupInfo.get("group_no").asInt)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            chalDetailL = res.get("result").asJsonArray
                            liveChalDetailL.value = chalDetailL
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "챌린지상세가져오기 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }

    suspend fun 챌린지만들기총분량수계산(params: JsonArray, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.챌린지만들기총분량수계산(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            totalVerseCount = res.get("result").asInt
                            Log.e("[GroupVm]", "챌린지만들기총분량수계산 onResponse: $res")
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "챌린지만들기총분량수계산 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }


    suspend fun 챌린지만들기완료하기(params: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.챌린지만들기완료하기(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
//                            totalVerseCount = res.get("result").asInt
                            Log.e("[GroupVm]", "챌린지만들기완료하기 onResponse: $res")
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "챌린지만들기완료하기 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }

    suspend fun 챌린지인증진행하기(params: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.챌린지인증진행하기(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            //받은 리스트의 요소가 없다면 npe 가 뜨기에 사이즈가 0일경우의 처리를 해준다.
                            chalDetailVideoInfo = res.get("result").asJsonObject.get("chalDetailVideoInfo").asJsonArray.run {
                                if(size() != 0){
                                    get(0).asJsonObject
                                } else{
                                    JsonObject()
                                }
                            }
                            chalDetailVerseL = res.get("result").asJsonObject.get("chalDetailVerseL").asJsonArray
                            liveChalDetailVerseL.value = chalDetailVerseL
                            //좋아요 관련 데이터
//                            val likeInfo = res.get("result").asJsonObject.get("likeInfo").asJsonArray
                            val likeInfo = if(res.get("result").asJsonObject.get("likeInfo").asJsonArray.size() > 0){
                                res.get("result").asJsonObject.get("likeInfo").asJsonArray
                            } else {
                                null
                            }
                            val likeMyInfo = if(res.get("result").asJsonObject.get("likeMyInfo").asJsonArray.size() > 0){
                                res.get("result").asJsonObject.get("likeMyInfo").asJsonArray.get(0).asJsonObject
                            } else {
                                null//JsonObject() //after page 가 아닌 detail page 라면 좋아요가 표시되지 않아서 빈 객체를 임시로 넣어줌.
                            }
                            chalDetailInfo.add("likeInfo", likeInfo)
                            chalDetailInfo.add("likeMyInfo", likeMyInfo)
                            liveChalDetailInfo.value = chalDetailInfo
//                            Log.e("[GroupVm]", "챌린지인증진행하기 onResponse: $res")
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "챌린지인증진행하기 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }

    //영상 녹화시 코루틴 해제 할지 말지 결정해보자
    suspend fun 챌린지인증체크업데이트(params: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.챌린지인증체크업데이트(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
//                            chalDetailVerseL = res.get("result").asJsonObject.get("verseL").asJsonArray
                            chalDetailVerseL = res.get("result").asJsonArray
                            liveChalDetailVerseL.value = chalDetailVerseL
//                            Log.e("[GroupVm]", "챌린지인증체크업데이트 onResponse: $res")
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "챌린지인증체크업데이트 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }


    //영상 녹화 업로드 진행  - chal_detail_no, user_no                chal_video
    suspend fun 챌린지인증영상업로드(params: Map<String, RequestBody>, video:MultipartBody.Part, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.챌린지인증영상업로드(params, video)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
//                            Log.e("[GroupVm]", "챌린지인증영상업로드 onResponse: $res")
//                            Toast.makeText(MyApp.getApplication(),"업로드완료 콜백옴",Toast.LENGTH_SHORT).show()
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "챌린지인증체크업데이트 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }


    suspend fun 챌린지인증영상업로드사전작업(chal_detail_no: Int, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.챌린지인증영상업로드사전작업(chal_detail_no)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
//                            Log.e("[GroupVm]", "챌린지인증영상업로드 onResponse: $res")
//                            Toast.makeText(MyApp.getApplication(),"업로드완료 콜백옴",Toast.LENGTH_SHORT).show()
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "챌린지인증영상업로드사전작업 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }


    suspend fun 챌린지상세좋아요클릭(params: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.챌린지상세좋아요클릭(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            val likeInfo = res.get("result").asJsonObject.get("likeInfo").asJsonArray
//                            val likeMyInfo = res.get("result").asJsonObject.get("likeMyInfo").asJsonArray
                            val likeMyInfo = if(res.get("result").asJsonObject.get("likeMyInfo").asJsonArray.size() > 0){
                                res.get("result").asJsonObject.get("likeMyInfo").asJsonArray.get(0).asJsonObject
                            } else {
                                null//JsonObject() //after page 가 아닌 detail page 라면 좋아요가 표시되지 않아서 빈 객체를 임시로 넣어줌.
                            }
                            chalDetailInfo.add("likeInfo", likeInfo)
                            chalDetailInfo.add("likeMyInfo", likeMyInfo)
                            liveChalDetailInfo.value = chalDetailInfo
//                            Log.e("[GroupVm]", "챌린지인증영상업로드 onResponse: $res")
//                            Toast.makeText(MyApp.getApplication(),"업로드완료 콜백옴",Toast.LENGTH_SHORT).show()
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "챌린지인증영상업로드사전작업 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }


    suspend fun 모임멤버목록로드(params: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.모임멤버목록로드(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            memberL = res.get("result").asJsonArray
                            liveMemberL.value = memberL
//                            Log.e("[GroupVm]", "모임멤버목록로드 onResponse: $res")
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "모임멤버목록로드 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }


    suspend fun 모임멤버탈퇴(params: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.모임멤버탈퇴(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            memberL = res.get("result").asJsonArray
                            liveMemberL.value = memberL
//                            Log.e("[GroupVm]", "모임멤버탈퇴 onResponse: $res")
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "모임멤버탈퇴 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }

    suspend fun 모임멤버추방(params: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.모임멤버추방(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            memberL = res.get("result").asJsonArray
                            liveMemberL.value = memberL
//                            Log.e("[GroupVm]", "모임멤버추방 onResponse: $res")
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "모임멤버추방 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }

    suspend fun 모임멤버검색(params: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.모임멤버검색(params)
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            memberL = res.get("result").asJsonArray
                            liveMemberL.value = memberL
//                            Log.e("[GroupVm]", "모임멤버검색 onResponse: $res")
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "모임멤버검색 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }


//    suspend fun 채팅방만들기(roomInfo: Map<String, RequestBody>, chatRoomImage: MultipartBody.Part, isExeInVm: Boolean): Call<JsonObject>? {
//        val retrofit = Http.getRetrofitInstance(host)
//        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
//        val call = httpGroup.채팅방만들기(roomInfo, chatRoomImage)
//        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
//            val resp = suspendCoroutine { cont: Continuation<Unit> ->
//                call.enqueue(object : Callback<JsonObject?> {
//                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
//                        if (response.isSuccessful) {
//                            val res = response.body()!!
//                            //채팅방정보 - 방제, 방장정보, 생성일...
//                            chatRoomInfo = res.get("result").asJsonObject.get("chat_room_info").asJsonObject
//                            liveChatRoomInfo.value = chatRoomInfo
//                            //참가유저목록
//                            chatRoomInfoL = res.get("result").asJsonObject.get("chat_room_userL").asJsonArray
//                            liveChatRoomInfoL.value = chatRoomInfoL
//                            //채팅리스트
////                            chatL = res.get("result").asJsonObject.get("chat_list").asJsonArray
////                            liveChatL.value = chatL
////                            Log.e("[GroupVm]", "채팅방만들기 onResponse: $res")
//                            cont.resumeWith(Result.success(Unit))
//                        }
//                    }
//                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
//                        Log.e("[GroupVm]", "채팅방만들기 onFailure: " + t.message)
//                    }
//                })
//            }
//        }
//        return call
//    }


    suspend fun 채팅방목록(jo: JsonObject, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.채팅방목록(jo )
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            if (res.get("result") != null) {
                                chatRoomInfoL = res.get("result").asJsonArray
                                liveChatRoomInfoL.value = chatRoomInfoL
    //                            Log.e("[GroupVm]", "채팅방목록 onResponse: $res")
                            }
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "채팅방목록 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }



    suspend fun 채팅방참가클릭(chat_room_no: Int, user_no: Int, group_no: Int, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.채팅방참가클릭(chat_room_no, user_no, group_no )
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            if(!res.get("result").isJsonNull){
                                //채팅방정보 - 방제, 방장정보, 생성일...
                                chatRoomInfo = res.get("result").asJsonObject.get("chat_room_info").asJsonObject
                                liveChatRoomInfo.value = chatRoomInfo
                                //참가유저목록
                                chatRoomUserL = res.get("result").asJsonObject.get("chat_room_userL").asJsonArray
                                liveChatRoomUserL.value = chatRoomUserL
                                //채팅리스트
                                chatL = res.get("result").asJsonObject.get("chat_list").asJsonArray
                                날짜표시기(chatL)
                                liveChatL.value = chatL
                                //채팅방 전체 이미지리스트
                                chatImageVhL = res.get("result").asJsonObject.get("chat_room_image_list").asJsonArray
                                liveChatImageVhL.value = chatImageVhL
    //                            Log.e("[GroupVm]", "채팅방참가클릭 onResponse: $res")
                            }
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "채팅방참가클릭 onFailure: " + t.message)
                    }
                })
            }
        }
        return call
    }


   suspend fun 채팅방이미지업로드클릭(chatInfo :Map<String, RequestBody>, uploadImages :List<MultipartBody.Part>, isExeInVm: Boolean): Call<JsonObject>? {
        val retrofit = Http.getRetrofitInstance(host)
        val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
        val call = httpGroup.채팅방이미지업로드클릭(chatInfo, uploadImages )
        if (isExeInVm) { //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
            val resp = suspendCoroutine { cont: Continuation<Unit> ->
                call.enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                        if (response.isSuccessful) {
                            val res = response.body()!!
                            if(!res.get("result").isJsonNull){
                                //채팅방 전체 이미지리스트
//                                chatImageVhL = res.get("result").asJsonObject
//                                liveChatImageVhL.value = chatImageVhL
    //                            Log.e("[GroupVm]", "채팅방이미지업로드클릭 onResponse: $res")
                            }
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        Log.e("[GroupVm]", "채팅방이미지업로드클릭 onFailure: " + t.message)
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
//    "0": { // 모임 정보 -- groupInfo
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