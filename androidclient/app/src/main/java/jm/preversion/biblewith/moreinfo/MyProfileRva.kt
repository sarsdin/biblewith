package jm.preversion.biblewith.moreinfo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.bible.dto.BibleDto
import jm.preversion.biblewith.bible.BibleVm
import jm.preversion.biblewith.databinding.MyProfileFmVhBinding
import jm.preversion.biblewith.group.GroupVm

class MyProfileRva(val bibleVm: BibleVm, val groupVm: GroupVm, val myProfileFm: MyProfileFm) : RecyclerView.Adapter<MyProfileRva.MyProfileRvaVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyProfileRvaVh {
        return MyProfileRvaVh(MyProfileFmVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyProfileRvaVh, position: Int) {
        holder.bind(bibleVm.highL[position]);
    }

    override fun getItemCount(): Int {
        return bibleVm.highL.size;
    }

    inner class MyProfileRvaVh(var binding: MyProfileFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
//        var mItem: BibleDto? = null
        init {
        }

        fun bind(mItem: BibleDto) {
//            this.mItem = mItem;
//            binding.dto = mItem
//            Log.e("[MyHighLightRva]", "bind() mItem: ${binding.dto}")

//            binding.myHighLightFmVhDateTv.text = MyApp.getTime("ui", mItem.highlight_date)

//            binding.root.setOnClickListener {
//                bibleVm.책장번호[0] = mItem.book!!
//                bibleVm.책장번호[1] = mItem.chapter!!
//                bibleVm.책장번호[2] = mItem.verse!!
//                val crud = Bundle()
//                crud.putString("signal", "hl_verse_page")
//                bibleVm.tempObj.addProperty("signal", "hl_verse_page") //하이라이트된 절 위치로 가기
//                Navigation.findNavController(it).navigate(R.id.action_global_bible_fm, crud)
//
//            }

        }



    }
}