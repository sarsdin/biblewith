package com.example.androidclient.group

import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.activity.result.registerForActivityResult
import androidx.appcompat.app.AlertDialog
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.example.androidclient.MyApp
import com.example.androidclient.MyService
import com.example.androidclient.databinding.GroupChatInnerFmBinding
import com.example.androidclient.home.MainActivity
import com.example.androidclient.util.FileHelper
import com.example.androidclient.util.Helper.날짜표시기
import com.example.androidclient.util.Http
import com.example.androidclient.util.ImageHelper
import com.google.gson.*
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine


class GroupChatInnerFm : Fragment() {

    val tagName = "[GroupChatInnerFm]"
    lateinit var groupVm: GroupVm
    lateinit var rva: GroupChatInnerRva
    lateinit var rv: RecyclerView
    var mbinding: GroupChatInnerFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    lateinit var imgRv: RecyclerView
    lateinit var imgRva: GroupChatInnerImageRva
    lateinit var joinerRv: RecyclerView
    lateinit var joinerRva: GroupChatInnerJoinerRva
    val gson = Gson()

//    lateinit var cli: ChatClient

    //핸들러 콜백 등록 - onViewCreated 이상의 생명주기에서 붙이면 정상적으로 서비스의 스레드안 수신부 소켓에서 정상적으로 받아서 처리된다.
    //이유는 데이터를 갱신하는 옵저버가 onViewCreated 에 있기 때문이다.
    //정확히는 옵저버 선언 이후에 핸들러를 서비스에 붙이거나 onResume에서 처리해야함
    var handler : Handler? = Handler(Looper.getMainLooper(), object : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            if(msg.what == 1){
                val getJin = msg.data.getString("jin") //서비스에서 들어온 채팅 json 문자열 - 이걸 파싱해야함 여기서
                val jin = JsonParser.parseString(getJin).asJsonObject
                Log.i(tagName, "service -> handler -> main: 수신 받은 메시지: $jin")

                //현재 참가한 이 채팅방 번호가 받은 메시지에 있는 채팅방 번호와 같으면 현재 보여지는 채팅 리스트에 채팅을 추가함
//                if(jin.get("cmd_type").asString == "채팅" && jin.get("cmd").asString == "채팅전달" ){
//                    if(jin.get("chat_room_no").asInt == groupVm.chatRoomInfo.get("chat_room_no").asInt){
////                        groupVm.chatL.add(jin)
////                        groupVm.liveChalL.value = groupVm.chatL
//                    }
//                } else if (jin.get("cmd_type").asString == "채팅" && jin.get("cmd").asString == "채팅갱신" ){
//                    if(jin.get("chat_room_no").asInt == groupVm.chatRoomInfo.get("chat_room_no").asInt){
//                        groupVm.chatL = jin.get("chatL").asJsonArray
//                        groupVm.liveChalL.value = groupVm.chatL
//                    }
//                }

                if(jin.get("cmd").asString == "채팅방접속") {
                    //전달받은 원본채팅(채팅을보낸클라로부터)의 채팅방 번호가 현재 접속한 채팅방과 같은 경우 수행!
                    if(jin.get("rawChat").asJsonObject.get("chat_room_no").asInt == groupVm.chatRoomInfo.get("chat_room_no").asInt){
                        groupVm.chatL = jin.get("chat").asJsonObject.get("chatL").asJsonArray
                        날짜표시기(groupVm.chatL) //받은 채팅리스트의 각 채팅들을 서로 비교해 날짜가 변경되면 날짜표시기를 나타낼 수 있는 속성을 추가함
                        groupVm.liveChalL.value = groupVm.chatL
                        groupVm.chatRoomUserL = jin.get("memberL").asJsonArray //참가원이 변하면 슬라이드뷰도 업데이트해야함
                        groupVm.liveChatRoomUserL.value = groupVm.chatRoomUserL
                        스크롤컨트롤("") //내가 읽지 않은 곳으로 이동함
                    }

                } else if(jin.get("cmd").asString == "방나가기") {
                    if(jin.get("rawChat").asJsonObject.get("chat_room_no").asInt == groupVm.chatRoomInfo.get("chat_room_no").asInt){
                        groupVm.chatL = jin.get("chat").asJsonObject.get("chatL").asJsonArray
                        날짜표시기(groupVm.chatL)
                        groupVm.liveChalL.value = groupVm.chatL

                        //참가원이 변하면 슬라이드뷰의 참가원목록도 업데이트해야함
                        groupVm.chatRoomUserL = jin.get("memberL").asJsonArray
                        groupVm.liveChatRoomUserL.value = groupVm.chatRoomUserL
//                        스크롤컨트롤("")
                    }

                } else if (jin.get("cmd").asString == "채팅통합" || jin.get("cmd").asString == "채팅읽음처리") {
                    if(jin.get("chat").asJsonObject.get("chat_room_no").asInt == groupVm.chatRoomInfo.get("chat_room_no").asInt){
                        groupVm.chatL = jin.get("chat").asJsonObject.get("chatL").asJsonArray
                        날짜표시기(groupVm.chatL)
                        groupVm.liveChalL.value = groupVm.chatL

                        //이미지를 업로드하면 슬라이드뷰의 전체목록도 업데이트해야함
                        if(jin.get("imageL") != null){ //채팅읽음처리로 오는 통신은 서버에서 imageL를 포함해서 데이터를 주지않음. 그래서 예외처리
                            groupVm.chatImageVhL = jin.get("imageL").asJsonArray
                            groupVm.liveChatImageVhL.value = groupVm.chatImageVhL
                        }

//                        if (jin.get("cmd").asString == "채팅읽음처리") {
                            //여기까지읽음 값은 유지해야함..ㅜㅜ 어떻게하지? 일단 읽음처리는 리스트데이터의 개수는 변하진 않잖아?
                            //그럼, 현재 뷰홀더의 포지션 번호도 변하진 않겠네? 그렇다면!? 포지션번호를 저장했다가 다시 바로위의
                            //소켓통신으로 인한 갱신이 이루어지고 포지션번호를 적용하고 rva재갱신하면 ...안되네..ㅜ
//                        }

                        if (jin.get("cmd").asString == "채팅통합") { //스크롤을 이동시켜준다..
                            //현재 리사이클러뷰가 제일 하단(마지막 채팅홀더)에 있으면 스크롤컨트롤("맨아래로")를 이용해 갱신해준다.
                            val lastVisiblePosition = (rv.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                                Log.e(tagName, "왜 안올라오니??: $lastVisiblePosition")
                                Log.e(tagName, "왜 안올라오니??2: ${groupVm.chatL.size()}")
                            if (lastVisiblePosition+2 == groupVm.chatL.size()) {
                                Log.e(tagName, "왜!!??: $lastVisiblePosition")
                                스크롤컨트롤("맨아래로")
                            }
                        }
                    }

                }


            }
            return false
        }
    })


    /**
     * 맨아래로 - 채팅 보낼때 사용.
     * 그외 - 채팅방 접속시 사용
     * */
    fun 스크롤컨트롤(where: String) {
        //채팅을 치거나 클릭할때는 당연히 맨아래로 가야함 - 만약, 맨아래로 갔는데 안읽음수 이벤트가 발동하지 않으면 SmoothScroller 로 해야할듯? 이건 하나하나 짚어가며 스크롤하더라..
        if(where == "맨아래로"){
            Handler(Looper.getMainLooper()).post/*Delayed*/(Runnable {
                rv.scrollToPosition(rv.adapter!!.itemCount - 1)
//                rv.scrollToPosition(groupVm.chatL.size()-1)
            }/*,100*/)

        } else {
            if(groupVm.chatL.size() != 0){
                var count = 0
                kotlin.run bk@ {
                    groupVm.chatL.forEachIndexed { i, jsonElement ->
                        //반복문에서 is_unread 값이 1이면 내가 아직 안읽었다는것을 의미함. 안읽은 첫번째 뷰홀더를 찾기위한 로직임.
                        if (jsonElement.asJsonObject.get("is_unread").asInt == 1) {

                            Handler(Looper.getMainLooper()).post/*Delayed*/(Runnable {
//                                val smoothScroller: RecyclerView.SmoothScroller = object : LinearSmoothScroller(rv.context) {
//                                    override fun getVerticalSnapPreference(): Int {
//                                        return LinearSmoothScroller.SNAP_TO_END //자식뷰의 오른쪽 밑이 부모뷰의 오른쪽 밑에 정렬되게함(스크롤시)
//                                    }
//                                }
//                                smoothScroller.targetPosition = i - 1
//                                rv.layoutManager!!.startSmoothScroll(smoothScroller)
                                //현재 내가 안읽은 위치 그전의 위치까지만 보여주기 위해 -1을 한다.- 그전포지션이 나의 채팅홀더이면 여기까지읽음 뷰가
                                //안보이기때문에(뷰홀더레이아웃자체에 여기까지읽음뷰가 없음..) 그냥 제 포지션으로 가기로 변경 - 그래도 상관없더라~
                                rv.scrollToPosition(i/*-1*/ )

                                //여기까지 읽었다는 것을 표시하기 위해 '채팅방접속' 명령과 함께 방입장시 1로 변경하여 여기에 진입할 수있게
                                //하고, 진입하면 is_unread 의 값이 1이 되는 포지션 뷰홀더 바로 전의 포지션 번호를 넣어서
                                //리사이클러뷰 어댑터에 전달한다. 그리고, 바로 어댑터 갱신돌려서 여기까지 읽음 뷰를 보여주게 한다.
                                //그리고, 포지션번호에 해당하는 뷰홀더에서는 한번만 보여주고 바로 여기까지읽음 = 0 을 넣어줘서
                                //이 조건문이 다시 반응하지 않도록 해준다. 여기까지 읽음뷰는 스크롤이벤트가 발동되면 어뎁터가 재갱신되면서
                                // 다시 사라진다.(여기까지읽음 = 0 이기때문)
                                    Log.e(tagName, "여기까지읽음: $여기까지읽음")
                                if(여기까지읽음 != 0){
                                    여기까지읽음 = i/*-1*/
                                    Log.e(tagName, "여기까지읽음: $여기까지읽음")
                                    rva.notifyDataSetChanged()
                                }

                            }/*, 150*/) //스크롤 컨트롤러의 버그로 인해 지연을 조금 줘야 정상적으로 작동한다.(SmoothScroller 의 경우)

//                            Toast.makeText(requireActivity(),"count in foreach: $count",Toast.LENGTH_SHORT).show()
                            Log.e(tagName, "count in foreach: $count")
                            return@bk //이렇게 빠져나가면 정상적으로 break 대체해서 사용가능하다. 이렇게 안하면 return은 continue처럼 작동한다.
                        }
                        count++
                    }
                }

                //전체 요소를 모두 검사했는데 끝까지 안읽은채팅이 없었다면 맨밑으로 스크롤 이동해준다..
                if (count == groupVm.chatL.size()) {
//                    Toast.makeText(requireActivity(),"count: $count",Toast.LENGTH_SHORT).show()
                    Log.e(tagName, "count: $count")
                    rv.scrollToPosition(rv.adapter!!.itemCount - 1)
                }
           }
        }
    }


