package com.example.androidclient.group

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.androidclient.databinding.GroupChatInnerImageRvVhBinding

import com.google.gson.JsonObject

class GroupChatInnerImageRva(val groupVm: GroupVm, val groupChatInnerFm: GroupChatInnerFm) : RecyclerView.Adapter<GroupChatInnerImageRva.GroupChatInnerImageRvVh>() {
    val tagName = "[GroupChatInnerImageRva]"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatInnerImageRvVh {

        return GroupChatInnerImageRvVh(GroupChatInnerImageRvVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupChatInnerImageRvVh, position: Int) {
        holder.bind(groupVm.chatL[position] as JsonObject)
    }

    override fun onViewAttachedToWindow(holder: GroupChatInnerImageRvVh) {
        holder.setIsRecyclable(false)
        super.onViewAttachedToWindow(holder)
    }

    override fun getItemCount(): Int {
        if(groupVm.chatL.size() == 0 || groupVm.chatL.isJsonNull){
            return 0
        }
        return groupVm.chatL.size()
    }

    inner class GroupChatInnerImageRvVh(var binding: GroupChatInnerImageRvVhBinding) : RecyclerView.ViewHolder(binding.root) {
        //        var rva: GroupChatInnerFmVhRva? = null
        var rv: RecyclerView? = null

        init {
        }

        //mItem -- chatL
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;

        }


    }
}