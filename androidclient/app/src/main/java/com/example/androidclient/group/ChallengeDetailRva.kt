package com.example.androidclient.group

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.databinding.ChallengeDetailFmVhBinding
import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.google.gson.JsonObject


class ChallengeDetailRva(val groupVm: GroupVm, val challengeDetailFm: ChallengeDetailFm)
    : RecyclerView.Adapter<ChallengeDetailRva.ChallengeDetailFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeDetailRva.ChallengeDetailFmVh {
        return ChallengeDetailFmVh(ChallengeDetailFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: ChallengeDetailFmVh, position: Int) {
        holder.bind(groupVm.groupL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.groupL.size()
    }

    override fun onViewAttachedToWindow(holder: ChallengeDetailFmVh) {
        holder.setIsRecyclable(false) //홀더뷰 재사용 안함으로 설정하여 홀더의 절 ui들이 독립적으로 작동하게 함
        super.onViewAttachedToWindow(holder)
    }

    inner class ChallengeDetailFmVh(var binding: ChallengeDetailFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 모임목록가져오기()
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;
//            binding.groupIvInCardview.setImageURI(Uri.parse(UPLOADS_URL + mItem.get("group_main_image").asString))
//            binding.createCkboxVh


        }




    }
}