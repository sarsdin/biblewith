package com.example.androidclient.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.ChallengeDetailListVhBinding
import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class ChallengeDetailListRva(val groupVm: GroupVm, val challengeDetailListFm: ChallengeDetailListFm)
    : RecyclerView.Adapter<ChallengeDetailListRva.ChallengeDetailListFmVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeDetailListFmVh {
        return ChallengeDetailListFmVh(ChallengeDetailListVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: ChallengeDetailListFmVh, position: Int) {
        holder.bind(groupVm.chalDetailL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.chalDetailL.size()
    }

    override fun onViewAttachedToWindow(holder: ChallengeDetailListFmVh) {
        holder.setIsRecyclable(false) //홀더뷰 재사용 안함으로 설정하여 홀더의 절 ui들이 독립적으로 작동하게 함
        super.onViewAttachedToWindow(holder)
    }

    inner class ChallengeDetailListFmVh(var binding: ChallengeDetailListVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 챌린지상세목록가져오기
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;
//            binding.groupIvInCardview.setImageURI(Uri.parse(UPLOADS_URL + mItem.get("group_main_image").asString))

            //목록 클릭시 상세페이지로 감
            binding.root.setOnClickListener {
                CoroutineScope(Dispatchers.Main).launch {
                    val jo = JsonObject()
                    jo.addProperty("chal_no", mItem.get("chal_no").asInt)
                    jo.addProperty("progress_day", mItem.get("progress_day").asInt)
                    binding.progressBar.visibility = View.VISIBLE
                    //100%이고 영상인증이 되었으면 detailafter fm으로 가야하고, 아니면 detail 페이지로 가야함
                    if(mItem.get("progress_percent").asInt == 100){
//                        groupVm.챌린지인증한내용불러오기(jo , true)
                    } else {
                        groupVm.챌린지인증진행하기(jo , true)
                    }
                    binding.progressBar.visibility = View.GONE
                    groupVm.chalDetailInfo = mItem //현재 홀더 정보를 이후페이지에서 사용할 수 있게 참조
                    //진행 구간정보텍스트를 다음 상세 페이지에 번들로 넘김  
                    val verseScope = Bundle()
                    verseScope.putCharSequence("verseScope", binding.detailVhContentTv.text )
                    Navigation.findNavController(it).navigate(R.id.action_challengeDetailListFm_to_challengeDetailFm, verseScope)
                }
            }


            //iv는 경과일이 지나고 + 완료안됐으면 빨간색마크, 진행완료(체크수)면 초록색마크 , 오늘 당일은 노마크 + 현재 일(D-day마크)
            //현재일 보다 적으면 == 안지났으면 -> today 마크,     현재일 보다 많으면 == 지났으면 + 진행율 not 100% -> 빨간마크
            // 진행율 100% -> 초록마크
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss")
            val currentDate = LocalDateTime.now()
            val relativeDate = LocalDateTime.parse(mItem.get("progress_date").asString, format)
            val relativeDatePlusOneDay = relativeDate.plusDays(1L) //현재일에서 24시간 지난 시간
            if(mItem.get("progress_percent").asInt == 100){ //100% 진행율이면 그냥 초록마크!
                binding.detailListFmVhIv.setImageResource(R.drawable.ic_smile_icon)

            } else if(currentDate.isBefore(relativeDatePlusOneDay)){ //현재일이 예정일보다 이전이냐 == 안지났냐?하면 참이고 진행가능한 today 마크
                binding.detailListFmVhIv.setImageResource(R.drawable.ic_today)

            } else if (currentDate.isAfter(relativeDatePlusOneDay)) { // 예정일이 지났는데 100%도 아니다? == 빨간마크
                binding.detailListFmVhIv.setImageResource(R.drawable.ic_sorrow_icon)
            }
//            binding.detailListFmVhIv.rotation = -30f


            //진행율 표시
            binding.detailVhProgress.progress = mItem.get("progress_percent").asInt
            binding.detailVhProgressTv.text = "진행${mItem.get("progress_percent").asString}%"

            //시작 절, 끝 절
            val verseMin = mItem.get("first_verse").asJsonArray.get(0).asJsonObject
            val verseMax = mItem.get("last_verse").asJsonArray.get(0).asJsonObject
//            binding.detailListFmVhDateTv.text = "${mItem.get("progress_day").asString}일차"
            //예정일, 일차, 진행할 구간
            binding.detailListFmVhDateTv.text = "예정일: ${MyApp.getTime(".ui", mItem.get("progress_date").asString)} - " +
                    "${mItem.get("progress_day").asString}일차"
            binding.detailVhContentTv.text =
                "진행 구간:  " +
                        "${verseMin.get("book_name").asString} ${verseMin.get("chapter").asString}장 ${verseMin.get("verse").asString}절 - " +
                        "${verseMax.get("book_name").asString} ${verseMax.get("chapter").asString}장 ${verseMax.get("verse").asString}절"
//                "${mItem.get("book_name").asString} ${mItem.get("chapter").asString}장 ${mItem.get("verse").asString}절 - " +
//                        "${mItem.get("book_name").asString} ${mItem.get("chapter").asString}장 ${mItem.get("verse").asString}절"





        }




    }
}













