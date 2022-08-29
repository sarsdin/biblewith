package com.example.androidclient.group

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.androidclient.databinding.GroupChatInnerJoinerRvVhBinding

import com.google.gson.JsonObject

class GroupChatInnerJoinerRva(val groupVm: GroupVm, val groupChatInnerFm: GroupChatInnerFm) : RecyclerView.Adapter<GroupChatInnerJoinerRva.GroupChatInnerJoinnerVh>() {
    val tagName = "[GroupChatInnerJoinnerRva]"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatInnerJoinerRva.GroupChatInnerJoinnerVh {
        return GroupChatInnerJoinnerVh(GroupChatInnerJoinerRvVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupChatInnerJoinerRva.GroupChatInnerJoinnerVh, position: Int) {
        holder.bind(groupVm.chatL[position] as JsonObject)
    }

//    override fun onViewAttachedToWindow(holder: GroupChatInnerJoinnerVh) {
//        holder.setIsRecyclable(false)
//        super.onViewAttachedToWindow(holder)
//    }

    override fun getItemCount(): Int {
        if(groupVm.chatL.isJsonNull || groupVm.chatL.size() == 0){
            return 0
        }
        return groupVm.chatL.size()
    }

    inner class GroupChatInnerJoinnerVh(var binding: GroupChatInnerJoinerRvVhBinding) : RecyclerView.ViewHolder(binding.root) {
        //        var rva: GroupChatInnerFmVhRva? = null
        var rv: RecyclerView? = null

        init {
        }

        //mItem -- chatL
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;
            //다른 사용자의 뷰홀더일때


        }


    }
}