package com.example.androidclient.group

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.androidclient.databinding.GroupChatFmVhBinding

import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.google.gson.JsonObject

class GroupChatRva(val groupVm: GroupVm, val groupChatFm: GroupChatFm) : RecyclerView.Adapter<GroupChatRva.GroupChatFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatRva.GroupChatFmVh {
        return GroupChatFmVh(GroupChatFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupChatRva.GroupChatFmVh, position: Int) {
//        holder.bind(groupVm.noteL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
//        return groupVm.noteL.size()
        return 3
    }



    inner class GroupChatFmVh(var binding: GroupChatFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 노트목록가져오기() :getNoteList 메소드로부터
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;



        }


    }
}