//    var myService : MyService? = null
//    var isService = false
//    val serviceConn = object :ServiceConnection { //connection 인터페이스 구현 후 객체로 할당
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            val binder = service as MyService.MyBinder //서비스에서 받아온 MyBinder 객체
//            myService = binder.service              //위의 객체로부터 MyService 객체를 얻어옴
//            isService = true                        //서비스 On 이라고 처리
//            Log.e(tagName, "서비스: MyService에 연결되었습니다.")
//        }
//        override fun onServiceDisconnected(name: ComponentName?) {
//            isService = false   //서비스 Off 이라고 처리. 이 메소드는 비정상 서비스 종료시에만 호출됨. 정상 종료시에는 호출안되니 주의!!
//            Log.e(tagName, "서비스: MyService가 비정상 종료되었습니다.")
//        }
//    }

    //서비스 관련 변수들 및 바인딩 서비스 연결 구현 - MainActivity에서 구현하고 여기서는 밑의 onCreate()때 그냥 가져옴
    var myService : MyService? = null
    var isService = false
    lateinit var serviceConn : ServiceConnection



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        myService = (requireActivity() as MainActivity).myService
        isService = (requireActivity() as MainActivity).isService
        serviceConn = (requireActivity() as MainActivity).serviceConn
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupChatInnerFmBinding.inflate(inflater, container, false)

        rv = binding.chatList
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = GroupChatInnerRva(groupVm, this)
        rva = rv.adapter as GroupChatInnerRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        binding.inputEt.inputType = InputType.TYPE_CLASS_TEXT
