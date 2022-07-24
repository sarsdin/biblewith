package com.example.androidclient.group
import android.util.Log

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.launch
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.databinding.GroupInUpdateImageListVhBinding
import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.example.androidclient.util.ImageHelper

class GroupInUpdateRva(val groupVm: GroupVm, val groupInUpdateFm: GroupInUpdateFm) : RecyclerView.Adapter<GroupInUpdateRva.GroupInUpdateRvaVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupInUpdateRva.GroupInUpdateRvaVh {
        return GroupInUpdateRvaVh(GroupInUpdateImageListVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupInUpdateRva.GroupInUpdateRvaVh, position: Int) {
        holder.bind(groupVm.groupWriteImageUriL?.get(position))
    }

    override fun getItemCount(): Int {
        return groupVm.groupWriteImageUriL?.size ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        return super.getItemViewType(position)
    }

    inner class GroupInUpdateRvaVh(var binding: GroupInUpdateImageListVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 포토앱에서 받아온 이미지 목록
        fun bind(mItem: Uri?) {

            binding.root.setOnClickListener {
                Toast.makeText(groupInUpdateFm.requireContext(),"position: $absoluteAdapterPosition",Toast.LENGTH_SHORT).show()
            }



            //끝번 인덱스 홀더에는 보이고 나머지 홀더는 추가이미지 안보이게
            if(absoluteAdapterPosition == (groupVm.groupWriteImageUriL!!.size) - 1) {
                binding.groupInUpdateImageListVhCardViewI0.visibility = View.VISIBLE
            }
            else {
                binding.groupInUpdateImageListVhCardViewI0.visibility = View.GONE
            }

            //이미지 뷰에 해당 uri 를 적용시켜 이미지 보이게함. write와는 달리 로컬에 있는 이미지의 Uri 가 아닌 원격 Uri 이기때문에 글라이드로 불러와야함.
            ImageHelper.getImageUsingGlideForURI(groupInUpdateFm.requireContext(), mItem, binding.groupInUpdateImageListVhIv)

            /*//Uri 리스트에 아무것도 없으면 이미지 버튼 보이기.
            if(groupVm.groupWriteImageUriL?.size == 0) {
                groupInUpdateFm.binding.groupInUpdateToolbarAddImageBt.visibility = View.VISIBLE
            } else {
                groupInUpdateFm.binding.groupInUpdateToolbarAddImageBt.visibility = View.GONE
            }*/

            //이미지 제거 버튼 클릭시
            binding.groupInUpdateImageListVhRemoveFab.setOnClickListener {
                //클릭된 해당 이미지의 절대인덱스를 이용해 리스트에서 제거
                (groupVm.groupWriteImageUriL as MutableList<Uri>).removeAt(absoluteAdapterPosition)
                groupInUpdateFm.binding.groupInUpdateToolbarAddImageBt.text = "이미지 ${ if (groupVm.groupWriteImageUriL?.size==0) "+" else groupVm.groupWriteImageUriL?.size }"
              /*  //Uri 리스트에 아무것도 없으면 이미지 버튼 보이기.
                if(groupVm.groupWriteImageUriL?.size == 0) {
                    groupInUpdateFm.binding.groupInUpdateToolbarAddImageBt.visibility = View.VISIBLE
                } else {
                    groupInUpdateFm.binding.groupInUpdateToolbarAddImageBt.visibility = View.GONE
                }*/
                notifyDataSetChanged()
            }

            //이미지 추가 이미지 클릭시 - 사진앱에서 이미지 가져오는 인텐트 실행
            binding.groupInUpdateImageListVhAddFabI0.setOnClickListener {
                groupInUpdateFm.addImageContent.launch()
            }


        }


    }
}