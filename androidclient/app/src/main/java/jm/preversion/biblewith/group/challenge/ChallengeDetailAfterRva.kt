package jm.preversion.biblewith.group.challenge

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.databinding.ChallengeDetailAfterFmVhBinding
import jm.preversion.biblewith.group.GroupVm
import jm.preversion.biblewith.moreinfo.MyNoteRvaInner
import com.google.gson.JsonObject


class ChallengeDetailAfterRva(val groupVm: GroupVm, val challengeDetailAfterFm: ChallengeDetailAfterFm)
    : RecyclerView.Adapter<ChallengeDetailAfterRva.ChallengeDetailAfterFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeDetailAfterFmVh {
        return ChallengeDetailAfterFmVh(ChallengeDetailAfterFmVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: ChallengeDetailAfterFmVh, position: Int) {
        holder.bind(groupVm.chalDetailVerseL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.chalDetailVerseL.size()
    }

    override fun onViewAttachedToWindow(holder: ChallengeDetailAfterFmVh) {
        holder.setIsRecyclable(false) //홀더뷰 재사용 안함으로 설정하여 홀더의 절 ui들이 독립적으로 작동하게 함
        super.onViewAttachedToWindow(holder)
    }

    inner class ChallengeDetailAfterFmVh(var binding: ChallengeDetailAfterFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- getChallengeDetailVerseList
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;

            //클릭시 체크 여부 판별하여 데이터 & ui 업데이트 작업
            binding.root.setOnClickListener {
//                Toast.makeText(challengeDetailFm.requireActivity(),"${mItem.get("is_checked").asBoolean}",Toast.LENGTH_SHORT).show()
//                val jo = JsonObject()
//                jo.addProperty("chal_detail_verse_no", mItem.get("chal_detail_verse_no").asInt)
//                jo.addProperty("chal_no", mItem.get("chal_no").asInt)
//                jo.addProperty("progress_day", mItem.get("progress_day").asInt)
//                jo.addProperty("is_checked", mItem.get("is_checked").asBoolean)
//                CoroutineScope(Dispatchers.Main).launch {
//                    challengeDetailAfterFm.binding.progressBar.visibility = View.VISIBLE
//                    groupVm.챌린지인증체크업데이트(jo, true)
//                    challengeDetailAfterFm.binding.progressBar.visibility = View.GONE
//                }
//                notifyDataSetChanged()
            }


            //todo Gson asBoolean 은 버그가 있다. 조심하자! if문 & log.e 에서  정상적으로 값이 나오지 않는다!.. 리사이클러뷰에서만 그런듯하다!
//                Log.e("오류태그", "test ${mItem.get("is_checked").asString} ") //${mItem.get("is_checked").asBoolean}
//                Log.e("오류태그", "test ${mItem.get("is_checked").getAsBoolean()} ") //${mItem.get("is_checked").asBoolean}
            //after페이지는 이미 인증완료한 페이지라 체크여부를 보여줄 필요가 없다.
            /*val bol = mItem.get("is_checked").asString
            if(bol == "1"){
                binding.completeIv.visibility = View.VISIBLE

            } else {
                binding.completeIv.visibility = View.INVISIBLE
            }*/

            binding.verseNo.text = "${mItem.get("book_name").asString} ${mItem.get("chapter").asString}장 ${mItem.get("verse").asString}절"
            binding.verseContent.text = mItem.get("content").asString




        }




    }
}








//getChallengeDetailVerseList    "result": [
//2022-07-30 20:28:27.243 D/OkHttp:         {
//    2022-07-30 20:28:27.243 D/OkHttp:             "chal_detail_verse_no": "1435",
//    2022-07-30 20:28:27.243 D/OkHttp:             "chal_no": "12",
//    2022-07-30 20:28:27.243 D/OkHttp:             "bible_no": "6539",
//    2022-07-30 20:28:27.243 D/OkHttp:             "is_checked": "0",
//    2022-07-30 20:28:27.243 D/OkHttp:             "progress_day": "3",
//    2022-07-30 20:28:27.243 D/OkHttp:             "book": "7",
//    2022-07-30 20:28:27.243 D/OkHttp:             "chapter": "1",
//    2022-07-30 20:28:27.243 D/OkHttp:             "verse": "29",
//    2022-07-30 20:28:27.243 D/OkHttp:             "content": "에브라임이 게셀에 거한 가나안 사람을 쫓아내지 못하매 가나안 사람이 게셀에서 그들 중에 거하였더라",
//    2022-07-30 20:28:27.243 D/OkHttp:             "book_no": "7",
//    2022-07-30 20:28:27.243 D/OkHttp:             "book_name": "사사기",
//    2022-07-30 20:28:27.243 D/OkHttp:             "book_category": "구약"
//    2022-07-30 20:28:27.244 D/OkHttp:         },