//        binding.inputEt.setImeActionLabel("Custom text",  KeyEvent.KEYCODE_ENTER)

        imgRv = binding.includedLayout.imgRv
        imgRv.layoutManager = LinearLayoutManager(context).apply { orientation = LinearLayoutManager.HORIZONTAL}
        imgRv.adapter = GroupChatInnerImageRva(groupVm, this)
        imgRva = imgRv.adapter as GroupChatInnerImageRva

        joinerRv = binding.includedLayout.joinerRv
        joinerRv.layoutManager = LinearLayoutManager(context)
        joinerRv.adapter = GroupChatInnerJoinerRva(groupVm, this)
        joinerRva = joinerRv.adapter as GroupChatInnerJoinerRva


        //채팅방 안에 있는지 확인여부 - GroupChatinnerfm 안에 있으면 true 그외는 false 처리해야함 - 변경: 방번호로 비교해서 그 방외에만 알림
        MyApp.inChatRoom = groupVm.chatRoomInfo.get("chat_room_no").asInt
        채팅방접속() //채팅서버와 연결하여 지금 들어가는 방있는지 확인하고

        //채팅 보내기 버튼
        binding.sendIbt.setOnClickListener {
            채팅보내기이벤트()
        }
        binding.inputEt.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if ( (event!!.action == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    채팅보내기이벤트()
                    return true
                }
                return false
            }
        })

        //드로어 레이아웃 나오게 하기
        binding.toolbarSlide.setOnClickListener {
//            binding.includedLayout.root.
            if(!binding.root.isDrawerOpen(GravityCompat.END)){
                binding.root.openDrawer(GravityCompat.END)
            }
        }


        //채팅 갱신
        groupVm.liveChalL.observe(viewLifecycleOwner, Observer {
            rva.notifyDataSetChanged()
            Log.i(tagName, "옵져버에서 수신 받은 메시지 chatL: ${groupVm.gson.toJson(groupVm.chatL)}")
        })
        //채팅방안 전체 이미지 목록 갱신
        groupVm.liveChatImageVhL.observe(viewLifecycleOwner, Observer {
            imgRva.notifyDataSetChanged()
            Log.i(tagName, "옵져버에서 수신 받은 메시지 chatImageVhL: ${groupVm.gson.toJson(groupVm.chatImageVhL)}")
        })
        //참가원 목록 갱신
        groupVm.liveChatRoomUserL.observe(viewLifecycleOwner, Observer {
            joinerRva.notifyDataSetChanged()
            Log.i(tagName, "옵져버에서 수신 받은 메시지 chatRoomUserL: ${groupVm.gson.toJson(groupVm.chatRoomUserL)}")
        })



       //스크롤 이벤트 - 채팅 읽음으로 변경하기
        rv.addOnScrollListener(object : RecyclerView.OnScrollListener(){
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if(newState == SCROLL_STATE_IDLE){
                    if(여기까지읽음스크롤시초기화여부){
                        여기까지읽음 = 0
                    }
                    val lastVisiblePosition = (rv.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                    //디버깅 용도 로그
//                    val firstVisiblePosition = (rv.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
//                    Toast.makeText(requireActivity(),"lastVisiblePosition:${lastVisiblePosition} \n" +
//                            "firstVisiblePosition:${firstVisiblePosition}",Toast.LENGTH_SHORT).show()
//                    Log.e(tagName, "lastVisiblePosition:${lastVisiblePosition}, firstVisiblePosition:${firstVisiblePosition}")

                    if(lastVisiblePosition != -1){
                        val jo = JsonObject()
                        jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asInt)
                        jo.addProperty("chat_room_no", groupVm.chatRoomInfo.get("chat_room_no").asInt)
                        jo.addProperty("user_no", MyApp.userInfo.user_no)
                        jo.addProperty("user_nick", MyApp.userInfo.user_nick)
                        jo.addProperty("user_image", MyApp.userInfo.user_image?:"")
        //                jo.addProperty("create_date", MyApp.getTime("data", ""))
                        jo.addProperty("chat_content", "")
                        jo.addProperty("chat_type", "")
                        jo.addProperty("cmd_type", "채팅")
                        jo.addProperty("cmd", "채팅읽음처리")
                        jo.addProperty("chat_no", groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("chat_no").asString)
                        jo.addProperty("first_chat_no", groupVm.chatL.get(0).asJsonObject.get("chat_no").asString)


                        //insert를 이용한 로직을 짰을때 쓴것 - 지금은 delete방식으로 짜서 쓰지 않음..
                        //내가 현재 스크롤 이벤트의 대상이 된 뷰홀더의 채팅을 읽지 않아서 해당 채팅의 ChatIsRead 테이블에 read_date가 없을것이다.
                        //그럼 null 이니 읽은 시간을 넣어줘서 갱신해야함 - 지금 스크롤 위치의 채팅 날짜보다 이전(작으면) 모두 read_date를 넣어준다.
//                        if(!groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("my_read_date").isJsonNull &&
//                            groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("my_read_date").asString == ""){
//                            jo.addProperty("my_read_date", groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("my_read_date").asString)
//                        }

                        CoroutineScope(Dispatchers.IO).launch {
                            myService!!.cli.outMsg.println(jo.toString())
                        }

                    //-1일경우는 표시되는 포지션이 더이상 없는 위치를 뜻하는듯? 맨위나 맨아래?
                    } else {

                    }

                }

            }
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }
        })

        //방제목,이미지변경 버튼활성 - 방장일 경우만하기
        if(groupVm.chatRoomInfo.get("owner_no").asInt == MyApp.userInfo.user_no){
            binding.includedLayout.titleModify.setOnClickListener {
                onResume()
            }
            binding.includedLayout.imageModify.setOnClickListener {

            }
        }


        //이미지 업로드 버튼 클릭시
        binding.imageIbt.setOnClickListener {
            getImageContent.launch()
        }

        //방나가기 클릭시
        binding.includedLayout.exitIb.setOnClickListener {
            AlertDialog.Builder(requireActivity())
                .setTitle("방나가기")
                .setMessage("채팅방에서 나가시겠습니까?")
                .setNegativeButton("취소") { dialog, which ->
                    //취소누르면 뒤로가기함
//                findNavController().popBackStack(R.id.challengeDetailListFm, false)
//                    findNavController().navigateUp()
                }
                .setPositiveButton("확인") { dialogInterface, i ->
                    val jo = JsonObject()
                    jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asInt)
                    jo.addProperty("chat_room_no", groupVm.chatRoomInfo.get("chat_room_no").asInt)
                    jo.addProperty("user_no", MyApp.userInfo.user_no)
                    jo.addProperty("user_nick", MyApp.userInfo.user_nick)
                    jo.addProperty("user_image", MyApp.userInfo.user_image?:"")
                    jo.addProperty("chat_content", "")
                    jo.addProperty("create_date", MyApp.getTime("data", ""))
                    jo.addProperty("chat_type", "나가기알림")
                    jo.addProperty("cmd_type", "채팅")
                    jo.addProperty("cmd", "방나가기")

                    CoroutineScope(Dispatchers.IO).launch {
                        myService!!.cli.outMsg.println(jo.toString())
                    }
                    findNavController().navigateUp()
                }

                .create()
                .show()

        }




    } // onViewCreated

