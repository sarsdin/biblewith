package jm.preversion.biblewith.moreinfo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.bible.dto.BibleDto
import jm.preversion.biblewith.bible.BibleVm
import jm.preversion.biblewith.databinding.MyNoteFmUpdateVhBinding
import jm.preversion.biblewith.bible.dto.NoteVerseDto

class MyNoteFmUpdateRva(val bibleVm: BibleVm, val myNoteFmUpdate: MyNoteFmUpdate) : RecyclerView.Adapter<MyNoteFmUpdateRva.MyNoteFmUpdateVh>() {
    lateinit var newL : List<BibleDto>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyNoteFmUpdateVh {
        return MyNoteFmUpdateVh(MyNoteFmUpdateVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyNoteFmUpdateVh, position: Int) {
        holder.bind(newL[position])
    }

    override fun getItemCount(): Int {
//        newL = bibleVm.verseL.filter{ it.highlight_selected }
        var verseL = bibleVm.noteUpdateO.getNote_verseL()
        var nL :List<BibleDto> = verseL.map {
            BibleDto(
                it.bible_no ?: 0,
                "",
                it.book,
                bibleVm.bookL[it.book - 1].book_name,
                it.chapter,
                it.verse,
                it.content,
                0,
                "",
                0,
                0,
                false,
                false
            )
        }
        newL = nL
//        newL = bibleVm.verseL.filter{ it.highlight_selected }
        return newL.size
    }



    inner class MyNoteFmUpdateVh(var binding: MyNoteFmUpdateVhBinding) : RecyclerView.ViewHolder(binding.root) {
        //        var mItem: BibleDto? = null
        init {
        }

        fun bind(mItem: BibleDto) {
//            this.mItem = mItem;
            binding.dto = mItem
//            Log.d("디버그태그","$mItem")
            //뷰홀더 하단에 책장절 위치 표시
            binding.myNoteFmUpdateVhWhereTv.text = "${bibleVm.bookL[mItem.book!! - 1].book_name} ${mItem.chapter}장 ${mItem.verse}절"



        }


    }
}