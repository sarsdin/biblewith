package com.example.androidclient.group

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ServiceConnection
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.impl.utils.ContextUtil.getApplicationContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.whenResumed
import androidx.lifecycle.withResumed
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.example.androidclient.MyApp
import com.example.androidclient.MyService
import com.example.androidclient.databinding.GroupChatInnerFmBinding
import com.example.androidclient.home.MainActivity
import com.example.androidclient.util.ImageHelper
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import okhttp3.internal.notify


class GroupChatInnerFm : Fragment() {

    val tagName = "[GroupChatInnerFm]"
    lateinit var groupVm: GroupVm
    lateinit var rva: GroupChatInnerRva
    lateinit var rv: RecyclerView
    var mbinding: GroupChatInnerFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

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
                Log.e(tagName, "service -> handler -> main: 수신 받은 메시지: $jin")

                //현재 참가한 이 채팅방 번호가 받은 메시지에 있는 채팅방 번호와 같으면 현재 보여지는 채팅 리스트에 채팅을 추가함
                if(jin.get("cmd_type").asString == "채팅" && jin.get("cmd").asString == "채팅전달" ){
                    if(jin.get("chat_room_no").asInt == groupVm.chatRoomInfo.get("chat_room_no").asInt){
//                        groupVm.chatL.add(jin)
//                        groupVm.liveChalL.value = groupVm.chatL
                    }

                } else if (jin.get("cmd_type").asString == "채팅" && jin.get("cmd").asString == "채팅갱신" ){
                    if(jin.get("chat_room_no").asInt == groupVm.chatRoomInfo.get("chat_room_no").asInt){
                        groupVm.chatL = jin.get("chatL").asJsonArray
                        groupVm.liveChalL.value = groupVm.chatL
                    }
                }
            }
            return false
        }
    })


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

        MyApp.inChat = true //채팅방 안에 있는지 확인여부 - GroupChatinnerfm 안에 있으면 true 그외는 false 처리해야함
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


        //채팅 갱신
        groupVm.liveChalL.observe(viewLifecycleOwner, Observer {
            rva.notifyDataSetChanged()
            Log.e(tagName, "옵져버에서 수신 받은 메시지 chatL: ${groupVm.gson.toJson(groupVm.chatL)}")
//            groupVm.chatL.find {
//                it.asJsonObject.get("read_date").isJsonNull
//            }
//            rv.scrollToPosition(rv.adapter!!.itemCount - 1) //여기가 아니라 view_holder에서 해야할듯 한데? read_date == null이면
        })


        //스크롤 이벤트 - 채팅 읽음으로 변경하기
//        rv.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
//        rv.addOnScrollListener(object : RecyclerView.OnScrollListener(){
//            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                super.onScrollStateChanged(recyclerView, newState)
//                if(newState == SCROLL_STATE_IDLE){
//                    val lastVisiblePosition = (rv.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
//
//                    if(lastVisiblePosition != -1){
//                        val jo = JsonObject()
//                        jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asInt)
//                        jo.addProperty("chat_room_no", groupVm.chatRoomInfo.get("chat_room_no").asInt)
//                        jo.addProperty("user_no", MyApp.userInfo.user_no)
//                        jo.addProperty("user_nick", MyApp.userInfo.user_nick)
//                        jo.addProperty("user_image", MyApp.userInfo.user_image?:"")
//        //                jo.addProperty("create_date", MyApp.getTime("data", ""))
//                        jo.addProperty("chat_content", "")
//                        jo.addProperty("chat_type", "")
//                        jo.addProperty("cmd_type", "채팅")
//                        jo.addProperty("cmd", "채팅읽음처리")
//                        jo.addProperty("chat_no", groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("chat_no").asString)
//                        jo.addProperty("first_chat_no", groupVm.chatL.get(0).asJsonObject.get("chat_no").asString)
//
//
//                        //내가 현재 스크롤 이벤트의 대상이 된 뷰홀더의 채팅을 읽지 않아서 해당 채팅의 ChatIsRead 테이블에 read_date가 없을것이다.
//                        //그럼 null 이니 읽은 시간을 넣어줘서 갱신해야함 - 지금 스크롤 위치의 채팅 날짜보다 이전(작으면) 모두 read_date를 넣어준다.
//                        if(!groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("my_read_date").isJsonNull &&
//                            groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("my_read_date").asString == ""){
//                            jo.addProperty("my_read_date", groupVm.chatL.get(lastVisiblePosition).asJsonObject.get("my_read_date").asString)
//                            CoroutineScope(Dispatchers.IO).launch {
//                                myService!!.cli.outMsg.println(jo.toString())
//                            }
//                        }
//                    }
//                }
//
//            }
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                super.onScrolled(recyclerView, dx, dy)
//            }
//        })

//            if(scrollY > oldScrollY) { //이전 위치보다 높다는 말은 밑으로 스크롤 한다는 말
//            }
//        }

    } // onViewCreated


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
            jo.addProperty("cmd", "채팅전달")
            CoroutineScope(Dispatchers.IO).launch {
                myService!!.cli.outMsg.println(jo.toString())
            }
        }
        binding.inputEt.setText("")
    }


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
                myService!!.cli.outMsg.println(jo.toString())
//                Thread.sleep(3700)
                delay(500L)
            }
        }

//        myService!!.initMsg(jo) // 서비스 시작시 소켓연결시 초기화용 유저 정보를 보냄
    }

    override fun onResume() {
        super.onResume()
//        myService?.getHandler()
//        handler.obtainMessage(2)
        binding.toolbarTv.text = groupVm.chatRoomInfo.get("chat_room_title").asString

        채팅갱신()


        //상단바 프로필 이미지 클릭시
        binding.toolbarIv.setOnClickListener {
            findNavController().navigate(com.example.androidclient.R.id.action_global_myProfileFm)
        }
        //상단바 프로필 이미지 로딩
        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.toolbarIv)


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
        MyApp.inChat = false //채팅방 안에 있는지 확인여부 - GroupChatinnerfm 안에 있으면 true 그외는 false 처리해야함
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