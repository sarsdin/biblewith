package jm.preversion.biblewith.group.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.databinding.GroupChatInnerImageRvVhBinding
import jm.preversion.biblewith.group.GroupVm
import jm.preversion.biblewith.util.ImageHelper
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class GroupChatInnerRvaInImgRva (val groupVm: GroupVm, val images : JsonArray, val groupChatInnerFm: GroupChatInnerFm)
    : RecyclerView.Adapter<GroupChatInnerRvaInImgRva.GroupChatInnerRvaInImgRvaVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatInnerRvaInImgRvaVh {
        return GroupChatInnerRvaInImgRvaVh(GroupChatInnerImageRvVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupChatInnerRvaInImgRvaVh, position: Int) {
        holder.bind(images.asJsonArray[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        if(images.asJsonArray.size() == 0 || images.asJsonArray.isJsonNull ){
            return 0
        }
        return images.asJsonArray.size()
    }

    override fun onViewAttachedToWindow(holder: GroupChatInnerRvaInImgRvaVh) {
        holder.setIsRecyclable(false)
        super.onViewAttachedToWindow(holder)
    }

    inner class GroupChatInnerRvaInImgRvaVh(var binding: GroupChatInnerImageRvVhBinding) : RecyclerView.ViewHolder(binding.root) {

        init {
        }

        //mItem --
        fun bind(mItem: JsonObject) {
//        Log.e("오류태그", "test image!!!!!!")
            ImageHelper.getImageUsingGlide(groupChatInnerFm.requireContext(),
                mItem.get("stored_file_name").asString,
                binding.imageIv)








        }


    }
}