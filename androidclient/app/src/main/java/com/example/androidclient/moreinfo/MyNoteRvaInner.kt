package com.example.androidclient.moreinfo
import android.util.Log

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.MyNoteFmVhInVhBinding
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject

class MyNoteRvaInner(
    val bibleVm: BibleVm, val myNoteFm: MyNoteFm, val myNoteFmVh: MyNoteRva.MyNoteFmVh, val note_verseL: JsonArray
) : RecyclerView.Adapter<MyNoteRvaInner.MyNoteFmVhInVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyNoteFmVhInVh {
        return MyNoteFmVhInVh(MyNoteFmVhInVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyNoteFmVhInVh, position: Int) {
        holder.bind(note_verseL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return note_verseL.size()
    }



    inner class MyNoteFmVhInVh(var binding: MyNoteFmVhInVhBinding) : RecyclerView.ViewHolder(binding.root) {
        //        var mItem: BibleDto? = null
        init {
        }

        fun bind(mItem: JsonObject) {
//            Log.e("오류태그", "$mItem")
//            this.mItem = mItem;
//            binding.dto = mItem
            binding.myNoteFmVhInVhWhereTv.text = "${bibleVm.bookL[(mItem.get("book").asInt) - 1].book_name} ${mItem.get("chapter").asString}장 ${mItem.get("verse").asString}절"
            binding.myNoteFmVhInVhVerseTv.text = mItem.get("content").asString

        }


    }
}