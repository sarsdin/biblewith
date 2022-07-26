package com.example.androidclient.group
import android.util.Log

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.databinding.GroupInFmVhImageRvaVhBinding
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonArray
import com.google.gson.JsonObject


class GroupInVhImageRva(val groupVm: GroupVm, val images : JsonArray, val groupInFm: GroupInFm) : RecyclerView.Adapter<GroupInVhImageRva.GroupInVhImageRvaVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupInVhImageRva.GroupInVhImageRvaVh {
        return GroupInVhImageRvaVh(GroupInFmVhImageRvaVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupInVhImageRva.GroupInVhImageRvaVh, position: Int) {
        holder.bind(images[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return images.size()
    }


    inner class GroupInVhImageRvaVh(var binding: GroupInFmVhImageRvaVhBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
        }

        //mItem -- 모임상세가져오기()
        fun bind(mItem: JsonObject) {
//        Log.e("오류태그", "test image!!!!!!")
        ImageHelper.getImageUsingGlide(groupInFm.requireContext(), mItem.get("stored_file_name").asString, binding.groupInVhImageRvaListVhIv)








        }


    }
}