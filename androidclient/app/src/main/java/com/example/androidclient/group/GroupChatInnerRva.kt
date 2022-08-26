package com.example.androidclient.group

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun getItemCount(): Int {
        return groupVm.chatL.size()
    }

    override fun getItemViewType(position: Int): Int {
        super.getItemViewType(position)
        //채팅 리스트가 존재하고, 그 채팅 요소의 글쓴이가 현재 앱의 사용자의 번호와 같으면 나의 채팅으로 간주하고 오른쪽에 표시되는 뷰홀더를 선택한다!
        if (groupVm.chatL.size() != 0) {
            //최초 접속시 chat_type 을 접속알림이라고 정해서 보내고 다시 받은 메시지가 chatL 에 추가되어
            // 뷰홀더를 갱신하면 2번 viewType 을 인플레이트해야한다. 최초 방에 접속시에만 알리고 그후 방에서 나가기를 하지 않으면
            // 다름 접속시부터는 알리지 않는다.
            return if(groupVm.chatL.get(position).asJsonObject.get("chat_type").asString == "접속알림") {
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

                val now = LocalDateTime.now()
                val chatDate = LocalDateTime.parse(mItem.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
                val todayStartTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val todayStartTime2 = LocalDateTime.parse("$todayStartTime 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
//                val chatDate2 = chatDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                if(chatDate.isAfter(todayStartTime2) ){ //채팅쓴날짜시간이 오늘의 시작시간(0시 0분) 보다 더 이후(지났다)면 true
                    //검증일이 널이 아니고, 하루시작시간+24시간의 값이 채팅쓴시간보다 이전일 경우 true
                    if(groupVm.dayChangeVerify != null){
                        if(todayStartTime2.plusDays(1L).isAfter(chatDate)){
                            if(groupVm.dayChangeVerify!!.isBefore(chatDate)){
                                groupVm.dayChangeVerify = chatDate
                            } else {
                                val uiDate = chatDate.format(DateTimeFormatter.ofPattern("MM월 dd일 yyyy년"))
                                lBinding.dateLayout.visibility = View.VISIBLE
                                lBinding.dateDelimiter.text = uiDate
                            }
                        }
                    }
                }

                //0이 아니면 읽지않은수 표시
                if(mItem.get("unread_count") != null && mItem.get("unread_count").asString != "0"){
                    lBinding.unreadTv.text = mItem.get("unread_count").asString
                    lBinding.unreadTv.visibility = View.VISIBLE
                } else {
                    lBinding.unreadTv.visibility = View.GONE
                }

                //문자열일 경우와 이미지일 경우 값이 넣어지는 뷰를 달리해준다 - 에러방지위함
                if(mItem.get("chat_type") !=null && mItem.get("chat_type").asString == "문자열"){
                    lBinding.chatContent.text = mItem.get("chat_content").asString
                    lBinding.chatWriter.text = mItem.get("user_nick").asString
                    lBinding.chatDate.text = mItem.get("create_date").asString

                } else {
//                    lBinding.chatIv.setImageURI()
                }





            //나의 뷰홀더일때
            } else if(mBinding is GroupChatInnerFmVhBinding){
                rBinding = mBinding as GroupChatInnerFmVhBinding

//                rBinding.unreadTv.text = mItem.get("chat_content").asString

                val now = LocalDateTime.now()
                val chatDate = LocalDateTime.parse(mItem.get("create_date").asString, DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
                val todayStartTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val todayStartTime2 = LocalDateTime.parse("$todayStartTime 00:00:00", DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss"))
//                val chatDate2 = chatDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                if(chatDate.isAfter(todayStartTime2) ){ //채팅쓴날짜시간이 오늘의 시작시간(0시 0분) 보다 더 이후(지났다)면 true
                    //검증일이 널이 아니고, 하루시작시간+24시간의 값이 채팅쓴시간보다 이전일 경우 true
                    if(groupVm.dayChangeVerify != null){
                        if(todayStartTime2.plusDays(1L).isAfter(chatDate)){
                            if(groupVm.dayChangeVerify!!.isBefore(chatDate)){
                                groupVm.dayChangeVerify = chatDate
                            } else {
                                val uiDate = chatDate.format(DateTimeFormatter.ofPattern("MM월 dd일 yyyy년"))
                                rBinding.dateLayout.visibility = View.VISIBLE
                                rBinding.dateDelimiter.text = uiDate
                            }
                        }
                    }
                }

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
                    rBinding.chatDate.text = mItem.get("create_date").asString

                }
//                else if(mItem.get("chat_type").asString == "접속알림"){
//
//
//                } else {
////                    rBinding.chatIv.setImageURI()
//                }




            //접속알림 뷰일때 사용자 접속알림 알림
            } else if (mBinding is GroupChatInnerFmVhConnNotiBinding){
                this.conBinding = (mBinding as GroupChatInnerFmVhConnNotiBinding)
                conBinding.alimDelimiterTv.text = "${mItem.get("user_nick").asString}님이 접속하셨습니다."
            }



        }


    }
}