package com.example.androidclient.moreinfo;

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.bible.BibleDto
import com.example.androidclient.bible.BibleVm;
import com.example.androidclient.databinding.MyNoteFmVhBinding;

class MyNoteRva(val bibleVm: BibleVm, val myNoteFm: MyNoteFm) : RecyclerView.Adapter<MyNoteRva.MyNoteFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyNoteFmVh {
        return MyNoteFmVh(MyNoteFmVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyNoteFmVh, position: Int) {
        holder.bind(bibleVm.highL[position])
    }

    override fun getItemCount(): Int {
        return bibleVm.highL.size
    }



    inner class MyNoteFmVh(var binding: MyNoteFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
//        var mItem: BibleDto? = null
        init {
        }

        fun bind(mItem: BibleDto) {
//            this.mItem = mItem;
            binding.dto = mItem

        }


    }
}