//    var groupWriteImageUriL : List<Uri>? = null  //모임 글쓰기 이미지 추가용 리스트
//    lateinit var startForImageResult : ActivityResultLauncher<Intent>
    //이미지+ 버튼 클릭시 - 포토앱에서 선택한 이미지 uri 목록을 받아오는 intent result 의 콜백을 받는 리스너 등록
    val getImageContent = registerForActivityResult(ActivityResultContracts.GetMultipleContents(), "image/*"){
        if(it.size == 0){
            Toast.makeText(requireActivity(), "선택을 취소했습니다.", Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }
        it?.let {
//            Toast.makeText(requireActivity(), "uri get! ", Toast.LENGTH_SHORT).show()
//            Log.e("getImageContent", "$it")
//            groupWriteImageUriL = it
//            groupVm.groupWriteImageUriL = it //이미지 리사이클러뷰 어뎁터에 쓰임
//            binding.groupInWriteImageList.visibility = View.VISIBLE //리사이클러 뷰 보이게 처리
//            binding.groupInWriteToolbarAddImageBt.text = "이미지 ${groupVm.groupWriteImageUriL?.size?:"+" }"
//            binding.groupInWriteToolbarAddImageBt.text = "이미지 ${ if (groupVm.groupWriteImageUriL?.size==0) "+" else groupVm.groupWriteImageUriL?.size }"

            val uploadO = mutableMapOf<String, RequestBody>()
            uploadO.put("user_no", MyApp.userInfo.user_no.toString().toRequestBody("text/plain".toMediaTypeOrNull()))
            uploadO.put("chat_room_no", groupVm.chatRoomInfo.get("chat_room_no").asString.toRequestBody("text/plain".toMediaTypeOrNull()))
            uploadO.put("group_no", groupVm.chatRoomInfo.get("group_no").asString.toRequestBody("text/plain".toMediaTypeOrNull()))
            val fileHelper = FileHelper()
            val uploadImages = fileHelper.getPartBodyFromUriList(requireActivity(), it, "chat_image[]")
            CoroutineScope(Dispatchers.IO).launch {

                val retrofit = Http.getRetrofitInstance(Http.HOST_IP)
                val httpGroup = retrofit.create(Http.HttpGroup::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
                val call = httpGroup.채팅방이미지업로드클릭(uploadO, uploadImages )
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
                                    val chat = res.get("result").asJsonObject

                                    val jo = JsonObject()
                                    jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asInt)
                                    jo.addProperty("chat_room_no", groupVm.chatRoomInfo.get("chat_room_no").asInt)
                                    jo.addProperty("user_no", MyApp.userInfo.user_no)
                                    jo.addProperty("user_nick", MyApp.userInfo.user_nick)
                                    jo.addProperty("user_image", MyApp.userInfo.user_image?:"")
                                    jo.addProperty("create_date", MyApp.getTime("data", ""))
//                                    jo.addProperty("chat_content", sendMsg.toString())
                                    jo.addProperty("chat_type", "이미지")
                                    jo.addProperty("cmd_type", "채팅")
                                    jo.addProperty("cmd", "채팅통합")  //채팅전달에서 변경
                                    jo.addProperty("first_chat_no", groupVm.chatL.get(0).asJsonObject.get("chat_no").asString) //채팅을 쓰면 모두 읽음 처리 해야함. 그때 사용
                                    jo.add("chat_image", chat.get("chat_image").asJsonArray) //web서버로 부터 받은 insert한 chat_image 정보(서버의 uploads경로가 담겨있음)
                                    jo.add("chat", chat) //web서버로 부터 받은 insert한 chat 정보(chat_no, user_no, chat_room_no, chat_type)
                                    jo.addProperty("chat_no", chat.get("chat_no").asInt) //chat_no까지 최상단에 넣어줌. 통일성 위함(혹시모를게 쓸일 잇을듯)

                                    //todo 여기 실행안되는듯하다.. 테스트 더해보자
                                    val handler = Handler(Looper.getMainLooper())
                                    handler.post {
                                        Log.e(tagName, "이미지전송하나??1") //2
                                    }
                                    CoroutineScope(Dispatchers.IO).launch {
                                        handler.post {
                                            Log.e(tagName, "이미지전송하나??2") //3
                                        }
                                        myService!!.cli.outMsg.println(jo.toString())
                                        스크롤컨트롤("맨아래로")
                                    }
                                    handler.post {
                                        Log.e(tagName, "이미지전송했나3") //4
                                    }

                                    cont.resumeWith(Result.success(Unit))
                                }
                            }
                        }
                        override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                            Log.e("[GroupVm]", "채팅방이미지업로드클릭 onFailure: " + t.message)
                        }
                    })
                }
