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

//    override fun onViewAttachedToWindow(holder: GroupInVhImageRvaVh) {
//        holder.setIsRecyclable(false) //홀더뷰 재사용 안함으로 설정하여 홀더의 절 ui들이 독립적으로 작동하게 함
//        super.onViewAttachedToWindow(holder)
//    }

    inner class GroupInVhImageRvaVh(var binding: GroupInFmVhImageRvaVhBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
        }

        //mItem -- 모임상세가져오기()
        fun bind(mItem: JsonObject) {
//        Log.e("GroupInVhImageRvaVh", "test image!!!!!! ${groupVm.gson.toJson(mItem)}")
            //mItem.get("stored_file_name") 은 JsonArray 의 요소인데 JsonArray가 기본적으로 서버로부터 [] 을 가져오기에 요소가 없으면 여기까지
            //실행도 안되니 mItem.get("stored_file_name")가 있는지(null인지) 확인할 필요도 없음.
            ImageHelper.getImageUsingGlide(groupInFm.requireContext(), mItem.get("stored_file_name").asString, binding.groupInVhImageRvaListVhIv)









        }


    }
}