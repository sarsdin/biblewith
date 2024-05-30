package jm.preversion.biblewith.moreinfo;

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.R
import jm.preversion.biblewith.bible.BibleVm
import jm.preversion.biblewith.databinding.MyNoteFmVhBinding
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyNoteRva(val bibleVm: BibleVm, val myNoteFm: MyNoteFm) : RecyclerView.Adapter<MyNoteRva.MyNoteFmVh>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyNoteFmVh {

        return MyNoteFmVh(MyNoteFmVhBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: MyNoteFmVh, position: Int) {
        holder.bind(bibleVm.noteL[position] as JsonObject)
    }

    override fun getItemCount(): Int {
        return bibleVm.noteL.size()
    }



    inner class MyNoteFmVh(var binding: MyNoteFmVhBinding) : RecyclerView.ViewHolder(binding.root) {
//        var mItem: BibleDto? = null
//        lateinit var rva: MyNoteRvaInner
//        lateinit var rv: RecyclerView
        var rva: MyNoteRvaInner? = null
        var rv: RecyclerView? =null

        init {
        }

        //mItem -- 노트목록가져오기() :getNoteList 메소드로부터
        fun bind(mItem: JsonObject) {
//            this.mItem = mItem;
            binding.myNoteFmVhWhereTv.text = "${bibleVm.bookL[(mItem.get("note_verseL").asJsonArray.get(0).asJsonObject.get("book").asInt) - 1].book_name} ${mItem.get("note_verseL").asJsonArray.get(0).asJsonObject.get("chapter").asString}장"
            binding.myNoteFmVhDateTv.text = MyApp.getTime("ui", mItem.get("note_date").asString)
            binding.myNoteFmVhContentTv.text = mItem.get("note_content").asString

//            Log.e("오류태그outter", "$mItem")
            rv = binding.myNoteFmVhVerseList
            rv!!.layoutManager = LinearLayoutManager(binding.root.context)
            binding.myNoteFmVhVerseList.adapter = MyNoteRvaInner(bibleVm, myNoteFm, this, mItem.get("note_verseL").asJsonArray)
            rva = rv?.adapter as MyNoteRvaInner
//            binding.dto = mItem

            //노트 팝업 메뉴 클릭시 - 수정, 삭제
            binding.myNoteFmVhMenuIbt.setOnClickListener {
                //팝업 메뉴 생성 후 각 메뉴에 대한 xml 파일 인플레이트 
                var notePopupMenu = PopupMenu(MyApp.application, it)
                myNoteFm.requireActivity().menuInflater.inflate(R.menu.note_popup, notePopupMenu.menu)
                //각 메뉴항목 클릭했을때의 동작 설정
                notePopupMenu.setOnMenuItemClickListener { menuItem ->
                    when(menuItem.itemId){
                        //수정 클릭시
                        R.id.note_popup_update -> {
                            Toast.makeText(myNoteFm.context, "수정클릭",Toast.LENGTH_SHORT).show()
                            bibleVm.noteUpdateO = mItem //수정용 임시 객체
                            Navigation.findNavController(it).navigate(R.id.action_global_myNoteFmUpdate)
                            return@setOnMenuItemClickListener true
                        }
                        //삭제 클릭시
                        R.id.note_popup_delete -> {
                            // 다이얼로그 생성 - 삭제 여부를 물음
                            val alertdialog = AlertDialog.Builder(myNoteFm.requireContext())
                            // 확인버튼 클릭시
                            alertdialog.setPositiveButton("확인") { dialog, which ->
                                // Toast toast = Toast.makeText(getActivity(), "확인 버튼 눌림", Toast.LENGTH_SHORT ).show;
                                //비동기 정보 가져옴
                                bibleVm.노트삭제(mItem.get("note_no").asInt, false).enqueue(object : Callback<JsonObject?> {
                                    override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                                        if (response.isSuccessful) {
                                            val res = response.body()
                                            if(res!!.get("result").asBoolean){
                                                Toast.makeText(myNoteFm.context, "노트를 삭제하였습니다",Toast.LENGTH_SHORT).show()
                                                bibleVm.노트목록가져오기(false).enqueue(object : Callback<JsonArray?> {
                                                    override fun onResponse(call: Call<JsonArray?>, response: Response<JsonArray?>) {
                                                        if (response.isSuccessful) {
                                                            val res = response.body()
                                                            Log.e("[MyNoteFm]", "노트목록가져오기 onResponse: $res")
                                                            bibleVm.noteL = res;
                                                            bibleVm.liveNoteL.value = bibleVm.noteL
                                                            notifyDataSetChanged()
                                                        }
                                                    }
                                                    override fun onFailure(call: Call<JsonArray?>, t: Throwable) {
                                                        Log.e("[MyNoteFm]", "노트목록가져오기 onFailure: " + t.message)
                                                    }
                                                })
                                            }
                                        }
                                    }
                                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                                        Log.e("[BibleVm]", "노트삭제 onFailure: " + t.message)
                                    }
                                })
                            }
                            // 취소버튼 클릭시
                            alertdialog.setNegativeButton("취소" ) { dialog, which ->
                            }
                            // 다이얼로그 빌더로 설정한 내용을 이용해 인스턴스 생성
                            val alert = alertdialog.create()
                            // 아이콘 설정
//                            alert.setIcon(R.drawable.ic_baseline_dashboard_24)
                            // 타이틀
                            alert.setTitle("삭제하시겠습니까?")
                            // 다이얼로그 띄우기
                            alert.show()

                            return@setOnMenuItemClickListener true
                        }
                        else -> {
                            return@setOnMenuItemClickListener false
                        }
                    }
                }
                //위에서 설정된 노트 팝업 메뉴를 화면(해당뷰)에 띄움
                notePopupMenu.show()
            }

        }


    }
}