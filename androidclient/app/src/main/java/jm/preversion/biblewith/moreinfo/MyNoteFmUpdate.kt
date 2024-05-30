package jm.preversion.biblewith.moreinfo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import jm.preversion.biblewith.MyApp
import jm.preversion.biblewith.R
import jm.preversion.biblewith.bible.BibleVm
import jm.preversion.biblewith.databinding.MyNoteFmUpdateBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


class MyNoteFmUpdate : Fragment() {
    lateinit var bibleVm: BibleVm
    var mbinding: MyNoteFmUpdateBinding? = null
    lateinit var rva: MyNoteFmUpdateRva
    lateinit var rv: RecyclerView
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재선언

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)
        mbinding = MyNoteFmUpdateBinding.inflate(inflater, container, false)

        rv = binding.myNoteFmUpdateVerseList
        rv.layoutManager = LinearLayoutManager(binding.root.context)
        //        recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        rv.adapter = MyNoteFmUpdateRva(bibleVm, this)
        rva = rv.adapter as MyNoteFmUpdateRva


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //모임 툴바 셋팅
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.more_fm).build()
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
//        binding.groupInToolbar.setupWithNavController(navController, appBarConfiguration)
        NavigationUI.setupWithNavController( binding.noteUpdateToolbar, navController, appBarConfiguration )

        //툴바에 소속된 노트 추가 버튼에 이벤트 리스너 설정
        binding.noteUpdateToolbarModifyBt.setOnClickListener {
            val gson = Gson()
            var sendJsonO = JsonObject()
            sendJsonO.addProperty("user_no", MyApp.userInfo.user_no)
            sendJsonO.addProperty("note_no", bibleVm.noteUpdateO.get("note_no").asInt )
            sendJsonO.addProperty("note_content", binding.myNoteContentInput.text.toString())
//            sendJsonO.add("note_verseL", JsonParser.parseString(gson.toJson(rva.newL)).asJsonArray)
            //추가 비동기 실행
            bibleVm.노트수정(sendJsonO, false).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful ){
                        var res : JsonObject? = response.body()
                        Log.e("[MyNoteFmUpdate]", "노트수정 onResponse: $res")
                        //서버로부터 받은 추가된 절이 1개 이상이면 정상추가로 판단하고 이전 페이지로 돌아감.
                        if(res!!.get("result").asBoolean ){
                            Toast.makeText(activity,"노트를 수정하였습니다.", Toast.LENGTH_SHORT).show()
                            Navigation.findNavController(view).navigateUp()
                        } else {
                            Toast.makeText(activity,"노트를 수정할 사항이 없습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Log.e("[MyNoteFmUpdate]", "노트수정 onFailure: ${t.message}")
                }
            })


        }


    }

    override fun onResume() {
        super.onResume()
        //현재 노트에 추가할 선택된 절이 속한 책,장 정보를 표시
//        binding.myNoteFmUpdateWhereTv.text = bibleVm.bookL[bibleVm.책장번호[0] - 1].book_name + " " + bibleVm.chapterL[bibleVm.책장번호[1] - 1].chapter + "장"
        binding.myNoteFmUpdateWhereTv.text = "${bibleVm.bookL[(bibleVm.noteUpdateO.get("note_verseL").asJsonArray.get(0).asJsonObject.get("book").asInt) - 1].book_name} ${bibleVm.noteUpdateO.get("note_verseL").asJsonArray.get(0).asJsonObject.get("chapter").asString}장"
        binding.myNoteFmUpdateDateTv.text = getTime("ui", bibleVm.noteUpdateO.get("note_date").asString)
        binding.myNoteContentInput.setText(bibleVm.noteUpdateO.get("note_content").asString)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }

    private fun getTime(ui표시orData: String, datetime: String): String? {
        if (ui표시orData == "ui") {
            val format = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm:ss")
            val out_format = DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm")
            val out_format2 = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm")
            val out_format3 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val ldt = LocalDateTime.parse(datetime, format)
            //            LocalDateTime.now().toInstant();
//            ldt.toEpochSecond(ZoneOffset.UTC);
            val res_st = ldt.format(out_format)
            val res_st2 = ldt.format(out_format2)
            val res_st3 = ldt.format(out_format3)
            val currentTime = LocalDateTime.now()
                .toEpochSecond(ZoneOffset.UTC) /*atZone(ZoneId.systemDefault()).toEpochSecond()*/
            //zoneoffset의 구분은 중요하다. systemdefault zone으로 정하면 서울 시간을 기준으로 계산되고 utc기준이랑은 시차가 생기게 되니 주의해야한다.
            val mNow = System.currentTimeMillis()
            val mDate = Date(mNow) //1644034298
            val mDate_muter = mDate.toInstant().epochSecond
            val second = currentTime - ldt.toEpochSecond(ZoneOffset.UTC)
            val minute = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC)) / 60L
            val hour = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC)) / 60 / 60
            val day = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC)) / 60 / 60 / 24
            val year = (currentTime - ldt.toEpochSecond(ZoneOffset.UTC)) / 60 / 60 / 24 / 365
//            Log.e("MyNoteFmUpdate", "res_st: $res_st")
//            Log.e("MyNoteFmUpdate", "res_st2: $res_st2")
//            Log.e("MyNoteFmUpdate", "res_st3: $res_st3")
//            Log.e("MyNoteFmUpdate", "currentTime: $currentTime")
//            Log.e("MyNoteFmUpdate", "ldt.toEpochSecond(ZoneOffset.UTC): " + ldt.toEpochSecond(ZoneOffset.UTC) )
//            Log.e("MyNoteFmUpdate", "현재시간 - 저장된 시간 (초): $second")
//            Log.e("MyNoteFmUpdate", "현재시간 - 저장된 시간 (분): $minute")
//            Log.e("MyNoteFmUpdate", "현재시간 - 저장된 시간 (시간): $hour")
//            Log.e("MyNoteFmUpdate", "현재시간 - 저장된 시간 (일): $day")
//            Log.e("MyNoteFmUpdate", "현재시간 - 저장된 시간 (년): $year")
            val res = ""
            if (minute < 1) {
                return second.toString() + "초전"
            } else if (hour < 1) {
                return minute.toString() + "분전"
            } else if (day < 1) {
                return hour.toString() + "시간전"
            } else if (year < 1) {
                return day.toString() + "일전"
            }
            return res_st

        } else if (ui표시orData == "data") {
            val mNow = System.currentTimeMillis()
            val mDate = Date(mNow)
            //            mDate.toInstant().getEpochSecond();
            val mtime = SimpleDateFormat()
            return SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(mDate)
        }
        return ""
    }


}
