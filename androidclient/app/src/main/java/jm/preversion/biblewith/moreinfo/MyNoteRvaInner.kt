package jm.preversion.biblewith.moreinfo
import android.util.Log

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.bible.BibleVm
import jm.preversion.biblewith.databinding.MyNoteFmVhInVhBinding
import jm.preversion.biblewith.bible.dto.NoteVerseDto

class MyNoteRvaInner(
    val bibleVm: BibleVm, val myNoteFm: MyNoteFm, val myNoteFmVh: MyNoteRva.MyNoteFmVh, val note_verseL: List<NoteVerseDto>
) : RecyclerView.Adapter<MyNoteRvaInner.MyNoteFmVhInVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyNoteFmVhInVh {
        return MyNoteFmVhInVh(MyNoteFmVhInVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyNoteFmVhInVh, position: Int) {
        holder.bind(note_verseL[position])
    }

    override fun getItemCount(): Int {
        return note_verseL.size
    }



    inner class MyNoteFmVhInVh(var binding: MyNoteFmVhInVhBinding) : RecyclerView.ViewHolder(binding.root) {
        //        var mItem: BibleDto? = null
        init {
        }

        fun bind(mItem: NoteVerseDto) {
//            Log.e("오류태그", "$mItem")
//            this.mItem = mItem;
//            binding.dto = mItem
            binding.myNoteFmVhInVhWhereTv.text = "${bibleVm.bookL[mItem.book - 1].book_name} ${mItem.chapter}장 ${mItem.verse}절"
            binding.myNoteFmVhInVhVerseTv.text = mItem.content

        }


    }
}