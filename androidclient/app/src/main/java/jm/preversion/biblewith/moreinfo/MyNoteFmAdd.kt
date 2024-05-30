package jm.preversion.biblewith.moreinfo
import android.widget.Toast

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import jm.preversion.biblewith.databinding.MyNoteFmAddBinding
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyNoteFmAdd : Fragment() {
    lateinit var bibleVm: BibleVm
    var mbinding: MyNoteFmAddBinding? = null
    lateinit var rva: MyNoteFmAddRva
    lateinit var rv: RecyclerView
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재선언

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)
        mbinding = MyNoteFmAddBinding.inflate(inflater, container, false)

        rv = binding.myNoteFmAddVerseList
        rv.layoutManager = LinearLayoutManager(binding.root.context)
        //        recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        rv.adapter = MyNoteFmAddRva(bibleVm, this)
        rva = rv.adapter as MyNoteFmAddRva


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //모임 툴바 셋팅
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.bible_fm).build()
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
//        binding.groupInToolbar.setupWithNavController(navController, appBarConfiguration)
        NavigationUI.setupWithNavController( binding.noteAddToolbar, navController, appBarConfiguration )

        //툴바에 소속된 노트 추가 버튼에 이벤트 리스너 설정
        binding.noteAddToolbarAddBt.setOnClickListener {
            //추가클릭시 정보 db로
            val gson = Gson()
            var sendJsonO = JsonObject()
            sendJsonO.addProperty("user_no", MyApp.userInfo.user_no)
            sendJsonO.addProperty("note_content", binding.myNoteContentInput.text.toString())
            sendJsonO.add("note_verseL",JsonParser.parseString(gson.toJson(rva.newL)).asJsonArray)
            //추가 비동기 실행
            bibleVm.노트추가(sendJsonO, false).enqueue(object : Callback<JsonObject> {
                override fun onResponse(call: Call<JsonObject>, response: Response<JsonObject>) {
                    if (response.isSuccessful ){
                        var res : JsonObject? = response.body()
                        Log.e("[MyNoteAddFm]", "노트추가 onResponse: $res")
                        //서버로부터 받은 추가된 절이 1개 이상이면 정상추가로 판단하고 이전 페이지로 돌아감.
                        if(res!!.get("result").asInt > 0){
                            Toast.makeText(activity,"노트를 추가하였습니다.",Toast.LENGTH_SHORT).show()
                            Navigation.findNavController(view).navigateUp()
                        }
//                        bibleVm.highL = res
//                        bibleVm.liveHighL.value = bibleVm.highL
                    }
                }
                override fun onFailure(call: Call<JsonObject>, t: Throwable) {
                    Log.e("[MyNoteAddFm]", "노트추가 onFailure: ${t.message}")
                }
            })
            
            
        }


    }

    override fun onResume() {
        super.onResume()
        //현재 노트에 추가할 선택된 절이 속한 책,장 정보를 표시
        binding.myNoteFmAddWhereTv.text = bibleVm.bookL[bibleVm.책장번호[0] - 1].book_name + " " + bibleVm.chapterL[bibleVm.책장번호[1] - 1].chapter + "장"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }


}










/*

companion object {
    */
/**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MyNoteFmAdd.
     *//*

    // TODO: Rename and change types and number of parameters
    @JvmStatic
    fun newInstance(param1: String, param2: String) =
        MyNoteFmAdd().apply {
            arguments = Bundle().apply {
                putString(ARG_PARAM1, param1)
                putString(ARG_PARAM2, param2)
            }
        }
}*/
