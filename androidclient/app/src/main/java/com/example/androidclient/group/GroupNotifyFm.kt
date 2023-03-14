package com.example.androidclient.group

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.databinding.GroupListFmBinding
import com.example.androidclient.databinding.GroupNotifyFmBinding
import com.example.androidclient.util.ImageHelper

class GroupNotifyFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var rva: GroupNotifyRva
    lateinit var rv: RecyclerView
    var mbinding: GroupNotifyFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        //true를 받으면 여기서(vm) 실행하고 결과완료된 call을 리턴. false면 완료안된 call을 리턴해서 호출한 fragment or rva에서 비동기 로직 진행.
        // -- 비동기라서 데이터가 미리 로딩이 안됨.. bibleVm에서 미리 로딩해놔야함.
        /*bibleVm.노트목록가져오기(true).enqueue(object : Callback<JsonArray?> {
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

        mbinding = GroupNotifyFmBinding.inflate(inflater, container, false)

        rv = binding.groupNotifyList
//        rv.layoutManager = GridLayoutManager(context, 2);
        rv.layoutManager = LinearLayoutManager(context);
        rv.adapter = GroupNotifyRva(groupVm, this)
        rva = rv.adapter as GroupNotifyRva

        return binding.root
    }


    override fun onResume() {
        super.onResume()

        //상단바 프로필 이미지 클릭시
//        binding.homeToolbarIv.setOnClickListener {
//            findNavController().navigate(R.id.action_global_myProfileFm)
//        }
//        //상단바 프로필 이미지 로딩
//        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.homeToolbarIv)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}