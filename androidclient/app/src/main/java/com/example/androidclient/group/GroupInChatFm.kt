package com.example.androidclient.group
import android.widget.Toast

import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import com.example.androidclient.MyApp
import com.example.androidclient.MyService
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupInChatFmBinding
import com.example.androidclient.home.MainActivity
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.*

class GroupInChatFm : Fragment() {

    val tagName = "[GroupInChatFm]"
    lateinit var groupVm: GroupVm
    lateinit var rva: GroupInChatRva
    lateinit var rv: RecyclerView
    var mbinding: GroupInChatFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    //서비스 관련 변수들 및 바인딩 서비스 연결 구현 - MainActivity에서 구현하고 여기서는 밑의 onCreate()때 그냥 가져옴
    var myService : MyService? = null
    var isService = false
    lateinit var serviceConn : ServiceConnection

    //핸들러 콜백 등록 - onViewCreated 이상의 생명주기에서 붙이면 정상적으로 서비스의 스레드안 수신부 소켓에서 정상적으로 받아서 처리된다.
    //이유는 데이터를 갱신하는 옵저버가 onViewCreated 에 있기 때문이다.
    //정확히는 옵저버 선언 이후에 핸들러를 서비스에 붙이거나 onResume에서 처리해야함
    var handler : Handler = Handler(Looper.getMainLooper(), object : Handler.Callback {
        override fun handleMessage(msg: Message): Boolean {
            if(msg.what == 1){
                val getJin = msg.data.getString("jin") //서비스에서 들어온 채팅 json 문자열 - 이걸 파싱해야함 여기서
                val jin = JsonParser.parseString(getJin).asJsonObject
                Log.e(tagName, "service -> handler -> main: 수신 받은 메시지: ${groupVm.gson.toJson(jin)}")

                //현재 참가한 이 모임 번호가 받은 메시지에 있는 모임 번호와 같으면 현재 보여지는 모임 리스트에 모임내용을 갱신함
//                if(jin.get("cmd_type").asString == "채팅" && jin.get("cmd").asString == "채팅방목록"){
//                    if(jin.get("group_no").asInt == groupVm.groupInfo.get("group_no").asInt){
//                        groupVm.chatRoomInfoL = jin.get("chatRoomInfoL").asJsonArray
//                        groupVm.liveChatRoomInfoL.value = groupVm.chatRoomInfoL
//                    }
//                }
                if(jin.get("cmd").asString == "채팅방목록") {
                    if (jin.get("group_no").asInt == groupVm.groupInfo.get("group_no").asInt) {
                        groupVm.chatRoomInfoL = jin.get("chatRoomInfoL").asJsonArray
                        groupVm.liveChatRoomInfoL.value = groupVm.chatRoomInfoL
                    }

                //다른 클라에서 소켓을 통하고 서버를 통해 이클라의 핸들러로 전달되어져 온 채팅 데이터들에 반응하기 위한 명령으로
                //핸들러에서 "채팅통합" 이라는 명령으로 채팅방과 채팅목록에 둘다 보내게된다. 굳이 채팅통합이라고 명령을 서버에서 합치는 이유는
                //여기 클라의 채팅방과 채팅목록에서 채팅알림을 받기 위함이다!
                } else if (jin.get("cmd").asString == "채팅통합") {
                    if (jin.get("chatRoom").asJsonObject.get("group_no").asInt == groupVm.groupInfo.get("group_no").asInt) {
                        groupVm.chatRoomInfoL = jin.get("chatRoom").asJsonObject.get("chatRoomInfoL").asJsonArray
                        groupVm.liveChatRoomInfoL.value = groupVm.chatRoomInfoL
                    }
                }
            }
            return false
        }
    })
    // 채팅방목록
    // 채팅전달의 msg를 json객체로 변환해서 채팅방목록에서 받은 json 객체랑 통합하여 cliL 에 뿌린다.
    // 그럼 이곳 핸들러에서 뿌려진 jin에서 채팅방목록(chatRoomInfoL)만 받아서 그 객체안에 있는 group_no랑 비교하여 목록을 업데이트하면된다.

    // 채팅은 반대로 chatInner 안 chatL 에서 받아오면 된다.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        myService = (requireActivity() as MainActivity).myService
        isService = (requireActivity() as MainActivity).isService
        serviceConn = (requireActivity() as MainActivity).serviceConn
        myService!!.putHandler(handler)

