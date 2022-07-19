package com.example.androidclient.group

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.launch
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.databinding.GroupInWriteImageListVhBinding
import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.google.gson.JsonObject

class GroupInWriteRva(val groupVm: GroupVm, val groupInWriteFm: GroupInWriteFm) : RecyclerView.Adapter<GroupInWriteRva.GroupInWriteRvaVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupInWriteRva.GroupInWriteRvaVh {
        return GroupInWriteRvaVh(GroupInWriteImageListVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupInWriteRva.GroupInWriteRvaVh, position: Int) {
        holder.bind(groupVm.groupWriteImageUriL?.get(position))
    }

    override fun getItemCount(): Int {
        return groupVm.groupWriteImageUriL?.size ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    inner class GroupInWriteRvaVh(var binding: GroupInWriteImageListVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 포토앱에서 받아온 이미지 목록
        fun bind(mItem: Uri?) {

            binding.root.setOnClickListener {
                Toast.makeText(groupInWriteFm.requireContext(),"position: $absoluteAdapterPosition",Toast.LENGTH_SHORT).show()
            }
            //끝번 인덱스 홀더에는 보이고 나머지 홀더는 추가이미지 안보이게
            if(absoluteAdapterPosition == (groupVm.groupWriteImageUriL!!.size) - 1) {
                binding.groupInWriteImageListVhCardViewI0.visibility = View.VISIBLE
            }
            else {
                binding.groupInWriteImageListVhCardViewI0.visibility = View.GONE
            }

            binding.groupInWriteImageListVhIv.setImageURI(mItem)

            //이미지 제거 버튼 클릭시
            binding.groupInWriteImageListVhRemoveFab.setOnClickListener {
                (groupVm.groupWriteImageUriL as MutableList<Uri>).removeAt(absoluteAdapterPosition)
                groupInWriteFm.binding.groupInWriteToolbarAddImageBt.text = "이미지 ${ if (groupVm.groupWriteImageUriL?.size==0) "+" else groupVm.groupWriteImageUriL?.size }"
                notifyDataSetChanged()
            }

            //이미지 추가 이미지 클릭시
            binding.groupInWriteImageListVhAddFabI0.setOnClickListener {
                groupInWriteFm.addImageContent.launch()
            }


        }


    }
}