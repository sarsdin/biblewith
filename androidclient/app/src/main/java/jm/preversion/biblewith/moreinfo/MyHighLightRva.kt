package jm.preversion.biblewith.moreinfo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.R
import jm.preversion.biblewith.bible.BibleDto
import jm.preversion.biblewith.bible.BibleVm
import jm.preversion.biblewith.databinding.MyHighLightFmVhBinding

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

            binding.myHighLightFmVhDateTv.text = MyApp.getTime("ui", mItem.highlight_date)

            binding.root.setOnClickListener {
                bibleVm.책장번호[0] = mItem.book!!
                bibleVm.책장번호[1] = mItem.chapter!!
                bibleVm.책장번호[2] = mItem.verse!!
                val crud = Bundle()
                crud.putString("signal", "hl_verse_page")
                bibleVm.tempObj.addProperty("signal", "hl_verse_page") //하이라이트된 절 위치로 가기
                Navigation.findNavController(it).navigate(R.id.action_global_bible_fm, crud)

            }

        }



    }
}