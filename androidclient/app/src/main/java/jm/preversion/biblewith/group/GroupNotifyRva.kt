package jm.preversion.biblewith.group

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import jm.preversion.biblewith.databinding.GroupNotifyFmVhBinding

import jm.preversion.biblewith.moreinfo.MyNoteRvaInner
import com.google.gson.JsonObject

class GroupNotifyRva(val groupVm: GroupVm, val groupNotifyFm: GroupNotifyFm) : RecyclerView.Adapter<GroupNotifyRva.GroupNotifyFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupNotifyRva.GroupNotifyFmVh {
        return GroupNotifyFmVh(GroupNotifyFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: GroupNotifyRva.GroupNotifyFmVh, position: Int) {
//        holder.bind(groupVm.noteL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
//        return groupVm.noteL.size()
        return 3
    }



    inner class GroupNotifyFmVh(var binding: GroupNotifyFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
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