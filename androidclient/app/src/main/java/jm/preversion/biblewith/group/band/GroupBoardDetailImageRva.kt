package jm.preversion.biblewith.group.band

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.databinding.GroupBoardDetailImageListVhBinding
import jm.preversion.biblewith.group.GroupVm
import jm.preversion.biblewith.util.ImageHelper
import com.google.gson.JsonObject

class GroupBoardDetailImageRva (val groupVm: GroupVm, /*val images : JsonArray,*/ val groupBoardDetail: GroupBoardDetail)
    : RecyclerView.Adapter<GroupBoardDetailImageRva.GroupBoardDetailImageRvaVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupBoardDetailImageRvaVh {
        return GroupBoardDetailImageRvaVh(GroupBoardDetailImageListVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupBoardDetailImageRvaVh, position: Int) {
        holder.bind(groupVm.gboardInfo.get("gboard_image").asJsonArray[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.gboardInfo.get("gboard_image").asJsonArray.size()
    }


    inner class GroupBoardDetailImageRvaVh(var binding: GroupBoardDetailImageListVhBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
        }

        //mItem -- 모임상세가져오기()
        fun bind(mItem: JsonObject) {
//        Log.e("오류태그", "test image!!!!!!")
            ImageHelper.getImageUsingGlide(groupBoardDetail.requireContext(),
                mItem.get("stored_file_name").asString,
                binding.imageIv)








        }


    }
}