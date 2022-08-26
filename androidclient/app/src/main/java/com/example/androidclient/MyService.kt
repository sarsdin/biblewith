package com.example.androidclient

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import com.example.androidclient.group.ChatClient
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket


class MyService : Service() {

    val tagName = "[MyService]"
    //mBinder 는 밑의 onBind 호출시 호출한 Activity에 전달되고 이 객체를 이용해 MyService인스턴스를 get()을 이용하여 얻을 수 있다.
    var mBinder: IBinder = MyBinder()

    inner class MyBinder : Binder() {
        val service: MyService
            get() = this@MyService
    }
    
    lateinit var cli: ChatClient //채팅 클라이언트 등록 - 소켓을 채팅서버와 연결하는 스레드 객체 - 소켓을 서비스쪽으로 옴김
    var handler : Handler? = null //액티비티와 교류할 핸들러
    val ip: String = "10.0.2.2"
//    lateinit var contents: String
    //서비스 시작시 소켓관련 초기화
//    lateinit var socket: Socket
//    lateinit var inMsg: BufferedReader
//    lateinit var outMsg: PrintWriter

    val gson = Gson()


    override fun onBind(intent: Intent): IBinder {
        // 액티비티에서 bindService(intent, connenction, Context.BIND_AUTO_CREATE) 를 실행하면 호출됨
        //unbindService(connenction) 을 이용하면 바인드 해제가능한데, 서비스가 연결되지 않아있으면
        // 오류발생하니 서비스 연결여부 is_service와 같은 값을 두고 확인해야함
        // 리턴한 IBinder 객체는 서비스와 클라이언트 사이의 인터페이스 정의한다
        return mBinder
    }


//    fun 서버와소켓연결(): Int {
//        socket = Socket(ip, 8088) //예외발생 가능성
//        println("[Client]Server 연결 성공!!")
//        Log.e(tagName, "채팅 Server 연결 성공!!")
//        inMsg = BufferedReader(InputStreamReader(socket.getInputStream())) //예외발생 가능성
//        outMsg = PrintWriter(socket.getOutputStream(), true)
//        return 1
//    }
    override fun onCreate() {
        super.onCreate()
//        서버와소켓연결() //이걸 ChatClient안에서 실행해야한다.
        cli = ChatClient("10.0.2.2", this/*, groupVm*/) //127.0.0.1 << avd에서 안드로이드os 자신을 가리킴. 내 컴퓨터의 로컬 서버가 아님..!
        cli.start()
        Log.e(tagName, "ChatClient 스레드 start() 완료")
    }

    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    //액티비티에서 보내온 핸들러 - 서비스시작시 액티비티에서 실행해줌
    fun putHandler(handler: Handler) {
        this.handler = handler
    }

    //처음 서비스 생성시 실행하게 하기
    fun initMsg(jo : JsonObject){ //프래그먼트로부터 받은 서버로 보낼 객체(메시지&사용자정보)!
        Log.e(tagName, "initMsg 메소드 실행")
        cli.outMsg.println(jo.toString())
//        CoroutineScope(Dispatchers.Default).launch {
////            myService!!.cli.outMsg.println(gson.toJson(jo))
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }

 

}