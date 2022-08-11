package com.example.androidclient.group

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.androidclient.databinding.GroupChatInnerFmVhBinding
import com.example.androidclient.databinding.GroupChatInnerFmVhLeftBinding

import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.google.gson.JsonObject

class GroupChatInnerRva(val groupVm: GroupVm, val groupChatFm: GroupChatInnerFm) : RecyclerView.Adapter<GroupChatInnerRva.GroupChatInnerFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatInnerRva.GroupChatInnerFmVh {
        return GroupChatInnerFmVh(GroupChatInnerFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupChatInnerRva.GroupChatInnerFmVh, position: Int) {
//        holder.bind(groupVm.noteL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
//        return groupVm.noteL.size()
        return 3
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    inner class GroupChatInnerFmVh(var mBinding: ViewBinding) : RecyclerView.ViewHolder(mBinding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        private lateinit var rBinding: GroupChatInnerFmVhBinding
        private lateinit var lBinding: GroupChatInnerFmVhLeftBinding

        init {
        }

        //mItem -- 노트목록가져오기() :getNoteList 메소드로부터
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;
            if(mBinding is GroupChatInnerFmVhBinding){
                rBinding = mBinding as GroupChatInnerFmVhBinding
            } else if (mBinding is GroupChatInnerFmVhLeftBinding){
                lBinding = mBinding as GroupChatInnerFmVhLeftBinding
            }



        }


    }
}