//                rva.notifyDataSetChanged()
                                    Log.e(tagName, "이미지전송했나4") //5
            }
                                    Log.e(tagName, "이미지전송했나5")  //1

        }
    }

    fun 채팅전체읽음처리(){
        val jo = JsonObject()
        jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asInt)
        jo.addProperty("chat_room_no", groupVm.chatRoomInfo.get("chat_room_no").asInt)
        jo.addProperty("user_no", MyApp.userInfo.user_no)
        jo.addProperty("user_nick", MyApp.userInfo.user_nick)
        jo.addProperty("user_image", MyApp.userInfo.user_image?:"")
        //                jo.addProperty("create_date", MyApp.getTime("data", ""))
        jo.addProperty("chat_content", "")
        jo.addProperty("chat_type", "")
        jo.addProperty("cmd_type", "채팅")
        jo.addProperty("cmd", "채팅읽음처리")
        jo.addProperty("chat_no", groupVm.chatL.get(rv.adapter!!.itemCount - 1).asJsonObject.get("chat_no").asString)
        jo.addProperty("first_chat_no", groupVm.chatL.get(0).asJsonObject.get("chat_no").asString)


        //insert를 이용한 로직을 짰을때 쓴것 - 지금은 delete방식으로 짜서 쓰지 않음..
        //내가 현재 스크롤 이벤트의 대상이 된 뷰홀더의 채팅을 읽지 않아서 해당 채팅의 ChatIsRead 테이블에 read_date가 없을것이다.
        //그럼 null 이니 읽은 시간을 넣어줘서 갱신해야함 - 지금 스크롤 위치의 채팅 날짜보다 이전(작으면) 모두 read_date를 넣어준다.
