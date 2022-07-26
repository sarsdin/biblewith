package com.example.androidclient.group

import android.graphics.*
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.ChallengeFmListVhBinding
import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.google.android.material.button.MaterialButton
import com.google.gson.JsonObject

class ChallengeFmRva(val groupVm: GroupVm, val challengeFm: ChallengeFm)
    : RecyclerView.Adapter<ChallengeFmRva.ChallengeFmRvaVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeFmRva.ChallengeFmRvaVh {
        return ChallengeFmRvaVh(ChallengeFmListVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: ChallengeFmRva.ChallengeFmRvaVh, position: Int) {
        holder.bind(groupVm.groupL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.groupL.size()
    }


    inner class ChallengeFmRvaVh(var binding: ChallengeFmListVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 모임목록가져오기()
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;
//            binding.groupIvInCardview.setImageURI(Uri.parse(UPLOADS_URL + mItem.get("group_main_image").asString))



        }


    }
}