        채팅방목록초기화()
    }

    fun 채팅방목록초기화(){
        val jo = JsonObject()
        jo.addProperty("user_no", MyApp.userInfo.user_no)
        jo.addProperty("user_nick", MyApp.userInfo.user_nick)
        jo.addProperty("user_image", MyApp.userInfo.user_image?:"")
        jo.addProperty("cmd_type", "채팅")
        jo.addProperty("cmd", "채팅방목록")
        jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asString)

        CoroutineScope(Dispatchers.IO).launch {
//            groupVm.채팅방목록(jo,true)
            myService!!.cli.outMsg.println(jo.toString())
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mbinding = GroupInChatFmBinding.inflate(inflater, container, false)

        rv = binding.chatList
        rv.layoutManager = LinearLayoutManager(context)
        rv.adapter = GroupInChatRva(groupVm, this)
        rva = rv.adapter as GroupInChatRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //onViewCreated() 생명주기 이상에 붙여주는것 매우 중요!!!!!
        myService!!.putHandler(handler) // 이 fm에 등록된 핸들러를 서비스에 보내줌

        //바텀네비 리스너 설정
        binding.bottomNavi.setOnItemSelectedListener {
//            onNavDestinationSelected(it, navController)  << navigate()와 충돌함.
            if(it.itemId == R.id.groupInFm){
                Navigation.findNavController(view).navigate(R.id.action_global_groupInFm)
            } else if(it.itemId == R.id.groupInMemberFm){
                Navigation.findNavController(view).navigate(R.id.action_global_groupInMemberFm)
            } else if(it.itemId == R.id.group_in_challenge_fm){
                Navigation.findNavController(view).navigate(R.id.action_global_group_in_challenge_fm)
            }
            return@setOnItemSelectedListener false
        }

        //채팅방 만들기 버튼 클릭시
        binding.toolbarAddBt.setOnClickListener {
            송신반복작업.cancel()
            findNavController().navigate(R.id.groupInChatCreateDialogFm)
        }

        //GroupInChatCreateDialogFm onDismiss()에서 dismiss하고 true로바꾸면 방생성이 취소됐다는 것을 vm을 통해 옵저버에 알리고 groupInChatFm 채팅방목록갱신을 다시 시작함
        groupVm.liveChatCreateDialogFmIsDismiss.observe(viewLifecycleOwner, Observer {
            if(it){
                채팅방목록갱신()
                groupVm.liveChatCreateDialogFmIsDismiss.value = false //갱신후에는 다시 써먹게 초기화시켜줌
            }
        })

        //데이터 갱신에 따른 UI 업데이트
        groupVm.liveChatRoomInfoL.observe(viewLifecycleOwner, Observer {
            rva.notifyDataSetChanged()
            Log.e(tagName, "채팅방 목록 데이터 갱신됨")
        })

    }

    lateinit var 송신반복작업 : Job
    fun 채팅방목록갱신(){
        val jo = JsonObject()
        jo.addProperty("user_no", MyApp.userInfo.user_no)
        jo.addProperty("user_nick", MyApp.userInfo.user_nick)
        jo.addProperty("user_image", MyApp.userInfo.user_image?:"")
        jo.addProperty("cmd_type", "채팅")
        jo.addProperty("cmd", "채팅방목록")
        jo.addProperty("group_no", groupVm.groupInfo.get("group_no").asString)

        송신반복작업 = Job()
        CoroutineScope(Dispatchers.IO + 송신반복작업).launch {
//            groupVm.채팅방목록(groupVm.groupInfo.get("group_no").asInt,true)
            while(isActive){ //코루틴 스코프가 활성중이면 true, 취소되면 false
                myService!!.cli.outMsg.println(jo.toString())
                delay(200000L)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //상단바 채팅명
        binding.toolbarTv.text = "모임 채팅 - ${groupVm.groupInfo.get("group_name").asString}"
//        myService!!.putHandler(handler) // 이 fm에 등록된 핸들러를 서비스에 보내줌
//        채팅방목록초기화()
        채팅방목록갱신()

        //상단바 프로필 이미지 클릭시
        binding.toolbarIv.setOnClickListener {
            findNavController().navigate(R.id.action_global_myProfileFm)
        }
        //상단바 프로필 이미지 로딩
        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.toolbarIv)


    }

    override fun onPause() {
        super.onPause()
//        송신반복작업.cancel()
//        Toast.makeText(requireActivity(),"test1: onPause()",Toast.LENGTH_LONG).show()
    }

    override fun onStop() {
        super.onStop()
        송신반복작업.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}