//                        if(!groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("my_read_date").isJsonNull &&
//                            groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("my_read_date").asString == ""){
//                            jo.addProperty("my_read_date", groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("my_read_date").asString)
//                        }

        CoroutineScope(Dispatchers.IO).launch {
            myService!!.cli.outMsg.println(jo.toString())
        }
    }


    fun 채팅보내기이벤트() {
        //입력창의 텍스트를 가져와 클라이언트 쓰레드의 서버로 보내는 메시지아웃풋스트림으로 텍스트 보냄
        if( binding.inputEt.text == null){
            return  //et에 보낼 글이 없으면 안보냄(종료함)
        }
        val sendMsg = binding.inputEt.text
        sendMsg?.let {
            val jo = JsonObject()
            jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asInt)
            jo.addProperty("chat_room_no", groupVm.chatRoomInfo.get("chat_room_no").asInt)
            jo.addProperty("user_no", MyApp.userInfo.user_no)
            jo.addProperty("user_nick", MyApp.userInfo.user_nick)
            jo.addProperty("user_image", MyApp.userInfo.user_image?:"")
            jo.addProperty("create_date", MyApp.getTime("data", ""))
            jo.addProperty("chat_content", sendMsg.toString())
            jo.addProperty("chat_type", "문자열")
            jo.addProperty("cmd_type", "채팅")
            jo.addProperty("cmd", "채팅통합")  //채팅전달에서 변경
            //클라에서 처음 채팅방에 들어가서 채팅 목록이 하나도 없으면 first_chat_no 의 값은 null 이 된다.
            //그러면, 클라에서 먼저 npe가 걸리고 이어 여기서도 npe가 걸릴 것이다.
            // 채팅목록이 없는 경우에 대비해, 없는 경우 오류 방지를 위해 채팅읽음처리를 하지 않는다.
            if(groupVm.chatL.size() > 0){
                jo.addProperty("first_chat_no", groupVm.chatL.get(0).asJsonObject.get("chat_no").asString) //채팅을 쓰면 모두 읽음 처리 해야함. 그때 사용
            }
            CoroutineScope(Dispatchers.IO).launch {
                myService!!.cli.outMsg.println(jo.toString())
                스크롤컨트롤("맨아래로")
            }
        }
        binding.inputEt.setText("")
    }

    var 여기까지읽음 = 0
    var 여기까지읽음스크롤시초기화여부 = false //나의 스크롤이벤트에만 반응하게 하기 위한 장치
    fun 채팅방접속() {
//        cli = ChatClient("10.0.2.2", groupVm) //127.0.0.1 << avd에서 안드로이드os 자신을 가리킴. 내 컴퓨터의 로컬 서버가 아님..!
//        cli.start()
        //서비스 시작 - BIND_AUTO_CREATE : 서비스가 켜저있으면 자동으로 바인딩하고, 없으면 만들어서 바인딩함
//        val intent = Intent(requireContext(), MyService::class.java)
//        requireActivity().bindService(intent, serviceConn, Context.BIND_AUTO_CREATE)
        
        val jo = JsonObject()
        jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asInt)
        jo.addProperty("chat_room_no", groupVm.chatRoomInfo.get("chat_room_no").asInt)
        jo.addProperty("user_no", MyApp.userInfo.user_no)
        jo.addProperty("user_nick", MyApp.userInfo.user_nick)
        jo.addProperty("user_image", MyApp.userInfo.user_image?:"")
        jo.addProperty("create_date", MyApp.getTime("data", ""))
        jo.addProperty("chat_content", "")
        jo.addProperty("chat_type", "접속알림") //방에 첫접속시 접속알림을 할지 검사하는 용도(Rva viewType에서 다룸)
        jo.addProperty("cmd", "채팅방접속")      //채팅서버의 readin() 명령인식커맨드를 지정
        jo.addProperty("cmd_type", "채팅")
//        jo.addProperty("cmd", "초기화")
        CoroutineScope(Dispatchers.IO).launch {
            myService!!.cli.outMsg.println(jo.toString())
            여기까지읽음 = 1
            스크롤컨트롤("")
        }
//        myService!!.initMsg(jo) // 서비스 시작시 소켓연결시 초기화용 유저 정보를 보냄
    }


    fun 서비스바인딩해제(){ //Activity에 바인딩된 서비스를 수동 해제함
        if(isService){ //서비스가 실행중이지 않으면 오류가 나기때문에 이렇게 true일때만(서비스실행중) 해제해야함
            requireActivity().unbindService(serviceConn)
            isService = false
        }
    }

    lateinit var 송신반복작업 : Job
//    lateinit var 송신반복작업 : CoroutineScope
//    lateinit var 송신반복작업 : Thread
    fun 채팅갱신() {
        myService!!.putHandler(handler!!) // 이 fm에 등록된 핸들러를 서비스에 보내줌
        val jo = JsonObject()
        jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asInt)
        jo.addProperty("chat_room_no", groupVm.chatRoomInfo.get("chat_room_no").asInt)
        jo.addProperty("user_no", MyApp.userInfo.user_no)
        jo.addProperty("user_nick", MyApp.userInfo.user_nick)
        jo.addProperty("user_image", MyApp.userInfo.user_image?:"")
        jo.addProperty("create_date", MyApp.getTime("data", ""))
        jo.addProperty("chat_content", "")
        jo.addProperty("chat_type", "") //방에 첫접속시 접속알림을 할지 검사하는 용도(Rva viewType에서 다룸)
        jo.addProperty("cmd", "채팅갱신")
        jo.addProperty("cmd_type", "채팅")
