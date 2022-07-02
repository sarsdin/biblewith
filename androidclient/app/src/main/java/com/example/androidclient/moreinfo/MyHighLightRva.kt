package com.example.androidclient.moreinfo

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.bible.BibleDto
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.MyHighLightFmVhBinding

class MyHighLightRva(val bibleVm: BibleVm, val myHighLightFm: MyHighLightFm) : RecyclerView.Adapter<MyHighLightRva.MyHighLightFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHighLightFmVh {
        return MyHighLightFmVh(MyHighLightFmVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHighLightFmVh, position: Int) {
        holder.bind(bibleVm.highL[position]);
    }

    override fun getItemCount(): Int {
        return bibleVm.highL.size;
    }

    inner class MyHighLightFmVh(var binding: MyHighLightFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
//        var mItem: BibleDto? = null
        init {
        }

        fun bind(mItem: BibleDto) {
//            this.mItem = mItem;
            binding.dto = mItem
            Log.e("[MyHighLightRva]", "bind() mItem: ${binding.dto}")


        }



    }
}