package com.example.androidclient.bible

import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.BibleVerseBtsVhBinding
import com.example.androidclient.util.Http
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class BibleVerseBtsRva( val bibleVm: BibleVm, val bibleVerseFm: BibleVerseFm) : RecyclerView.Adapter<BibleVerseBtsRva.BibleVerseBtsRvaVh>() {

         // This property is only valid between onCreateView and onDestroyView.
//         private val binding get() = _binding!!
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BibleVerseBtsRvaVh {

            return BibleVerseBtsRvaVh(BibleVerseBtsVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: BibleVerseBtsRvaVh, position: Int) {
            holder.bind(bibleVm.colorL.get(position))
        }

        override fun getItemCount(): Int {
            return 10
        }

        override fun onViewAttachedToWindow(holder: BibleVerseBtsRvaVh) {
            holder.setIsRecyclable(false) //홀더뷰 재사용 안함으로 설정하여 홀더의 절 ui들이 독립적으로 작동하게 함
            super.onViewAttachedToWindow(holder)
    //        Log.e("[BibleVerseBtsRva]", "onViewAttachedToWindow "+ holder.getAbsoluteAdapterPosition());
        }

    inner class BibleVerseBtsRvaVh internal constructor(binding: BibleVerseBtsVhBinding)
        : RecyclerView.ViewHolder(binding.root) {
        private val vhBinding: BibleVerseBtsVhBinding = binding

        fun bind(mItem: BibleBtsDto) {
            vhBinding.dto = mItem

            //미리 BibleVm에서 지정해놓은 viewType 2 일경우(맨처음 뷰홀더) 현재 선택된 색깔정보를 표시하고 체크표시 이미지를 덧씌운다
            if (mItem.viewType == 2) {
                val di: Drawable? = ResourcesCompat.getDrawable(bibleVerseFm.requireContext().resources,
                    R.drawable.ic_xmark, null)
                vhBinding.myHighLightBtsVhIv.setImageDrawable(di)
            }

            //색깔 클릭시
            vhBinding.root.setOnClickListener {
                var signalDel = false
                //색깔 삭제 클릭시(0번홀더)
                if(absoluteAdapterPosition == 0){
                    signalDel = true    //0번 뷰홀더를 클릭하였다면 삭제 시그널을 주고 삭제 로직을 진행
                    //프로그래스바 비통기 통신 완료 전 까지 온시킴.
                    bibleVerseFm.binding.bibleVerseFmProgressbar.visibility = View.VISIBLE
                    //클릭한 뷰홀더의 색을 (현재 색깔표시하는)맨처음 뷰홀더에 적용함.
                    bibleVm.colorL[0].highlight_color = 0

                    //todo 실질적으로 하이라이트 목록을 만드는 곳.
                    //todo highL 목록에 추가된 하이라이트 절 들의 색깔을 지금 선택된 첫번째 홀더의 색깔로 변경해줘야함.
                    //todo  -- verseL에서 highL의 절들을 찾아서 highlight_color를 bibleVm.colorL[0].highlight_color 으로 갱신 후
                    //todo  절 리사이클러뷰 새로고침. 그후 highL 목록을 db에 update - selected all false로 변경
                    //0은 색깔을 제거한다는 것. 어차피 삭제처리이기때문에..
                    bibleVm.verseL.forEach{
                        if (it.highlight_selected == true) {
                            it.highlight_color = bibleVm.colorL[0].highlight_color
                        }
                    }

                    val tmpHighL = bibleVm.verseL.filter { it.highlight_selected == true } //하이라이트될 리스트
                    val delHighL = tmpHighL.map{ it.bible_no } //하이라이트 되지 않을 리스트(하이라이트 삭제)
//                Log.e("[BibleVerseBtsRva]", "delHighL : $delHighL")
                    //삭제할 하이라이트절이 없으면 함수 종료
                    if(delHighL.isEmpty()){
                        bibleVerseFm.binding.bibleVerseFmProgressbar.visibility = View.GONE
                        return@setOnClickListener
                    }

                    var map = mapOf("user_no" to MyApp.userInfo.user_no,
                        "tmpHighL" to tmpHighL,
                        "delHighL" to delHighL,
                        "book" to bibleVm.책장번호[0],
                        "chapter" to bibleVm.책장번호[1],
                        "signalDel" to signalDel
                    )
                    val retrofit = Http.getRetrofitInstance(bibleVm.host)
                    val httpBible = retrofit.create(Http.HttpBible::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
                    httpBible.getHlDelete(map)?.enqueue(object : Callback<List<BibleDto>> {
                        override fun onResponse(call: Call<List<BibleDto>>, response: Response<List<BibleDto>>) {
                            if (response.isSuccessful ){
                                var res : List<BibleDto>? = response.body()
                                Log.e("[BibleVerseBtsRva]", "delHighL 삭제 onResponse: $res")
                                bibleVm.highL = res
                                bibleVm.liveHighL.value = bibleVm.highL

                                //verseL 에 색깔 정보 업데이트하기
                                bibleVm.verseL.forEach{
                                    delHighL.forEach bk@ { delNo ->
                                        if (it.bible_no == delNo ) {    //삭제 리스트에 있었던 절의 번호와 같은 절의 색깔을 없앤다.
                                            it.highlight_color = 0
                                            return@bk       //8/28일 추가 - 코틀린의 foreach return문은 continue처럼 작동함.
                                                        // break 가 아님!! break 처럼 작동하게 할려면 Loop 절을 써서 범위밖으로 나가야함!
                                        }
                                    }
                                }
                                //verseL 에 선택(밑줄) 초기화
                                bibleVm.verseL.forEach{ it.highlight_selected = false }
                                bibleVm.liveVerseL.value = bibleVm.verseL
                                //바텀시트뷰의 절선택 텍스트뷰 초기화
                                bibleVerseFm.binding.includeLayout.bibleVerseBtsTvVerse.text = ""

                                //통신완료시 진행바 없애기
//                            bibleVerseFm.binding.bibleVerseFmProgressbar.visibility = View.GONE

                                bibleVerseFm.rva.notifyDataSetChanged() //절 목록 새로고침
                                notifyDataSetChanged() //색선택 리사이클러 새로고침
                            }
                        }
                        override fun onFailure(call: Call<List<BibleDto>>, t: Throwable) {
                            Log.e("[BibleVerseBtsRva]", "delHighL 삭제 onFailure: ${t.message}")
                        }
                    })



                //색깔 클릭시(나머지홀더)
                } else {
                    //프로그래스바 비통기 통신 완료 전 까지 온시킴.
                    bibleVerseFm.binding.bibleVerseFmProgressbar.visibility = View.VISIBLE
                    //클릭한 뷰홀더의 색을 (현재 색깔표시하는)맨처음 뷰홀더에 적용함.
                    bibleVm.colorL[0].highlight_color = mItem.highlight_color

                    //todo 실질적으로 하이라이트 목록을 만드는 곳.
                    //todo highL 목록에 추가된 하이라이트 절 들의 색깔을 지금 선택된 첫번째 홀더의 색깔로 변경해줘야함.
                    //todo  -- verseL에서 highL의 절들을 찾아서 highlight_color를 bibleVm.colorL[0].highlight_color 으로 갱신 후
                    //todo  절 리사이클러뷰 새로고침. 그후 highL 목록을 db에 update - selected all false로 변경
                    //선택된 절들 색깔을 클릭한 홀더의 색으로 변경해줌
                    bibleVm.verseL.forEach{
                        if (it.highlight_selected == true) {
                            it.highlight_color = bibleVm.colorL[0].highlight_color
                        }
                    }

                    val tmpHighL = bibleVm.verseL.filter { it.highlight_selected == true } //하이라이트될 리스트
                    val delHighL = tmpHighL.map{ it.bible_no } //하이라이트 되지 않을 리스트(하이라이트 삭제)
//                Log.e("[BibleVerseBtsRva]", "delHighL : $delHighL")


                    //tmpHighL 이 절 페이지의 하이라이트된 목록을 db에 업데이트
                    var map = mapOf("user_no" to MyApp.userInfo.user_no,
                        "tmpHighL" to tmpHighL,
                        "delHighL" to delHighL,
                        "book" to bibleVm.책장번호[0],
                        "chapter" to bibleVm.책장번호[1],
                        "signalDel" to signalDel
                    )
                    val retrofit = Http.getRetrofitInstance(bibleVm.host)
                    val httpBible = retrofit.create(Http.HttpBible::class.java) // 통신 구현체 생성(미리 보낼 쿼리스트링 설정해두는거)
                    httpBible.getHlUpdate(map)?.enqueue(object : Callback<List<BibleDto>> {
                        override fun onResponse(call: Call<List<BibleDto>>, response: Response<List<BibleDto>>) {
                            if (response.isSuccessful ){
                                var res : List<BibleDto>? = response.body()
                                Log.e("[BibleVerseBtsRva]", "tmpHighL 업데이트 onResponse: $res")
                                bibleVm.highL = res
                                bibleVm.liveHighL.value = bibleVm.highL

                                //verseL 에 색깔 정보 업데이트하기
                                bibleVm.verseL.forEach{
                                    bibleVm.highL.forEach bk@ { hl ->
                                        if (it.bible_no == hl.bible_no ) {
                                            it.highlight_color = hl.highlight_color
                                            return@bk
                                        }
                                    }
                                }
                                //verseL 에 선택(밑줄) 초기화
                                bibleVm.verseL.forEach{ it.highlight_selected = false }
                                bibleVm.liveVerseL.value = bibleVm.verseL
                                //바텀시트뷰의 절선택 텍스트뷰 초기화
                                bibleVerseFm.binding.includeLayout.bibleVerseBtsTvVerse.text = ""

                                //통신완료시 진행바 없애기
//                            bibleVerseFm.binding.bibleVerseFmProgressbar.visibility = View.GONE

                                bibleVerseFm.rva.notifyDataSetChanged() //절 목록 새로고침
                                notifyDataSetChanged() //색선택 리사이클러 새로고침
                            }
                        }
                        override fun onFailure(call: Call<List<BibleDto>>, t: Throwable) {
                            Log.e("[BibleVerseBtsRva]", "tmpHighL 업데이트 onFailure: ${t.message}")
                        }
                    })

                }

            } //색깔 뷰홀더 클릭 종료 부분




        } //bind() 끝 부분


    }

}





// TODO: Customize parameter argument names
//const val ARG_ITEM_COUNT = "item_count"
//companion object {
//
//    // TODO: Customize parameters
//    fun newInstance(itemCount: Int): MyHighLightBts =
//            MyHighLightBts().apply {
//                arguments = Bundle().apply {
//                    putInt(ARG_ITEM_COUNT, itemCount)
//                }
//            }
//
//}