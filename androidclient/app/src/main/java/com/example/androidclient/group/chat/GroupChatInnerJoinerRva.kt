package com.example.androidclient.group.chat

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.androidclient.MyApp
import com.example.androidclient.databinding.GroupChatInnerJoinerRvVhBinding
import com.example.androidclient.group.GroupVm
import com.example.androidclient.util.ImageHelper

import com.google.gson.JsonObject

class GroupChatInnerJoinerRva(val groupVm: GroupVm, val groupChatInnerFm: GroupChatInnerFm) : RecyclerView.Adapter<GroupChatInnerJoinerRva.GroupChatInnerJoinnerVh>() {
    val tagName = "[GroupChatInnerJoinnerRva]"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatInnerJoinnerVh {
        return GroupChatInnerJoinnerVh(GroupChatInnerJoinerRvVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupChatInnerJoinnerVh, position: Int) {
        holder.bind(groupVm.chatRoomUserL[position] as JsonObject)
    }

//    override fun onViewAttachedToWindow(holder: GroupChatInnerJoinnerVh) {
//        holder.setIsRecyclable(false)
//        super.onViewAttachedToWindow(holder)
//    }

    override fun getItemCount(): Int {
        if(groupVm.chatRoomUserL.size() == 0 || groupVm.chatRoomUserL.isJsonNull ){
            return 0
        }
        return groupVm.chatRoomUserL.size()
//        return groupVm.chatJoinerVhL.size()
    }

    inner class GroupChatInnerJoinnerVh(var binding: GroupChatInnerJoinerRvVhBinding) : RecyclerView.ViewHolder(binding.root) {
        //        var rva: GroupChatInnerFmVhRva? = null
        var rv: RecyclerView? = null

        init {
        }

        //mItem -- chatL
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;

            binding.memberWriterTv.text = mItem.get("user_nick").asString
            binding.memberDateTv.text = "${MyApp.getTime(".ui", mItem.get("user_chat_join_date").asString)}"
            if(mItem.get("user_image") != null && !mItem.get("user_image").isJsonNull){
                ImageHelper.getImageUsingGlide(groupChatInnerFm.requireActivity(), mItem.get("user_image").asString, binding.memberIv)
            }


        }


    }
}