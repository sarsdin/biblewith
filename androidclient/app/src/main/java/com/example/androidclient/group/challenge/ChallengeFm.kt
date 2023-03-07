package com.example.androidclient.group.challenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidclient.MyApp
import com.example.androidclient.R
import com.example.androidclient.bible.BibleVm
import com.example.androidclient.databinding.ChallengeFmBinding
import com.example.androidclient.group.GroupVm
import com.example.androidclient.util.ImageHelper
import com.google.gson.JsonArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        CoroutineScope(Dispatchers.Main).launch {
            groupVm.챌린지목록가져오기(true)
        }
        rv = binding.chalList
        rv.layoutManager = LinearLayoutManager(context)
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

        //바텀네비 리스너 설정
        binding.chalBottomNavi.setOnItemSelectedListener {
//            onNavDestinationSelected(it, navController)  << navigate()와 충돌함.
            if(it.itemId == R.id.groupInFm){
                Navigation.findNavController(view).navigate(R.id.action_global_groupInFm)
            } else if (it.itemId == R.id.groupInMemberFm){
                Navigation.findNavController(view).navigate(R.id.action_global_groupInMemberFm)
            } else if(it.itemId == R.id.groupInChatFm){
                Navigation.findNavController(view).navigate(R.id.action_global_groupInChatFm)
            }
            return@setOnItemSelectedListener false
        }


        //챌린지 만들기 선택상황 초기화 - 생명주기로 인해 fm이 재활성된다면 is_selected 의 값이 그대로 적용되므로 초기화해야함
        //초기화 위치는 만들기 페이지에서 뒤로가기 했을때 나오는 이 fm 에서 해야함.
        groupVm.createList.forEach {
            it.asJsonObject.addProperty("is_selected", false) //선택유무
        }
        groupVm.progressCountVerse = 20 //next page 에서 조정한 seeckbar 의 수치 또한 초기화
        groupVm.progressCountDay = 3
        groupVm.selectedCreateList = JsonArray() //기존에 선택되어 있을지도 모를 데이터들도 초기화해줌
        groupVm.liveSelectedCreateList.value = groupVm.selectedCreateList


//        findNavController().previousBackStackEntry?.destination?.id == R.id.challengeCreateFm)
//            findNavController().popBackStack(R.id.group_in_challenge_fm, false) << 뒤로가기로 이미 팝된걸 왜 팝하니..?


        //챌린지 만들기 버튼 클릭시 이동
        binding.chalToolbarWriteBt.setOnClickListener {
            findNavController().navigate(R.id.action_group_in_challenge_fm_to_challengeCreateFm)
        }


        // Ui 갱신
        groupVm.liveGroupInfo.observe(viewLifecycleOwner, Observer {
            //툴바에 현재 모임과 사용자명 표시
            binding.chalToolbarTv.text = "${groupVm.groupInfo.get("group_name").asString} - ${MyApp.userInfo.user_nick}"
        })
        groupVm.liveMemberL.observe(viewLifecycleOwner, Observer {
            //멤버수
            binding.chalSummaryMemberTv.text = groupVm.memberL.size().toString()
        })

        groupVm.liveChalL.observe(viewLifecycleOwner, Observer {
            //챌린지 목록 갱신
            rva.notifyDataSetChanged()
        })
    }

    override fun onResume() {
        super.onResume()
        //진행 챌린지 현황 개수
        binding.chalSummaryNameTvEa.text = "${groupVm.chalL.size()}개"

        //상단바 프로필 이미지 클릭시
        binding.chalToolbarIv.setOnClickListener {
            findNavController().navigate(com.example.androidclient.R.id.action_global_myProfileFm)
        }
        //상단바 프로필 이미지 로딩
        ImageHelper.getImageUsingGlide(requireActivity(), MyApp.userInfo.user_image, binding.chalToolbarIv)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        mbinding = null
    }
}