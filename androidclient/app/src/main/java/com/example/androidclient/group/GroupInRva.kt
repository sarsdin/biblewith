package com.example.androidclient.group

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.androidclient.databinding.GroupInFmVhBinding

import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonObject

class GroupInRva(val groupVm: GroupVm, val groupInFm: GroupInFm) : RecyclerView.Adapter<GroupInRva.GroupInFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupInRva.GroupInFmVh {
        return GroupInFmVh(GroupInFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupInRva.GroupInFmVh, position: Int) {
        holder.bind(groupVm.gboardL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.gboardL.size()
    }



    inner class GroupInFmVh(var binding: GroupInFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 모임목록가져오기()
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;
            val ih =  ImageHelper()
//            binding.groupIvInCardview.setImageURI(Uri.parse(UPLOADS_URL + mItem.get("group_main_image").asString))
//            ih.getImageUsingGlide(groupListFm.requireContext(), mItem.get("group_main_image").asString, binding.groupIvInCardview)
//            binding.myNoteFmVhContentTv.text = mItem.get("group_name").asString


        }


    }
}