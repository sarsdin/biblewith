package com.example.androidclient.group

import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupListFmVhBinding

import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.example.androidclient.util.Http.UPLOADS_URL
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonObject

class GroupListRva(val groupVm: GroupVm, val groupListFm: GroupListFm) : RecyclerView.Adapter<GroupListRva.GroupListFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupListRva.GroupListFmVh {
        return GroupListFmVh(GroupListFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupListRva.GroupListFmVh, position: Int) {
        holder.bind(groupVm.groupL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.groupL.size()
    }



    inner class GroupListFmVh(var binding: GroupListFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 모임목록가져오기()
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;
            val ih =  ImageHelper()
//            binding.groupIvInCardview.setImageURI(Uri.parse(UPLOADS_URL + mItem.get("group_main_image").asString))
            ih.getImageUsingGlide(groupListFm.requireContext(), mItem.get("group_main_image").asString, binding.groupIvInCardview)
            binding.myNoteFmVhContentTv.text = mItem.get("group_name").asString

            //홀더 클릭시 해당하는 모임 상세 페이지로 이동
            binding.root.setOnClickListener(View.OnClickListener {
                groupVm.currentGroupIn = mItem.get("group_no").asInt
                groupVm.모임상세불러오기(true)
                Navigation.findNavController(it).navigate(R.id.action_group_fm_to_groupInFm)

            })
        }


    }
}