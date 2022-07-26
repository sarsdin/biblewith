package com.example.androidclient.moreinfo

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.MyNoteFmListBinding
import com.google.gson.JsonArray
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyNoteFm : Fragment() {
    lateinit var bibleVm: BibleVm
    lateinit var rva: MyNoteRva
    lateinit var rv: RecyclerView
    var mbinding: MyNoteFmListBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)
        //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
        // -- 비동기라서 데이터가 미리 로딩이 안됨.. bibleVm에서 미리 로딩해놔야함.
        bibleVm.노트목록가져오기(true)/*.enqueue(object : Callback<JsonArray?> {
            override fun onResponse(call: Call<JsonArray?>, response: Response<JsonArray?>) {
                if (response.isSuccessful) {
                    val res = response.body()
                    Log.e("[MyNoteFm]", "노트목록가져오기 onResponse: $res")
                    bibleVm.noteL = res;
                    bibleVm.liveNoteL.value = bibleVm.noteL
                }
            }
            override fun onFailure(call: Call<JsonArray?>, t: Throwable) {
                Log.e("[MyNoteFm]", "노트목록가져오기 onFailure: " + t.message)
            }
        })*/

        mbinding = MyNoteFmListBinding.inflate(inflater, container, false)

        rv = binding.myNoteFmList
        rv.layoutManager = LinearLayoutManager(binding.root.context)
        //        recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
        rv.adapter = MyNoteRva(bibleVm, this)
        rva = rv.adapter as MyNoteRva

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
        NavigationUI.setupWithNavController( binding.noteToolbar, navController, appBarConfiguration )
    }

    override fun onResume() {
        super.onResume()
        rva.notifyDataSetChanged()
        //노트수정 시그널
        bibleVm.liveNoteL.observe(viewLifecycleOwner, Observer {
                rva.notifyDataSetChanged()
//                Log.e("오류태그", "it:${it}")
        })
    }


    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}