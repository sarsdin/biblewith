package com.example.androidclient.group.challenge

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.ChallengeFmListVhBinding
import com.example.androidclient.group.GroupVm
import com.example.androidclient.moreinfo.MyNoteRvaInner
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChallengeFmRva(val groupVm: GroupVm, val challengeFm: ChallengeFm)
    : RecyclerView.Adapter<ChallengeFmRva.ChallengeFmRvaVh>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeFmRvaVh {
        return ChallengeFmRvaVh(ChallengeFmListVhBinding.inflate(LayoutInflater.from(parent.context), parent,false))
    }

    override fun onBindViewHolder(holder: ChallengeFmRvaVh, position: Int) {
        holder.bind(groupVm.chalL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return groupVm.chalL.size()
    }


    inner class ChallengeFmRvaVh(var binding: ChallengeFmListVhBinding) : RecyclerView.ViewHolder(binding.root) {
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem --  selected_bibleL
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;
//            binding.groupIvInCardview.setImageURI(Uri.parse(UPLOADS_URL + mItem.get("group_main_image").asString))

            binding.chalVhFmVhWriterTv.text = mItem.get("user_nick").asString
            binding.chalVhFmVhDateTv.text =
                "${MyApp.getTime(".ui", mItem.get("chal_create_date").asString)}일 시작. ${MyApp.getTime("ui", mItem.get("chal_create_date").asString)}"
            binding.chalVhFmVhContentTv.text = mItem.get("chal_title").asString
            ImageHelper.getImageUsingGlide(challengeFm.requireActivity(), mItem.get("user_image").asString, binding.chalVhFmVhIv)

            //선택한 목록 표시 - 선택된 성경책 목록을 표시
            val st = StringBuilder()
            val ja = mItem.get("selected_bibleL").asJsonArray
            ja.forEachIndexed { i, jo ->
                st.append("${i+1}.")
                st.append(jo.asJsonObject.get("book_name").asString)
                if(groupVm.selectedCreateList.size() != i+1){ //요소의 인덱스가 마지막이 아니라면 ' ' 추가
                    st.append(" ")
                }
            }
            binding.chalVhContentTv.text = "$st "


            //해당 챌린지의 디테일리스트페이지로 가기
            binding.root.setOnClickListener {
                groupVm.chalLInfo = mItem //이 챌린지의 정보를 상세페이지에서 활용할 수 있게 넘겨준다
                binding.progressBar.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.Main).launch {
                    groupVm.챌린지상세목록가져오기(mItem.get("chal_no").asInt, true)
                    //IO 쓰레드에서 네비게이션을 사용하면 에러남. 메인스레드에서만 사용해야함.
                    binding.progressBar.visibility = View.GONE
                    Navigation.findNavController(it).navigate(R.id.action_group_in_challenge_fm_to_challengeDetailListFm)
                }
            }




        }


    }
}


//                              "result": [
//2022-07-30 17:08:28.375 D/OkHttp:         {
//2022-07-30 17:08:28.375 D/OkHttp:             "chal_no": "12",
//2022-07-30 17:08:28.375 D/OkHttp:             "user_no": "0",
//2022-07-30 17:08:28.375 D/OkHttp:             "chal_title": "챌린지1",
//2022-07-30 17:08:28.375 D/OkHttp:             "chal_create_date": "2022-07-30 16:57:08",
//2022-07-30 17:08:28.375 D/OkHttp:             "isclear": "0",
//2022-07-30 17:08:28.375 D/OkHttp:             "group_no": "1",
//2022-07-30 17:08:28.375 D/OkHttp:             "user_email": "sjeys14@gmail.com",
//2022-07-30 17:08:28.375 D/OkHttp:             "user_pwd": "tjfwjdahr1!",
//2022-07-30 17:08:28.375 D/OkHttp:             "user_nick": "정목",
//2022-07-30 17:08:28.375 D/OkHttp:             "user_create_date": "2022-06-21 12:26:20",
//2022-07-30 17:08:28.375 D/OkHttp:             "user_name": "설정목",
//2022-07-30 17:08:28.375 D/OkHttp:             "user_image": null,
//2022-07-30 17:08:28.375 D/OkHttp:             "selected_bibleL": [
//2022-07-30 17:08:28.375 D/OkHttp:                 {
//2022-07-30 17:08:28.375 D/OkHttp:                     "chal_no": "12",
//2022-07-30 17:08:28.375 D/OkHttp:                     "book": "7",
//2022-07-30 17:08:28.375 D/OkHttp:                     "chal_selected_bible_no": "19",
//2022-07-30 17:08:28.375 D/OkHttp:                     "book_no": "7",
//2022-07-30 17:08:28.375 D/OkHttp:                     "book_name": "사사기",
//2022-07-30 17:08:28.375 D/OkHttp:                     "book_category": "구약"
//2022-07-30 17:08:28.375 D/OkHttp:                 },
//2022-07-30 17:08:28.375 D/OkHttp:                 {
//2022-07-30 17:08:28.375 D/OkHttp:                     "chal_no": "12",
//2022-07-30 17:08:28.375 D/OkHttp:                     "book": "8",
//2022-07-30 17:08:28.375 D/OkHttp:                     "chal_selected_bible_no": "20",
//2022-07-30 17:08:28.375 D/OkHttp:                     "book_no": "8",
//2022-07-30 17:08:28.375 D/OkHttp:                     "book_name": "룻기",
//2022-07-30 17:08:28.375 D/OkHttp:                     "book_category": "구약"
//2022-07-30 17:08:28.375 D/OkHttp:                 }
//2022-07-30 17:08:28.375 D/OkHttp:             ]
//2022-07-30 17:08:28.375 D/OkHttp:         }
//2022-07-30 17:08:28.376 D/OkHttp:     ],
//2022-07-30 17:08:28.376 D/OkHttp:     "msg": "ok"