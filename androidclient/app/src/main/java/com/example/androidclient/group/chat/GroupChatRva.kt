package com.example.androidclient.group.chat

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.navigation.Navigation.findNavController
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupChatFmVhBinding
import com.example.androidclient.group.GroupVm

import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GroupChatRva(val groupVm: GroupVm, val groupChatFm: GroupChatFm) : RecyclerView.Adapter<GroupChatRva.GroupChatFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatFmVh {
        return GroupChatFmVh(GroupChatFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupChatFmVh, position: Int) {
        holder.bind(groupVm.chatRoomInfoL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.chatRoomInfoL.size()
    }



    inner class GroupChatFmVh(var binding: GroupChatFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 노트목록가져오기() :getNoteList 메소드로부터
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;

            ImageHelper.getImageUsingGlide(groupChatFm.requireContext(), mItem.asJsonObject.get("chat_room_image").asString, binding.chatIv)
            binding.chatTitle.text = mItem.asJsonObject.get("chat_room_title").asString
            //마지막 채팅내용을 실시간으로 표시해야하는데, 백그라운드의 소켓통신 스트림의 받은 채팅내용을 채팅리스트변수에 업데이트 시켜줌과 동시에
            //알림의 읽지않은 채팅 개수 유무도 표시해야함
//            binding.lastChat.text = mItem.asJsonObject.get("chat_room_title").asString
//            binding.chatDate.text = mItem.asJsonObject.get("chat_room_title").asString

            //채팅방 클릭시 - 참가하기
            binding.root.setOnClickListener {
//                setProgressDialog()
                CoroutineScope(Dispatchers.IO).launch {
                    groupVm.채팅방참가클릭(mItem.asJsonObject.get("chat_room_image").asInt,
                        MyApp.userInfo.user_no,
                        groupVm.groupInfo.get("group_no").asInt, // todo 이건 수정해야함.. 어뎁터에 받아오는 리스트에 모임번호가 있어야할듯
                        true)
                    findNavController(it).navigate(R.id.action_global_groupChatInnerFm)
                }
            }

        }



        // Function to display ProgressBar
        // inside AlertDialog
        @SuppressLint("SetTextI18n")
        fun setProgressDialog() {
            // Creating a Linear Layout
            val llPadding = 30
            val ll = LinearLayout(groupChatFm.requireContext())
            ll.orientation = LinearLayout.HORIZONTAL
            ll.setPadding(llPadding, llPadding, llPadding, llPadding)
            ll.gravity = Gravity.CENTER
            var llParam = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            llParam.gravity = Gravity.CENTER
            ll.layoutParams = llParam
            ll.setBackgroundResource(0)

            // Creating a ProgressBar inside the layout
            val progressBar = ProgressBar(groupChatFm.requireContext())
            progressBar.isIndeterminate = true
            progressBar.setPadding(0, 0, llPadding, 0)
            progressBar.layoutParams = llParam
            llParam = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            llParam.gravity = Gravity.CENTER

            // Creating a TextView inside the layout
            val tvText = TextView(groupChatFm.requireContext())
            tvText.text = "채팅방에 들어가는 중입니다 ..."
            tvText.setTextColor(Color.parseColor("#000000"))
            tvText.textSize = 20f
            tvText.layoutParams = llParam
            ll.addView(progressBar)
            ll.addView(tvText)

            // Setting the AlertDialog Builder view
            // as the Linear layout created above
            val builder: AlertDialog.Builder = AlertDialog.Builder(groupChatFm.requireContext())
            builder.setCancelable(true)
            builder.setView(ll)

            // Displaying the dialog
            val dialog: AlertDialog = builder.create()
            dialog.show()

            val window: Window? = dialog.window
            if (window != null) {
                val layoutParams = WindowManager.LayoutParams()
                layoutParams.copyFrom(dialog.window?.attributes)
                layoutParams.width = LinearLayout.LayoutParams.WRAP_CONTENT
                layoutParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
                dialog.window?.attributes = layoutParams

                // Disabling screen touch to avoid exiting the Dialog
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            }
        }



    }
}