//                      getChallengeDetailList
//                                  "result": [
//2022-07-30 17:16:26.711 D/OkHttp:         {
//    2022-07-30 17:16:26.711 D/OkHttp:             "chal_no": "12",
//    2022-07-30 17:16:26.711 D/OkHttp:             "progress_day": "1",
//    2022-07-30 17:16:26.711 D/OkHttp:             "is_checked": "0",
//    2022-07-30 17:16:26.711 D/OkHttp:             "start_date": "2022-07-30 16:57:08",
//    2022-07-30 17:16:26.711 D/OkHttp:             "progress_date": "2022-07-30 16:57:08",
//    2022-07-30 17:16:26.711 D/OkHttp:             "chal_detail_no": "10192",
//    2022-07-30 17:16:26.711 D/OkHttp:             "first_verse": [
//    2022-07-30 17:16:26.711 D/OkHttp:                 {
//        2022-07-30 17:16:26.711 D/OkHttp:                     "chal_detail_verse_no": "1407",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "chal_no": "12",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "bible_no": "6511",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "is_checked": "0",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "progress_day": "1",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "book": "7",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "chapter": "1",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "verse": "1",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "content": "여호수아가 죽은 후에 이스라엘 자손이 여호와께 묻자와 가로되 우리 중 누가 먼저 올라가서 가나안 사람과 싸우리이까",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "book_no": "7",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "book_name": "사사기",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "book_category": "구약"
//        2022-07-30 17:16:26.711 D/OkHttp:                 }
//    2022-07-30 17:16:26.711 D/OkHttp:             ],
//    2022-07-30 17:16:26.711 D/OkHttp:             "last_verse": [
//    2022-07-30 17:16:26.711 D/OkHttp:                 {
//        2022-07-30 17:16:26.711 D/OkHttp:                     "chal_detail_verse_no": "1420",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "chal_no": "12",
//        2022-07-30 17:16:26.711 D/OkHttp:                     "bible_no": "6524",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "is_checked": "0",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "progress_day": "1",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "book": "7",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "chapter": "1",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "verse": "14",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "content": "악사가 출가할 때에 그에게 청하여 자기 아비에게 밭을 구하자 하고 나귀에서 내리매 갈렙이 묻되 네가 무엇을 원하느냐",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "book_no": "7",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "book_name": "사사기",
//        2022-07-30 17:16:26.712 D/OkHttp:                     "book_category": "구약"
//        2022-07-30 17:16:26.712 D/OkHttp:                 }
//    2022-07-30 17:16:26.712 D/OkHttp:             ],
//    2022-07-30 17:16:26.712 D/OkHttp:             "verse_count": 14,
//    2022-07-30 17:16:26.712 D/OkHttp:             "progress_percent": 0
//    2022-07-30 17:16:26.712 D/OkHttp:         },