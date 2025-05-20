package jm.preversion.biblewith.moreinfo;

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.bible.dto.BibleDto
import jm.preversion.biblewith.bible.BibleVm;
import jm.preversion.biblewith.databinding.MyNoteFmAddVhBinding

class MyNoteFmAddRva(val bibleVm: BibleVm, val myNoteFmAdd: MyNoteFmAdd) : RecyclerView.Adapter<MyNoteFmAddRva.MyNoteFmAddVh>() {
    lateinit var newL : List<BibleDto>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyNoteFmAddVh {
        return MyNoteFmAddVh(MyNoteFmAddVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyNoteFmAddVh, position: Int) {
        holder.bind(newL[position])
    }

    override fun getItemCount(): Int {
        newL = bibleVm.verseL.filter{ it.highlight_selected == true }
        return newL.size
    }



    inner class MyNoteFmAddVh(var binding: MyNoteFmAddVhBinding) : RecyclerView.ViewHolder(binding.root) {
        //        var mItem: BibleDto? = null
        init {
        }

        fun bind(mItem: BibleDto) {
//            this.mItem = mItem;
            binding.dto = mItem
//            Log.d("디버그태그","$mItem")
            //뷰홀더 하단에 책장절 위치 표시
            binding.myNoteFmAddVhWhereTv.text = "${bibleVm.bookL[mItem.book!! - 1].book_name} ${mItem.chapter}장 ${mItem.verse}절"



        }


    }
}