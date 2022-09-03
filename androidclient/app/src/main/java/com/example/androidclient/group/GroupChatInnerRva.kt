package com.example.androidclient.group
import android.util.Log

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewbinding.ViewBinding
import com.example.androidclient.MyApp
import com.example.androidclient.databinding.GroupChatInnerFmVhBinding
import com.example.androidclient.databinding.GroupChatInnerFmVhConnNotiBinding
import com.example.androidclient.databinding.GroupChatInnerFmVhLeftBinding

import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class GroupChatInnerRva(val groupVm: GroupVm, val groupChatInnerFm: GroupChatInnerFm) : RecyclerView.Adapter<GroupChatInnerRva.GroupChatInnerFmVh>() {
    val tagName = "[GroupChatInnerRva]"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatInnerRva.GroupChatInnerFmVh {

        return if(viewType == 0){
            //1이 아니면 다른 사용자용 뷰홀더 표시
            GroupChatInnerFmVh(GroupChatInnerFmVhLeftBinding.inflate(LayoutInflater.from(parent.context), parent,false))
            } else if (viewType == 1){
            //1이면 자신의 채팅을 보여주는 뷰홀더 표시
            GroupChatInnerFmVh(GroupChatInnerFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
            } else {
            //2이면 사용자 접속 알림을 보여주는 뷰홀더 표시
            GroupChatInnerFmVh(GroupChatInnerFmVhConnNotiBinding.inflate(LayoutInflater.from(parent.context), parent,false))
            }
    }

    override fun onBindViewHolder(holder: GroupChatInnerRva.GroupChatInnerFmVh, position: Int) {
        holder.bind(groupVm.chatL[position] as JsonObject)
    }


    override fun onViewAttachedToWindow(holder: GroupChatInnerFmVh) {
        holder.setIsRecyclable(false)
        super.onViewAttachedToWindow(holder)
    }

    override fun getItemCount(): Int {
        if(groupVm.chatL.isJsonNull || groupVm.chatL.size() == 0){
            return 0
        }
        return groupVm.chatL.size()
    }

    override fun getItemViewType(position: Int): Int {
        super.getItemViewType(position)
        //채팅 리스트가 존재하고, 그 채팅 요소의 글쓴이가 현재 앱의 사용자의 번호와 같으면 나의 채팅으로 간주하고 오른쪽에 표시되는 뷰홀더를 선택한다!
        if (groupVm.chatL.size() != 0) {
            //최초 접속시 chat_type 을 접속알림이라고 정해서 보내고 다시 받은 메시지가 chatL 에 추가되어
            // 뷰홀더를 갱신하면 2번 viewType 을 인플레이트해야한다. 최초 방에 접속시에만 알리고 그후 방에서 나가기를 하지 않으면
            // 다름 접속시부터는 알리지 않는다.
            return if(groupVm.chatL.get(position).asJsonObject.get("chat_type").asString == "접속알림"
                || groupVm.chatL.get(position).asJsonObject.get("chat_type").asString == "나가기알림") {
                2
            } else if(groupVm.chatL.get(position).asJsonObject.get("user_no").asInt == MyApp.userInfo.user_no){
                1
            } else {
                0
            }
        }
        return 0
    }

    inner class GroupChatInnerFmVh(var mBinding: ViewBinding) : RecyclerView.ViewHolder(mBinding.root) {

        //        var rva: GroupChatInnerFmVhRva? = null
        var rv: RecyclerView? = null

        private lateinit var lBinding: GroupChatInnerFmVhLeftBinding
        private lateinit var rBinding: GroupChatInnerFmVhBinding
        private lateinit var conBinding: GroupChatInnerFmVhConnNotiBinding

        init {
        }

        //mItem -- chatL
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;

            //다른 사용자의 뷰홀더일때
            if (mBinding is GroupChatInnerFmVhLeftBinding){
                lBinding = mBinding as GroupChatInnerFmVhLeftBinding

                ImageHelper.getImageUsingGlide(groupChatInnerFm.requireActivity(),
                    mItem.get("user_image")?.run { if (this is JsonNull) "" else asString },
                    lBinding.profileIv)
//                lBinding.unreadTv.text = mItem.get("chat_content").asString

                //날짜 표시기 처리 - 3가지 깐깐한 조건이 충족되지 않으면 표시되지않으니 안심!
                if (mItem.get("is_dayChanged") != null && !mItem.get("is_dayChanged").isJsonNull && mItem.get("is_dayChanged").asInt == 1) {
                    lBinding.dateLayout.visibility = View.VISIBLE
                    val chatDate = LocalDateTime.parse(mItem.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
                    val uiDate = chatDate.format(DateTimeFormatter.ofPattern("MM월 dd일 yyyy년"))
                    lBinding.dateDelimiter.text = uiDate
                } else {
                    lBinding.dateLayout.visibility = View.GONE
                }




//                val now = LocalDateTime.now()
//                val chatDate = LocalDateTime.parse(mItem.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
//                val todayStartTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
//                val todayStartTime2 = LocalDateTime.parse("$todayStartTime 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))

                //0번 홀더 일경우 일단 초기화 하고 로직 시작
//                Log.e(tagName, "lbinding absoluteAdapterPosition:${absoluteAdapterPosition}, ${mItem.get("chat_content")}")
//                if(absoluteAdapterPosition == 0){
//                    groupVm.daySaved = null
//                    Log.e(tagName, "groupVm.daySaved = null  실행")
//                }
//                if(groupVm.daySaved != null){
//                    //하루가 지나고 다음날이 시작하는 시점에 검사한 현재뷰홀더의 날짜시간이 그전날에 계산한 (다음날)시간을 지났을때 == 하루일과 종료(0시) 이후
//                    //바뀌고 처음 통과하는 뷰홀더의 시간 기준으로 다시 날짜딜리미터를 보여주고, 로직 반복..
//                    if(chatDate.isAfter(groupVm.dayChangeVerify)){
//                        //바뀌고 처음이면 이 chatDate 를 daySaved에 넣어야함
//                        groupVm.daySaved = chatDate //null 유무만 판단기준으로 사용 - 다른용도는 아직 없음
//                        val st뷰홀더기준그날시작시간 = chatDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
//                        val 뷰홀더기준그날시작시간 = LocalDateTime.parse("$st뷰홀더기준그날시작시간 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
//                        val 뷰홀더기준그다음날시작시간 = 뷰홀더기준그날시작시간.plusDays(1L)
//                        groupVm.dayChangeVerify = 뷰홀더기준그다음날시작시간
//
//                        val uiDate = chatDate.format(DateTimeFormatter.ofPattern("MM월 dd일 yyyy년"))
//                        lBinding.dateLayout.visibility = View.VISIBLE
//                        lBinding.dateDelimiter.text = uiDate
//                        Log.e(tagName, "2 ${groupVm.dayChangeVerify}")
//
//
//                    //현재 뷰홀더가 저장된 그다음날 전까지의 시간이라면(아직하루일과중: 새벽0시전 이라면) 데이딜리미터가 안보여야함
//                    } else {
//                        lBinding.dateLayout.visibility = View.GONE
//                        Log.e(tagName, "3 ${groupVm.dayChangeVerify}")
//                    }

                
                //저장된 현재 시간이 없다면 : 보통 포지션0번 일때 - 그 0번 포지션값을 기준으로 초기화함
//                } else {
//                    groupVm.daySaved = chatDate
//                    val st뷰홀더기준그날시작시간 = chatDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
//                    val 뷰홀더기준그날시작시간 = LocalDateTime.parse("$st뷰홀더기준그날시작시간 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
//                    val 뷰홀더기준그다음날시작시간 = 뷰홀더기준그날시작시간.plusDays(1L)
//                    groupVm.dayChangeVerify = 뷰홀더기준그다음날시작시간
//                    Log.e(tagName, "$absoluteAdapterPosition st뷰홀더기준그날시작시간:$st뷰홀더기준그날시작시간 , 뷰홀더기준그날시작시간:$뷰홀더기준그날시작시간 , 뷰홀더기준그다음날시작시간:$뷰홀더기준그다음날시작시간")
//
//                    val uiDate = chatDate.format(DateTimeFormatter.ofPattern("MM월 dd일 yyyy년"))
//                    lBinding.dateLayout.visibility = View.VISIBLE
//                    lBinding.dateDelimiter.text = uiDate
//
//                }



//                if(chatDate.isAfter(todayStartTime2) ){ //채팅쓴날짜시간이 오늘의 시작시간(0시 0분) 보다 더 이후(지났다)면 true
//                    //검증일이 널이 아니고, 하루시작시간+24시간의 값이 채팅쓴시간보다 이전일 경우 true
//                    if(groupVm.dayChangeVerify != null){
//                        if(todayStartTime2.plusDays(1L).isAfter(chatDate)){
//                            if(groupVm.dayChangeVerify!!.isBefore(chatDate)){
//                                groupVm.dayChangeVerify = chatDate
//                            } else {
//                                val uiDate = chatDate.format(DateTimeFormatter.ofPattern("MM월 dd일 yyyy년"))
//                                lBinding.dateLayout.visibility = View.VISIBLE
//                                lBinding.dateDelimiter.text = uiDate
//                            }
//                        }
//
//                    //0번인덱스 기준으로 잡고 dayChangeVerify 에 0번인덱스의 날짜를 집어넣어함 그리고 그 이후로 +1 day 식 날짜 계산해서 주욱 이어가야함..
//                    } else {
//                        groupVm.dayChangeVerify = chatDate
//                        val uiDate = chatDate.format(DateTimeFormatter.ofPattern("MM월 dd일 yyyy년"))
//                        lBinding.dateLayout.visibility = View.VISIBLE
//                        lBinding.dateDelimiter.text = uiDate
//                    }
//                }





                //0이 아니면 읽지않은수 표시
                if(mItem.get("unread_count") != null && mItem.get("unread_count").asString != "0"){
                    lBinding.unreadTv.text = mItem.get("unread_count").asString
                    lBinding.unreadTv.visibility = View.VISIBLE
                } else {
                    lBinding.unreadTv.visibility = View.GONE
                }

//                Log.e(tagName, "groupChatInnerFm.여기까지읽음: $absoluteAdapterPosition ${groupChatInnerFm.여기까지읽음}")
                //여기까지 읽었습니다. 표시
                // 0번 포지션일때는 참이되니 그냥 false 되게 조건하나 더걸어줌
                if(absoluteAdapterPosition == groupChatInnerFm.여기까지읽음 && absoluteAdapterPosition != 0 && absoluteAdapterPosition != 1){
                    lBinding.readPositionLayout.visibility = View.VISIBLE
//                    groupChatInnerFm.여기까지읽음 = 0 //0으로 바꿔서 핸들러에서 두번 반응하지 않게 초기화 시켜줌

                    //이것을 true로 바꾸면 groupChatInnerFm에서 스크롤이벤트시 여기까지읽음=0 으로 바꿔서 읽음표시 없애기!
                    //이러면 다른참가자의 스크롤이벤트로 인한 소켓통신으로 인해 옵저버의 갱신에 영향을 받지 않고
                    // 나의 스크롤이벤트에만 반응하여 여기까지읽음 뷰를 초기화할 수 있다!
                    groupChatInnerFm.여기까지읽음스크롤시초기화여부 = true
                } else {
                    lBinding.readPositionLayout.visibility = View.GONE
                }

                //문자열일 경우와 이미지일 경우 값이 넣어지는 뷰를 달리해준다 - 에러방지위함
                if(mItem.get("chat_type") !=null && mItem.get("chat_type").asString == "문자열"){
                    lBinding.chatContent.text = mItem.get("chat_content").asString
                    lBinding.chatIrv.visibility = View.GONE

                } else {
//                    lBinding.chatIv.setImageURI()
                    //chat_type 이 '이미지' 일때는 리사이클러뷰 adapter 를 만들어줘야한다.
                    lBinding.chatIrv.visibility = View.VISIBLE
                    val rv = lBinding.chatIrv
                    rv.layoutManager = LinearLayoutManager(groupChatInnerFm.requireActivity()).apply { orientation = LinearLayoutManager.HORIZONTAL}
                    rv.adapter = GroupChatInnerRvaInImgRva(groupVm, mItem.get("chat_image").asJsonArray, groupChatInnerFm) //이미지가 있으니 '이미지' 타입으로 저장됐겠지!?
                    val rva = rv.adapter as GroupChatInnerRvaInImgRva
                    //이미지일때는 content의 값은 문자열이 없으니 이미지 개수를 표시해준다
                    lBinding.chatContent.text = "받은 이미지 ${mItem.get("chat_image").asJsonArray.size()}장"
                }

                lBinding.chatWriter.text = mItem.get("user_nick").asString
                lBinding.chatDate.text = mItem.get("create_date").asString




            //나의 뷰홀더일때
            } else if(mBinding is GroupChatInnerFmVhBinding){
                rBinding = mBinding as GroupChatInnerFmVhBinding

//                rBinding.unreadTv.text = mItem.get("chat_content").asString


                //날짜 표시기 처리 - 3가지 깐깐한 조건이 충족되지 않으면 표시되지않으니 안심!
                if (mItem.get("is_dayChanged") != null && !mItem.get("is_dayChanged").isJsonNull && mItem.get("is_dayChanged").asInt == 1) {
                    rBinding.dateLayout.visibility = View.VISIBLE
                    val chatDate = LocalDateTime.parse(mItem.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
                    val uiDate = chatDate.format(DateTimeFormatter.ofPattern("MM월 dd일 yyyy년"))
                    rBinding.dateDelimiter.text = uiDate
                } else {
                    rBinding.dateLayout.visibility = View.GONE
                }
                

//                val now = LocalDateTime.now()
//                val chatDate = LocalDateTime.parse(mItem.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
//                val todayStartTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
//                val todayStartTime2 = LocalDateTime.parse("$todayStartTime 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
////                val chatDate2 = chatDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
//                if(chatDate.isAfter(todayStartTime2) ){ //채팅쓴날짜시간이 오늘의 시작시간(0시 0분) 보다 더 이후(지났다)면 true
//                    //검증일이 널이 아니고, 하루시작시간+24시간의 값이 채팅쓴시간보다 이전일 경우 true
//                    if(groupVm.dayChangeVerify != null){
//                        if(todayStartTime2.plusDays(1L).isAfter(chatDate)){
//                            if(groupVm.dayChangeVerify!!.isBefore(chatDate)){
//                                groupVm.dayChangeVerify = chatDate
//                            } else {
//                                val uiDate = chatDate.format(DateTimeFormatter.ofPattern("MM월 dd일 yyyy년"))
//                                rBinding.dateLayout.visibility = View.VISIBLE
//                                rBinding.dateDelimiter.text = uiDate
//                            }
//                        }
//                    }
//                }

                //0이 아니면 읽지않은수 표시
                if(mItem.get("unread_count")!=null && mItem.get("unread_count").asString != "0"){
                    rBinding.unreadTv.text = mItem.get("unread_count").asString
                    rBinding.unreadTv.visibility = View.VISIBLE
                } else {
                    rBinding.unreadTv.visibility = View.GONE
                }

                //문자열일 경우와 이미지일 경우 값이 넣어지는 뷰를 달리해준다 - npe 에러방지위함
                if(mItem.get("chat_type")!=null && mItem.get("chat_type").asString == "문자열"){
                    rBinding.chatContent.text = mItem.get("chat_content").asString
                    rBinding.chatIrv.visibility = View.GONE

                } else {
//                    rBinding.chatIv.setImageURI()
                    //chat_type 이 '이미지' 일때는 리사이클러뷰 adapter 를 만들어줘야한다.
                    rBinding.chatIrv.visibility = View.VISIBLE
                    val rv = rBinding.chatIrv
                    rv.layoutManager = LinearLayoutManager(groupChatInnerFm.requireActivity()).apply { orientation = LinearLayoutManager.HORIZONTAL}
                    rv.adapter = GroupChatInnerRvaInImgRva(groupVm, mItem.get("chat_image").asJsonArray, groupChatInnerFm) //이미지가 있으니 '이미지' 타입으로 저장됐겠지!?
                    val rva = rv.adapter as GroupChatInnerRvaInImgRva
                    //이미지일때는 content의 값은 문자열이 없으니 이미지 개수를 표시해준다
                    rBinding.chatContent.text = "보낸 이미지 ${mItem.get("chat_image").asJsonArray.size()}장"
                }

                rBinding.chatDate.text = mItem.get("create_date").asString




            //접속알림 뷰일때 사용자 접속알림 알림
            } else if (mBinding is GroupChatInnerFmVhConnNotiBinding){
                this.conBinding = (mBinding as GroupChatInnerFmVhConnNotiBinding)
                if (mItem.get("chat_type").asString == "접속알림") {
                    conBinding.alimDelimiterTv.text = "${mItem.get("user_nick").asString}님이 접속하셨습니다."

                } else { // '나가기알림' 일 경우
                    conBinding.alimDelimiterTv.text = "${mItem.get("user_nick").asString}님이 나가셨습니다."

                }
            }



        }


    }
}