//        jo.addProperty("cmd", "초기화")

//        송신반복작업 = Thread(object : Runnable{
//            override fun run() {
////                송신반복작업 = this
//                while(!송신반복작업.isInterrupted){
//                    try {
//                        Thread.sleep(5700) //main스레드를 포함한 스레드들을 중지할지도 모르니 이 스레드 내부의 sleep을 쓰는게?
//                        myService!!.cli.outMsg.println(jo.toString())
//
//                    }catch(e: Exception){
//                        e.printStackTrace()
//                        송신반복작업.interrupt()
//                        Log.e(tagName, "예외발생! 송신반복작업 쓰레드 종료")
//                    }
//                }
//            }
//        })
//        송신반복작업.start()

//        송신반복작업 = Job()
//        CoroutineScope(Dispatchers.IO + 송신반복작업).launch {
        송신반복작업 = CoroutineScope(Dispatchers.IO).launch {
            while(isActive){
//                myService!!.cli.outMsg.println(jo.toString())
//                Thread.sleep(3700)
                delay(500000L)
//                delay(500L)
            }
        }

//        myService!!.initMsg(jo) // 서비스 시작시 소켓연결시 초기화용 유저 정보를 보냄
    }

    /**
     * 날짜를 표시하는 딜리미터를 어느 인덱스의 요소에 나타낼지를 비교하여 찾아내고 표시하는 메소드
     * 표시될 요소를 찾아내면 속성값으로 is_dayChanged = 1 을 넣어줘서 뷰홀더에서 적용한다
     * 이때 != null && !isJsonNull 둘다비교처리해야함(뷰홀더) */
    fun 날짜표시기(it : JsonArray){
        var 그다음날시작시간 :LocalDateTime? = null
//        var 날짜비교위한임시저장 :LocalDateTime? = null

        if (it.size() == 0) {
            return
        }
        //일단 리스트의 0번 인덱스를 초기 비교기준으로 초기화해준다.
        val 채팅시간 = LocalDateTime.parse(it.get(0).asJsonObject.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
        val st그날시작날짜 = 채팅시간.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        Log.e(tagName, "st그날시작날짜: $st그날시작날짜")
        val 그날시작시간 = LocalDateTime.parse("$st그날시작날짜 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
        Log.e(tagName, "그날시작시간: $그날시작시간")
        그다음날시작시간 = 그날시작시간.plusDays(1L)

        it.forEachIndexed { i, je ->
            val st_chat_date = je.asJsonObject.get("create_date").asString
            val chatDate = LocalDateTime.parse(st_chat_date, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
//                날짜비교위한임시저장 = chatDate

//            Log.e(tagName, "chatDate: $chatDate")

//            '그다음날인덱스의 시작시간'보다 현재 저장된 '그다음날시작시간'이 이전(적을경우)일경우 '그담날인덱스의 시작시간'으로 '그다음날시작시간'을 대체
            //반복문 마지막 인덱스시에에는 indexOutOfBound 가 뜨기때문에 조건을 리스트크기까지만 비교하는 걸로 맞춘다.
            val plus1indexOfChatDate = if (i+1 != it.size()) {
                 LocalDateTime.parse(it.get(i+1).asJsonObject.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
            } else { //마지막 인덱스일경우 현재 chatDate와 같은 데이터를 넣어서 다음 if(plus1indexOfChatDate.isAfter(그다음날시작시간))에서 반응안하게 하면됨(false되게).
                 LocalDateTime.parse(it.get(i).asJsonObject.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
            }
            if (plus1indexOfChatDate.isAfter(그다음날시작시간)) {
                val tmp_st = plus1indexOfChatDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                그다음날시작시간 = LocalDateTime.parse("$tmp_st 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
//                그다음날시작시간 = plus1indexOfChatDate.truncatedTo(ChronoUnit.HOURS) //시간단위 이하는 0으로 날려버리고 반환됨- 근데 버그 있음.. 1시간 보다 작은 숫자가 반복적으로 나오면 시간에 2:00으로 표시됨.. 치명적!
//                Log.e(tagName, "그다음날시작시간 = 그담인덱스시간의 그날시작시간으로 건너띈 시작시간: $그다음날시작시간")
            }

//            Log.e(tagName, "chatDate: $chatDate, 그다음날시작시간: $그다음날시작시간 res: ${chatDate.isAfter(그다음날시작시간)}")
            if(chatDate.equals(그다음날시작시간) || chatDate.isAfter(그다음날시작시간)){
                //visible처리해줘야지
                val st그날시작날짜 = chatDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val 그날시작시간 = LocalDateTime.parse("$st그날시작날짜 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
                je.asJsonObject.addProperty("is_dayChanged",1) //!= null && !isJsonNull 둘다비교처리해야함(뷰홀더)
                //그리고, 현재 인덱스의 날짜의 시작시간+1day를 다음 dayChanged 레이아웃을 보여주는 비교기준으로 삼기위해 넣어줌
                //1 day를 기준으로 삼는 이유는 당연히 날짜 딜리미터(뷰)를 하루단위로 끊어서 보여줘야하기 때문에 1day 마다 비교를 해야함!
                그다음날시작시간 = 그날시작시간.plusDays(1L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
//        myService?.getHandler()
//        handler.obtainMessage(2)
        binding.toolbarTv.text = groupVm.chatRoomInfo.get("chat_room_title").asString

        채팅갱신() //여기서 핸들러 등록하는 중임
//        스크롤컨트롤()

        //상단바 프로필 이미지 클릭시
        binding.toolbarIv.setOnClickListener {
            findNavController().navigate(com.example.androidclient.R.id.action_global_myProfileFm)
        }
        //상단바 프로필 이미지 로딩
        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.toolbarIv)


        //채팅방 정보들
        binding.includedLayout.title.text = "${groupVm.chatRoomInfo.get("chat_room_title").asString}"
        binding.includedLayout.joinCount.text = "${groupVm.chatRoomUserL.size()}명 참여중"
        binding.includedLayout.createDate.text = "개설일 ${MyApp.getTime(".ui", groupVm.chatRoomInfo.get("create_date").asString)}"
        binding.includedLayout.chatRoomDesc.text = "${groupVm.chatRoomInfo.get("chat_room_desc").asString}"
        binding.includedLayout.roomOwner.text = "방장:${groupVm.chatRoomInfo.get("user_nick").asString}"





    }

    override fun onStop() {
        super.onStop()
//        handler = null
        송신반복작업.cancel()   //채팅방 나갈때 반복적으로 송신하는 코루틴 제거함
//        송신반복작업.interrupt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rv.clearOnScrollListeners() // onViewCreated 에서 생성한 리사이클러뷰 스크롤리스너를 제거함
        MyApp.inChatRoom = 0 //채팅방 안에 있는지 확인여부 - GroupChatinnerfm 안에 있으면 true 그외는 false 처리해야함

        mbinding = null
//        cli.status = false // 접속한 채팅서버에 연결된 쓰레드의 while문 리스너를 끊어줌(접속종료와 동시에 쓰레드 종료)
//        cli.stopClient() //소켓까지 확실하게 닫음 -- 닫았었는데 닫을 필요 없음. 아니 닫으면 안됨 - 백그라운드 서비스에서 소켓 유지시켜야 알림받기 가능함
        //그리고, 앱이 꺼져있거나 폰이 재시작되도 백그라운드 서비스는 실행되어야함 그리고, 채팅서버랑 지속적으로 연결을 유지해야함. 실시간 알림을 위해..
    }
}








//    채팅룸내 자료구조
//
//{
// "result": {
//    "chat_room_info": {

//    "chat_room_no": "10",
//    "owner_no": "0",
//    "chat_room_title": "테스트1",
//    "create_date": "2022-08-15 11:46:49",
//    "chat_room_image": "20220813/1660381480_648f75e05c1650e23744.jpeg",
//    "chat_room_desc": "소개",
//    "group_no": "2",
//    "user_no": "0",
//    "user_chat_join_date": "2022-08-15 11:46:49",
//    "chat_room_pk": "10",
//    "user_email": "sjeys14@gmail.com",
//    "user_pwd": "tjfwjdahr1!",
//    "user_nick": "정목d3",
//    "user_create_date": "2022-06-21 12:26:20",
//    "user_name": "설정목",
//    "user_image": "20220813/1660381480_648f75e05c1650e23744.jpeg"
//  },
//    "chat_room_userL": [
//    {
//        "chat_room_no": "10",
//        "owner_no": "0",
//        "chat_room_title": "테스트1",
//        "create_date": "2022-08-15 11:46:49",
//        "chat_room_image": "20220813/1660381480_648f75e05c1650e23744.jpeg",
//        "chat_room_desc": "소개",
//        "group_no": "2",
//        "user_no": "0",
//        "user_chat_join_date": "2022-08-15 11:46:49",
//        "chat_room_pk": "10",
//        "user_email": "sjeys14@gmail.com",
//        "user_pwd": "tjfwjdahr1!",
//        "user_nick": "정목d3",
//        "user_create_date": "2022-06-21 12:26:20",
//        "user_name": "설정목",
//        "user_image": "20220813/1660381480_648f75e05c1650e23744.jpeg"
//    },
//    {
//        "chat_room_no": "10",
//        "owner_no": "0",
//        "chat_room_title": "테스트1",
//        "create_date": "2022-08-15 11:46:49",
//        "chat_room_image": "20220813/1660381480_648f75e05c1650e23744.jpeg",
//        "chat_room_desc": "소개",
//        "group_no": "2",
//        "user_no": "4",
//        "user_chat_join_date": "2022-08-15 21:36:11",
//        "chat_room_pk": "11",
//        "user_email": "sarsdin@gmail.com",
//        "user_pwd": "tjfwjdahr1!",
//        "user_nick": "알파\t",
//        "user_create_date": "2022-08-09 23:45:40",
//        "user_name": "설정목2",
//        "user_image": "20220815/1660566737_9eee04d47cefeebb748c.jpg"
//    }
//    ],
//    "chat_list": []
//},
//    "msg": "ok"
//}