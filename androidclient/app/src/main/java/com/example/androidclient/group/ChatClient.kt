package com.example.androidclient.group

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.androidclient.MyApp
import com.example.androidclient.MyApp.application
import com.example.androidclient.MyService
import com.example.androidclient.R
import com.example.androidclient.home.MainActivity
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket


class ChatClient(private val ip: String, val myService: MyService /*, val groupVm: GroupVm*/) : /*Runnable*/ Thread() {

    val tagName = "[ChatClient]"
    lateinit var socket: Socket
    lateinit var inMsg: BufferedReader
    lateinit var outMsg: PrintWriter
    var status = false
    var ct = 0          //notification notify 시 사용할 등록id : 등록된 것을 취소할때 위한 용도인듯?

    val gson = GsonBuilder().setPrettyPrinting().create()

    init {
        Log.e(tagName, "ChatClient 스레드 생성 초기화")
    }


    fun stopClient() {
        println("연결끊음")
//        msgOut.setText("") //채팅창 비우기
//        listOut.setText(" ") //참가자 창 비우기
//        msgInput.setEditable(false) //채팅입력불가
//        clayout.show(tab, "login")
        status = false
        if (socket != null && !socket!!.isClosed) {
            try {
                socket!!.close() //예외 발생 가능성
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun 알림(jin: JsonObject) {
        Log.e(tagName, "알림 메소드 실행")
//        if(jin.get("cmd").asString == "채팅전달" || jin.get("cmd").asString == "채팅갱신" )
        val channelId = "default" //getString(R.string.default_notification_channel_id);

        val intent = Intent(myService, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(myService,0 /* Request code */, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val defaultSoundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        //알림 빌더 만들기
        val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(
            myService.application, channelId)           //여기 채널 id 지정(빌더만들시)
            .setSmallIcon(R.mipmap.ic_launcher) //drawable.splash)
            .setLargeIcon(BitmapFactory.decodeResource(myService.application.resources, R.drawable.ic_smile_icon)) //drawable.splash)
            .setContentTitle(jin.get("user_nick").asString)
            .setContentText(jin.get("chat_content").asString)
//            .setCategory(NotificationCompat.CATEGORY_ALARM)
//            .setStyle(NotificationCompat.BigTextStyle().bigText(jin.get("chat_content").asString))
            .setStyle( androidx.media.app.NotificationCompat.MediaStyle())
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setPriority(NotificationCompat.PRIORITY_MAX)
//            .setPriority(NotificationManager.IMPORTANCE_HIGH) //이거 아님
            .setContentIntent(pendingIntent)
//            .addAction(R.drawable.ic_bible, "열기", pendingIntent) //액션을 걸면 행동을 취할수 있는 버튼이 생김
//            .setFullScreenIntent(pendingIntent, true)     //풀스크린 인텐트를 걸면 중요도가 특수레벨로 올라서 알림이 꺼지지 않음


        Log.e(tagName, "알림 메소드 중간")
        //알림매니저 가져옴
        val notificationManager = myService.application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        // Since android Oreo notification channel is needed.
        // 알림 채널 만들기 - 버전에 따라 채널을 만들어야할지 말지 결정함 - 오레오 이상은 채널 만들기 필수!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel( channelId,"default2", NotificationManagerCompat.IMPORTANCE_HIGH) //여기 채널 id 지정(채널만들시)
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.setLightColor(R.color.colorRed)
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
            channel.setShowBadge(true)
            channel.setName("스토리타임")
            notificationManager!!.createNotificationChannel(channel)  // 채널 만듦 - 빌더에 채널을 등록시키는것(등록할 채널id만 서로 맞추면됨!)
        }
        Log.e(tagName, "알림 메소드 거의 끝 - 채널만듦")

        notificationManager!!.notify(/*jin.get("user_nick").asString,*/ ct /* ID of notification */, notificationBuilder.build() )
        Log.e(tagName, "알림 게시 완료")

    }


    override fun run() {
        var msg: String
        status = true

//        myService.서버와소켓연결().run {
//            if(this == 1){
//                this@ChatClient.inMsg = myService.inMsg
//                this@ChatClient.outMsg = myService.outMsg
//                Log.e(tagName, "채팅 클라이언트 시작 성공!!")
//            }
//        } //서비스의 서버와의 소켓연결 실행
//        Log.e(tagName, "채팅 클라이언트 시작 성공22!!")


        //socket ~ outMsg 까지 MyService로 옮김 - 옮겼다가 다시 여기로 복귀.. 소켓 초기화 위치가 까다로워서..;
        socket = Socket(ip, 8088) //예외발생 가능성
        println("[Client]Server 연결 성공!!")
        inMsg = BufferedReader(InputStreamReader(socket.getInputStream())) //예외발생 가능성
        outMsg = PrintWriter(socket.getOutputStream(), true)
        Log.e(tagName, "채팅 Server 연결 성공!!")


        while (!socket.isClosed) { //수신부
//        while (status) { //수신부
            try {
                msg = /*myService.*/inMsg.readLine()
                //fm 에서 등록된 핸들러 객체를 서비스로 받아와서 서비스에서 이 스레드를 생성할때 참조로 넣어준다.
                //이 후 그 참조된 핸들러를 이용해 서버로부터 메시지를 수신하면 수신된 메시지를 핸들러 메시지로 fm에 전달해준다.
                val jin = JsonParser.parseString(msg).asJsonObject
                Log.e(tagName, "서버로부터 수신 받은 메시지: ${gson.toJson(jin)}")
                //todo 이 위치에 노티피케이션 작업을 해야할듯하다.. 핸들러는 null이 아닐때만 제할일 하니깐 냅두고 수신받은 메시지를
                // 분석하여 채팅전달 cmd 라면 액티비티 or 프래그먼트 위치에 관계없이 서비스만 살아있으면 어느위치에서도 알림을 보여 줄 수 있어야함

                if(!MyApp.inChat){ //채팅방 안에 있는지 확인여부 - GroupChatinnerfm 안에 있으면 true 그외는 false 처리해야함.
                    if (!jin.isJsonNull && jin != null) {
                        if(jin.get("cmd").asString == "채팅전달" || jin.get("cmd").asString == "채팅갱신" ){
                            ct++
                            Log.e(tagName, "알림 실행 준비")
                            알림(jin)
                        }
                    }
                }

                myService.handler?.let {
                    it.sendMessage(Message().apply {
                        this.data = Bundle().apply { putString("jin", msg) }
                        this.what = 1
                    })
                }
                
//                myService.handler?.let {  //통합된 자료를 받을려면 이렇게 로직을 나눌 필요가 없음. msg만 핸들러로 전달해서 각 fm에서 조건문 판단하면됨
//                    it.sendMessage(Message().apply {
//                        if(jin.get("cmd").asString == "채팅전달" || jin.get("cmd").asString == "채팅갱신" ){
//
//                            this.data = Bundle().apply { putString("jin", msg) }
//                            this.what = 1
//
//                        } else if(jin.get("cmd").asString == "채팅방목록"){
//                            this.data = Bundle().apply { putString("jin", msg) }
//                            this.what = 2
//                        }
//                    })
//                }

//                val mhandler = Handler(Looper.getMainLooper());
//                JsonReader(inMsg)
//                val jin = JsonParser.parseReader(JsonReader(inMsg)).asJsonObject
//                val jin = JsonParser.parseString(msg).asJsonObject
//                mhandler.post {
//                    val jin = JsonParser.parseString(msg).asJsonObject
//                    Log.e(tagName, "수신 받은 메시지: $jin")
//                    //현재 방에 접속되어있는 상태임 - 접속할때 php에서 불러온 채팅리스트에 소켓에서 받아온 채팅을 추가추가한뒤 리사이클러뷰에서 갱신하여 보여줌
//                    // - 물론 채팅서버에서는 보낸 채팅이 이미 jdbc를 통해 db에 저장된 상태이고, 여긴 그것을 그냥 리다이렉트 해온 메시지임
//                    //그래서 이방에 재접속할때나 다른 방에 접속할때에는 db에 저장된 채팅리스트들을 새롭게 갱신받아 보여줄 수 있는 것임
//                    groupVm.chatL.add(jin)
//                    groupVm.liveChalL.value = groupVm.chatL
////                    Log.e(tagName, "메시지 chatL: ${groupVm.gson.toJson(groupVm.chatL)}")
//                }



            } catch (e: Exception) {
                 e.printStackTrace()
                status = false
                Log.e("[ChatClient]", "$name 예외발생! 클라이언트 쓰레드 종료예정")
                stopClient() //socket은 끊을 이유가 없지 유지되어야하는데.. 스레드만 끝내면되지..
            //                소켓만 유지되면 스레드는 서비스에서 시작메소드를 통해 다시 생성해서 시작하면되니까??
            }

        } //while

        println("[ChatClient]" + /*thread!!.*/name + "종료됨")
        Log.e("[ChatClient]", "$name 클라이언트 쓰레드 종료됨")
        stopClient()
    }

}


















//    companion object {
//        private const val LOGIN = 100
//        private const val LOGOUT = 200
//        private const val EXIT = 300
//        private const val NOMAL = 400
//        private const val WISPER = 500
//        private const val VAN = 600
//        private const val CPLIST = 700
//        private const val ERR_DUP = 800
//        @JvmStatic
//        fun main(args: Array<String>) {
//            val mcc = ChatClient("127.0.0.1")
//        }
//    }


//    fun connectServer() {
//        try {
//            socket = Socket(ip, 8088) //예외발생 가능성
//            println("[Client]Server 연결 성공!!")
//            inMsg = BufferedReader(InputStreamReader(socket!!.getInputStream())) //예외발생 가능성
//            outMsg = PrintWriter(socket!!.getOutputStream(), true)
//            outMsg!!.println(LOGIN.toString() + "/ 난 클라1" ) //LOGIN 명령어로 해당 ID 출력
//            thread = Thread(this)
//            thread!!.start()
////            start()
//        } catch (e: IOException) { //해당포트에 서버가 실행하고 있지 않은 경우
//            // e.printStackTrace();
//            println("서버연결불가")
//            if (!socket!!.isClosed) {
//                stopClient()
//            }
//            return
//        }
//    }





//    override fun actionPerformed(arg0: ActionEvent) {
//        val obj: Any = arg0.getSource()
//        if (obj === exitButton) {
//            outMsg!!.println(EXIT.toString() + "/" + id)
//            stopClient()
//            System.exit(0)
//
//        } else if (obj === loginButton) {
//            id = idInput.getText().trim { it <= ' ' }
//            label2.setText("대화명 : $id")
//            clayout.show(tab, "logout")
//            msgInput.setEditable(true) //채팅입력 창 활성화(채팅입력 가능)
//            connectServer()
//
//        } else if (obj === logoutButton) {
//            outMsg!!.println(LOGOUT.toString() + "/" + id)
//            stopClient()
//
//        } else if (obj === msgInput) {
//            val thread: Thread = object : Thread() {
//                //출력 쓰레드 새로생성(도배방지 기능 구현 관계상 출력 쓰레드만 sleep 시키기 위해)
//                //입력 스레드는 계속 일을 해야 채팅제한시간에도 내용이 채팅창에 추가되므로
//                override fun run() {
//
//                } //run
//            } // thread
//
//            thread.start()
//        } //else if(obj == msgInput)
//    } //action


//                var rmsg: Array<String>
//                rmsg = msg.split("/").toTypedArray()
//                val commend = rmsg[0].toInt()
//                when (commend) {
//                    WISPER -> {
//                        //귓속말이 온 경우
//                        msgOut.append( """${rmsg[1]}>>${rmsg[2]} ${rmsg[3]}""".trimIndent())
//                    }
//                    CPLIST -> {
//                        //채팅참가자 리스트가 온 경우
//                        val userlist = rmsg[1].split(",").toTypedArray()
//                        // 1번 인덱스에 있는 참가자 ID SET을 ,를 구분자로 하여 userlist배열에 담기
//                        val size = userlist.size
//                        listOut.setText(" ") //참가자 리스트창 비우기
//                        var i = 0
//                        while (i < size) {
//                            // 요소 하나씩 읽어들여서 참가자 리스트에 추가
//                            listOut.append(userlist[i])
//                            listOut.append("\n")
//                            i++
//                        }
//                    }
//                    VAN -> {
//                        run {
//                            clayout.show(tab, "login") //로그인버튼 바꾸기
//                            stopClient()
//                        }
//                        run {
//                            //id 중복으로 접속이 팅겼을 경우에 처리
//                            stopClient()
//                            msgOut.append("""${rmsg[1]}>${rmsg[2]}""".trimIndent())
//                        }
//                    }
//                    ERR_DUP -> {
//                        stopClient()
//                        msgOut.append("""${rmsg[1]}>${rmsg[2]}""".trimIndent())
//                    }
//
//                    else -> msgOut.append("""${rmsg[1]}>${rmsg[2]}""".trimIndent())
//                }
//                outMsg.setCaretPosition(msgOut.getDocument().getLength())










//
//package com.example.androidclient.group
//
//import java.awt.CardLayout
//import java.io.BufferedReader
//import java.io.IOException
//import java.io.InputStreamReader
//import java.io.PrintWriter
//import java.net.Socket
//import javax.swing.JButton
//import javax.swing.JFrame
//import javax.swing.JLabel
//import javax.swing.JPanel
//import javax.swing.JTextArea
//import javax.swing.JTextField
//
//
//class ChatClient(private val ip: String) : Runnable {
//
//    lateinit var id: String
//    lateinit var contents: String
//    lateinit var socket: Socket
//    lateinit var inMsg: BufferedReader
//    lateinit var outMsg: PrintWriter
//    lateinit var thread: Thread
//    var status = false
//
//    companion object {
//        private const val LOGIN = 100
//        private const val LOGOUT = 200
//        private const val EXIT = 300
//        private const val NOMAL = 400
//        private const val WISPER = 500
//        private const val VAN = 600
//        private const val CPLIST = 700
//        private const val ERR_DUP = 800
////        @JvmStatic
////        fun main(args: Array<String>) {
////            val mcc = ChatClient("127.0.0.1")
////        }
//    }
//
//    lateinit var loginPanel: JPanel
//    lateinit var loginButton: JButton
//    lateinit var label1: JLabel
//    lateinit var idInput: JTextField
//
//    lateinit var logoutPanel: JPanel
//    lateinit var label2: JLabel
//    lateinit var logoutButton: JButton
//
//    lateinit var msgPanel: JPanel
//    lateinit var msgInput: JTextField
//    lateinit var exitButton: JButton
//
//    lateinit var jframe: JFrame
//    lateinit var msgOut: JTextArea
//
//    lateinit var chatpListPanel: JPanel
//    lateinit var label3: JLabel
//    lateinit var listOut: JTextArea
//
//    lateinit var tab: Container
//    lateinit var clayout: CardLayout
//
//    init {
//    }
//
//    fun connectServer() {
//        try {
//            socket = Socket(ip, 8088) //예외발생 가능성
//            println("[Client]Server 연결 성공!!")
//            inMsg = BufferedReader(InputStreamReader(socket!!.getInputStream())) //예외발생 가능성
//            outMsg = PrintWriter(socket!!.getOutputStream(), true)
//            outMsg!!.println(LOGIN.toString() + "/" + id) //LOGIN 명령어로 해당 ID 출력
//            thread = Thread(this)
//            thread!!.start()
//        } catch (e: IOException) { //해당포트에 서버가 실행하고 있지 않은 경우
//            // e.printStackTrace();
//            println("서버연결불가")
//            if (!socket!!.isClosed) {
//                stopClient()
//            }
//            return
//        }
//    }
//
//    fun stopClient() {
//        println("연결끊음")
//        msgOut.setText("") //채팅창 비우기
//        listOut.setText(" ") //참가자 창 비우기
//        msgInput.setEditable(false) //채팅입력불가
//        clayout.show(tab, "login")
//        status = false
//        if (socket != null && !socket!!.isClosed) {
//            try {
//                socket!!.close() //예외 발생 가능성
//            } catch (e: IOException) {
//            }
//        }
//    }
//
//    override fun actionPerformed(arg0: ActionEvent) {
//        val obj: Any = arg0.getSource()
//        if (obj === exitButton) {
//            outMsg!!.println(EXIT.toString() + "/" + id)
//            stopClient()
//            System.exit(0)
//
//        } else if (obj === loginButton) {
//            id = idInput.getText().trim { it <= ' ' }
//            label2.setText("대화명 : $id")
//            clayout.show(tab, "logout")
//            msgInput.setEditable(true) //채팅입력 창 활성화(채팅입력 가능)
//            connectServer()
//
//        } else if (obj === logoutButton) {
//            outMsg!!.println(LOGOUT.toString() + "/" + id)
//            stopClient()
//
//        } else if (obj === msgInput) {
//            val thread: Thread = object : Thread() {
//                //출력 쓰레드 새로생성(도배방지 기능 구현 관계상 출력 쓰레드만 sleep 시키기 위해)
//                //입력 스레드는 계속 일을 해야 채팅제한시간에도 내용이 채팅창에 추가되므로
//                override fun run() {
//                    contents = msgInput.getText()
//                    //입력창의 내용 contents에 대입
//                    if (contents!!.indexOf("to") == 0) {
//                        // 처음 시작이 to (예전 코드는 중간에 to가 들어갈경우 구분 불가)
//                        val begin = contents!!.indexOf(" ") + 1
//                        //  to 1111 안녕하세요 일 경우 처음 빈칸 다음자리부터
//                        val end = contents!!.indexOf(" ", begin)
//                        //끝자리 포함x(+1 안함)  // 다음 빈칸까지(마지막 자리는 포함 안됨)
//                        val toid = contents!!.substring(begin, end)
//                        //contents에서 해당 부분을 찾아 id에 대입
//                        val wisper = contents!!.substring(end + 1)
//                        //두번째 빈칸 다음자리부터 끝까지를 뽑아서 wisper에 저장(내용)
//                        outMsg!!.println(WISPER.toString() + "/" + id + "/" + toid + "/" + wisper)
//                        // 각 내용을 /로 구분해서 출력
//
//                    } else if (contents!!.indexOf("van") == 0) { //처음 시작이 van
//                        val begin = contents!!.indexOf(" ") + 1
//                        //  to 1111 안녕하세요 일 경우 처음 빈칸 다음자리부터
//                        val vanid = contents!!.substring(begin)
//                        //contents에서 해당 부분을 찾아 vanid에 대입
//                        outMsg!!.println(VAN.toString() + "/" + id + "/" + vanid) // 각 내용을 /로 구분해서 출력
//
//                    } else {
//                        outMsg!!.println(NOMAL.toString() + "/" + id + "/" + contents)
//                        val len = contents!!.length
//                        if (len > 30) {
//                            try {
//                                msgOut.append("30자를 초과하여 도배방지를 위해 1분간 입력을 제한합니다.\n")
//                                //해당 클라이언트에서 채팅창에 메시지 출력
//                                msgInput.setText("") //입력창 비우기
//                                msgInput.setEditable(false) // 채팅입력칸 수정불가
//                                sleep(60000) //60초간 재우기
//                                msgInput.setEditable(true) //다시 살림
//                            } catch (e: Exception) {
//                                // TODO Auto-generated catch block
//                                e.printStackTrace()
//                            }
//                        } //if
//                    } //else
//                    msgInput.setText("")
//                } //run
//            } // thread
//            thread.start()
//        } //else if(obj == msgInput)
//    } //action
//
//    override fun run() {
//        var msg: String
//        var rmsg: Array<String>
//        status = true
//        while (status) { //수신부
//            try {
//                msg = inMsg!!.readLine()
//                rmsg = msg.split("/").toTypedArray()
//                val commend = rmsg[0].toInt()
//                when (commend) {
//                    WISPER -> {
//                        //귓속말이 온 경우
//                        msgOut.append(
//                            """
//                                ${rmsg[1]}>>${rmsg[2]}
//                                ${rmsg[3]}
//
//                                """.trimIndent()
//                        )
//                    }
//                    CPLIST -> {
//                        //채팅참가자 리스트가 온 경우
//                        val userlist = rmsg[1].split(",").toTypedArray()
//                        // 1번 인덱스에 있는 참가자 ID SET을 ,를 구분자로 하여 userlist배열에 담기
//                        val size = userlist.size
//                        listOut.setText(" ") //참가자 리스트창 비우기
//                        var i = 0
//                        while (i < size) {
//                            // 요소 하나씩 읽어들여서 참가자 리스트에 추가
//                            listOut.append(userlist[i])
//                            listOut.append("\n")
//                            i++
//                        }
//                    }
//                    VAN -> {
//                        run {
//                            clayout.show(tab, "login") //로그인버튼 바꾸기
//                            stopClient()
//                        }
//                        run {
//                            //id 중복으로 접속이 팅겼을 경우에 처리
//                            stopClient()
//                            msgOut.append(
//                                """
//                                    ${rmsg[1]}>${rmsg[2]}
//
//                                    """.trimIndent()
//                            )
//                        }
//                    }
//                    ERR_DUP -> {
//                        stopClient()
//                        msgOut.append(
//                            """
//                                ${rmsg[1]}>${rmsg[2]}
//
//                                """.trimIndent()
//                        )
//                    }
//                    else -> msgOut.append(
//                        """
//                            ${rmsg[1]}>${rmsg[2]}
//
//                            """.trimIndent()
//                    )
//                }
//                msgOut.setCaretPosition(msgOut.getDocument().getLength())
//            } catch (e: Exception) {
//                // e.printStackTrace();
//                status = false
//            }
//        } //while
//        println("[MultiChatClient]" + thread!!.name + "종료됨")
//    }
//
//}