package com.example.androidclient.group

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.R
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.ChallengeFmBinding
import com.google.gson.JsonArray

class ChallengeFm : Fragment() {
    lateinit var groupVm: GroupVm
    lateinit var bibleVm: BibleVm
    lateinit var rva: ChallengeFmRva
    lateinit var rv: RecyclerView
    var mbinding: ChallengeFmBinding? = null
    val binding get() = mbinding!! //null체크를 매번 안하게끔 재 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        groupVm = ViewModelProvider(requireActivity()).get(GroupVm::class.java)
        bibleVm = ViewModelProvider(requireActivity()).get(BibleVm::class.java)
        mbinding = ChallengeFmBinding.inflate(inflater, container, false)

        rv = binding.chalList
        rv.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        rv.adapter = ChallengeFmRva(groupVm, this)
        rva = rv.adapter as ChallengeFmRva

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = Navigation.findNavController(view)
//        val appBarConfiguration = AppBarConfiguration(navController.graph)
        val appBarConfiguration = AppBarConfiguration.Builder(R.id.group_fm).build()
//        binding.groupMainCollapsingToolbar.setupWithNavController(binding.groupMainToolbar, navController, appBarConfiguration)
//        binding.groupInToolbar.setupWithNavController(navController, appBarConfiguration)
        setupWithNavController(binding.chalToolbar, navController, appBarConfiguration)
//        setupWithNavController(binding.chalBottomNavi, navController)


        //챌린지 만들기 선택상황 초기화 - 생명주기로 인해 fm이 재활성된다면 is_selected 의 값이 그대로 적용되므로 초기화해야함
        //초기화 위치는 만들기 페이지에서 뒤로가기 했을때 나오는 이 fm 에서 해야함.
        groupVm.createList.forEach {
            it.asJsonObject.addProperty("is_selected", false) //선택유무
        }
        groupVm.selectedCreateList = JsonArray() //기존에 선택되어 있을지도 모를 데이터들도 초기화해줌
        groupVm.liveSelectedCreateList.value = groupVm.selectedCreateList


//        findNavController().previousBackStackEntry?.destination?.id == R.id.challengeCreateFm)
//            findNavController().popBackStack(R.id.group_in_challenge_fm, false) << 뒤로가기로 이미 팝된걸 왜 팝하니..?


        //챌린지 만들기 버튼 클릭시 이동
        binding.chalToolbarWriteBt.setOnClickListener {
            findNavController().navigate(R.id.action_group_in_challenge_fm_to_challengeCreateFm)
        }




    }
}